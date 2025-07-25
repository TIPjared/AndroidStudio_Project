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

    private TextView profileName, profileEmail;
    private EditText emailEditText, usernameEditText, ageEditText, genderEditText;
    private LinearLayout changePasswordOption, privacySettingsOption, logoutOption;
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

        // Set click listeners
        findViewById(R.id.saveButton).setOnClickListener(v -> saveUserData());

        findViewById(R.id.returnButton).setOnClickListener(v -> {
            finish();
        });

        // Profile Image Click Listener
        profileImageCard.setOnClickListener(v -> showImagePickerDialog());
        
        // Camera overlay click listener - same action as clicking the profile image
        View cameraOverlay = findViewById(R.id.cameraOverlay);
        if (cameraOverlay != null) {
            cameraOverlay.setOnClickListener(v -> showImagePickerDialog());
        }

        // Additional options
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
        
        // Form fields
        emailEditText = findViewById(R.id.emailEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        ageEditText = findViewById(R.id.ageEditText);
        genderEditText = findViewById(R.id.genderEditText);
        
        // Options
        changePasswordOption = findViewById(R.id.changePasswordOption);
        privacySettingsOption = findViewById(R.id.privacySettingsOption);
        logoutOption = findViewById(R.id.logoutOption);

        // Profile Image
        profileImage = findViewById(R.id.profileImage);
        profileImageCard = findViewById(R.id.profileImageCard);
    }

    private void loadUserData() {
        // Set the email address from Firebase Authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            String email = currentUser.getEmail();
            emailEditText.setText(email);
            profileEmail.setText(email);
        }

        // Load other user data from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String age = documentSnapshot.getString("age");
                String gender = documentSnapshot.getString("gender");
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                if (username != null && !username.isEmpty()) {
                    profileName.setText(username);
                    usernameEditText.setText(username);
                }
                
                ageEditText.setText(age != null ? age : "");
                genderEditText.setText(gender != null ? gender : "");

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

    private void saveUserData() {
        String username = usernameEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        String gender = genderEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("age", age);
        userData.put("gender", gender);

        userRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    profileName.setText(username);
                    Toast.makeText(Profile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(Profile.this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take Photo option
                checkCameraPermission();
            } else if (which == 1) {
                // Choose from Gallery option
                checkStoragePermission();
            } else {
                // Cancel option
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, use READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            // For Android 12 and below, use READ_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                // Get image from camera
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        profileImage.setImageBitmap(imageBitmap);
                        uploadImageToFirebase(imageBitmap);
                    }
                }
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                // Get image from gallery
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        profileImage.setImageBitmap(bitmap);
                        uploadImageToFirebase(selectedImageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap) {
        try {
            // Resize bitmap to reduce file size
            bitmap = getResizedBitmap(bitmap, 800); // Max width/height of 800px
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Use higher compression (lower quality) to reduce file size
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();
            
            Toast.makeText(Profile.this, "Processing image...", Toast.LENGTH_SHORT).show();
            uploadImageData(data);
        } catch (Exception e) {
            Toast.makeText(Profile.this, "Error processing image: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to resize bitmap while maintaining aspect ratio
    private Bitmap getResizedBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap; // No need to resize
        }
        
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            // Width is greater than height
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // Height is greater than width
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri != null) {
            try {
                // Debug information
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(Profile.this, "ERROR: Not logged in", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    // Show user ID to verify authentication
                    Toast.makeText(Profile.this, "User ID: " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
                }

                // Show uploading message
                Toast.makeText(Profile.this, "Starting upload...", Toast.LENGTH_SHORT).show();
                
                // Create a simple path without spaces
                StorageReference profileImagesRef = storageRef.child("profiles/" + currentUser.getUid() + "/profile.jpg");
                
                // Set upload options
                StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();
                
                // Show a progress dialog
                AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle("Uploading")
                    .setMessage("Uploading profile picture...")
                    .setCancelable(false)
                    .create();
                progressDialog.show();
                
                // Start the upload
                UploadTask uploadTask = profileImagesRef.putFile(imageUri, metadata);
                
                // Add progress listener for debugging
                uploadTask.addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setMessage("Upload progress: " + (int)progress + "%");
                });
                
                // Success listener
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(Profile.this, "Upload completed, getting URL...", Toast.LENGTH_SHORT).show();
                    
                    // Get download URL
                    profileImagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Image URL retrieved", Toast.LENGTH_SHORT).show();
                        saveProfileImageUrl(uri.toString());
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
                
                // Failure listener with detailed error information
                uploadTask.addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    
                    String errorMessage = "Upload failed: " + e.getMessage();
                    if (e instanceof StorageException) {
                        StorageException storageException = (StorageException) e;
                        int errorCode = storageException.getErrorCode();
                        
                        switch(errorCode) {
                            case StorageException.ERROR_NOT_AUTHENTICATED:
                                errorMessage = "ERROR: User not authenticated";
                                break;
                            case StorageException.ERROR_NOT_AUTHORIZED:
                                errorMessage = "ERROR: Not authorized. Check Firebase rules";
                                break;
                            case StorageException.ERROR_QUOTA_EXCEEDED:
                                errorMessage = "ERROR: Storage quota exceeded";
                                break;
                            case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                                errorMessage = "ERROR: Retry limit exceeded. Check connection";
                                break;
                            default:
                                errorMessage = "ERROR: Storage error " + errorCode + ": " + e.getMessage();
                        }
                    }
                    
                    // Show detailed error
                    Toast.makeText(Profile.this, errorMessage, Toast.LENGTH_LONG).show();
                    
                    // Log error for debugging
                    Log.e("ProfileUpload", errorMessage, e);
                });
            } catch (Exception e) {
                // Log general exceptions
                Log.e("ProfileUpload", "General error: " + e.getMessage(), e);
                Toast.makeText(Profile.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void uploadImageData(byte[] data) {
        try {
            // Debug information
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(Profile.this, "ERROR: Not logged in", Toast.LENGTH_LONG).show();
                return;
            } else {
                // Show user ID to verify authentication
                Toast.makeText(Profile.this, "User ID: " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
            }
            
            // Show uploading message
            Toast.makeText(Profile.this, "Starting upload from camera...", Toast.LENGTH_SHORT).show();
            
            // Create a simple path without spaces
            StorageReference profileImagesRef = storageRef.child("profiles/" + currentUser.getUid() + "/profile.jpg");
            
            // Set metadata
            StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
            
            // Show a progress dialog
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Uploading")
                .setMessage("Uploading profile picture...")
                .setCancelable(false)
                .create();
            progressDialog.show();
            
            // Start upload with bytes
            UploadTask uploadTask = profileImagesRef.putBytes(data, metadata);
            
            // Add progress listener
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressDialog.setMessage("Upload progress: " + (int)progress + "%");
            });
            
            // Success listener
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(Profile.this, "Upload completed, getting URL...", Toast.LENGTH_SHORT).show();
                
                // Get download URL
                profileImagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    progressDialog.dismiss();
                    Toast.makeText(Profile.this, "Image URL retrieved", Toast.LENGTH_SHORT).show();
                    saveProfileImageUrl(uri.toString());
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(Profile.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            });
            
            // Failure listener
            uploadTask.addOnFailureListener(e -> {
                progressDialog.dismiss();
                
                String errorMessage = "Upload failed: " + e.getMessage();
                if (e instanceof StorageException) {
                    StorageException storageException = (StorageException) e;
                    int errorCode = storageException.getErrorCode();
                    
                    switch(errorCode) {
                        case StorageException.ERROR_NOT_AUTHENTICATED:
                            errorMessage = "ERROR: User not authenticated";
                            break;
                        case StorageException.ERROR_NOT_AUTHORIZED:
                            errorMessage = "ERROR: Not authorized. Check Firebase rules";
                            break;
                        case StorageException.ERROR_QUOTA_EXCEEDED:
                            errorMessage = "ERROR: Storage quota exceeded";
                            break;
                        case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                            errorMessage = "ERROR: Retry limit exceeded. Check connection";
                            break;
                        default:
                            errorMessage = "ERROR: Storage error " + errorCode + ": " + e.getMessage();
                    }
                }
                
                // Show detailed error
                Toast.makeText(Profile.this, errorMessage, Toast.LENGTH_LONG).show();
                
                // Log error for debugging
                Log.e("ProfileUpload", errorMessage, e);
            });
        } catch (Exception e) {
            // Log general exceptions
            Log.e("ProfileUpload", "General error: " + e.getMessage(), e);
            Toast.makeText(Profile.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveProfileImageUrl(String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("profileImageUrl", imageUrl);
            
            db.collection("users").document(currentUser.getUid())
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Profile.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Profile.this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
