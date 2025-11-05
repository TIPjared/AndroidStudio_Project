package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Handler;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailField;
    private TextInputEditText passwordField;
    private Button loginButton;
    private TextView goToSignup, forgotPasswordLink;
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private View googleSignInButton;

    private String SERVER_URL;
    private String SERVER_PING_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pingServer();

        String baseUrl = getString(R.string.otp_server_url);
        SERVER_URL = baseUrl + "/send-otp";
        SERVER_PING_URL = baseUrl + "/";

        startLoginAnimations();

        mAuth = FirebaseAuth.getInstance();

        try {
            emailField = findViewById(R.id.emailField);
            passwordField = findViewById(R.id.passwordField);
            loginButton = findViewById(R.id.loginButton);
            googleSignInButton = findViewById(R.id.googleSignInButton);
            goToSignup = findViewById(R.id.goToSignup);
            forgotPasswordLink = findViewById(R.id.forgotPasswordLink);

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.d("GoogleSignIn", "Google Sign-In client initialized");
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing views or Google Sign-In", e);
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        googleSignInButton.setOnClickListener(view -> {
            animateButton(googleSignInButton);
            new Handler().postDelayed(this::signInWithGoogle, 100);
        });

        loginButton.setOnClickListener(view -> {
            animateButton(loginButton);
            new Handler().postDelayed(this::loginWithEmail, 100);
        });

        goToSignup.setOnClickListener(view -> {
            animateButton(goToSignup);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
            }, 100);
        });

        forgotPasswordLink.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void startLoginAnimations() {
        try {
            TextView welcomeText = findViewById(R.id.textView3);
            TextView signInText = findViewById(R.id.textView4);

            if (welcomeText == null || signInText == null) return;

            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);

            welcomeText.startAnimation(fadeIn);

            new Handler().postDelayed(() -> signInText.startAnimation(slideInLeft), 200);
            new Handler().postDelayed(() -> {
                View formContainer = findViewById(R.id.loginFormContainer);
                if (formContainer != null) formContainer.startAnimation(slideUp);
            }, 400);
            new Handler().postDelayed(() -> loginButton.startAnimation(bounceIn), 700);
            new Handler().postDelayed(() -> {
                View googleContainer = findViewById(R.id.googleSignInContainer);
                if (googleContainer != null) googleContainer.startAnimation(bounceIn);
            }, 800);
        } catch (Exception e) {
            Log.e("MainActivity", "Error in animations", e);
        }
    }

    private void animateButton(View button) {
        try {
            if (button != null) {
                Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
                button.startAnimation(buttonPress);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error animating button", e);
        }
    }

    // ✅ OTP BYPASSED — direct login after success
    private void loginWithEmail() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty()) {
            emailField.setError("Email is required");
            emailField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordField.setError("Password is required");
            passwordField.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkOTPRequirement(user.getUid());
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkOTPRequirement(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Check account status first
                        String accountStatus = document.getString("accountStatus");
                        if (accountStatus == null) {
                            accountStatus = "ACTIVE";
                        }

                        if ("SUSPENDED".equals(accountStatus) || "BANNED".equals(accountStatus)) {
                            // Show account status dialog
                            showAccountStatusDialog(accountStatus);
                            return;
                        }

                        // Check email verification
                        Boolean emailVerified = document.getBoolean("emailVerified");
                        if (emailVerified == null || !emailVerified) {
                            // Email not verified, redirect to email verification
                            Intent intent = new Intent(MainActivity.this, EmailVerificationActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        Boolean isGoogleUser = document.getBoolean("isGoogleUser");
                        String savedDeviceId = document.getString("deviceId");

                        String currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                        String hashedDeviceId = hashDeviceId(currentDeviceId);

                        if (isGoogleUser != null && isGoogleUser) {
                            if (savedDeviceId != null && savedDeviceId.equals(hashedDeviceId)) {
                                // ✅ Trusted device
                                startActivity(new Intent(MainActivity.this, MainPage.class));
                                finish();
                                return;
                            } else {
                                // ⚠️ New or unknown device
                                Toast.makeText(MainActivity.this,
                                        "New device detected for Google account. Please confirm your identity.",
                                        Toast.LENGTH_SHORT).show();

                                // Optional: future verification logic can go here
                                startActivity(new Intent(MainActivity.this, MainPage.class));
                                finish();
                                return;
                            }
                        }

                        Boolean phoneVerified = document.getBoolean("phoneVerified");
                        String phoneNumber = document.contains("phone") ? document.getString("phone") : "";

                        if (phoneVerified != null && phoneVerified) {
                            if (savedDeviceId != null && savedDeviceId.equals(hashedDeviceId)) {
                                // Already verified on this device → go to MainPage
                                startActivity(new Intent(MainActivity.this, MainPage.class));
                                finish();
                                return;
                            } else {
                                // New device, send OTP but don't reset phoneVerified globally
                                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                    sendOtpRequest(phoneNumber, userId);
                                } else {
                                    Toast.makeText(this, "No phone number found", Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }
                        } else {
                            // Not verified → send OTP
                            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                sendOtpRequest(phoneNumber, userId);
                            } else {
                                Toast.makeText(this, "No phone number found", Toast.LENGTH_SHORT).show();
                            }
                        }

                    } else {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {

                            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                            String hashedDeviceId = hashDeviceId(deviceId);

                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("email", user.getEmail());
                            newUser.put("phoneVerified", false);
                            newUser.put("deviceId", hashedDeviceId);
                            newUser.put("phone", "");
                            newUser.put("name", "");
                            newUser.put("birthdate", "");
                            newUser.put("gender", "");
                            newUser.put("profileImageUrl", "");
                            newUser.put("emailVerified", true);
                            newUser.put("accountStatus", "ACTIVE");
                            newUser.put("isGoogleUser", true);
                            newUser.put("createdAt", System.currentTimeMillis());

                            db.collection("users").document(userId)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        startActivity(new Intent(MainActivity.this, MainPage.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(MainActivity.this, "Failed to create Google user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Error fetching user data", Toast.LENGTH_SHORT).show());
    }


    private String normalizePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith("0")) return "+63" + digits.substring(1);
        if (digits.startsWith("63")) return "+" + digits;
        return "+63" + digits;
    }

    private void sendOtpRequest(String phoneNumber, String userId) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number found for user", Toast.LENGTH_SHORT).show();
            return;
        }

        String normalizedPhone = normalizePhone(phoneNumber);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("phone", normalizedPhone);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    SERVER_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                boolean success = response.getBoolean("success");
                                if (success) {
                                    Toast.makeText(MainActivity.this, "OTP sent to " + normalizedPhone, Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(MainActivity.this, OTPVerification.class);
                                    intent.putExtra("fromLogin", true);
                                    startActivity(intent);
                                    finish();

                                } else {
                                    String message = response.optString("message", "Failed to send OTP");
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(MainActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(MainActivity.this, "Failed to send OTP: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            queue.add(request);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }

    private void signInWithGoogle() {
        Log.d("GoogleSignIn", "Starting Google Sign-In...");
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            try {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            } catch (Exception e) {
                Log.e("GoogleSignIn", "Error creating intent: " + e.getMessage());
                Toast.makeText(this, "Google Sign-In error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAccountStatusDialog(String accountStatus) {
        AccountStatusDialog dialog = new AccountStatusDialog(this, accountStatus);
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                String errorMessage = "Google Sign-In failed: " + e.getStatusCode() + " - " + e.getMessage();
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e("GoogleSignIn", "Sign-in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user exists in Firestore and verify status
                            checkOTPRequirement(user.getUid());
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Firebase Google Sign-In failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pingServer() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest pingRequest = new JsonObjectRequest(
                Request.Method.GET,
                SERVER_PING_URL,
                null,
                response -> Log.d("ServerPing", "Server is awake and responding."),
                error -> Log.w("ServerPing", "Server still waking up... (Render cold start)")
        );
        queue.add(pingRequest);
    }


    private String hashDeviceId(String deviceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return deviceId; // fallback
        }
    }

}

