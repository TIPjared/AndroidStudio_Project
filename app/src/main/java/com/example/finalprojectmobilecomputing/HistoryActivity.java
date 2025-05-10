package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import java.util.*;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RideLogAdapter adapter;
    private List<RideLog> rideLogs = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;
    
    private LinearLayout emptyStateLayout;
    private TextView totalRidesCount, totalDistanceCount, avgRatingCount;
    private FloatingActionButton fabFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.historyToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewHistory);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        totalRidesCount = findViewById(R.id.totalRidesCount);
        totalDistanceCount = findViewById(R.id.totalDistanceCount);
        avgRatingCount = findViewById(R.id.avgRatingCount);
        fabFilter = findViewById(R.id.fabFilter);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RideLogAdapter(rideLogs);
        recyclerView.setAdapter(adapter);

        // Setup Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            // Fetch ride logs
            fetchRideLogs();
        } else {
            // Handle not logged in state
            updateUI();
        }
        
        // Setup FAB click listener
        fabFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Implement filter functionality
        });
        
        // Start ride button (in empty state)
        findViewById(R.id.startRideButton).setOnClickListener(v -> {
            Toast.makeText(this, "Start a new ride feature coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Implement start ride functionality
        });
    }

    private void fetchRideLogs() {
        db.collection("ride_logs")
            .whereEqualTo("user_id", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                    if (error != null) {
                        Toast.makeText(HistoryActivity.this, "Error loading history", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    rideLogs.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            RideLog log = doc.toObject(RideLog.class);
                            log.setUid(doc.getId()); // Make sure to set the document ID
                            rideLogs.add(log);
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    
                    // Update UI based on ride logs
                    updateUI();
                }
            });
    }
    
    private void updateUI() {
        if (rideLogs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            
            // Update statistics
            totalRidesCount.setText(String.valueOf(rideLogs.size()));
            
            // Calculate total distance
            double totalDistance = 0;
            double totalRating = 0;
            int ratingCount = 0;
            
            for (RideLog log : rideLogs) {
                if (log.getDistance() != null) {
                    totalDistance += log.getDistance();
                }
                
                if (log.getRating() != null && log.getRating() > 0) {
                    totalRating += log.getRating();
                    ratingCount++;
                }
            }
            
            totalDistanceCount.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
            
            if (ratingCount > 0) {
                avgRatingCount.setText(String.format(Locale.getDefault(), "%.1f", totalRating / ratingCount));
            } else {
                avgRatingCount.setText("0.0");
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 