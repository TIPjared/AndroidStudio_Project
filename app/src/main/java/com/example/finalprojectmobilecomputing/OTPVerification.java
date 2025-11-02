package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

public class OTPVerification extends AppCompatActivity {

    private TextInputEditText otpEditText;
    private Button verifyOtpButton;
    private TextView resendOtpButton;
    private ImageButton backButton;

    private String phoneNumber;
    private String userId;
    private boolean isNewSignUp = false;
    private CountDownTimer resendTimer;

    // Constants for rate limiting
    private static final String PREFS_NAME = "OTPVerificationPrefs";
    private static final String LAST_REQUEST_TIME = "lastRequestTime";
    private static final String REQUEST_COUNT = "requestCount";
    private static final String DAILY_OTP_COUNT = "dailyOtpCount";
    private static final String LAST_OTP_DATE = "lastOtpDate";
    private static final long COOLDOWN_PERIOD = 30000; // 30s
    private static final int MAX_REQUESTS = 5;
    private static final int DAILY_OTP_LIMIT = 10;

    // 🔧 Replace this with your Render server later
    private static final String VERIFY_URL = "https://philsms-otp-server.onrender.com/verify-otp";
    private static final String RESEND_URL = "https://philsms-otp-server.onrender.com/send-otp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);

        otpEditText = findViewById(R.id.otpEditText);
        verifyOtpButton = findViewById(R.id.verifyOtpButton);
        resendOtpButton = findViewById(R.id.resendOtpButton);
        backButton = findViewById(R.id.backButton);

        // 🔙 Back button → login
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            fetchPhoneFromFirestore(userId);
        } else {
            Toast.makeText(this, "User not logged in, please login again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(OTPVerification.this, MainActivity.class));
            finish();
        }

        // ✅ Step 1: Verify OTP
        verifyOtpButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter OTP first", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpWithServer(code);
        });

        // ✅ Step 2: Resend OTP
        resendOtpButton.setOnClickListener(v -> {
            if (canSendVerification()) {
                resendOtpWithServer();
            } else {
                showCooldownMessage();
            }
        });
    }
    private void fetchPhoneFromFirestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        phoneNumber = document.getString("phone");
                        if (phoneNumber == null || phoneNumber.isEmpty()) {
                            Toast.makeText(this, "No phone number found. Please update your profile.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Verifying number: " + phoneNumber, Toast.LENGTH_SHORT).show();
                            // Automatically send OTP when activity starts
                            sendInitialOtp();
                        }
                    } else {
                        Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching phone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    // 📤 Send resend request to your server
    private void resendOtpWithServer() {
        setResendButtonEnabled(false);
        Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("phone", phoneNumber);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    RESEND_URL,
                    jsonBody,
                    response -> {
                        boolean success = response.optBoolean("success", false);
                        if (success) {
                            Toast.makeText(this, "OTP resent successfully!", Toast.LENGTH_SHORT).show();
                            startResendTimer();
                            updateQuotaCount();
                        } else {
                            Toast.makeText(this, response.optString("message", "Failed to resend OTP"), Toast.LENGTH_SHORT).show();
                            setResendButtonEnabled(true);
                        }
                    },
                    error -> {
                        if (error.networkResponse == null) {
                            Toast.makeText(this, "Server is waking up... please wait a moment.", Toast.LENGTH_SHORT).show();

                            // Auto-retry after 3 seconds if first attempt failed (Render cold start)
                            new android.os.Handler().postDelayed(this::sendInitialOtp, 3000);
                        } else {
                            int code = error.networkResponse.statusCode;
                            Log.e("OTP", "Server responded with " + code);
                            Toast.makeText(this, "Server error (" + code + "). Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            Volley.newRequestQueue(this).add(request);
        } catch (JSONException e) {
            Toast.makeText(this, "Error preparing request", Toast.LENGTH_SHORT).show();
            setResendButtonEnabled(true);
        }
    }

    // 📥 Verify OTP via PhilSMS backend
    private void verifyOtpWithServer(String otp) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("phone", phoneNumber);
            jsonBody.put("otp", otp);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    VERIFY_URL,
                    jsonBody,
                    response -> {
                        boolean success = response.optBoolean("success", false);
                        if (success) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String deviceId = android.provider.Settings.Secure.getString(
                                    getContentResolver(),
                                    android.provider.Settings.Secure.ANDROID_ID
                            );

                            db.collection("users").document(userId)
                                    .update(
                                            "phoneVerified", true,
                                            "deviceId", deviceId
                                    )
                                    .addOnSuccessListener(aVoid -> Log.d("OTP", "User verified & device ID saved"))
                                    .addOnFailureListener(e -> Log.e("OTP", "Failed to update verification", e));

                            Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, MainPage.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    else {
                            Toast.makeText(this, response.optString("message", "Invalid OTP"), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "Verification failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            );

            Volley.newRequestQueue(this).add(request);
        } catch (JSONException e) {
            Toast.makeText(this, "Error preparing request", Toast.LENGTH_SHORT).show();
        }
    }

    // 📊 Rate limiting
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
            int seconds = (int) (timeRemaining / 1000);
            Toast.makeText(this, "Try again in " + seconds + "s", Toast.LENGTH_SHORT).show();
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

    private void updateQuotaCount() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int currentCount = settings.getInt(DAILY_OTP_COUNT, 0);
        settings.edit().putInt(DAILY_OTP_COUNT, currentCount + 1).apply();

        int remaining = DAILY_OTP_LIMIT - (currentCount + 1);
        if (remaining > 0) {
            Toast.makeText(this, "OTP sent! " + remaining + " remaining today.", Toast.LENGTH_SHORT).show();
        }
    }

    // Send initial OTP when activity starts
    private void sendInitialOtp() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number found", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("phone", phoneNumber);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    RESEND_URL,
                    jsonBody,
                    response -> {
                        boolean success = response.optBoolean("success", false);
                        if (success) {
                            Toast.makeText(this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                            startResendTimer();
                            updateQuotaCount();
                        } else {
                            Toast.makeText(this, response.optString("message", "Failed to send OTP"), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                        } else {
                            Log.e("OTP", "Volley network error: " + error.toString());
                            Toast.makeText(this, "Unable to reach server. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            Volley.newRequestQueue(this).add(request);
        } catch (JSONException e) {
            Toast.makeText(this, "Error preparing request", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }
}
