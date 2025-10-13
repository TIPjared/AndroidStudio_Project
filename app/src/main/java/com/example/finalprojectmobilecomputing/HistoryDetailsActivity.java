package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private TextView durationText, distanceText, amountText, statusText, methodText;
    private TextView startTimeText, endTimeText, avgSpeedText;

    private String rideId;

    // Animation
    private Handler polylineHandler;
    private Runnable polylineRunnable;
    private List<LatLng> polylinePoints;
    private com.google.android.gms.maps.model.Polyline animatedPolyline;
    private Marker movingMarker;
    private int polylineIndex = 0;
    private boolean isAnimating = false;

    // Loading overlay
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_details);

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Top-right menu button
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(this::showPopupMenu);

        // Initialize MapView
        mapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Prevent scroll conflict with MapView
        mapView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        durationText = findViewById(R.id.durationText);
        distanceText = findViewById(R.id.distanceText);
        amountText = findViewById(R.id.amountText);
        statusText = findViewById(R.id.statusText);
        methodText = findViewById(R.id.methodText);

        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);
        avgSpeedText = findViewById(R.id.avgSpeedText);

        rideId = getIntent().getStringExtra("RIDE_ID");
        if (rideId != null) loadRideDetails(rideId);
        else Toast.makeText(this, "Ride ID not found", Toast.LENGTH_SHORT).show();

        setupLoadingOverlay();
    }

    // ================= Map & Ride Loading =================
    private void loadRideDetails(String rideId) {
        FirebaseFirestore.getInstance()
                .collection("ride_logs")
                .document(rideId)
                .get()
                .addOnSuccessListener(this::displayRideRoute)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load ride: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void displayRideRoute(DocumentSnapshot rideDoc) {
        if (rideDoc == null || !rideDoc.exists()) return;

        List<Map<String, Object>> points = (List<Map<String, Object>>) rideDoc.get("points");
        double totalDistanceKm = 0.0;

        if (points != null && !points.isEmpty()) {
            polylinePoints = new ArrayList<>();
            for (Map<String, Object> point : points) {
                double lat = Double.parseDouble(point.get("latitude").toString());
                double lng = Double.parseDouble(point.get("longitude").toString());
                polylinePoints.add(new LatLng(lat, lng));
            }

            if (gMap != null && !polylinePoints.isEmpty()) {
                gMap.clear();

                // Start marker
                gMap.addMarker(new MarkerOptions()
                        .position(polylinePoints.get(0))
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                );

                // Animated polyline
                PolylineOptions polylineOptions = new PolylineOptions()
                        .color(getResources().getColor(R.color.sikad_green, null))
                        .width(12f)
                        .jointType(JointType.ROUND)
                        .startCap(new RoundCap())
                        .endCap(new RoundCap());
                animatedPolyline = gMap.addPolyline(polylineOptions);

                // Moving marker
                movingMarker = gMap.addMarker(new MarkerOptions()
                        .position(polylinePoints.get(0))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title("Riding...")
                );

                // Fit camera
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng point : polylinePoints) builder.include(point);
                gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));

                // Start animation
                startPolylineAnimation();
            }

            // Compute total distance
            for (int i = 1; i < polylinePoints.size(); i++) {
                totalDistanceKm += distanceBetween(polylinePoints.get(i - 1), polylinePoints.get(i));
            }
            distanceText.setText(String.format("%.2f km", totalDistanceKm));
        } else distanceText.setText("-");

        // Duration, Start/End Time, Average Speed
        try {
            long startMillis = ((com.google.firebase.Timestamp) rideDoc.get("startTime")).toDate().getTime();
            long endMillis = ((com.google.firebase.Timestamp) rideDoc.get("endTime")).toDate().getTime();
            long minutes = (endMillis - startMillis) / (1000 * 60);
            durationText.setText(minutes + " min");

            double hours = minutes / 60.0;
            if (hours > 0 && totalDistanceKm > 0) {
                avgSpeedText.setText(String.format(Locale.getDefault(), "%.2f km/h", totalDistanceKm / hours));
            } else {
                avgSpeedText.setText("-");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            startTimeText.setText(sdf.format(((com.google.firebase.Timestamp) rideDoc.get("startTime")).toDate()));
            endTimeText.setText(sdf.format(((com.google.firebase.Timestamp) rideDoc.get("endTime")).toDate()));

        } catch (Exception e) {
            durationText.setText("-");
            avgSpeedText.setText("-");
            startTimeText.setText("-");
            endTimeText.setText("-");
        }

        // Payment
        String paymentId = rideDoc.getString("paymentId");
        if (paymentId != null && !paymentId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("payments")
                    .document(paymentId)
                    .get()
                    .addOnSuccessListener(paymentDoc -> {
                        if (paymentDoc.exists()) {
                            Object amount = paymentDoc.get("amount");
                            Object status = paymentDoc.get("paymentStatus");
                            Object method = paymentDoc.get("paymentType");
                            if (amount != null) amountText.setText("₱" + amount.toString());
                            if (status != null) statusText.setText(status.toString());
                            if (method != null) methodText.setText(method.toString());
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load payment: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            amountText.setText("-");
            statusText.setText("-");
            methodText.setText("-");
        }
    }

    // ================= Polyline Animation =================
    private void startPolylineAnimation() {
        polylineHandler = new Handler(Looper.getMainLooper());
        polylineIndex = 0;
        isAnimating = true;

        polylineRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnimating) return; // pause if needed

                if (polylineIndex < polylinePoints.size()) {
                    List<LatLng> sublist = polylinePoints.subList(0, polylineIndex + 1);
                    animatedPolyline.setPoints(sublist);
                    movingMarker.setPosition(polylinePoints.get(polylineIndex));
                    polylineIndex++;
                    polylineHandler.postDelayed(this, 15);
                } else {
                    // End marker
                    gMap.addMarker(new MarkerOptions()
                            .position(polylinePoints.get(polylinePoints.size() - 1))
                            .title("End")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    );
                    isAnimating = false;
                }
            }
        };
        polylineHandler.post(polylineRunnable);
    }

    private void pausePolylineAnimation() {
        isAnimating = false;
    }

    // ================= Distance Calculation =================
    private double distanceBetween(LatLng p1, LatLng p2) {
        double R = 6371;
        double dLat = Math.toRadians(p2.latitude - p1.latitude);
        double dLng = Math.toRadians(p2.longitude - p1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ================= Popup Menu & Delete =================
    private void showPopupMenu(View view) {
        if (rideId == null) return;

        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add("Delete Ride");
        popupMenu.setOnMenuItemClickListener(item -> {
            if ("Delete Ride".equals(item.getTitle())) {
                confirmDeleteRide(rideId);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void confirmDeleteRide(String rideId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Ride")
                .setMessage("Are you sure you want to delete this ride? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteRide(rideId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRide(String rideId) {
        pausePolylineAnimation();
        showLoading();

        FirebaseFirestore.getInstance()
                .collection("ride_logs")
                .document(rideId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    Toast.makeText(this, "Ride deleted successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, LogHistoryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to delete ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================= Loading Overlay =================
    private void setupLoadingOverlay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_loading).setCancelable(false);
        loadingDialog = builder.create();
    }

    private void showLoading() {
        if (loadingDialog != null && !loadingDialog.isShowing()) loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    // ================= Map Lifecycle =================
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}
