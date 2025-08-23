package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

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

        // Initialize views
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

        // Set click listeners
        signUpButton.setOnClickListener(view -> signUp());

        goToLogin.setOnClickListener(view -> {
            Intent loginIntent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(loginIntent);
            finish();
        });

        // Google sign-up button
        googleSignUpButton.setOnClickListener(view -> signUpWithGoogle());
    }

    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(confirmPassword)) {
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

                                            String deviceId = Settings.Secure.getString(
                                                    getContentResolver(), Settings.Secure.ANDROID_ID);

                                            Map<String, Object> newUser = new HashMap<>();
                                            newUser.put("email", email);
                                            newUser.put("phone", phone);
                                            newUser.put("deviceId", null);
                                            newUser.put("phoneVerified", false);

                                            db.collection("users").document(userId)
                                                    .set(newUser)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(SignUpActivity.this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                                        finish();
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
                Toast.makeText(this, "Google Sign-Up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        
                        // Create a user entry in Firestore (without phone initially)
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(userId).set(new UserModel(email, ""))
                                .addOnSuccessListener(aVoid -> {
                                    // Navigate to OTP verification to collect phone number
                                    Intent intent = new Intent(SignUpActivity.this, OTPVerification.class);
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
}