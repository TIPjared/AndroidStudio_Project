package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private TextInputEditText currentPasswordField, newPasswordField, confirmPasswordField;
    private MaterialButton updatePasswordButton, cancelButton;
    private RelativeLayout progressOverlay;
    private FirebaseAuth mAuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize UI components
        initViews();
        
        // Set up click listeners
        setupListeners();
    }
    
    private void initViews() {
        // TextInputLayouts for error messages
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        
        // EditText fields
        currentPasswordField = findViewById(R.id.currentPasswordField);
        newPasswordField = findViewById(R.id.newPasswordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        
        // Buttons
        updatePasswordButton = findViewById(R.id.updatePasswordButton);
        cancelButton = findViewById(R.id.cancelButton);
        
        // Back button in toolbar
        ImageButton backButton = findViewById(R.id.backButton);
        
        // Progress overlay
        progressOverlay = findViewById(R.id.progressOverlay);
    }
    
    private void setupListeners() {
        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        
        // Cancel button
        cancelButton.setOnClickListener(v -> finish());
        
        // Update password button
        updatePasswordButton.setOnClickListener(v -> validateAndUpdatePassword());
    }
    
    private void validateAndUpdatePassword() {
        // Clear any previous errors
        currentPasswordLayout.setError(null);
        newPasswordLayout.setError(null);
        confirmPasswordLayout.setError(null);
        
        // Get input values
        String currentPassword = currentPasswordField.getText().toString().trim();
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();
        
        // Validate inputs
        boolean isValid = true;
        
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordLayout.setError(getString(R.string.current_password) + " is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordLayout.setError(getString(R.string.new_password) + " is required");
            isValid = false;
        } else if (newPassword.length() < 6) {
            newPasswordLayout.setError(getString(R.string.password_min_length));
            isValid = false;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            confirmPasswordLayout.setError(getString(R.string.password_mismatch));
            isValid = false;
        }
        
        if (isValid) {
            // If current password and new password are the same
            if (currentPassword.equals(newPassword)) {
                newPasswordLayout.setError(getString(R.string.password_different));
                return;
            }
            
            // Proceed with password update
            updatePassword(currentPassword, newPassword);
        }
    }
    
    private void updatePassword(String currentPassword, String newPassword) {
        // Show progress overlay
        showProgress(true);
        
        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        
        if (user != null && user.getEmail() != null) {
            // Re-authenticate the user
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User re-authenticated successfully, now update the password
                        user.updatePassword(newPassword)
                            .addOnCompleteListener(passwordTask -> {
                                showProgress(false);
                                
                                if (passwordTask.isSuccessful()) {
                                    Toast.makeText(ChangePasswordActivity.this, 
                                            getString(R.string.password_update_success), Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    String errorMessage = getString(R.string.password_update_failure);
                                    if (passwordTask.getException() != null) {
                                        errorMessage = passwordTask.getException().getMessage();
                                    }
                                    Toast.makeText(ChangePasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                    } else {
                        showProgress(false);
                        // If re-authentication fails, it's likely because the current password is incorrect
                        currentPasswordLayout.setError(getString(R.string.password_incorrect));
                    }
                });
        } else {
            showProgress(false);
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            // You might want to navigate to login screen here
        }
    }
    
    private void showProgress(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        
        // Disable/Enable interaction with form fields while processing
        currentPasswordField.setEnabled(!show);
        newPasswordField.setEnabled(!show);
        confirmPasswordField.setEnabled(!show);
        updatePasswordButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }
} 