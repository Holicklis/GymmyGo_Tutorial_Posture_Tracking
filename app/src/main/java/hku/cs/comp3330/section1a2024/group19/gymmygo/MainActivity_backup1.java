package hku.cs.comp3330.section1a2024.group19.gymmygo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;

import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity_backup1 extends AppCompatActivity {

    private static final String TAG = "MainActivity_backup1";
    private static final int PERMISSION_REQUESTS = 1;

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @ExperimentalGetImage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        previewView = findViewById(R.id.previewView);

        // Initialize CameraX executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize CameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));

        // Check and request permissions
        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }

    @ExperimentalGetImage
    void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Select the back camera as default
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview Use Case
        Preview preview = new Preview.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        try {
            // Bind use cases to lifecycle
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            Log.d(TAG, "Camera use cases bound to lifecycle");

            // Connect the preview use case to the PreviewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            Toast.makeText(this, "Failed to bind camera use cases: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String[] getRequiredPermissions() {
        return new String[]{android.Manifest.permission.CAMERA};
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUESTS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted");
                bindCameraAfterPermission();
            } else {
                Log.w(TAG, "Permissions not granted");
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show();
                previewView.setVisibility(View.GONE);  // Hide the preview if permissions are denied
            }
        }
    }

    private void bindCameraAfterPermission() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}

