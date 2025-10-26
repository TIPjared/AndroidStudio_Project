package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {
    private static final String TAG = "EmailVerificationActivity";
    private EmailVerificationService verificationService;
    private MaterialButton verifyButton, resendButton;
    private TextView backToLoginButton, statusText, emailText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        verificationService = new EmailVerificationService();

        initializeViews();
        setupClickListeners();
        checkInitialStatus();
    }

    private void initializeViews() {
        verifyButton = findViewById(R.id.verifyButton);
        resendButton = findViewById(R.id.resendButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);
        statusText = findViewById(R.id.statusText);
        emailText = findViewById(R.id.emailText);

        // Set user email
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            emailText.setText("Please check your email: " + user.getEmail());
        }
    }

    private void setupClickListeners() {
        verifyButton.setOnClickListener(v -> {
            animateButton(verifyButton);
            new Handler().postDelayed(this::checkVerification, 100);
        });

        resendButton.setOnClickListener(v -> {
            animateButton(resendButton);
            new Handler().postDelayed(this::resendVerification, 100);
        });

        backToLoginButton.setOnClickListener(v -> {
            animateButton(backToLoginButton);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(EmailVerificationActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, 100);
        });
    }

    private void checkInitialStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            // Email already verified, proceed to main app
            proceedToMainApp();
        } else {
            statusText.setText("Please verify your email to continue");
        }
    }

    private void checkVerification() {
        verificationService.checkEmailVerification(new EmailVerificationService.VerificationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    statusText.setText("Email verified successfully!");
                    Toast.makeText(EmailVerificationActivity.this, "Email verified! Welcome!", Toast.LENGTH_SHORT).show();
                    proceedToMainApp();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    statusText.setText("Email not verified yet. Please check your email and click the verification link.");
                    Toast.makeText(EmailVerificationActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void resendVerification() {
        verificationService.sendEmailVerification(this, new EmailVerificationService.VerificationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    statusText.setText("New verification email sent! Please check your inbox.");
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EmailVerificationActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void proceedToMainApp() {
        // Check account status before proceeding
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            verificationService.checkAccountStatus(user.getUid(), status -> {
                runOnUiThread(() -> {
                    if ("ACTIVE".equals(status)) {
                        // Proceed to OTP verification or main app
                        Intent intent = new Intent(EmailVerificationActivity.this, OTPVerification.class);
                        startActivity(intent);
                        finish();
                    } else if ("SUSPENDED".equals(status) || "BANNED".equals(status)) {
                        // Show account status dialog
                        showAccountStatusDialog(status);
                    } else {
                        // Unknown status, proceed with caution
                        Intent intent = new Intent(EmailVerificationActivity.this, OTPVerification.class);
                        startActivity(intent);
                        finish();
                    }
                });
            });
        }
    }

    private void showAccountStatusDialog(String status) {
        AccountStatusDialog dialog = new AccountStatusDialog(this, status);
        dialog.show();
    }

    private void animateButton(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100);
                });
    }
}
