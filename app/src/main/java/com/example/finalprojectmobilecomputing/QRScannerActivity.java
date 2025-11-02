package com.example.finalprojectmobilecomputing;

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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.HashMap;
import java.util.Map;
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

        // Request camera permission if not granted
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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first!", Toast.LENGTH_LONG).show();
            isScanned = false;
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Toast.makeText(this, "Checking QR code...", Toast.LENGTH_SHORT).show();

        String cleanCode = code.trim();
        android.util.Log.d("QRScanner", "Checking QR code: " + cleanCode);

        db.collection("qr_codes")
                .document(cleanCode)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String bikeId = document.getString("bike_id");
                        Boolean isActive = document.getBoolean("isActive");
                        String status = document.getString("status");
                        String rentedBy = document.getString("rentedBy");
                        Timestamp lockedAt = document.getTimestamp("lockedAt");

                        String currentUserId = auth.getCurrentUser().getUid();

                        // 🔁 Auto-release if rented but no payment within 5 minutes
                        Timestamp reservedAt = document.getTimestamp("reserved_at");
                        if ("rented".equals(status) && rentedBy != null) {
                            if (reservedAt != null) {
                                long minutesSinceReserved = (System.currentTimeMillis() - reservedAt.toDate().getTime()) / 60000;
                                if (minutesSinceReserved > 5) {
                                    Map<String, Object> reset = new HashMap<>();
                                    reset.put("status", "locked");
                                    reset.put("rentedBy", null);
                                    reset.put("reserved_at", null);
                                    db.collection("qr_codes").document(cleanCode).update(reset);
                                    status = "locked";
                                    rentedBy = null;
                                    android.util.Log.d("QRScanner", "Bike auto-reset after 5 minutes without payment");
                                }
                            }
                        }

                        // 🔒 Block if rented by someone else
                        if ("locked".equals(status) && rentedBy != null && !rentedBy.equals(currentUserId)) {
                            Toast.makeText(this, "Bike is already rented by another user!", Toast.LENGTH_LONG).show();
                            isScanned = false;
                            return;
                        }

                        // ✅ Lock for current user
                        if ("available".equals(status) || rentedBy == null) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "locked");
                            updates.put("isActive", true);
                            updates.put("rentedBy", currentUserId);
                            updates.put("lockedAt", FieldValue.serverTimestamp());
                            updates.put("reserved_at", FieldValue.serverTimestamp());

                            db.collection("qr_codes")
                                    .document(cleanCode)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        AppData.getInstance().setQrCode(cleanCode);
                                        AppData.getInstance().setUserId(currentUserId);
                                        AppData.getInstance().setBikeId(bikeId);

                                        Intent i = new Intent(QRScannerActivity.this, PaymentActivity.class);
                                        i.putExtra("qr_code", cleanCode);
                                        i.putExtra("user_id", currentUserId);
                                        i.putExtra("isActive", true);
                                        i.putExtra("status", "locked");
                                        i.putExtra("bike_id", bikeId);

                                        startActivity(i);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to lock bike: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        isScanned = false;
                                    });
                        } else {
                            Toast.makeText(this, "Invalid or unavailable bike!", Toast.LENGTH_LONG).show();
                            isScanned = false;
                        }
                    } else {
                        Toast.makeText(this, "Invalid QR Code! Please try again.", Toast.LENGTH_LONG).show();
                        isScanned = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Connection error! Please check your internet.", Toast.LENGTH_LONG).show();
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
