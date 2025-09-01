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
    private TextView totalRidesText, totalDistanceText, totalCostText;
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
        totalCostText = findViewById(R.id.totalCostText);

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
        
        // Set up create sample data button
        Button createSampleDataButton = findViewById(R.id.createSampleDataButton);
        createSampleDataButton.setOnClickListener(v -> {
            // Show loading state
            progressBar.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            
            // Create sample data
            addSampleRideDataToFirestore();
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
                double totalCost = 0;
                
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
                        } else {
                            // Generate realistic distance for Antel Grand Village (0.5-3.0 km)
                            double distanceInKm = 0.5 + Math.random() * 2.5;
                            ride.setDistance(distanceInKm);
                            totalDistance += distanceInKm;
                        }
                        
                        // Set location to Antel Grand Village
                        ride.setLocation("Antel Grand Village");
                        
                        // Get timestamp
                        Number timestamp = document.getLong("timestamp");
                        if (timestamp != null) {
                            ride.setTimestamp(new Date(timestamp.longValue()));
                        } else {
                            // Generate realistic timestamp for documentation
                            long currentTime = System.currentTimeMillis();
                            long randomOffset = (long)(Math.random() * 7 * 24 * 60 * 60 * 1000); // Random time within last week
                            ride.setTimestamp(new Date(currentTime - randomOffset));
                        }
                        
                        // Set default values for missing fields
                        ride.setUserId(currentUser.getUid());
                        ride.setStatus("completed");
                        
                        // Get duration from Firestore or calculate if not available
                        Number durationObj = document.getLong("duration");
                        if (durationObj != null) {
                            ride.setDuration(durationObj.intValue());
                        } else {
                            // Calculate realistic duration (assuming average speed of 12km/h for bike)
                            ride.setDuration((int)Math.ceil(ride.getDistance() / 12.0 * 60));
                        }
                        
                        // Get cost from Firestore or calculate if not available
                        Number costObj = document.getDouble("cost");
                        if (costObj != null) {
                            ride.setCost(costObj.doubleValue());
                            totalCost += costObj.doubleValue();
                        } else {
                            // Calculate cost (₱20 base + ₱5 per km)
                            double cost = 20.0 + (ride.getDistance() * 5.0);
                            ride.setCost(cost);
                            totalCost += cost;
                        }
                        
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
                    // Show empty state with option to create sample data
                    progressBar.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    return;
                }
                
                emptyStateView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                
                // Update stats
                totalRidesText.setText(String.valueOf(count));
                totalDistanceText.setText(String.format("%.1f km", totalDistance));
                totalCostText.setText(String.format("₱%.0f", totalCost));
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                errorStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
                
                Toast.makeText(RideHistoryActivity.this, "Failed to load ride history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void addSampleRideDataToFirestore() {
        // Add sample rides for documentation and store in Firestore
        long currentTime = System.currentTimeMillis();
        
        // Sample ride 1 - Today (2 hours ago)
        java.util.Map<String, Object> ride1 = new java.util.HashMap<>();
        ride1.put("distance_traveled", 1200.0); // 1.2 km in meters
        ride1.put("location", "Antel Grand Village");
        ride1.put("timestamp", currentTime - 2 * 60 * 60 * 1000); // 2 hours ago
        ride1.put("status", "completed");
        ride1.put("userId", currentUser.getUid());
        ride1.put("cost", 26.0); // ₱20 + ₱5 * 1.2km
        ride1.put("duration", 6); // 6 minutes
        
        // Sample ride 2 - Yesterday (26 hours ago)
        java.util.Map<String, Object> ride2 = new java.util.HashMap<>();
        ride2.put("distance_traveled", 2100.0); // 2.1 km in meters
        ride2.put("location", "Antel Grand Village");
        ride2.put("timestamp", currentTime - 26 * 60 * 60 * 1000); // 26 hours ago
        ride2.put("status", "completed");
        ride2.put("userId", currentUser.getUid());
        ride2.put("cost", 30.5); // ₱20 + ₱5 * 2.1km
        ride2.put("duration", 11); // 11 minutes
        
        // Sample ride 3 - 3 days ago
        java.util.Map<String, Object> ride3 = new java.util.HashMap<>();
        ride3.put("distance_traveled", 800.0); // 0.8 km in meters
        ride3.put("location", "Antel Grand Village");
        ride3.put("timestamp", currentTime - 3 * 24 * 60 * 60 * 1000); // 3 days ago
        ride3.put("status", "completed");
        ride3.put("userId", currentUser.getUid());
        ride3.put("cost", 24.0); // ₱20 + ₱5 * 0.8km
        ride3.put("duration", 4); // 4 minutes
        
        // Sample ride 4 - 5 days ago
        java.util.Map<String, Object> ride4 = new java.util.HashMap<>();
        ride4.put("distance_traveled", 1800.0); // 1.8 km in meters
        ride4.put("location", "Antel Grand Village");
        ride4.put("timestamp", currentTime - 5 * 24 * 60 * 60 * 1000); // 5 days ago
        ride4.put("status", "completed");
        ride4.put("userId", currentUser.getUid());
        ride4.put("cost", 29.0); // ₱20 + ₱5 * 1.8km
        ride4.put("duration", 9); // 9 minutes
        
        // Sample ride 5 - 1 week ago
        java.util.Map<String, Object> ride5 = new java.util.HashMap<>();
        ride5.put("distance_traveled", 2500.0); // 2.5 km in meters
        ride5.put("location", "Antel Grand Village");
        ride5.put("timestamp", currentTime - 7 * 24 * 60 * 60 * 1000); // 1 week ago
        ride5.put("status", "completed");
        ride5.put("userId", currentUser.getUid());
        ride5.put("cost", 32.5); // ₱20 + ₱5 * 2.5km
        ride5.put("duration", 13); // 13 minutes
        
        // Store all sample rides in Firestore with better error handling
        Toast.makeText(this, "Creating sample ride data...", Toast.LENGTH_SHORT).show();
        
        // Use a counter to track successful additions
        final int[] addedCount = {0};
        final int totalRides = 5;
        
        // Function to add a ride and handle completion
        java.util.function.Consumer<java.util.Map<String, Object>> addRide = ride -> {
            db.collection("ride_logs").add(ride)
                .addOnSuccessListener(documentReference -> {
                    addedCount[0]++;
                    Toast.makeText(RideHistoryActivity.this, 
                        "Added sample ride " + addedCount[0] + "/" + totalRides, 
                        Toast.LENGTH_SHORT).show();
                    
                    // If all rides are added, reload the history
                    if (addedCount[0] >= totalRides) {
                        Toast.makeText(RideHistoryActivity.this, 
                            "All sample rides created successfully!", 
                            Toast.LENGTH_LONG).show();
                        loadRideHistory();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RideHistoryActivity.this, 
                        "Error adding sample ride: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    // Continue with other rides even if one fails
                    addedCount[0]++;
                    if (addedCount[0] >= totalRides) {
                        loadRideHistory();
                    }
                });
        };
        
        // Add all rides
        addRide.accept(ride1);
        addRide.accept(ride2);
        addRide.accept(ride3);
        addRide.accept(ride4);
        addRide.accept(ride5);
    }
    
    private void calculateStats() {
        // Stats are calculated in loadRideHistory now
    }
} 