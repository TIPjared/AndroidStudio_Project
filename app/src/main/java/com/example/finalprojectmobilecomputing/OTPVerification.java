
package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.TimeUnit;

public class OTPVerification extends AppCompatActivity {

    private TextInputEditText otpEditText;
    private Button verifyOtpButton;
    private TextView resendOtpButton;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String phoneNumber;
    private String userId;
    private boolean isNewSignUp = false;
    private CountDownTimer resendTimer;
    
    // Firebase Phone Auth variables
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    // Constants for rate limiting
    private static final String PREFS_NAME = "OTPVerificationPrefs";
    private static final String LAST_REQUEST_TIME = "lastRequestTime";
    private static final String REQUEST_COUNT = "requestCount";
    private static final String DAILY_OTP_COUNT = "dailyOtpCount";
    private static final String LAST_OTP_DATE = "lastOtpDate";
    private static final long COOLDOWN_PERIOD = 30000; // 30 seconds (simplified)
    private static final int MAX_REQUESTS = 5; // Max requests per cooldown
    private static final int DAILY_OTP_LIMIT = 10; // 10 OTP per day

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

        // 🔙 Back button → login
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
            return;
        }
        phoneNumber = getIntent().getStringExtra("phone");
        userId = getIntent().getStringExtra("userId");
        isNewSignUp = getIntent().getBooleanExtra("isNewSignUp", false);
        
        // If userId is not provided, get it from current user
        if (userId == null || userId.isEmpty()) {
            userId = currentUser.getUid();
        }

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number missing, please login again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
            return;
        }
        
        // 📤 Send initial OTP
        Log.d("OTPVerification", "Starting OTP verification for phone: " + phoneNumber);
        
        // Validate phone number before sending
        String normalizedPhone = normalizePhone(phoneNumber);
        if (normalizedPhone.isEmpty()) {
            Toast.makeText(this, "Invalid phone number format. Please use: 09XXXXXXXXX", Toast.LENGTH_LONG).show();
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
            return;
        }
        
        Log.d("OTPVerification", "Using phone number: " + normalizedPhone + " for Firebase Phone Auth");
        
        // Always use real Firebase Phone Auth for actual phone numbers
        sendOtpWithFirebase(phoneNumber);

        // ✅ Step 2: Verify OTP
        verifyOtpButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter OTP first", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpWithFirebase(code);
        });

        // ✅ Step 3: Resend OTP
        resendOtpButton.setOnClickListener(v -> {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                if (canSendVerification()) {
                    sendOtpWithFirebase(phoneNumber); // 🔁 Call Firebase again
                } else {
                    showCooldownMessage();
                }
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private String normalizePhone(String raw) {
        if (raw == null || raw.isEmpty()) {
            Log.e("OTPVerification", "Phone number is null or empty");
            return "";
        }
        
        String digits = raw.replaceAll("[^0-9+]", "");
        Log.d("OTPVerification", "Normalizing phone: " + raw + " -> " + digits);
        
        // For Philippine numbers, always convert to +63 format for Firebase
        if (digits.startsWith("0") && digits.length() == 11) {
            // Convert 09XXXXXXXXX to +639XXXXXXXXX for Firebase
            String result = "+63" + digits.substring(1);
            Log.d("OTPVerification", "Converted Philippine format: " + result);
            return result;
        }
        
        if (digits.startsWith("+63") && digits.length() == 13) {
            // Already in correct format
            Log.d("OTPVerification", "Phone already in correct format: " + digits);
            return digits;
        }
        
        if (digits.startsWith("63") && digits.length() == 12) {
            // Convert 63XXXXXXXXX to +639XXXXXXXXX
            String result = "+" + digits;
            Log.d("OTPVerification", "Converted 63 format: " + result);
            return result;
        }
        
        Log.e("OTPVerification", "Invalid Philippine phone format: " + digits);
        return "";
    }


    // 📤 Send OTP with Firebase Phone Auth
    private void sendOtpWithFirebase(String phoneFromDb) {
        setResendButtonEnabled(false);
        Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show();
        
        String phone = normalizePhone(phoneFromDb);
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            setResendButtonEnabled(true);
            return;
        }
        
        Log.d("OTPVerification", "Attempting to send OTP to: " + phone);
        
        // Check quota before sending
        if (!checkQuotaLimit()) {
            Toast.makeText(this, "Daily OTP limit reached. Try again tomorrow.", Toast.LENGTH_LONG).show();
            setResendButtonEnabled(true);
            return;
        }
        
        // Debug Firebase configuration
        Log.d("OTPVerification", "Firebase project: " + mAuth.getApp().getOptions().getProjectId());
        Log.d("OTPVerification", "Firebase app name: " + mAuth.getApp().getName());
        Log.d("OTPVerification", "Firebase API key: " + mAuth.getApp().getOptions().getApiKey());
        Log.d("OTPVerification", "Firebase application ID: " + mAuth.getApp().getOptions().getApplicationId());
        
        // Check if Firebase is properly initialized
        if (mAuth.getApp() == null) {
            Log.e("OTPVerification", "Firebase app is null!");
            Toast.makeText(this, "Firebase not initialized properly", Toast.LENGTH_LONG).show();
            setResendButtonEnabled(true);
            return;
        }
        
        // Log phone number for debugging
        Log.d("OTPVerification", "Sending OTP to: " + phone + " (normalized from: " + phoneFromDb + ")");
        
        // Initialize Firebase Phone Auth callbacks
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Auto-verification completed
                Log.d("OTPVerification", "Auto-verification completed");
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                Log.e("OTPVerification", "Verification failed: " + e.getMessage(), e);
                Log.e("OTPVerification", "Error class: " + e.getClass().getSimpleName());
                
                runOnUiThread(() -> {
                    String errorMessage = e.getMessage();
                    String errorClass = e.getClass().getSimpleName();
                    
                    if (errorMessage != null && errorMessage.contains("not allowed")) {
                        Toast.makeText(OTPVerification.this, 
                            "Phone verification not enabled. Go to Firebase Console → Authentication → Sign-in method → Enable Phone", 
                            Toast.LENGTH_LONG).show();
                    } else if (errorMessage != null && errorMessage.contains("invalid phone number")) {
                        Toast.makeText(OTPVerification.this, 
                            "Invalid phone number format. Please use format: 09XXXXXXXXX", 
                            Toast.LENGTH_LONG).show();
                    } else if (errorMessage != null && errorMessage.contains("missing a valid")) {
                        Toast.makeText(OTPVerification.this, 
                            "Firebase configuration error. Check Firebase Console → Authentication → Sign-in method → Phone provider", 
                            Toast.LENGTH_LONG).show();
                        Log.e("OTPVerification", "Firebase config error - check Firebase Console Phone provider settings");
                    } else if (errorMessage != null && errorMessage.contains("already in use")) {
                        handlePhoneNumberInUse(phone);
                    } else if (errorMessage != null && errorMessage.contains("quota")) {
                        Toast.makeText(OTPVerification.this, 
                            "Daily SMS quota exceeded. Try again tomorrow.", 
                            Toast.LENGTH_LONG).show();
                    } else if (errorClass.contains("FirebaseAuthInvalidCredentialsException")) {
                        Toast.makeText(OTPVerification.this, 
                            "Invalid credentials. Check Firebase Console → Authentication → Sign-in method → Phone provider", 
                            Toast.LENGTH_LONG).show();
                    } else if (errorMessage != null && errorMessage.contains("missing a valid app identifier")) {
                        Toast.makeText(OTPVerification.this, 
                            "Play Integrity API not enabled. Please enable Play Integrity API in Google Cloud Console for your project.", 
                            Toast.LENGTH_LONG).show();
                        Log.e("OTPVerification", "Play Integrity API not enabled - go to Google Cloud Console and enable Play Integrity API");
                        Log.e("OTPVerification", "Steps: Google Cloud Console → Search 'Play Integrity API' → Enable");
                    } else {
                        Toast.makeText(OTPVerification.this, "Verification failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                    setResendButtonEnabled(true);
                });
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                Log.d("OTPVerification", "Code sent successfully");
                runOnUiThread(() -> {
                    OTPVerification.this.verificationId = verificationId;
                    OTPVerification.this.resendToken = token;
                    Toast.makeText(OTPVerification.this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                    startResendTimer();
                    updateQuotaCount(); // Update quota after successful send
                });
            }
        };
        
        try {
            // Start phone number verification
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(phone)
                    .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(mCallbacks)
                    .build();
                    
            Log.d("OTPVerification", "Starting Firebase Phone Auth verification");
            PhoneAuthProvider.verifyPhoneNumber(options);
        } catch (Exception e) {
            Log.e("OTPVerification", "Error starting phone verification", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setResendButtonEnabled(true);
        }
    }



    // 📥 Verify OTP with Firebase Phone Auth
    private void verifyOtpWithFirebase(String code) {
        if (verificationId == null || verificationId.isEmpty()) {
            Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (code.length() != 6) {
            Toast.makeText(this, "Please enter a 6-digit OTP code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d("OTPVerification", "Verifying OTP code: " + code);
        
        // Real Firebase Phone Auth verification only
        Log.d("OTPVerification", "Verifying with real Firebase Phone Auth");
        
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }
    
    // 🔐 Sign in with phone auth credential
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Phone verification successful
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            // Update user's phone verification status in Firestore
                            updateUserPhoneVerification(user.getUid(), phoneNumber);
                            Toast.makeText(this, "Phone verified successfully!", Toast.LENGTH_SHORT).show();
                            
                            // Navigate to main page
                            Intent intent = new Intent(OTPVerification.this, MainPage.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // Verification failed
                        Toast.makeText(this, "Invalid OTP code", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    // 📝 Update user phone verification status in Firestore
    private void updateUserPhoneVerification(String userId, String phoneNumber) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("phoneVerified", true, "phone", phoneNumber, "deviceId", 
                        Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("FIREBASE_UPDATE", "Phone verification status updated successfully");
                        Toast.makeText(this, "Phone verified successfully!", Toast.LENGTH_SHORT).show();
                        
                        if (isNewSignUp) {
                            // For new sign-ups, go to MainPage after verification
                            startActivity(new Intent(OTPVerification.this, MainPage.class));
                        } else {
                            // For existing users, go back to login
                            startActivity(new Intent(OTPVerification.this, MainActivity.class));
                        }
                        finish();
                    } else {
                        android.util.Log.e("FIREBASE_UPDATE", "Failed to update phone verification status", task.getException());
                        Toast.makeText(this, "Failed to update verification status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 📊 Rate limiting methods
    private boolean canSendVerification() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long lastRequestTime = settings.getLong(LAST_REQUEST_TIME, 0);
        int requestCount = settings.getInt(REQUEST_COUNT, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRequestTime > COOLDOWN_PERIOD) {
            settings.edit().putInt(REQUEST_COUNT, 0).apply();
            return true;
        }
        return requestCount < MAX_REQUESTS;
    }

    private void updateRequestCounter() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int requestCount = settings.getInt(REQUEST_COUNT, 0);
        settings.edit()
                .putLong(LAST_REQUEST_TIME, System.currentTimeMillis())
                .putInt(REQUEST_COUNT, requestCount + 1)
                .apply();
    }

    private void showCooldownMessage() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long lastRequestTime = settings.getLong(LAST_REQUEST_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long timeRemaining = COOLDOWN_PERIOD - (currentTime - lastRequestTime);

        if (timeRemaining > 0) {
            int minutesRemaining = (int) (timeRemaining / 60000);
            Toast.makeText(this,
                    "Too many attempts. Try again in " +
                            (minutesRemaining > 0 ? minutesRemaining + " minutes" : "a moment"),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startResendTimer() {
        if (resendTimer != null) resendTimer.cancel();

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
        resendOtpButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(OTPVerification.this, MainActivity.class));
        finish();
    }

    // 🔧 Development fallback method (disabled for production)
    private void simulateOTPForDevelopment() {
        Log.d("OTPVerification", "Development mode disabled - using real Firebase Phone Auth");
        Toast.makeText(this, "Using real Firebase Phone Auth for OTP verification", Toast.LENGTH_SHORT).show();
    }
    
    // 🔄 Handle phone number already in use
    private void handlePhoneNumberInUse(String phone) {
        Log.d("OTPVerification", "Phone number already in use: " + phone);
        
        // Show options to user
        runOnUiThread(() -> {
            Toast.makeText(this, 
                "Phone number already linked to another account.\n\n" +
                "Solutions:\n" +
                "1. Use a different phone number\n" +
                "2. Delete the old account from Firebase Console\n" +
                "3. Wait 24 hours for automatic unlinking", 
                Toast.LENGTH_LONG).show();
        });
        
        // For phone number conflicts, provide clear guidance
        Log.d("OTPVerification", "Phone number conflict - user needs to use different number");
    }
    
    // 🔧 Handle phone number conflicts in production
    private void clearPhoneNumberFromFirebase(String phone) {
        Log.d("OTPVerification", "Phone number conflict detected: " + phone);
        
        Toast.makeText(this, 
            "Phone number already linked to another account. Please use a different phone number.", 
            Toast.LENGTH_LONG).show();
    }

    // 📊 Quota management methods
    private boolean checkQuotaLimit() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String lastDate = settings.getString(LAST_OTP_DATE, "");
        String today = java.text.DateFormat.getDateInstance().format(new java.util.Date());
        
        if (!today.equals(lastDate)) {
            // New day, reset counter
            settings.edit().putString(LAST_OTP_DATE, today).putInt(DAILY_OTP_COUNT, 0).apply();
            return true;
        }
        
        int dailyCount = settings.getInt(DAILY_OTP_COUNT, 0);
        return dailyCount < DAILY_OTP_LIMIT;
    }
    
    private void updateQuotaCount() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int currentCount = settings.getInt(DAILY_OTP_COUNT, 0);
        settings.edit().putInt(DAILY_OTP_COUNT, currentCount + 1).apply();
        
        int remaining = DAILY_OTP_LIMIT - (currentCount + 1);
        if (remaining > 0) {
            Toast.makeText(this, "OTP sent! " + remaining + " remaining today.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }
}
