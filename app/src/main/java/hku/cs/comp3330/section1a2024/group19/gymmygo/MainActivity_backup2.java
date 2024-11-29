package hku.cs.comp3330.section1a2024.group19.gymmygo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity_backup2 extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUESTS = 1;

    private PreviewView previewView;

    private PoseDetector poseDetector;

    private ExecutorService cameraExecutor;

    private boolean isProcessing = false;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @ExperimentalGetImage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure this layout matches the one provided above

        // Initialize views
        previewView = findViewById(R.id.previewView);

        // Initialize Pose Detector with STREAM_MODE for real-time detection
        PoseDetectorOptions options =
                new PoseDetectorOptions.Builder()
                        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);


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

        // ImageAnalysis Use Case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Set analyzer for ImageAnalysis
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            processImageProxy(imageProxy);
        });

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        try {

            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            Log.d(TAG, "Camera use cases bound to lifecycle");

            // Connect the preview use case to the PreviewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // After the PreviewView has been laid out, log its dimensions
            previewView.post(() -> {
                int previewWidth = previewView.getWidth();
                int previewHeight = previewView.getHeight();
                Log.d(TAG, "PreviewView dimensions: " + previewWidth + "x" + previewHeight);
                // No need to set camera info in Display view since we'll handle scaling dynamically
            });
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            Toast.makeText(this, "Failed to bind camera use cases: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String[] getRequiredPermissions() {
        return new String[]{android.Manifest.permission.CAMERA};
    }

    /**
     * Checks if all required permissions are granted.
     *
     * @return True if all permissions are granted, false otherwise.
     */
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a specific permission is granted.
     *
     * @param context    The context.
     * @param permission The permission to check.
     * @return True if granted, false otherwise.
     */
    private static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests runtime permissions that are not yet granted.
     */
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
        if (requestCode == PERMISSION_REQUESTS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted");
                // Re-bind camera use cases
                cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        bindCameraUseCases(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, "Error starting camera", e);
                        Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }, ContextCompat.getMainExecutor(this));
            } else {
                Log.w(TAG, "Permissions not granted");
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show();
                // Disable camera-related functionality
                previewView.setVisibility(View.GONE);

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            Log.d(TAG, "Camera executor shut down");
        }
        if (poseDetector != null) {
            poseDetector.close();
            Log.d(TAG, "Pose detector closed");
        }
    }

    @ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        isProcessing = true;

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            // Convert YUV to Bitmap
            Bitmap bitmap = yuvToBitmap(mediaImage);
            if (bitmap != null) {
                // Rotate the bitmap to match display orientation
                Bitmap rotatedBitmap = rotateBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());

                // Perform pose detection
                InputImage inputImage = InputImage.fromBitmap(rotatedBitmap, 0);
                poseDetector.process(inputImage)
                        .addOnSuccessListener(pose -> {
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Pose detection failed", e);
                        })
                        .addOnCompleteListener(task -> {
                            imageProxy.close();
                            isProcessing = false;
                        });
            } else {
                imageProxy.close();
                isProcessing = false;
            }
        } else {
            imageProxy.close();
            isProcessing = false;
        }
    }

    /**
     * Converts YUV_420_888 Image to NV21 byte array.
     *
     * @param image The YUV Image.
     * @return NV21 byte array.
     */
    private byte[] YUV_420_888toNV21(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int yRowStride = image.getPlanes()[0].getRowStride();
        int yPixelStride = image.getPlanes()[0].getPixelStride();
        int uRowStride = image.getPlanes()[1].getRowStride();
        int uPixelStride = image.getPlanes()[1].getPixelStride();
        int vRowStride = image.getPlanes()[2].getRowStride();
        int vPixelStride = image.getPlanes()[2].getPixelStride();

        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the size of Y, U, and V data
        int ySize = yRowStride * height;
        int uvHeight = height / 2;

        byte[] nv21 = new byte[ySize + width * height / 2];

        // Extract Y data
        yBuffer.get(nv21, 0, ySize);

        // Extract VU data
        int nv21Index = ySize;
        for (int row = 0; row < uvHeight; row++) {
            for (int col = 0; col < width / 2; col++) {
                int uIndex = row * uRowStride + col * uPixelStride;
                int vIndex = row * vRowStride + col * vPixelStride;

                // NV21 format requires V before U
                nv21[nv21Index++] = vBuffer.get(vIndex);
                nv21[nv21Index++] = uBuffer.get(uIndex);
            }
        }

        return nv21;
    }

    /**
     * Converts YUV Image to Bitmap.
     *
     * @param image The YUV Image.
     * @return The converted Bitmap.
     */
    private Bitmap yuvToBitmap(Image image) {
        byte[] nv21 = YUV_420_888toNV21(image);
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    /**
     * Rotates the bitmap to match the display orientation.
     *
     * @param bitmap          The original bitmap.
     * @param rotationDegrees The rotation degrees.
     * @return The rotated bitmap.
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

}
