package com.example.finalprojectmobilecomputing;

import static androidx.core.content.ContextCompat.startActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class QRScannerActivity extends AppCompatActivity {

    private PreviewView previewView;
    private BarcodeScanner scanner;
    private boolean isScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qrscanner);

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);
        scanner = BarcodeScanning.getClient();

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder().build();
        analysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            if (image.getImage() == null) {
                image.close();
                return;
            }

            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );

            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            if (!isScanned) {
                                String qrData = barcode.getRawValue();
                                validateQR(qrData);
                                isScanned = true;
                                break;
                            }
                        }
                        image.close();
                    })
                    .addOnFailureListener(e -> {
                        image.close();
                        Toast.makeText(QRScannerActivity.this, 
                            "Error processing image: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
        });

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, selector, preview, analysis);
    }

    private void validateQR(String code) {
        // First check if user is logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first!", Toast.LENGTH_LONG).show();
            isScanned = false;
            return;
        }

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Show loading message
        Toast.makeText(this, "Checking QR code...", Toast.LENGTH_SHORT).show();

        // Clean the QR code (remove any whitespace)
        String cleanCode = code.trim();

        // Log the QR code being checked (for debugging)
        android.util.Log.d("QRScanner", "Checking QR code: " + cleanCode);

        db.collection("qr_codes")
          .document(cleanCode)
          .get()
          .addOnSuccessListener(document -> {
            if (document.exists()) {
                // Log successful document retrieval
                android.util.Log.d("QRScanner", "Document exists: " + document.getData());

                String bikeId = document.getString("bike_id");
                Boolean isActive = document.getBoolean("isActive");
                String status = document.getString("status");

                String currentUserId = auth.getCurrentUser().getUid();

                // Log the retrieved data
                android.util.Log.d("QRScanner", String.format(
                    "Retrieved data - bikeId: %s, isActive: %s, status: %s, userId: %s",
                    bikeId, isActive, status, currentUserId
                ));

                // Save to AppData singleton
                AppData.getInstance().setQrCode(cleanCode);
                AppData.getInstance().setUserId(currentUserId);
                AppData.getInstance().setBikeId(bikeId);

                Intent i = new Intent(QRScannerActivity.this, PaymentActivity.class);
                i.putExtra("qr_code", cleanCode);
                i.putExtra("user_id", currentUserId);
                i.putExtra("isActive", isActive != null ? isActive : false);
                i.putExtra("status", status);
                i.putExtra("bike_id", bikeId);

                startActivity(i);
                finish();
            } else {
                android.util.Log.e("QRScanner", "Document does not exist for code: " + cleanCode);
                Toast.makeText(this, "Invalid QR Code! Please try a different one.", Toast.LENGTH_LONG).show();
                isScanned = false;
            }
        })
        .addOnFailureListener(e -> {
            // Log the detailed error
            android.util.Log.e("QRScanner", "Error checking QR code: " + e.getMessage(), e);
            Toast.makeText(this, 
                "Connection error! Please check your internet connection and try again.", 
                Toast.LENGTH_LONG).show();
            isScanned = false;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}