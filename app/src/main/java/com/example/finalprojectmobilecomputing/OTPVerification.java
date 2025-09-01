
package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class OTPVerification extends AppCompatActivity {

    private TextInputEditText otpEditText;
    private Button verifyOtpButton;
    private TextView resendOtpButton;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String phoneNumber;
    private String userId;
    private CountDownTimer resendTimer;

    // Constants for rate limiting
    private static final String PREFS_NAME = "OTPVerificationPrefs";
    private static final String LAST_REQUEST_TIME = "lastRequestTime";
    private static final String REQUEST_COUNT = "requestCount";
    private static final long COOLDOWN_PERIOD = 3600000; // 1 hour
    private static final int MAX_REQUESTS = 5; // Max requests per cooldown

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

        // ✅ Step 2: Verify OTP
        verifyOtpButton.setOnClickListener(v -> {
            String code = otpEditText.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter OTP first", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtpWithTwilio(phoneNumber, code, userId);
        });

        // ✅ Step 3: Resend OTP
        resendOtpButton.setOnClickListener(v -> {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                if (canSendVerification()) {
                    sendOtpWithTwilio(phoneNumber); // 🔁 Call Twilio again
                } else {
                    showCooldownMessage();
                }
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private String normalizePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;         // assume already E.164
        if (digits.startsWith("0")) return "+63" + digits.substring(1);
        // fallback: if it starts with 63 (no +)
        if (digits.startsWith("63")) return "+" + digits;
        // last resort: treat as local PH without 0
        return "+63" + digits;
    }


    // 📤 Send OTP with Twilio backend
    private void sendOtpWithTwilio(String phoneFromDb) {
        setResendButtonEnabled(false);
        Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show();
        updateRequestCounter();

        String phone = normalizePhone(phoneFromDb);

        OkHttpClient client = new OkHttpClient();
        String url = "https://sikad-otp-server.onrender.com/otp/start";

        String json = "{ \"phone\": \"" + phone + "\" }";
        RequestBody body = RequestBody.create(
                json, okhttp3.MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(OTPVerification.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                    setResendButtonEnabled(true);
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                final String respBody = response.body() != null ? response.body().string() : "";
                android.util.Log.d("OTP_START", "code=" + response.code() + " body=" + respBody);
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(OTPVerification.this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                        startResendTimer();
                    } else {
                        Toast.makeText(OTPVerification.this, "Error sending OTP (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        setResendButtonEnabled(true);
                    }
                });
            }
        });
    }


    // 📥 Verify OTP with Twilio backend
    private void verifyOtpWithTwilio(String phoneFromDb, String code, String userId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final String phone = normalizePhone(phoneFromDb);

        user.getIdToken(true).addOnSuccessListener(result -> {
            String idToken = result.getToken();

            OkHttpClient client = new OkHttpClient();
            String url = "https://sikad-otp-server.onrender.com/otp/check";

            String json = "{ \"phone\": \"" + phone + "\", \"code\": \"" + code + "\" }";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(
                            json, okhttp3.MediaType.parse("application/json; charset=utf-8")))
                    .addHeader("Authorization", "Bearer " + idToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(OTPVerification.this, "Verification failed", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    final String respBody = response.body() != null ? response.body().string() : "";
                    android.util.Log.d("OTP_CHECK", "code=" + response.code() + " body=" + respBody);

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            // Server already set phoneVerified=true in Firestore.
                            // If you also want to pin the deviceId client-side, do it here:
                            FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .update("deviceId", Settings.Secure.getString(
                                            getContentResolver(), Settings.Secure.ANDROID_ID))
                                    .addOnCompleteListener(t -> {
                                        Toast.makeText(OTPVerification.this, "Verification successful!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(OTPVerification.this, MainPage.class));
                                        finish();
                                    });
                        } else {
                            Toast.makeText(OTPVerification.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }).addOnFailureListener(e ->
                Toast.makeText(OTPVerification.this, "Auth error, please re-login", Toast.LENGTH_SHORT).show()
        );
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

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }
}
