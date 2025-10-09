package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;

public class RideDurationActivity extends AppCompatActivity {

    private MaterialButton btn30Minutes, btn45Minutes, btn1Hour;
    private ImageButton backButton;
    private TextView titleText, subtitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_duration);

        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Apply professional styling
        applyStyling();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        btn30Minutes = findViewById(R.id.btn30Minutes);
        btn45Minutes = findViewById(R.id.btn45Minutes);
        btn1Hour = findViewById(R.id.btn1Hour);
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            finish(); // Go back to MainPage
        });

        // Duration selection buttons
        btn30Minutes.setOnClickListener(v -> selectDuration("30 minutes", "25.00"));
        btn45Minutes.setOnClickListener(v -> selectDuration("45 minutes", "35.00"));
        btn1Hour.setOnClickListener(v -> selectDuration("1 hour", "40.00"));
    }

    private void selectDuration(String duration, String amount) {
        // Save to AppData
        AppData.getInstance().setRideTime(duration);
        AppData.getInstance().setAmount(amount);
        // Navigate to Ride Details
        Intent intent = new Intent(RideDurationActivity.this, RideDetailsActivity.class);
        startActivity(intent);
        finish(); // Finish this activity to prevent going back
    }

    private void applyStyling() {
        // Set professional colors and styling
        getWindow().setStatusBarColor(Color.parseColor("#2E7D32"));
        
        // Apply button styling with ripple effects
        btn30Minutes.setBackgroundTintList(getColorStateList(R.color.colorPrimary));
        btn45Minutes.setBackgroundTintList(getColorStateList(R.color.colorPrimary));
        btn1Hour.setBackgroundTintList(getColorStateList(R.color.colorPrimary));
        
        // Add elevation and corner radius
        btn30Minutes.setElevation(8);
        btn45Minutes.setElevation(8);
        btn1Hour.setElevation(8);
    }
}
