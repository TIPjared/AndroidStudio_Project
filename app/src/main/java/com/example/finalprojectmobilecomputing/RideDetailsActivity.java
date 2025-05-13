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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    
    private TextView locationText, distanceText, durationText, dateText, timeText, statusText;
    private MaterialButton shareButton, exportButton;
    private MapView mapView;
    private GoogleMap googleMap;
    private FirebaseFirestore db;
    private String rideId;
    private RideHistory currentRide;
    private LatLng rideLatLng;
    
    // Predefined locations
    private static final LatLng RIZAL_PARK = new LatLng(14.5825, 120.9783);
    private static final LatLng FORT_SANTIAGO = new LatLng(14.5950, 120.9694);
    private static final LatLng CITY_HALL = new LatLng(14.589793, 120.981617);
    private static final LatLng CATHEDRAL = new LatLng(14.59147, 120.97356);
    private static final LatLng MUSEUM = new LatLng(14.5869, 120.9812);
    private static final LatLng QUIAPO_CHURCH = new LatLng(14.598782, 120.983783);
    private static final LatLng TIP_ARLEGUI = new LatLng(14.5964, 120.9904);
    
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
        dateText = findViewById(R.id.dateText);
        timeText = findViewById(R.id.timeText);
        statusText = findViewById(R.id.statusText);
        shareButton = findViewById(R.id.shareButton);
        exportButton = findViewById(R.id.exportButton);
        mapView = findViewById(R.id.mapView);
        
        // Initialize MapView
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        
        // Set up back button
        backButton.setOnClickListener(v -> finish());
        
        // Set up action buttons
        shareButton.setOnClickListener(v -> shareRideDetails());
        exportButton.setOnClickListener(v -> exportRideData());
        
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
        if (googleMap != null && rideLatLng != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(rideLatLng).title(currentRide.getLocation()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(rideLatLng, 15f));
        }
    }
    
    private void loadRideDetails() {
        DocumentReference rideRef = db.collection("ride_logs").document(rideId);
        
        rideRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                try {
                    // Create a RideHistory object from ride_logs data
                    currentRide = new RideHistory();
                    currentRide.setId(documentSnapshot.getId());
                    
                    // Get location
                    String location = documentSnapshot.getString("location");
                    // Check if location is in format "lat,lng"
                    if (location != null) {
                        if (location.matches("^[-+]?\\d+\\.\\d+,\\s*[-+]?\\d+\\.\\d+$")) {
                            // Parse the lat,lng coordinates
                            String[] latLng = location.split(",");
                            if (latLng.length == 2) {
                                try {
                                    double lat = Double.parseDouble(latLng[0].trim());
                                    double lng = Double.parseDouble(latLng[1].trim());
                                    rideLatLng = new LatLng(lat, lng);
                                    // Find closest predefined location name for these coordinates
                                    currentRide.setLocation(getNameFromLocation(rideLatLng));
                                } catch (NumberFormatException e) {
                                    // Find a suitable predefined location instead of defaulting
                                    rideLatLng = TIP_ARLEGUI; // Use as fallback coordinates
                                    currentRide.setLocation(getNameFromLocation(rideLatLng));
                                }
                            }
                        } else {
                            // It's already a named location
                            currentRide.setLocation(location);
                            // Try to get coordinates from the destination name using predefined locations
                            rideLatLng = getLocationFromName(location);
                        }
                    } else {
                        // No location data - find the nearest named location to current device location
                        rideLatLng = TIP_ARLEGUI; // Fallback to a known location
                        currentRide.setLocation(getNameFromLocation(rideLatLng));
                    }
                    
                    // Get distance_traveled (in meters) and convert to km
                    Number distanceObj = documentSnapshot.getDouble("distance_traveled");
                    double distanceInKm = 0;
                    if (distanceObj != null) {
                        distanceInKm = distanceObj.doubleValue() / 1000.0;
                        currentRide.setDistance(distanceInKm);
                    }
                    
                    // Calculate approximate duration (assuming average speed of 15km/h)
                    currentRide.setDuration((int)Math.ceil(distanceInKm / 15.0 * 60));
                    
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
    
    private LatLng getLocationFromName(String locationName) {
        // Known locations in Manila
        switch (locationName) {
            case "Rizal Park":
                return RIZAL_PARK;
            case "Fort Santiago":
                return FORT_SANTIAGO;
            case "City Hall":
                return CITY_HALL;
            case "Cathedral":
                return CATHEDRAL;
            case "Museum":
                return MUSEUM;
            case "Quiapo Church":
                return QUIAPO_CHURCH;
            case "TIP Arlegui":
                return TIP_ARLEGUI;
            default:
                // If location name doesn't match any predefined location,
                // return the default location
                return TIP_ARLEGUI;
        }
    }
    
    private String getNameFromLocation(LatLng location) {
        // Calculate distance to each location
        double distToRizal = computeDistance(location, RIZAL_PARK);
        double distToFort = computeDistance(location, FORT_SANTIAGO);
        double distToCity = computeDistance(location, CITY_HALL);
        double distToCathedral = computeDistance(location, CATHEDRAL);
        double distToMuseum = computeDistance(location, MUSEUM);
        double distToQuiapo = computeDistance(location, QUIAPO_CHURCH);
        double distToTIP = computeDistance(location, TIP_ARLEGUI);
        
        // Find the closest location
        double minDist = Math.min(Math.min(Math.min(distToRizal, distToFort), 
                Math.min(distToCity, distToCathedral)), 
                Math.min(Math.min(distToMuseum, distToQuiapo), distToTIP));
        
        if (minDist == distToRizal) return "Rizal Park";
        if (minDist == distToFort) return "Fort Santiago";
        if (minDist == distToCity) return "City Hall";
        if (minDist == distToCathedral) return "Cathedral";
        if (minDist == distToMuseum) return "Museum";
        if (minDist == distToQuiapo) return "Quiapo Church";
        if (minDist == distToTIP) return "TIP Arlegui";
        
        // Default fallback - should not reach here if distances are calculated correctly
        return "TIP Arlegui";
    }
    
    private double computeDistance(LatLng point1, LatLng point2) {
        // Simple Euclidean distance calculation
        double dx = point1.latitude - point2.latitude;
        double dy = point1.longitude - point2.longitude;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    private void updateRideDetailsUI() {
        if (currentRide == null) return;
        
        // Update UI with ride details - ensure we always have a proper location name
        locationText.setText(currentRide.getLocation() != null ? currentRide.getLocation() : getNameFromLocation(rideLatLng));
        distanceText.setText(String.format(Locale.getDefault(), "%.1f km", currentRide.getDistance()));
        durationText.setText(String.format(Locale.getDefault(), "%d min", currentRide.getDuration()));
        
        if (currentRide.getTimestamp() != null) {
            dateText.setText(DATE_FORMAT.format(currentRide.getTimestamp()));
            timeText.setText(TIME_FORMAT.format(currentRide.getTimestamp()));
        } else {
            dateText.setText("Unknown date");
            timeText.setText("00:00");
        }
        
        statusText.setText("Completed");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }
    
    private void shareRideDetails() {
        if (currentRide == null) return;
        
        // Make sure we have a valid location name
        String locationName = currentRide.getLocation();
        if (locationName == null || locationName.isEmpty()) {
            locationName = getNameFromLocation(rideLatLng);
        }
        
        String shareText = "I completed a ride of " + 
                String.format(Locale.getDefault(), "%.1f km", currentRide.getDistance()) + 
                " in " + String.format(Locale.getDefault(), "%d minutes", currentRide.getDuration()) + 
                " with the PedalGo App!";
        
        // Always include the route name
        shareText += "\nRoute: " + locationName;
        
        // Add date
        if (currentRide.getTimestamp() != null) {
            shareText += "\nDate: " + DATE_FORMAT.format(currentRide.getTimestamp());
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My PedalGo Journey");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share your ride"));
    }
    
    private void exportRideData() {
        if (currentRide == null) return;
        
        // Make sure we have a valid location name
        String locationName = currentRide.getLocation();
        if (locationName == null || locationName.isEmpty()) {
            locationName = getNameFromLocation(rideLatLng);
        }
        
        // In a real app, you would generate a CSV or PDF file here
        // For this example, we'll just show a toast
        Toast.makeText(this, "Ride data exported (CSV file would be saved in a real app)", Toast.LENGTH_SHORT).show();
        
        // Example of data that would be exported
        String exportData = "PedalGo Ride ID: " + currentRide.getId() + "\n" +
                "Date: " + (currentRide.getTimestamp() != null ? DATE_FORMAT.format(currentRide.getTimestamp()) : "Unknown") + "\n" +
                "Time: " + (currentRide.getTimestamp() != null ? TIME_FORMAT.format(currentRide.getTimestamp()) : "00:00") + "\n" +
                "Route: " + locationName + "\n" +
                "Distance: " + String.format(Locale.getDefault(), "%.1f km", currentRide.getDistance()) + "\n" +
                "Duration: " + String.format(Locale.getDefault(), "%d min", currentRide.getDuration()) + "\n" +
                "Status: Completed";
        
        // Log the data that would be exported (for debugging)
        android.util.Log.d("RideExport", exportData);
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