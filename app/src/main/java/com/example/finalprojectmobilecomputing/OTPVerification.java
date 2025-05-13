package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class OTPVerification extends AppCompatActivity {

    private TextInputEditText otpEditText;
    private Button verifyOtpButton;
    private TextView resendOtpButton;
    private ImageButton backButton;

    private String verificationId;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String phoneNumber;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer resendTimer;
    private boolean isVerified = false;
    
    // Constants for rate limiting
    private static final String PREFS_NAME = "OTPVerificationPrefs";
    private static final String LAST_REQUEST_TIME = "lastRequestTime";
    private static final String REQUEST_COUNT = "requestCount";
    private static final long COOLDOWN_PERIOD = 3600000; // 1 hour in milliseconds
    private static final int MAX_REQUESTS = 5; // Max requests in cooldown period

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);

        otpEditText = findViewById(R.id.otpEditText);
        verifyOtpButton = findViewById(R.id.verifyOtpButton);
        resendOtpButton = findViewById(R.id.resendOtpButton);
        backButton = findViewById(R.id.backButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up back button listener to return to login screen
        backButton.setOnClickListener(v -> {
            // Go back to login screen
            Intent intent = new Intent(OTPVerification.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // 🔍 Step 1: Retrieve phone number from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        phoneNumber = documentSnapshot.getString("phone");

                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            phoneNumber = "+63" + phoneNumber; // Format with country code
                            if (canSendVerification()) {
                                sendOTP(phoneNumber, false); // Send initial OTP
                            } else {
                                showCooldownMessage();
                            }
                        } else {
                            Toast.makeText(this, "Phone number is missing in your profile", Toast.LENGTH_SHORT).show();
                            // Redirect user to add their phone number
                            startActivity(new Intent(OTPVerification.this, Profile.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch phone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // ✅ Step 2: Verify OTP manually (if not auto)
        verifyOtpButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();

            if (code.isEmpty() || verificationId == null) {
                Toast.makeText(this, "Enter OTP first", Toast.LENGTH_SHORT).show();
                return;
            }

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneAuthCredential(credential);
        });
        
        // ✅ Step 3: Resend OTP if needed
        resendOtpButton.setOnClickListener(v -> {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                if (canSendVerification()) {
                    resendOTP();
                } else {
                    showCooldownMessage();
                }
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Check if we can send a verification code based on rate limits
     */
    private boolean canSendVerification() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long lastRequestTime = settings.getLong(LAST_REQUEST_TIME, 0);
        int requestCount = settings.getInt(REQUEST_COUNT, 0);
        long currentTime = System.currentTimeMillis();
        
        // If it's been more than the cooldown period, reset counter
        if (currentTime - lastRequestTime > COOLDOWN_PERIOD) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(REQUEST_COUNT, 0);
            editor.apply();
            return true;
        }
        
        // If we haven't exceeded max requests, allow it
        return requestCount < MAX_REQUESTS;
    }
    
    /**
     * Update the request counter
     */
    private void updateRequestCounter() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int requestCount = settings.getInt(REQUEST_COUNT, 0);
        long currentTime = System.currentTimeMillis();
        
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(LAST_REQUEST_TIME, currentTime);
        editor.putInt(REQUEST_COUNT, requestCount + 1);
        editor.apply();
    }
    
    /**
     * Show cooldown message to user
     */
    private void showCooldownMessage() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long lastRequestTime = settings.getLong(LAST_REQUEST_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long timeRemaining = COOLDOWN_PERIOD - (currentTime - lastRequestTime);
        
        if (timeRemaining > 0) {
            int minutesRemaining = (int) (timeRemaining / 60000);
            Toast.makeText(this, 
                "Too many verification attempts. Please try again in " + 
                (minutesRemaining > 0 ? minutesRemaining + " minutes" : "a moment"), 
                Toast.LENGTH_LONG).show();
        }
    }

    // 📤 Send OTP using Firebase PhoneAuth
    private void sendOTP(String phoneNumberWithCode, boolean isResend) {
        setResendButtonEnabled(false);
        
        // Show progress or loading indicator
        Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show();
        
        // Update request counter
        updateRequestCounter();
        
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumberWithCode)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Auto verification (happens on some devices)
                        Toast.makeText(OTPVerification.this, "Verification completed automatically", Toast.LENGTH_SHORT).show();
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        // This callback is invoked if verification fails
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(OTPVerification.this, "Invalid phone number format", Toast.LENGTH_LONG).show();
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            Toast.makeText(OTPVerification.this, "Firebase quota exceeded. Try again later or use a different phone number.", Toast.LENGTH_LONG).show();
                            
                            // Force a longer cooldown when quota is exceeded
                            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putLong(LAST_REQUEST_TIME, System.currentTimeMillis());
                            editor.putInt(REQUEST_COUNT, MAX_REQUESTS);
                            editor.apply();
                        } else {
                            Toast.makeText(OTPVerification.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        setResendButtonEnabled(true);
                    }

                    @Override
                    public void onCodeSent(String verificationIdFromFirebase, PhoneAuthProvider.ForceResendingToken token) {
                        // Save verification ID and token
                        verificationId = verificationIdFromFirebase;
                        resendToken = token;
                        Toast.makeText(OTPVerification.this, "Verification code sent", Toast.LENGTH_SHORT).show();
                        startResendTimer();
                    }
                });
                
        if (isResend && resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }
        
        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }
    
    // Resend OTP with saved token
    private void resendOTP() {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            sendOTP(phoneNumber, true);
        }
    }
    
    // Start countdown timer for resend button
    private void startResendTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        
        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendOtpButton.setText("Resend OTP in " + (millisUntilFinished / 1000) + "s");
                resendOtpButton.setEnabled(false);
            }

            @Override
            public void onFinish() {
                resendOtpButton.setText("Resend OTP");
                setResendButtonEnabled(true);
            }
        }.start();
    }
    
    private void setResendButtonEnabled(boolean enabled) {
        resendOtpButton.setEnabled(enabled);
        resendOtpButton.setClickable(enabled);
        resendOtpButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    // ✅ Sign in with OTP credential
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            isVerified = true;
                            Toast.makeText(OTPVerification.this, "Verification successful!", Toast.LENGTH_SHORT).show();
                            
                            // Save verification status to user's profile
                            FirebaseUser user = task.getResult().getUser();
                            if (user != null) {
                                db.collection("users").document(user.getUid())
                                    .update("phoneVerified", true)
                                    .addOnSuccessListener(aVoid -> {
                                        // Continue to MainPage after successful verification
                                        startActivity(new Intent(OTPVerification.this, MainPage.class));
                                        finish();
                                    });
                            }
                        } else {
                            // Verification failed
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(OTPVerification.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(OTPVerification.this, "Verification failed: " + task.getException().getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
    
    @Override
    public void onBackPressed() {
        // Navigate back to login screen
        Intent intent = new Intent(OTPVerification.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        super.onDestroy();
    }
}
