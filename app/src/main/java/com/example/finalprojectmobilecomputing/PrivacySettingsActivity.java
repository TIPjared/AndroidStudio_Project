package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrivacySettingsActivity extends AppCompatActivity {

    // Constants for SharedPreferences
    private static final String PREFS_NAME = "PedalGoPrivacyPrefs";
    private static final String KEY_SHARE_RIDE_HISTORY = "share_ride_history";
    private static final String KEY_SHOW_ON_LEADERBOARDS = "show_on_leaderboards";
    private static final String KEY_SHARE_ACHIEVEMENTS = "share_achievements";
    private static final String KEY_BACKGROUND_LOCATION = "background_location";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private static final String KEY_EMAIL_NOTIFICATIONS = "email_notifications";
    private static final String KEY_MARKETING_COMMUNICATIONS = "marketing_communications";
    private static final String KEY_USAGE_ANALYTICS = "usage_analytics";

    // UI Components
    private SwitchMaterial shareRideHistorySwitch;
    private SwitchMaterial showOnLeaderboardsSwitch;
    private SwitchMaterial shareAchievementsSwitch;
    private SwitchMaterial backgroundLocationSwitch;
    private SwitchMaterial pushNotificationsSwitch;
    private SwitchMaterial emailNotificationsSwitch;
    private SwitchMaterial marketingCommunicationsSwitch;
    private SwitchMaterial usageAnalyticsSwitch;
    private MaterialButton clearRideHistoryButton;
    private MaterialButton downloadDataButton;
    private LinearLayout privacyPolicyOption;
    private LinearLayout termsOfServiceOption;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_settings);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PrivacySettingsActivity.this, MainActivity.class));
            finish();
            return;
        }
        
        // Initialize UI components
        initializeUI();
        
        // Load saved preferences
        loadSavedPreferences();
        
        // Set up click listeners
        setupListeners();
    }
    
    private void initializeUI() {
        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        
        // Switches
        shareRideHistorySwitch = findViewById(R.id.shareRideHistorySwitch);
        showOnLeaderboardsSwitch = findViewById(R.id.showOnLeaderboardsSwitch);
        shareAchievementsSwitch = findViewById(R.id.shareAchievementsSwitch);
        backgroundLocationSwitch = findViewById(R.id.backgroundLocationSwitch);
        pushNotificationsSwitch = findViewById(R.id.pushNotificationsSwitch);
        emailNotificationsSwitch = findViewById(R.id.emailNotificationsSwitch);
        marketingCommunicationsSwitch = findViewById(R.id.marketingCommunicationsSwitch);
        usageAnalyticsSwitch = findViewById(R.id.usageAnalyticsSwitch);
        
        // Buttons
        clearRideHistoryButton = findViewById(R.id.clearRideHistoryButton);
        downloadDataButton = findViewById(R.id.downloadDataButton);
        
        // Click options
        privacyPolicyOption = findViewById(R.id.privacyPolicyOption);
        termsOfServiceOption = findViewById(R.id.termsOfServiceOption);
    }
    
    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Load saved preferences with default values
        shareRideHistorySwitch.setChecked(prefs.getBoolean(KEY_SHARE_RIDE_HISTORY, false));
        showOnLeaderboardsSwitch.setChecked(prefs.getBoolean(KEY_SHOW_ON_LEADERBOARDS, true));
        shareAchievementsSwitch.setChecked(prefs.getBoolean(KEY_SHARE_ACHIEVEMENTS, false));
        backgroundLocationSwitch.setChecked(prefs.getBoolean(KEY_BACKGROUND_LOCATION, true));
        pushNotificationsSwitch.setChecked(prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true));
        emailNotificationsSwitch.setChecked(prefs.getBoolean(KEY_EMAIL_NOTIFICATIONS, true));
        marketingCommunicationsSwitch.setChecked(prefs.getBoolean(KEY_MARKETING_COMMUNICATIONS, false));
        usageAnalyticsSwitch.setChecked(prefs.getBoolean(KEY_USAGE_ANALYTICS, true));
    }
    
    private void setupListeners() {
        // Save preference when each switch changes
        shareRideHistorySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_SHARE_RIDE_HISTORY, isChecked));
        
        showOnLeaderboardsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_SHOW_ON_LEADERBOARDS, isChecked));
        
        shareAchievementsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_SHARE_ACHIEVEMENTS, isChecked));
        
        backgroundLocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_BACKGROUND_LOCATION, isChecked));
        
        pushNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_PUSH_NOTIFICATIONS, isChecked));
        
        emailNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_EMAIL_NOTIFICATIONS, isChecked));
        
        marketingCommunicationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_MARKETING_COMMUNICATIONS, isChecked));
        
        usageAnalyticsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                savePreference(KEY_USAGE_ANALYTICS, isChecked));
        
        // Button click listeners
        clearRideHistoryButton.setOnClickListener(v -> showClearHistoryConfirmation());
        downloadDataButton.setOnClickListener(v -> downloadUserData());
        
        // Legal information options
        privacyPolicyOption.setOnClickListener(v -> openWebPage("https://pedalgo.com/privacy-policy"));
        termsOfServiceOption.setOnClickListener(v -> openWebPage("https://pedalgo.com/terms-of-service"));
    }
    
    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
        
        // Show a toast message to confirm the setting was changed
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }
    
    private void showClearHistoryConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Ride History")
                .setMessage("This will permanently delete all your ride history data. This action cannot be undone. Are you sure you want to continue?")
                .setPositiveButton("Clear History", (dialog, which) -> clearRideHistory())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    private void clearRideHistory() {
        // Delete all ride history for the current user from Firestore
        db.collection("ride_logs")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                            count++;
                        }
                        
                        Toast.makeText(PrivacySettingsActivity.this, 
                                "Cleared " + count + " ride records", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PrivacySettingsActivity.this, 
                                "Failed to clear history: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void downloadUserData() {
        // Show a toast to inform user that data download is starting
        Toast.makeText(this, "Preparing your data for download...", Toast.LENGTH_SHORT).show();
        
        // Query all user-related data from Firebase
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    // Get ride history for the user
                    db.collection("ride_logs")
                            .whereEqualTo("user_id", userId)
                            .get()
                            .addOnSuccessListener(rideDocuments -> {
                                // Create a CSV string with all user data
                                StringBuilder csvData = new StringBuilder();
                                
                                // Add user data
                                csvData.append("USER DATA\n");
                                csvData.append("Email,").append(mAuth.getCurrentUser().getEmail()).append("\n");
                                if (userDocument.exists()) {
                                    if (userDocument.contains("username")) {
                                        csvData.append("Username,").append(userDocument.getString("username")).append("\n");
                                    }
                                    if (userDocument.contains("age")) {
                                        csvData.append("Age,").append(userDocument.getString("age")).append("\n");
                                    }
                                    if (userDocument.contains("gender")) {
                                        csvData.append("Gender,").append(userDocument.getString("gender")).append("\n");
                                    }
                                }
                                csvData.append("\n");
                                
                                // Add ride history data
                                csvData.append("RIDE HISTORY\n");
                                csvData.append("Date,Time,Location,Distance (km),Duration (min)\n");
                                
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                
                                for (QueryDocumentSnapshot rideDoc : rideDocuments) {
                                    try {
                                        // Extract ride data
                                        Long timestamp = rideDoc.getLong("timestamp");
                                        String location = rideDoc.getString("location");
                                        Double distance = rideDoc.getDouble("distance_traveled");
                                        
                                        if (timestamp != null) {
                                            Date rideDate = new Date(timestamp);
                                            String dateStr = dateFormat.format(rideDate);
                                            String timeStr = timeFormat.format(rideDate);
                                            
                                            double distanceKm = (distance != null) ? distance / 1000.0 : 0;
                                            int durationMin = (int) Math.ceil(distanceKm / 15.0 * 60); // Rough calculation
                                            
                                            csvData.append(dateStr).append(",")
                                                    .append(timeStr).append(",")
                                                    .append(location != null ? location : "Unknown").append(",")
                                                    .append(String.format(Locale.US, "%.2f", distanceKm)).append(",")
                                                    .append(durationMin).append("\n");
                                        }
                                    } catch (Exception e) {
                                        // Skip this record if there was an error
                                    }
                                }
                                
                                // Save the CSV file
                                saveDataToFile(csvData.toString());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(PrivacySettingsActivity.this, 
                                        "Failed to download ride data: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PrivacySettingsActivity.this, 
                            "Failed to download user data: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void saveDataToFile(String data) {
        try {
            // Create filename with current date
            String fileName = "PedalGo_Data_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
            
            // Get the Downloads directory
            File downloadsDir = new File(getExternalFilesDir(null), "Downloads");
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            
            File file = new File(downloadsDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(data);
            writer.flush();
            writer.close();
            
            Toast.makeText(this, "Data saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No browser found to open the link", Toast.LENGTH_SHORT).show();
        }
    }
} 