package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LogHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LogHistoryAdapter adapter;
    private List<LogHistory> ridesList;
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

        db = FirebaseFirestore.getInstance();

        // Delay to allow FirebaseAuth to restore the user session
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LogHistoryActivity.this, MainActivity  .class));
                finish();
                return;
            }

            setupUI();
            loadRideHistory();

        }, 600); // 0.6s delay to give Firebase time to rehydrate
    }

    private void setupUI() {
        ImageButton backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.ridesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateView = findViewById(R.id.emptyStateView);
        errorStateView = findViewById(R.id.errorStateView);
        totalRidesText = findViewById(R.id.totalRidesText);
        totalDistanceText = findViewById(R.id.totalDistanceText);
        totalCostText = findViewById(R.id.totalCostText);

        backButton.setOnClickListener(v -> finish());

        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            errorStateView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            loadRideHistory();
        });

        ridesList = new ArrayList<>();
        adapter = new LogHistoryAdapter(this, ridesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadRideHistory() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        errorStateView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        ridesList.clear();

        db.collection("ride_logs")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("endTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        emptyStateView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        return;
                    }

                    AtomicDouble totalCost = new AtomicDouble(0);
                    AtomicDouble totalDistance = new AtomicDouble(0);
                    AtomicInteger processed = new AtomicInteger(0);
                    int totalDocs = querySnapshot.size();

                    for (DocumentSnapshot doc : querySnapshot) {
                        LogHistory ride = new LogHistory();
                        ride.setId(doc.getId());
                        ride.setUserId(currentUser.getUid());
                        ride.setBikeId(doc.getString("bikeId"));
                        ride.setStatus("completed");

                        // Timestamp and duration
                        Timestamp startTime = doc.getTimestamp("startTime");
                        Timestamp endTime = doc.getTimestamp("endTime");
                        if (endTime != null) {
                            ride.setTimestamp(endTime.toDate());
                        } else if (startTime != null) {
                            ride.setTimestamp(startTime.toDate());
                        } else {
                            ride.setTimestamp(new Date());
                        }

                        if (startTime != null && endTime != null) {
                            long diffMs = endTime.toDate().getTime() - startTime.toDate().getTime();
                            int minutes = (int) (diffMs / (1000 * 60));
                            ride.setDuration(Math.max(minutes, 1));
                        } else {
                            ride.setDuration(1);
                        }

                        // 🔹 Calculate distance from points array
                        List<Map<String, Object>> points = (List<Map<String, Object>>) doc.get("points");
                        double distanceMeters = 0;
                        if (points != null && points.size() > 1) {
                            distanceMeters = calculateTotalDistance(points);
                        }
                        ride.setDistance(distanceMeters / 1000.0); // convert to km
                        totalDistance.addAndGet(distanceMeters / 1000.0);

                        // Payment handling
                        String paymentId = doc.getString("paymentId");
                        if (paymentId == null || paymentId.isEmpty()) {
                            ridesList.add(ride);
                            adapter.notifyDataSetChanged();
                            checkCompletion(processed, totalDocs, totalCost.get(), totalDistance.get());
                            continue;
                        }

                        db.collection("payments").document(paymentId).get()
                                .addOnSuccessListener(paymentDoc -> {
                                    if (paymentDoc.exists()) {
                                        String status = paymentDoc.getString("paymentStatus");
                                        String type = paymentDoc.getString("paymentType");
                                        String account = paymentDoc.getString("paymentAccount");
                                        Double amount = null;

                                        try {
                                            String amountStr = paymentDoc.getString("amount");
                                            if (amountStr != null) amount = Double.parseDouble(amountStr);
                                        } catch (Exception ignored) {}

                                        ride.setStatus(status != null ? status : "completed");
                                        ride.setPaymentType(type != null ? type : "N/A");
                                        ride.setPaymentAccount(account != null ? account : "N/A");
                                        ride.setCost(amount != null ? amount : 0);
                                        totalCost.addAndGet(amount != null ? amount : 0);
                                    }
                                    ridesList.add(ride);
                                    adapter.notifyDataSetChanged();
                                    checkCompletion(processed, totalDocs, totalCost.get(), totalDistance.get());
                                })
                                .addOnFailureListener(e -> {
                                    ridesList.add(ride);
                                    adapter.notifyDataSetChanged();
                                    checkCompletion(processed, totalDocs, totalCost.get(), totalDistance.get());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    errorStateView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading rides: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkCompletion(AtomicInteger processed, int total, double totalCost, double totalDistance) {
        if (processed.incrementAndGet() == total) {
            updateStats(totalCost, totalDistance);
        }
    }

    private void updateStats(double totalCost, double totalDistance) {
        progressBar.setVisibility(View.GONE);
        if (ridesList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        totalRidesText.setText(String.valueOf(ridesList.size()));
        totalDistanceText.setText(String.format("%.2f km", totalDistance));
        totalCostText.setText(String.format("₱%.0f", totalCost));
    }

    // 🔹 Distance calculation using Haversine formula
    private double calculateTotalDistance(List<Map<String, Object>> points) {
        double totalDistance = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            Map<String, Object> p1 = points.get(i);
            Map<String, Object> p2 = points.get(i + 1);

            double lat1 = Double.parseDouble(p1.get("latitude").toString());
            double lon1 = Double.parseDouble(p1.get("longitude").toString());
            double lat2 = Double.parseDouble(p2.get("latitude").toString());
            double lon2 = Double.parseDouble(p2.get("longitude").toString());

            totalDistance += haversine(lat1, lon1, lat2, lon2);
        }
        return totalDistance;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // 🔹 Helper class to allow modifying doubles inside lambdas
    public static class AtomicDouble {
        private final AtomicLong bits = new AtomicLong();

        public AtomicDouble(double initialValue) {
            set(initialValue);
        }

        public AtomicDouble() {
            this(0.0);
        }

        public final void set(double newValue) {
            bits.set(Double.doubleToRawLongBits(newValue));
        }

        public final double get() {
            return Double.longBitsToDouble(bits.get());
        }

        public final double addAndGet(double delta) {
            while (true) {
                long current = bits.get();
                double currentVal = Double.longBitsToDouble(current);
                double nextVal = currentVal + delta;
                long next = Double.doubleToRawLongBits(nextVal);
                if (bits.compareAndSet(current, next)) {
                    return nextVal;
                }
            }
        }
    }
}
