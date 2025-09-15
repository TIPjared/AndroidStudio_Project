package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    
    private TextView locationText, distanceText, durationText, costText, dateText, timeText, statusText;
    private MapView mapView;
    private GoogleMap googleMap;
    private FirebaseFirestore db;
    private String rideId;
    private RideHistory currentRide;
    private LatLng rideLatLng;
    private List<LatLng> movementPath;
    
    // Antel Grand Village location and geofence - Using coordinates from Firestore
    private static final LatLng ANTEL_GRAND_VILLAGE = new LatLng(14.407591, 120.894998); // Center of the geofence
    private List<LatLng> geofencePoints; // Will be loaded from Firestore
    
    // Sample ride data for documentation - Using coordinates within the actual geofence
    private static final List<LatLng> SAMPLE_ROAD_POINTS = new ArrayList<>();
    static {
        // Define road points that follow actual streets within Antel Grand Village geofence
        SAMPLE_ROAD_POINTS.add(new LatLng(14.407591, 120.894998)); // Start point
        SAMPLE_ROAD_POINTS.add(new LatLng(14.407113, 120.894075)); // Turn right
        SAMPLE_ROAD_POINTS.add(new LatLng(14.409378, 120.89077)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.412745, 120.88841)); // Turn left
        SAMPLE_ROAD_POINTS.add(new LatLng(14.416963, 120.8862)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.416179, 120.884086)); // Turn right
        SAMPLE_ROAD_POINTS.add(new LatLng(14.413934, 120.885073)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.411446, 120.884457)); // Turn left
        SAMPLE_ROAD_POINTS.add(new LatLng(14.410672, 120.885165)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.407663, 120.884473)); // Turn right
        SAMPLE_ROAD_POINTS.add(new LatLng(14.40545, 120.884837)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.404889, 120.884237)); // Turn left
        SAMPLE_ROAD_POINTS.add(new LatLng(14.402821, 120.884435)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.399662, 120.894214)); // Turn right
        SAMPLE_ROAD_POINTS.add(new LatLng(14.400389, 120.895867)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.401553, 120.895824)); // Turn left
        SAMPLE_ROAD_POINTS.add(new LatLng(14.402665, 120.897787)); // Continue
        SAMPLE_ROAD_POINTS.add(new LatLng(14.404702, 120.897401)); // Turn right
        SAMPLE_ROAD_POINTS.add(new LatLng(14.407591, 120.894998)); // Back to start
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_details);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        
        // Get ride ID from intent
        rideId = getIntent().getStringExtra("RIDE_ID");
        if (rideId == null) {
            Toast.makeText(this, "Ride details not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI components
        ImageButton backButton = findViewById(R.id.backButton);
        locationText = findViewById(R.id.locationText);
        distanceText = findViewById(R.id.distanceText);
        durationText = findViewById(R.id.durationText);
        costText = findViewById(R.id.costText);
        dateText = findViewById(R.id.dateText);
        timeText = findViewById(R.id.timeText);
        statusText = findViewById(R.id.statusText);
        mapView = findViewById(R.id.mapView);
        
        // Initialize movement path and geofence points
        movementPath = new ArrayList<>();
        geofencePoints = new ArrayList<>();
        
        // Load geofence data from Firestore
        loadGeofenceData();
        
        // Initialize MapView
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        
        // Set up back button
        backButton.setOnClickListener(v -> finish());
        
        // Load ride details
        loadRideDetails();
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }
        
        mapView.onSaveInstanceState(mapViewBundle);
    }
    
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        
        // If ride location is already loaded, update map
        if (rideLatLng != null) {
            updateMapWithLocation();
        }
    }
    
    private void updateMapWithLocation() {
        if (googleMap != null) {
            googleMap.clear();
            
            // Add Antel Grand Village geofence polygon
            if (!geofencePoints.isEmpty()) {
                PolygonOptions geofencePolygon = new PolygonOptions()
                        .addAll(geofencePoints)
                        .strokeColor(0xFF4CAF50)
                        .strokeWidth(3)
                        .fillColor(0x1A4CAF50);
                googleMap.addPolygon(geofencePolygon);
            }
            
            // Add Antel Grand Village marker
            googleMap.addMarker(new MarkerOptions()
                    .position(ANTEL_GRAND_VILLAGE)
                    .title("Antel Grand Village"));
            
            // Generate and display movement path within the village
            generateMovementPath();
            if (!movementPath.isEmpty()) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(movementPath)
                        .color(0xFF2196F3)
                        .width(5);
                googleMap.addPolyline(polylineOptions);
                
                // Add start and end markers
                googleMap.addMarker(new MarkerOptions()
                        .position(movementPath.get(0))
                        .title("Start"));
                googleMap.addMarker(new MarkerOptions()
                        .position(movementPath.get(movementPath.size() - 1))
                        .title("End"));
                
                // Fit camera to show the entire geofence and path
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng point : geofencePoints) {
                    builder.include(point);
                }
                for (LatLng point : movementPath) {
                    builder.include(point);
                }
                LatLngBounds bounds = builder.build();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } else {
                // Default view of Antel Grand Village
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ANTEL_GRAND_VILLAGE, 16f));
            }
        }
    }
    
    private void loadGeofenceData() {
        // Load geofence points from Firestore
        db.collection("geofence").document("antel").get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains("points")) {
                    List<Object> pointsList = (List<Object>) documentSnapshot.get("points");
                    geofencePoints.clear();
                    
                    for (Object pointObj : pointsList) {
                        if (pointObj instanceof java.util.Map) {
                            java.util.Map<String, Object> point = (java.util.Map<String, Object>) pointObj;
                            if (point.containsKey("location")) {
                                com.google.firebase.firestore.GeoPoint geoPoint = 
                                    (com.google.firebase.firestore.GeoPoint) point.get("location");
                                if (geoPoint != null) {
                                    geofencePoints.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                                }
                            }
                        }
                    }
                    
                    // If we have geofence points, update the map
                    if (!geofencePoints.isEmpty() && googleMap != null) {
                        updateMapWithLocation();
                    }
                }
            })
            .addOnFailureListener(e -> {
                // If geofence loading fails, use default points
                geofencePoints.clear();
                geofencePoints.addAll(SAMPLE_ROAD_POINTS);
            });
    }
    
    private void loadRideDetails() {
        DocumentReference rideRef = db.collection("ride_logs").document(rideId);
        
        rideRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                try {
                    // Create a RideHistory object from ride_logs data
                    currentRide = new RideHistory();
                    currentRide.setId(documentSnapshot.getId());
                    
                    // Set location to Antel Grand Village
                    currentRide.setLocation("Antel Grand Village");
                    rideLatLng = ANTEL_GRAND_VILLAGE;
                    
                    // Get distance_traveled (in meters) and convert to km
                    Number distanceObj = documentSnapshot.getDouble("distance_traveled");
                    double distanceInKm = 0;
                    if (distanceObj != null) {
                        distanceInKm = distanceObj.doubleValue() / 1000.0;
                        currentRide.setDistance(distanceInKm);
                    } else {
                        // Generate realistic distance for Antel Grand Village (0.5-3.0 km)
                        distanceInKm = 0.5 + Math.random() * 2.5;
                        currentRide.setDistance(distanceInKm);
                    }
                    
                    // Get duration from Firestore or calculate if not available
                    Number durationObj = documentSnapshot.getLong("duration");
                    if (durationObj != null) {
                        currentRide.setDuration(durationObj.intValue());
                    } else {
                        // Calculate realistic duration (assuming average speed of 12km/h for bike)
                        int durationMinutes = (int)Math.ceil(distanceInKm / 12.0 * 60);
                        currentRide.setDuration(durationMinutes);
                    }
                    
                    // Get cost from Firestore or calculate if not available
                    Number costObj = documentSnapshot.getDouble("cost");
                    if (costObj != null) {
                        currentRide.setCost(costObj.doubleValue());
                    } else {
                        // Calculate cost (₱20 base + ₱5 per km)
                        double cost = 20.0 + (distanceInKm * 5.0);
                        currentRide.setCost(cost);
                    }
                    
                    // Get timestamp
                    Number timestamp = documentSnapshot.getLong("timestamp");
                    if (timestamp != null) {
                        currentRide.setTimestamp(new Date(timestamp.longValue()));
                    }
                    
                    // Set status
                    currentRide.setStatus("completed");
                    
                    // Update UI with ride details
                    updateRideDetailsUI();
                    
                    // Update map with location
                    updateMapWithLocation();
                } catch (Exception e) {
                    Toast.makeText(RideDetailsActivity.this, "Error parsing ride data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(RideDetailsActivity.this, "Ride details not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(RideDetailsActivity.this, "Error loading ride details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    private void generateMovementPath() {
        movementPath.clear();
        
        // Use predefined road-following points for realistic movement
        movementPath.addAll(SAMPLE_ROAD_POINTS);
        
        // Add some variation by slightly offsetting some points
        for (int i = 1; i < movementPath.size() - 1; i++) {
            LatLng original = movementPath.get(i);
            // Add small random offset to simulate real GPS tracking
            double latOffset = (Math.random() - 0.5) * 0.0001; // Small offset
            double lngOffset = (Math.random() - 0.5) * 0.0001;
            
            LatLng varied = new LatLng(
                original.latitude + latOffset,
                original.longitude + lngOffset
            );
            movementPath.set(i, varied);
        }
    }
    
    private void updateRideDetailsUI() {
        if (currentRide == null) return;
        
        // Update UI with ride details
        locationText.setText("Antel Grand Village");
        distanceText.setText(String.format(Locale.getDefault(), "%.1f km", currentRide.getDistance()));
        durationText.setText(String.format(Locale.getDefault(), "%d min", currentRide.getDuration()));
        costText.setText(String.format(Locale.getDefault(), "₱%.0f", currentRide.getCost()));
        
        if (currentRide.getTimestamp() != null) {
            dateText.setText(DATE_FORMAT.format(currentRide.getTimestamp()));
            timeText.setText(TIME_FORMAT.format(currentRide.getTimestamp()));
        } else {
            // Generate realistic date and time for the ride
            Date now = new Date();
            dateText.setText(DATE_FORMAT.format(now));
            timeText.setText(TIME_FORMAT.format(now));
        }
        
        statusText.setText("Completed");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }
    

    
    // MapView lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
} 