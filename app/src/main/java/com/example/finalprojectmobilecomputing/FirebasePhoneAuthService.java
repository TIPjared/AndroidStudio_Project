package com.example.finalprojectmobilecomputing;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebasePhoneAuthService {
    private static final String TAG = "FirebasePhoneAuth";
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Activity activity;
    private String storedVerificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    
    // Callback interfaces
    public interface PhoneAuthCallback {
        void onVerificationCompleted(PhoneAuthCredential credential);
        void onVerificationFailed(String error);
        void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token);
    }
    
    public interface SignInCallback {
        void onSignInSuccess(String userId);
        void onSignInFailed(String error);
    }
    
    public FirebasePhoneAuthService(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        
        // Configure Firebase Auth settings for testing (remove in production)
        FirebaseAuthSettings settings = mAuth.getFirebaseAuthSettings();
        // Uncomment the line below for testing with fictional phone numbers
        // settings.setAppVerificationDisabledForTesting();
    }
    
    /**
     * Send verification code to phone number
     */
    public void sendVerificationCode(String phoneNumber, PhoneAuthCallback callback) {
        Log.d(TAG, "Sending verification code to: " + phoneNumber);
        
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        Log.d(TAG, "onVerificationCompleted");
                        // This callback will be invoked in two situations:
                        // 1 - Instant verification. In some cases the phone number can be instantly
                        //     verified without needing to send or enter a verification code.
                        // 2 - Auto-retrieval. On some devices Google Play services can automatically
                        //     detect the incoming verification SMS and perform verification without
                        //     user action.
                        callback.onVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.w(TAG, "onVerificationFailed", e);
                        String errorMessage = "Verification failed";
                        
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid phone number format";
                        } else if (e.getMessage() != null && e.getMessage().contains("quota")) {
                            errorMessage = "SMS quota exceeded. Please try again later";
                        } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                            errorMessage = "reCAPTCHA verification failed";
                        }
                        
                        callback.onVerificationFailed(errorMessage);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "onCodeSent: " + verificationId);
                        // The SMS verification code has been sent to the provided phone number
                        storedVerificationId = verificationId;
                        resendToken = token;
                        callback.onCodeSent(verificationId, token);
                    }
                    
                    public void onCodeAutoRetrievalTimeout(String verificationId) {
                        Log.d(TAG, "onCodeAutoRetrievalTimeout: " + verificationId);
                        storedVerificationId = verificationId;
                        // Handle timeout - user needs to enter code manually
                    }
                })
                .build();
        
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    
    /**
     * Verify the code entered by user
     */
    public void verifyCode(String code, SignInCallback callback) {
        if (storedVerificationId == null) {
            callback.onSignInFailed("No verification ID found. Please request a new code.");
            return;
        }
        
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(storedVerificationId, code);
        signInWithPhoneAuthCredential(credential, callback);
    }
    
    /**
     * Resend verification code
     */
    public void resendVerificationCode(String phoneNumber, PhoneAuthCallback callback) {
        if (resendToken == null) {
            // If no resend token, send a new verification code
            sendVerificationCode(phoneNumber, callback);
            return;
        }
        
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        callback.onVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.w(TAG, "onVerificationFailed on resend", e);
                        String errorMessage = "Verification failed";
                        
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid phone number format";
                        } else if (e.getMessage() != null && e.getMessage().contains("quota")) {
                            errorMessage = "SMS quota exceeded. Please try again later";
                        }
                        
                        callback.onVerificationFailed(errorMessage);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "onCodeSent on resend: " + verificationId);
                        storedVerificationId = verificationId;
                        resendToken = token;
                        callback.onCodeSent(verificationId, token);
                    }
                    
                    public void onCodeAutoRetrievalTimeout(String verificationId) {
                        storedVerificationId = verificationId;
                        // Handle timeout - user needs to enter code manually
                    }
                })
                .setForceResendingToken(resendToken)
                .build();
        
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    
    /**
     * Sign in with phone auth credential
     */
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, SignInCallback callback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        
                        String userId = mAuth.getCurrentUser().getUid();
                        String phoneNumber = mAuth.getCurrentUser().getPhoneNumber();
                        
                        // Update user document with verification status
                        updateUserVerificationStatus(userId, phoneNumber, callback);
                        
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        String errorMessage = "Sign in failed";
                        
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid verification code";
                        }
                        
                        callback.onSignInFailed(errorMessage);
                    }
                });
    }
    
    /**
     * Update user verification status in Firestore
     */
    private void updateUserVerificationStatus(String userId, String phoneNumber, SignInCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("phoneVerified", true);
        update.put("otpVerified", true);
        update.put("accountStatus", "verified");
        update.put("verificationTimestamp", System.currentTimeMillis());
        update.put("lastLoginTimestamp", System.currentTimeMillis());
        update.put("phone", phoneNumber);
        
        // Update device ID
        String deviceId = android.provider.Settings.Secure.getString(
                activity.getContentResolver(), 
                android.provider.Settings.Secure.ANDROID_ID
        );
        update.put("deviceId", deviceId);
        
        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User verification status updated");
                    callback.onSignInSuccess(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user verification status", e);
                    // Still consider sign-in successful since Firebase auth worked
                    callback.onSignInSuccess(userId);
                });
    }
    
    /**
     * Check if user is already verified on this device
     */
    public void checkExistingVerification(String userId, SignInCallback callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean phoneVerified = documentSnapshot.getBoolean("phoneVerified");
                        Boolean otpVerified = documentSnapshot.getBoolean("otpVerified");
                        String savedDeviceId = documentSnapshot.getString("deviceId");
                        
                        String currentDeviceId = android.provider.Settings.Secure.getString(
                                activity.getContentResolver(), 
                                android.provider.Settings.Secure.ANDROID_ID
                        );
                        
                        if (Boolean.TRUE.equals(phoneVerified) && Boolean.TRUE.equals(otpVerified) &&
                                savedDeviceId != null && savedDeviceId.equals(currentDeviceId)) {
                            // User is already verified on this device
                            callback.onSignInSuccess(userId);
                        } else {
                            // User needs to verify phone number
                            callback.onSignInFailed("Phone verification required");
                        }
                    } else {
                        callback.onSignInFailed("User document not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check user verification status", e);
                    callback.onSignInFailed("Failed to check verification status");
                });
    }
    
    /**
     * Get current verification ID
     */
    public String getStoredVerificationId() {
        return storedVerificationId;
    }
    
    /**
     * Clear stored verification data
     */
    public void clearVerificationData() {
        storedVerificationId = null;
        resendToken = null;
    }
}
