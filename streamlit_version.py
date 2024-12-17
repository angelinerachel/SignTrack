import cv2
import numpy as np
import tensorflow as tf
import pickle
import streamlit as st
from collections import deque
from streamlit_webrtc import webrtc_streamer, VideoProcessorBase, RTCConfiguration
import mediapipe as mp
import av
import asyncio

# Try to handle asyncio loop issues on Windows
try:
    loop = asyncio.get_event_loop()
except RuntimeError:
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

# RTC Configuration for WebRTC
RTC_CONFIGURATION = {
    "iceServers": [{"urls": ["stun:stun.l.google.com:19302"]}]
}

# Load the TFLite model
tflite_model_path = "temporal_stgcn_model.tflite"
interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Load the Label Encoder
with open("label_encoder.pkl", "rb") as f:
    label_encoder = pickle.load(f)

# Mediapipe Holistic setup
mp_holistic = mp.solutions.holistic
holistic = mp_holistic.Holistic(min_detection_confidence=0.7, min_tracking_confidence=0.7)

# Constants
TEMPORAL_WINDOW_SIZE = 10
FIXED_LENGTH = 392
NUM_NODES = 4
FEATURES_PER_NODE = FIXED_LENGTH // NUM_NODES
HOOF_BINS = 8

# Temporal buffer
temporal_window = deque(maxlen=TEMPORAL_WINDOW_SIZE)

# Helper Functions
def draw_holistic_landmarks(frame, results):
    mp_drawing = mp.solutions.drawing_utils
    mp_drawing.draw_landmarks(
        frame, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS
    )
    mp_drawing.draw_landmarks(
        frame, results.left_hand_landmarks, mp_holistic.HAND_CONNECTIONS
    )
    mp_drawing.draw_landmarks(
        frame, results.right_hand_landmarks, mp_holistic.HAND_CONNECTIONS
    )

def preprocess_frame(frame):
    resized_frame = cv2.resize(frame, (128, 128))
    gray_frame = cv2.cvtColor(resized_frame, cv2.COLOR_BGR2GRAY)
    return gray_frame

# Streamlit Video Processor Class
class SignProcessor(VideoProcessorBase):
    def __init__(self):
        self.temporal_window = deque(maxlen=TEMPORAL_WINDOW_SIZE)
        self.prev_gray = None

    def recv(self, frame):
        # Convert frame to numpy array (OpenCV format)
        img = frame.to_ndarray(format="bgr24")

        # Process frame for holistic landmarks
        results = holistic.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        draw_holistic_landmarks(img, results)

        # Preprocess frame
        gray_frame = preprocess_frame(img)

        # Optical flow or placeholder features
        flow_hist = np.zeros(HOOF_BINS)  # Optical flow placeholder
        if self.prev_gray is not None:
            prev_pts, curr_pts = cv2.calcOpticalFlowPyrLK(self.prev_gray, gray_frame, None, None)
        self.prev_gray = gray_frame

        # Extract features and combine into a fixed-length vector
        keypoints = []
        for lm in results.pose_landmarks.landmark if results.pose_landmarks else []:
            keypoints.extend([lm.x, lm.y, lm.z])
        feature_vector = np.hstack([flow_hist, keypoints])
        feature_vector = np.pad(feature_vector, (0, FIXED_LENGTH - len(feature_vector)))[:FIXED_LENGTH]

        # Append to temporal window
        self.temporal_window.append(feature_vector)

        # Make predictions when window is full
        if len(self.temporal_window) == TEMPORAL_WINDOW_SIZE:
            input_data = np.array([self.temporal_window], dtype=np.float32).reshape(
                1, TEMPORAL_WINDOW_SIZE, NUM_NODES, FEATURES_PER_NODE
            )
            interpreter.set_tensor(input_details[0]['index'], input_data)
            interpreter.invoke()
            predictions = interpreter.get_tensor(output_details[0]['index'])[0]
            predicted_label = label_encoder.inverse_transform([np.argmax(predictions)])[0]

            # Overlay the prediction on the frame
            cv2.putText(img, f"Prediction: {predicted_label}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

        # Return the frame to Streamlit
        return av.VideoFrame.from_ndarray(img, format="bgr24")

# Streamlit UI
st.title("Sign Language Recognition in Real-Time")
st.markdown("This app uses TensorFlow Lite and MediaPipe for sign language detection.")

webrtc_streamer(
    key="sign-detection",
    video_processor_factory=SignProcessor,
    rtc_configuration=RTC_CONFIGURATION
)

st.markdown("---")
st.markdown("Ensure the camera is enabled and permissions are granted.")

