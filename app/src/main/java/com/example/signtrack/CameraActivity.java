package com.example.signtrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;
import androidx.core.content.PermissionChecker;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView viewPreview; // PreviewView
    private ImageButton backButton, flashToggleIB, flipCameraIB;
    private TextView translationText;
    private Camera camera;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialize UI components
        viewPreview = findViewById(R.id.viewPreview); // Ensure the ID matches the layout XML
        backButton = findViewById(R.id.backButton);
        flashToggleIB = findViewById(R.id.flashToggleIB);
        flipCameraIB = findViewById(R.id.flipCameraIB);
        translationText = findViewById(R.id.translationText);

        // Set click listener for back button
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.this, LandingPageActivity.class); // Go back to LandingPageActivity
            startActivity(intent);
            finish();
        });

        // Check and request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PermissionChecker.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

        // Flash toggle functionality
        flashToggleIB.setOnClickListener(v -> toggleFlash());

        // Camera flip functionality
        flipCameraIB.setOnClickListener(v -> flipCamera());
    }

    private void startCamera() {
        // Create an instance of ProcessCameraProvider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Get camera provider instance
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Create preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewPreview.getSurfaceProvider());

                // Select camera (back camera by default)
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Set up image analysis and preview use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

                // Bind use cases to camera provider
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        // Toggle flash on/off
        if (camera != null) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            flashToggleIB.setImageResource(isFlashOn ? R.drawable.flash_on : R.drawable.flash_off);
        }
    }

    private void flipCamera() {
        // Flip the camera (toggle between front and back)
        if (camera != null) {
            CameraSelector cameraSelector = (camera.getCameraInfo().getLensFacing()
                    == CameraSelector.LENS_FACING_BACK)
                    ? new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                    : new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            startCamera(cameraSelector); // Reinitialize camera with new selector
        }
    }

    private void startCamera(CameraSelector cameraSelector) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll(); // Unbind all use cases before rebinding
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewPreview.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Failed to flip camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
    }
}
