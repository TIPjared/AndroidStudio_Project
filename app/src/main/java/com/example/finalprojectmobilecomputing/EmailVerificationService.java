package com.example.finalprojectmobilecomputing;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailVerificationService {
    private static final String TAG = "EmailVerificationService";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public EmailVerificationService() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Interface for verification callbacks
    public interface VerificationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // Send email verification
    public void sendEmailVerification(Activity activity, VerificationCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No user logged in");
            return;
        }

        if (user.isEmailVerified()) {
            callback.onSuccess();
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email verification sent");
                        Toast.makeText(activity, "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show();
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Failed to send email verification", task.getException());
                        callback.onFailure("Failed to send verification email: " + task.getException().getMessage());
                    }
                });
    }

    // Check if email is verified
    public void checkEmailVerification(VerificationCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No user logged in");
            return;
        }

        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    // Update Firestore with email verification status
                    updateEmailVerificationStatus(user.getUid(), true, callback);
                } else {
                    callback.onFailure("Email not verified");
                }
            } else {
                callback.onFailure("Failed to reload user: " + task.getException().getMessage());
            }
        });
    }

    // Update email verification status in Firestore
    private void updateEmailVerificationStatus(String userId, boolean isVerified, VerificationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("emailVerified", isVerified);
        if (isVerified) {
            updates.put("verificationTimestamp", System.currentTimeMillis());
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Email verification status updated");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update email verification status", e);
                    callback.onFailure("Failed to update verification status: " + e.getMessage());
                });
    }

    // Check user account status
    public void checkAccountStatus(String userId, AccountStatusCallback callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("accountStatus");
                        if (status == null) {
                            status = "ACTIVE"; // Default status
                        }
                        callback.onStatusChecked(status);
                    } else {
                        callback.onStatusChecked("UNKNOWN");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check account status", e);
                    callback.onStatusChecked("ERROR");
                });
    }

    // Interface for account status callback
    public interface AccountStatusCallback {
        void onStatusChecked(String status);
    }
}
