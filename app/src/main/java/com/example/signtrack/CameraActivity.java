package com.example.signtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.signtrack.databinding.ActivityCameraBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String TAG = "CameraActivity";

    private ActivityCameraBinding binding;

    // Daftar izin yang diperlukan (CAMERA, RECORD_AUDIO)
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    // Executor untuk thread latar belakang (ImageAnalysis berjalan di thread ini)
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Meminta izin untuk kamera dan mikrofon
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Menambahkan listener untuk tombol back
        binding.backButton.setOnClickListener(v -> {
            // Menggunakan Intent untuk kembali ke LandingPageActivity
            Intent intent = new Intent(CameraActivity.this, LandingPageActivity.class);
            startActivity(intent);
            finish(); // Menutup CameraActivity setelah pindah ke LandingPageActivity
        });

        // Inisialisasi Executor untuk CameraX
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    // Memeriksa apakah izin yang diperlukan sudah diberikan
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    // Menangani hasil permintaan izin dari pengguna
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Memulai kamera dengan CameraX
    private void startCamera() {
        // Mendapatkan instance CameraProvider
        ProcessCameraProvider cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Mendapatkan CameraProvider yang sudah siap
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview - menampilkan tampilan kamera di SurfaceView
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewPreview.getSurfaceProvider());

                // CameraSelector untuk memilih kamera belakang (atau depan)
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Gunakan kamera belakang
                        .build();

                // ImageAnalysis untuk menganalisis gambar dari kamera
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        // Di sini Anda dapat mengolah gambar untuk mengenali gerakan tangan
                        // Anda bisa menambahkan model penerjemahan bahasa isyarat di sini
                        Log.d(TAG, "Image captured: " + image.getWidth() + "x" + image.getHeight());
                        image.close();
                    }
                });

                // Kamera yang akan digunakan
                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Menutup executor setelah aktivitas dihancurkan
        cameraExecutor.shutdown();
    }
}
