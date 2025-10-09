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

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailField;
    private TextInputEditText passwordField;
    private Button loginButton;
    private TextView goToSignup, forgotPasswordLink;
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private View googleSignInButton;

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
                            // ✅ Directly proceed to MainPage (OTP check removed)
                            Intent intent = new Intent(MainActivity.this, MainPage.class);
                            startActivity(intent);
                            finish();

                            // ❌ Disabled:
                            // checkOTPRequirement(user.getUid());
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
    ===================================================
     OTP FEATURE COMMENTED OUT (BYPASSED)
    ===================================================

    private void checkOTPRequirement(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean phoneVerified = document.getBoolean("phoneVerified");
                        String savedDeviceId = document.getString("deviceId");
                        String phoneNumber = document.getString("phone");

                        String currentDeviceId = Settings.Secure.getString(
                                getContentResolver(), Settings.Secure.ANDROID_ID);

                        if (phoneVerified != null && phoneVerified &&
                                savedDeviceId != null && savedDeviceId.equals(currentDeviceId)) {
                            startActivity(new Intent(MainActivity.this, MainPage.class));
                            finish();
                        } else {
                            sendOtpRequest(phoneNumber, userId);
                        }
                    } else {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("email", user.getEmail());
                            newUser.put("phoneVerified", false);
                            newUser.put("deviceId", null);
                            newUser.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                            newUser.put("username", "");
                            newUser.put("age", "");
                            newUser.put("gender", "");
                            newUser.put("profileImageUrl", "");
                            newUser.put("createdAt", System.currentTimeMillis());
                            db.collection("users").document(userId).set(newUser);

                            sendOtpRequest(user.getPhoneNumber(), userId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                });
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

        runOnUiThread(() -> {
            Intent intent = new Intent(MainActivity.this, OTPVerification.class);
            intent.putExtra("phone", phoneNumber);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        });
    }

    private void showTrialAccountError(String phoneNumber, String userId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Phone Number Verification Required")
                .setMessage("Your phone number (" + phoneNumber + ") needs to be verified with Twilio to receive SMS.\n\n" +
                           "To fix this:\n" +
                           "1. Go to twilio.com/user/account/phone-numbers/verified\n" +
                           "2. Add and verify your phone number\n" +
                           "3. Or upgrade to a paid Twilio account\n\n" +
                           "Please verify your phone number and try again.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
    ===================================================
    */

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
                            Intent intent = new Intent(MainActivity.this, MainPage.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Firebase Google Sign-In failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
