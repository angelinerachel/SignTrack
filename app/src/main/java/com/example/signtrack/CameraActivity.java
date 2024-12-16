package com.example.signtrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private PreviewView cameraPreview;
    private TextView translationText;
    private ImageButton backButton;

    private Interpreter tfliteInterpreter;
    private List<String> labelList;

    private Queue<float[]> temporalWindow = new ArrayDeque<>();
    private static final int TEMPORAL_WINDOW_SIZE = 10;
    private static final int FIXED_LENGTH = 392;
    private static final int NUM_NODES = 4;
    private static final int FEATURES_PER_NODE = FIXED_LENGTH / NUM_NODES;
    private static final int INPUT_RESOLUTION = 128;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);

        cameraPreview = findViewById(R.id.cameraPreview);
        translationText = findViewById(R.id.translationText);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        try {
            tfliteInterpreter = new Interpreter(loadModelFile());
            labelList = loadLabels();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TensorFlow Lite model", e);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera(); // Start the camera if permission is already granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(); // Start camera if permission is granted
            } else {
                Log.e(TAG, "Camera permission denied. The app requires camera permission to function.");
                finish();
            }
        }
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

                CameraSelector cameraSelector;

                // Check for back-facing camera; fallback to front if not available
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    Log.w(TAG, "No back-facing camera found. Using front-facing camera as fallback.");
                } else {
                    Log.e(TAG, "No available cameras on the device.");
                    return;
                }

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    Bitmap bitmap = convertImageProxyToBitmap(image);
                    processFrame(bitmap);
                    image.close();
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed. Check for available camera hardware.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        try (FileInputStream inputStream = new FileInputStream(getAssets().openFd("temporal_stgcn_model.tflite").getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, inputStream.available());
        }
    }

    private List<String> loadLabels() throws Exception {
        List<String> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("label_encoder.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        }
        return labels;
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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
        return new float[FIXED_LENGTH];
    }

    private float[][][][] prepareInputData() {
        float[][][][] inputData = new float[1][TEMPORAL_WINDOW_SIZE][NUM_NODES][FEATURES_PER_NODE];
        int i = 0;
        for (float[] feature : temporalWindow) {
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
