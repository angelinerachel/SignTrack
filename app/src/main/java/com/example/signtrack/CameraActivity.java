package com.example.signtrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.Xcore.CvType;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;

    private CameraBridgeViewBase openCvCameraView;
    private Interpreter tfliteInterpreter;
    private List<String> labelList;

    private Queue<float[]> temporalWindow = new ArrayDeque<>();
    private static final int TEMPORAL_WINDOW_SIZE = 10;
    private static final int FIXED_LENGTH = 392;

    private Mat frameMat;
    private Object CameraBridgeViewBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed.");
            Toast.makeText(this, "Failed to load OpenCV", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initialize OpenCV Camera View
        openCvCameraView = findViewById(R.id.cameraPreview);
        openCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);

        // Check permissions
        if (allPermissionsGranted()) {
            openCvCameraView.enableView();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }

        try {
            tfliteInterpreter = new Interpreter(loadModelFile());
            Log.i(TAG, "TensorFlow Lite model loaded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading TensorFlow Lite model", e);
            finish();
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                openCvCameraView.enableView();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frameMat = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        frameMat.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // Get current frame as a Mat object
        frameMat = inputFrame.rgba();

        // Perform processing here
        float[] featureVector = preprocessFrame(frameMat);
        temporalWindow.add(featureVector);

        if (temporalWindow.size() > TEMPORAL_WINDOW_SIZE) {
            temporalWindow.poll();
        }

        if (temporalWindow.size() == TEMPORAL_WINDOW_SIZE) {
            runInference();
        }

        return frameMat; // Return processed frame for display
    }

    private float[] preprocessFrame(Mat frame) {
        // Placeholder preprocessing: Flatten the frame and normalize
        float[] featureVector = new float[FIXED_LENGTH];
        // Perform your preprocessing here
        return featureVector;
    }

    private void runInference() {
        float[][][][] inputData = new float[1][TEMPORAL_WINDOW_SIZE][1][FIXED_LENGTH];
        float[][] output = new float[1][labelList.size()];
        tfliteInterpreter.run(inputData, output);

        int maxIndex = getMaxIndex(output[0]);
        String prediction = labelList.get(maxIndex);
        runOnUiThread(() -> Toast.makeText(this, "Prediction: " + prediction, Toast.LENGTH_SHORT).show());
    }

    private int getMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        try (FileInputStream inputStream = new FileInputStream(getAssets().openFd("temporal_stgcn_model.tflite").getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = getAssets().openFd("temporal_stgcn_model.tflite").getStartOffset();
            long declaredLength = getAssets().openFd("temporal_stgcn_model.tflite").getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }
}
