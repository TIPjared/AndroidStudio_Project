package com.example.finalprojectmobilecomputing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainPage extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;

    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private NavigationView navigationView;
    private Button payButton;
    private Button selectRouteButton;
    private Button stopButton;
    private Button packingAssistantButton;

    private Button changeDestinationButton;
    private TextView startLocationTextView;
    private TextView endLocationTextView;

    private Map<String, LatLng> predefinedLocations = new HashMap<>();
    private LatLng selectedEnd;
    private String selectedEndName;

    private Polyline currentRoute;
    private boolean transactionAuthorized = false;

    // Firebase Database reference
    private DatabaseReference rideLogsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        // --- Initialize services & views ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        drawerLayout = findViewById(R.id.drawerLayout);
        menuButton = findViewById(R.id.menuButton);
        navigationView = findViewById(R.id.navigationView);

        payButton               = findViewById(R.id.payButton);
        selectRouteButton       = findViewById(R.id.selectRouteButton);
        stopButton              = findViewById(R.id.stopButton);
        changeDestinationButton = findViewById(R.id.changeDestinationButton);

        startLocationTextView = findViewById(R.id.startLocationTextView);
        endLocationTextView   = findViewById(R.id.endLocationTextView);

        packingAssistantButton = findViewById(R.id.PackingAssistantButton);

        // Firebase reference for logging rides
        rideLogsRef = FirebaseDatabase.getInstance().getReference("rideLogs");

        // Initially hide route controls
        selectRouteButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
        changeDestinationButton.setVisibility(View.GONE);

        // --- Drawer toggle ---
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(MainPage.this, RideHistoryActivity.class));
            } else if (item.getItemId() == R.id.nav_support) {
                startActivity(new Intent(MainPage.this, SupportFeedbackActivity.class));
            } else if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainPage.this, LandingPage.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Find the header view from the NavigationView
        View headerView = navigationView.getHeaderView(0);
        
        // Load user profile data into the navigation header
        loadUserProfileData(headerView);
        
        // Setup click listener for profile link in the header
        Button profileLink = headerView.findViewById(R.id.profileLink);
        if (profileLink != null) {
            profileLink.setOnClickListener(v -> {
                goToProfile(v);
            });
        }
        
        // Setup click listener for profile container (profile image) in the header
        CardView profileContainer = headerView.findViewById(R.id.profileContainer);
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                goToProfile(v);
            });
        }

        // --- Pay / Cancel Transaction flow ---
        payButton.setOnClickListener(v -> {
            if (!transactionAuthorized) {
                startActivity(new Intent(this, QRScannerActivity.class));
            } else {
                confirmCancelTransaction();
            }
        });
        setupPaymentCallback();

        // --- Route / Stop / Change Destination buttons ---
        selectRouteButton.setOnClickListener(v -> {
            if (selectedEnd != null && currentLocation != null) {
                fetchAndDrawRoute(currentLocation, selectedEnd);
                stopButton.setVisibility(View.VISIBLE);
                changeDestinationButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(v -> {
            if (currentLocation != null && selectedEnd != null && isNearDestination(currentLocation, selectedEnd)) {
                Toast.makeText(this, "You have arrived at: " + selectedEndName, Toast.LENGTH_LONG).show();

                // Log the ride to Firebase when the ride stops
                logRideToFirebase();

                resetRoute();
            } else {
                Toast.makeText(this, "You are not near your destination yet.", Toast.LENGTH_SHORT).show();
            }
        });

        changeDestinationButton.setOnClickListener(v -> {
            resetRoute();
            Toast.makeText(this, "Please select a new destination marker.", Toast.LENGTH_SHORT).show();
        });

        packingAssistantButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainPage.this, PackingAssistantActivity.class);
            startActivity(intent);
        });

        // --- Map fragment init ---
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    // Method to navigate to the Profile activity
    public void goToProfile(View view) {
        Intent intent = new Intent(MainPage.this, Profile.class);
        startActivity(intent);
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Call payment callback again to handle the new intent
        setupPaymentCallback();
    }

    private void setupPaymentCallback() {
        // First check if there's a payment_status extra from PaymentResponseActivity
        String paymentStatus = getIntent().getStringExtra("payment_status");
        if (paymentStatus != null && "success".equals(paymentStatus)) {
            transactionAuthorized = true;
            updateTransactionUI();
            return;
        }
        
        // For backward compatibility, also check the URI data
        Uri data = getIntent().getData();
        if (data != null) {
            // Check old style myapp:// scheme
            if ("myapp".equals(data.getScheme()) && "main".equals(data.getHost())) {
                String status = data.getQueryParameter("status");
                if ("success".equals(status)) {
                    transactionAuthorized = true;
                }
            } 
            // Check HTTP scheme from sikad-static.onrender.com
            else if (data.toString().contains("sikad-static.onrender.com")) {
                String paymentParam = data.getQueryParameter("payment");
                if ("success".equals(paymentParam)) {
                    transactionAuthorized = true;
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        updateTransactionUI();
    }

    private void updateTransactionUI() {
        if (transactionAuthorized) {
            payButton.setText("Cancel Transaction");
            selectRouteButton.setVisibility(View.VISIBLE);
        } else {
            payButton.setText("Pay Now");
            selectRouteButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            changeDestinationButton.setVisibility(View.GONE);
            resetRoute();
        }
    }

    private void confirmCancelTransaction() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Transaction")
                .setMessage("Are you sure you want to cancel?")
                .setPositiveButton("Yes", (d, w) -> {
                    transactionAuthorized = false;
                    updateTransactionUI();
                    Toast.makeText(this, "Transaction cancelled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        requestLocationPermission();

        // Predefine markers
        addPredefinedMarker("TIP Arlegui",    new LatLng(14.5964, 120.9904)); // Approximate location
        addPredefinedMarker("City Hall",      new LatLng(14.589793, 120.981617)); // :contentReference[oaicite:0]{index=0}
        addPredefinedMarker("Rizal Park",     new LatLng(14.5825, 120.9783)); // :contentReference[oaicite:1]{index=1}
        addPredefinedMarker("Fort Santiago",  new LatLng(14.5950, 120.9694)); // :contentReference[oaicite:2]{index=2}
        addPredefinedMarker("Cathedral",      new LatLng(14.59147, 120.97356)); // :contentReference[oaicite:3]{index=3}
        addPredefinedMarker("Museum",         new LatLng(14.5869, 120.9812)); // :contentReference[oaicite:4]{index=4}
        addPredefinedMarker("Quiapo Church",  new LatLng(14.598782, 120.983783)); // :contentReference[oaicite:5]{index=5}

        mMap.setOnMarkerClickListener(marker -> {
            selectedEnd = marker.getPosition();
            selectedEndName = marker.getTitle();
            endLocationTextView.setText("Destination: " + selectedEndName);
            return false;
        });
    }

    private void addPredefinedMarker(String title, LatLng coords) {
        mMap.addMarker(new MarkerOptions().position(coords).title(title));
        predefinedLocations.put(title, coords);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        currentLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                        mMap.setMyLocationEnabled(true);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        startLocationTextView.setText(
                                "You: " + currentLocation.latitude + ", " + currentLocation.longitude
                        );
                    } else {
                        Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAndDrawRoute(LatLng origin, LatLng dest) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DirectionsAPI api = retrofit.create(DirectionsAPI.class);
        String o = origin.latitude + "," + origin.longitude;
        String d = dest.latitude   + "," + dest.longitude;
        String key = getString(R.string.google_maps_key);

        // Show a loading message
        Toast.makeText(this, "Fetching route...", Toast.LENGTH_SHORT).show();

        // Request directions with driving mode and alternatives for better results
        api.getDirections(o, d, "driving", true, key).enqueue(new Callback<DirectionsResponse>() {
            @Override public void onResponse(
                    Call<DirectionsResponse> call,
                    Response<DirectionsResponse> resp
            ) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    Toast.makeText(MainPage.this, "Route API request failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    DirectionsResponse result = resp.body();
                    List<LatLng> routePoints = result.getRoutePoints();
                    
                    if (routePoints.isEmpty()) {
                        Toast.makeText(MainPage.this, "No route found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentRoute != null) currentRoute.remove();
                    
                    // Create polyline with better visibility
                    PolylineOptions options = new PolylineOptions()
                        .addAll(routePoints)
                        .color(Color.RED)
                        .width(10)  // Wider line
                        .geodesic(true);  // Follow Earth's curvature
                        
                    currentRoute = mMap.addPolyline(options);
                    
                    // Move camera to show the route
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 12));
                    
                    // Show route info if available
                    if (!result.routes.isEmpty() && !result.routes.get(0).legs.isEmpty()) {
                        DirectionsResponse.Leg leg = result.routes.get(0).legs.get(0);
                        if (leg.distance != null && leg.duration != null) {
                            String routeInfo = "Distance: " + leg.distance.text + 
                                               " | Duration: " + leg.duration.text;
                            Toast.makeText(MainPage.this, routeInfo, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MainPage.this, "Error processing route: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(MainPage.this, "Error fetching route: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNearDestination(LatLng current, LatLng destination) {
        float[] results = new float[1];
        Location.distanceBetween(
                current.latitude, current.longitude,
                destination.latitude, destination.longitude, results);
        return results[0] < 100; // Distance threshold in meters
    }

    private void resetRoute() {
        if (currentRoute != null) currentRoute.remove();
        stopButton.setVisibility(View.GONE);
        changeDestinationButton.setVisibility(View.GONE);
    }

    private void logRideToFirebase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        // Calculate the distance traveled
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                selectedEnd.latitude, selectedEnd.longitude,
                results
        );
        float distanceInMeters = results[0];  // distance in meters

        // Create a map to store the data for ride_logs collection
        Map<String, Object> rideData = new HashMap<>();
        rideData.put("distance_traveled", distanceInMeters);
        rideData.put("location", selectedEndName);
        rideData.put("timestamp", timestamp);

        // Save to the "ride_logs" collection
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("ride_logs")
                .add(rideData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainPage.this, "Ride logged successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainPage.this, "Error logging ride data", Toast.LENGTH_SHORT).show()
                );

        // Also log to the original location_logs if needed
        DatabaseReference locationLogsRef = FirebaseDatabase.getInstance().getReference("location_logs").child("logs").push();
        Map<String, Object> logData = new HashMap<>();
        logData.put("distance_traveled", distanceInMeters);
        logData.put("location", selectedEnd.latitude + "," + selectedEnd.longitude);
        logData.put("timestamp", timestamp);
        locationLogsRef.setValue(logData);
    }
    
    private void updateUserStats(String userId, double distanceInKm) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.collection("users").document(userId);
        
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            
            // Get current stats or initialize if not exists
            String totalRidesStr = snapshot.exists() ? snapshot.getString("totalRides") : "0";
            String totalDistanceStr = snapshot.exists() ? snapshot.getString("totalDistance") : "0";
            
            // Parse current values
            int totalRides = 0;
            double totalDistance = 0;
            try {
                totalRides = Integer.parseInt(totalRidesStr);
                totalDistance = Double.parseDouble(totalDistanceStr.replace(" km", ""));
            } catch (NumberFormatException e) {
                // Use defaults if parsing fails
            }
            
            // Update with new ride
            totalRides++;
            totalDistance += distanceInKm;
            
            // Format and save
            Map<String, Object> updates = new HashMap<>();
            updates.put("totalRides", String.valueOf(totalRides));
            updates.put("totalDistance", String.format(Locale.getDefault(), "%.1f km", totalDistance));
            
            transaction.set(userRef, updates, SetOptions.merge());
            return null;
        });
    }

    private void loadUserProfileData(View headerView) {
        TextView profileName = headerView.findViewById(R.id.profileName);
        TextView profileEmail = headerView.findViewById(R.id.profileEmail);
        ImageView profileImage = headerView.findViewById(R.id.profileImage);
        
        // Get current Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            // Set email from Firebase Auth
            profileEmail.setText(currentUser.getEmail());
            
            // Get username from Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        
                        if (username != null && !username.isEmpty()) {
                            profileName.setText(username);
                        }
                        
                        // Load profile image if it exists
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            // Use Picasso library to load and cache the image
                            com.squareup.picasso.Picasso.get()
                                .load(profileImageUrl)
                                .placeholder(R.drawable.baseline_person_outline_24)
                                .error(R.drawable.baseline_person_outline_24)
                                .into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // In case of error, leave default text
                });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Refresh user profile data in navigation header when returning to this screen
        View headerView = navigationView.getHeaderView(0);
        loadUserProfileData(headerView);
    }
}

