package com.example.finalprojectmobilecomputing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 102;
    private static final int CAMERA_REQUEST_CODE = 103;
    private static final int GALLERY_REQUEST_CODE = 104;

    private TextView profileName, profileEmail, profilePhone;
    private LinearLayout editProfileOption, changePasswordOption, privacySettingsOption, logoutOption;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private ImageView profileImage;
    private CardView profileImageCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize UI elements
        initializeUI();

        // Initialize Firebase explicitly
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        
        // Verify authentication status
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Re-authenticate the user token to ensure it's fresh
            currentUser.getIdToken(true)
                .addOnSuccessListener(result -> {
                    Log.d("ProfileAuth", "Token refreshed successfully");
                    userRef = db.collection("users").document(currentUser.getUid());
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileAuth", "Failed to refresh token: " + e.getMessage());
                    Toast.makeText(this, "Authentication error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Still try to load user data
                    userRef = db.collection("users").document(currentUser.getUid());
                    loadUserData();
                });
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            // Redirect to login screen if not logged in
            startActivity(new Intent(Profile.this, MainActivity.class));
            finish();
        }

        // Additional options
        editProfileOption.setOnClickListener(v -> {
            // Launch edit profile activity
            Intent intent = new Intent(Profile.this, EditProfileActivity.class);
            startActivity(intent);
        });

        changePasswordOption.setOnClickListener(v -> {
            // Launch change password activity
            Intent intent = new Intent(Profile.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        privacySettingsOption.setOnClickListener(v -> {
            // Launch privacy settings activity
            Intent intent = new Intent(Profile.this, PrivacySettingsActivity.class);
            startActivity(intent);
        });

        logoutOption.setOnClickListener(v -> {
            // Log out the user
            mAuth.signOut();
            Toast.makeText(Profile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate to login screen
            Intent intent = new Intent(Profile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void initializeUI() {
        // User info
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePhone = findViewById(R.id.profilePhone);
        
        // Options
        editProfileOption = findViewById(R.id.editProfileOption);
        changePasswordOption = findViewById(R.id.changePasswordOption);
        privacySettingsOption = findViewById(R.id.privacySettingsOption);
        logoutOption = findViewById(R.id.logoutOption);

        // Profile Image (non-clickable in main profile view)
        profileImage = findViewById(R.id.profileImage);
        profileImageCard = findViewById(R.id.profileImageCard);
    }

    private void loadUserData() {
        // Set the email address from Firebase Authentication immediately
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            String email = currentUser.getEmail();
            profileEmail.setText(email);
        }

        // Load cached data first for instant display
        if (MainPage.dataLoaded) {
            profileName.setText(MainPage.cachedUsername.isEmpty() ? "User Name" : MainPage.cachedUsername);
            profilePhone.setText(MainPage.cachedPhone.isEmpty() ? "Phone not set" : MainPage.cachedPhone);
            
            if (!MainPage.cachedProfileImageUrl.isEmpty()) {
                Picasso.get().load(MainPage.cachedProfileImageUrl)
                        .placeholder(R.drawable.baseline_person_outline_24)
                        .error(R.drawable.baseline_person_outline_24)
                        .into(profileImage);
            }
        }

        // Load fresh data from Firestore in background
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String phone = documentSnapshot.getString("phone");
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                // Update cache
                MainPage.cachedUsername = username != null ? username : "";
                MainPage.cachedPhone = phone != null ? phone : "";
                MainPage.cachedProfileImageUrl = profileImageUrl != null ? profileImageUrl : "";
                MainPage.dataLoaded = true;

                // Update UI with fresh data
                if (username != null && !username.isEmpty()) {
                    profileName.setText(username);
                }
                
                if (phone != null && !phone.isEmpty()) {
                    profilePhone.setText(phone);
                } else {
                    profilePhone.setText("Phone not set");
                }

                // Load profile image if exists
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Picasso.get().load(profileImageUrl)
                            .placeholder(R.drawable.baseline_person_outline_24)
                            .error(R.drawable.baseline_person_outline_24)
                            .into(profileImage);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(Profile.this, "Failed to load data", Toast.LENGTH_SHORT).show();
        });
    }

}
