package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RideHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private List<RideHistory> ridesList;
    private ProgressBar progressBar;
    private View emptyStateView;
    private View errorStateView;
    private TextView totalRidesText, totalDistanceText, avgRatingText;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view ride history", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        ImageButton backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.ridesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateView = findViewById(R.id.emptyStateView);
        errorStateView = findViewById(R.id.errorStateView);
        totalRidesText = findViewById(R.id.totalRidesText);
        totalDistanceText = findViewById(R.id.totalDistanceText);
        avgRatingText = findViewById(R.id.avgRatingText);

        // Set up back button
        backButton.setOnClickListener(v -> finish());
        
        // Set up retry button
        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            errorStateView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            loadRideHistory();
            calculateStats();
        });
        
        // Set up start ride button in empty state
        Button startRideButton = findViewById(R.id.startRideButton);
        startRideButton.setOnClickListener(v -> {
            finish(); // Return to map screen to start a ride
        });

        // Initialize ride list and adapter
        ridesList = new ArrayList<>();
        adapter = new RideHistoryAdapter(this, ridesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load ride history data
        loadRideHistory();
    }

    private void loadRideHistory() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        errorStateView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        
        // Clear existing ride list
        ridesList.clear();

        db.collection("ride_logs")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = 0;
                double totalDistance = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Create a RideHistory object from ride_logs format
                        RideHistory ride = new RideHistory();
                        ride.setId(document.getId());
                        
                        // Get distance_traveled and convert from meters to km
                        Number distanceObj = document.getDouble("distance_traveled");
                        if (distanceObj != null) {
                            double distanceInMeters = distanceObj.doubleValue();
                            double distanceInKm = distanceInMeters / 1000.0;
                            ride.setDistance(distanceInKm);
                            totalDistance += distanceInKm;
                        }
                        
                        // Get location string
                        String location = document.getString("location");
                        if (location != null) {
                            ride.setLocation(location);
                        } else {
                            ride.setLocation("Unknown Location");
                        }
                        
                        // Get timestamp
                        Number timestamp = document.getLong("timestamp");
                        if (timestamp != null) {
                            ride.setTimestamp(new Date(timestamp.longValue()));
                        }
                        
                        // Set default values for missing fields
                        ride.setUserId(currentUser.getUid());
                        ride.setStatus("completed");
                        
                        // Calculate approximate duration (assuming average speed of 15km/h)
                        ride.setDuration((int)Math.ceil(ride.getDistance() / 15.0 * 60));
                        
                        ridesList.add(ride);
                        count++;
                    } catch (Exception e) {
                        // Skip this record if there's an error
                        continue;
                    }
                }
                
                // Update UI with results
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                
                if (ridesList.isEmpty()) {
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    
                    // Update stats
                    totalRidesText.setText(String.valueOf(count));
                    totalDistanceText.setText(String.format("%.1f km", totalDistance));
                    avgRatingText.setText("N/A");
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                errorStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
                
                Toast.makeText(RideHistoryActivity.this, "Failed to load ride history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void calculateStats() {
        // Stats are calculated in loadRideHistory now
    }
} 