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

import androidx.appcompat.app.AppCompatActivity;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private EditText emailEditText, phoneEditText;
    private TextInputEditText passwordEditText, confirmPasswordEditText;
    private Button signUpButton;
    private TextView goToLogin;
    private View googleSignUpButton;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views with error handling
        try {
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
            phoneEditText = findViewById(R.id.phoneEditText);
            signUpButton = findViewById(R.id.signUpButton);
            goToLogin = findViewById(R.id.goToLogin);
            googleSignUpButton = findViewById(R.id.googleSignUpButton);

            // Google Sign-In setup
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e("SignUpActivity", "Error initializing views or Google Sign-In", e);
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Set click listeners with animations
        signUpButton.setOnClickListener(view -> {
            animateButton(signUpButton);
            new Handler().postDelayed(() -> signUp(), 100);
        });

        goToLogin.setOnClickListener(view -> {
            animateButton(goToLogin);
            new Handler().postDelayed(() -> {
                Intent loginIntent = new Intent(SignUpActivity.this, MainActivity.class);
                startActivity(loginIntent);
                overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
                finish();
            }, 100);
        });

        // Google sign-up button
        googleSignUpButton.setOnClickListener(view -> {
            animateButton(googleSignUpButton);
            new Handler().postDelayed(() -> signUpWithGoogle(), 100);
        });

        // Start animations
        startSignUpAnimations();
    }

    private void startSignUpAnimations() {
        try {
            // Animate title texts
            TextView titleText = findViewById(R.id.textView8);
            TextView subtitleText = findViewById(R.id.textView9);

            if (titleText == null || subtitleText == null) {
                Log.w("SignUpActivity", "Animation views not found, skipping animations");
                return;
            }

            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);

            // Stagger animations
            titleText.startAnimation(fadeIn);

            new Handler().postDelayed(() -> {
                if (subtitleText != null) subtitleText.startAnimation(slideInLeft);
            }, 200);

            new Handler().postDelayed(() -> {
                View mainCard = findViewById(R.id.mainCardContainer);
                if (mainCard != null) mainCard.startAnimation(slideUp);
            }, 400);

            new Handler().postDelayed(() -> {
                if (signUpButton != null) signUpButton.startAnimation(bounceIn);
            }, 600);

            new Handler().postDelayed(() -> {
                View googleContainer = findViewById(R.id.googleSignInContainer);
                if (googleContainer != null) googleContainer.startAnimation(bounceIn);
            }, 700);
        } catch (Exception e) {
            Log.e("SignUpActivity", "Error in animations", e);
            // Don't crash, just skip animations
        }
    }

    private void animateButton(View button) {
        try {
            if (button != null) {
                Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
                button.startAnimation(buttonPress);
            }
        } catch (Exception e) {
            Log.e("SignUpActivity", "Error animating button", e);
            // Don't crash, just skip animation
        }
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phoneInput = phoneEditText.getText().toString().trim();

        // Validate phone number format
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneInput.isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Philippine phone format (must start with 09 and be 11 digits)
        if (!phoneInput.startsWith("09") || phoneInput.length() != 11) {
            Toast.makeText(SignUpActivity.this, "Phone number must start with 09 and be 11 digits (e.g., 09171234567)", Toast.LENGTH_LONG).show();
            phoneEditText.setError("Invalid format");
            phoneEditText.requestFocus();
            return;
        }

        // Convert to international format for Firebase
        final String phone = "+63" + phoneInput.substring(1);


        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        } else if (!isStrongPassword(password)) {
            Toast.makeText(SignUpActivity.this, "Password must be at least 8 characters and include upper, lower, digit, and special character.", Toast.LENGTH_LONG).show();
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(SignUpActivity.this, "Phone number already registered", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed with registration
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this, task -> {
                                        if (task.isSuccessful()) {
                                            String userId = mAuth.getCurrentUser().getUid();

                                            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                                            String hashedDeviceId = hashDeviceId(deviceId);

                                            Map<String, Object> newUser = new HashMap<>();
                                            newUser.put("email", email);
                                            newUser.put("phone", phone);
                                            newUser.put("deviceId", hashedDeviceId);
                                            newUser.put("phoneVerified", false);
                                            newUser.put("emailVerified", false);
                                            newUser.put("accountStatus", "ACTIVE");
                                            newUser.put("name", "");
                                            newUser.put("birthdate", "");
                                            newUser.put("gender", "");
                                            newUser.put("profileImageUrl", "");
                                            newUser.put("createdAt", System.currentTimeMillis());

                                            db.collection("users").document(userId)
                                                    .set(newUser)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Send email verification
                                                        sendEmailVerification();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(SignUpActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });

                                        } else {
                                            Toast.makeText(SignUpActivity.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpActivity.this, "Error checking phone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }
    }

    private boolean isStrongPassword(String password) {
        // At least 8 characters, 1 digit, 1 lower, 1 upper, 1 special char
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
    private void signUpWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
                String errorMessage = "Google Sign-Up failed: " + e.getStatusCode() + " - " + e.getMessage();
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e("GoogleSignIn", "Sign-in failed with code: " + e.getStatusCode(), e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save basic user info to Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();
                        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                        String hashedDeviceId = hashDeviceId(deviceId);

                        // Create a user entry in Firestore (without phone initially)
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("email", email);
                        newUser.put("phone", "");
                        newUser.put("deviceId", hashedDeviceId);
                        newUser.put("phoneVerified", false);
                        newUser.put("emailVerified", true); // Google accounts are pre-verified
                        newUser.put("accountStatus", "ACTIVE");
                        newUser.put("isGoogleUser", true);
                        newUser.put("name", "");
                        newUser.put("birthdate", "");
                        newUser.put("gender", "");
                        newUser.put("profileImageUrl", "");
                        newUser.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(userId).set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    // Navigate to OTP verification to collect phone number
                                    Intent intent = new Intent(SignUpActivity.this, MainPage.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "Failed to create user record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Account created! Please check your email to verify your account.", Toast.LENGTH_LONG).show();

                            // Navigate to email verification activity
                            Intent intent = new Intent(SignUpActivity.this, EmailVerificationActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                            // Still navigate to email verification activity
                            Intent intent = new Intent(SignUpActivity.this, EmailVerificationActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
        }
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
            return deviceId; // fallback if hashing fails
        }
    }

}