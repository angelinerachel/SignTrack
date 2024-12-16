// CameraActivity.java
package com.example.signtrack;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    private PreviewView cameraPreview;
    private TextView translationText;
    private ImageButton backButton;

    private Interpreter tfliteInterpreter;
    private List<String> labelList;

    private Queue<float[]> temporalWindow = new ArrayDeque<>();
    private static final int TEMPORAL_WINDOW_SIZE = 10;
    private static final int FIXED_LENGTH = 392; // Replace with actual feature length
    private static final int NUM_NODES = 4;
    private static final int FEATURES_PER_NODE = FIXED_LENGTH / NUM_NODES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);

        // Link layout components
        cameraPreview = findViewById(R.id.cameraPreview);
        translationText = findViewById(R.id.translationText);
        backButton = findViewById(R.id.backButton);

        // Set up back button click event
        backButton.setOnClickListener(v -> finish());

        // Initialize TensorFlow Lite interpreter and labels
        try {
            tfliteInterpreter = new Interpreter(loadModelFile());
            labelList = loadLabels();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TensorFlow Lite model", e);
        }

        // Start the camera feed
        startCamera();
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        InputStream is = getAssets().open("temporal_stgcn_model.tflite");
        FileChannel fileChannel = is.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, is.available());
    }

    private List<String> loadLabels() throws Exception {
        List<String> labels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("label_encoder.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    Bitmap bitmap = convertImageProxyToBitmap(image);
                    processFrame(bitmap);
                    image.close();
                });

                cameraProvider.bindToLifecycle(this, cameraProvider.getDefaultCameraSelector(), preview, imageAnalysis);
            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processFrame(Bitmap bitmap) {
        float[] featureVector = preprocessFrame(bitmap);
        temporalWindow.add(featureVector);

        if (temporalWindow.size() > TEMPORAL_WINDOW_SIZE) {
            temporalWindow.poll();
        }

        if (temporalWindow.size() == TEMPORAL_WINDOW_SIZE) {
            float[][][][] inputData = prepareInputData();
            float[][] output = new float[1][labelList.size()];
            tfliteInterpreter.run(inputData, output);

            int maxIdx = getMaxIndex(output[0]);
            String predictedLabel = labelList.get(maxIdx);
            runOnUiThread(() -> translationText.setText(predictedLabel));
        }
    }

    private float[] preprocessFrame(Bitmap bitmap) {
        // Resize, grayscale, and extract features
        return new float[FIXED_LENGTH]; // Replace with real preprocessing logic
    }

    private float[][][][] prepareInputData() {
        float[][][][] inputData = new float[1][TEMPORAL_WINDOW_SIZE][NUM_NODES][FEATURES_PER_NODE];
        int i = 0;
        for (float[] feature : temporalWindow) {
            // Populate inputData with temporalWindow data
            i++;
        }
        return inputData;
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
}
