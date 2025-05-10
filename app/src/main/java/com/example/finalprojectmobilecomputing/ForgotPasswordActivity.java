package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetPasswordButton;
    private ImageButton backButton;
    private RelativeLayout progressOverlay;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetButton);
        backButton = findViewById(R.id.backButton);
        progressOverlay = findViewById(R.id.progressOverlay);

        // Handle back button click
        backButton.setOnClickListener(v -> finish());

        // Handle reset password button click
        resetPasswordButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        // Show progress indicator
        progressOverlay.setVisibility(View.VISIBLE);

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressOverlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, 
                                "Password reset email sent to " + email, 
                                Toast.LENGTH_LONG).show();
                        finish(); // Return to login screen
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, 
                                "Failed to send reset email: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
} 