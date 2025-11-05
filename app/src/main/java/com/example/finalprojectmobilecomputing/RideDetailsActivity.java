package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RideDetailsActivity extends AppCompatActivity {

    private TextView greetingText;
    private TextView durationText;
    private TextView amountText;
    private MaterialButton continueButton;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_details);

        initializeViews();
        setupClickListeners();
        loadUserData();
        updateRideDetails();
    }

    private void initializeViews() {
        greetingText = findViewById(R.id.greetingText);
        durationText = findViewById(R.id.durationText);
        amountText = findViewById(R.id.amountText);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(RideDetailsActivity.this, QRScannerActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Extract username from email as fallback
            String emailUsername = MainPage.getUsernameFromEmail(currentUser.getEmail());
            
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        // Use Firestore name if available, otherwise email username
                        String displayName = (name != null && !name.isEmpty()) ? name : emailUsername;
                        greetingText.setText("Hi, " + displayName);
                    } else {
                        // Use email username if Firestore document doesn't exist
                        greetingText.setText("Hi, " + emailUsername);
                    }
                })
                .addOnFailureListener(e -> {
                    // Use email username on error
                    greetingText.setText("Hi, " + emailUsername);
                });
        }
    }

    private void updateRideDetails() {
        String duration = AppData.getInstance().getRideTime();
        if (duration != null) {
            switch (duration) {
                case "30 minutes":
                    durationText.setText("30 min");
                    amountText.setText("₱25");
                    break;
                case "45 minutes":
                    durationText.setText("45 min");
                    amountText.setText("₱35");
                    break;
                case "1 hour":
                    durationText.setText("1 hour");
                    amountText.setText("₱40");
                    break;
            }
        }
    }
}