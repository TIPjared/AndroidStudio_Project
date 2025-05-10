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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainPage.this, LandingPage.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // --- Pay / Cancel Transaction flow ---
        payButton.setOnClickListener(v -> {
            if (!transactionAuthorized) {
                startActivity(new Intent(this, PaymentActivity.class));
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

        // --- Map fragment init ---
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupPaymentCallback() {
        Uri data = getIntent().getData();
        if (data != null
                && "myapp".equals(data.getScheme())
                && "main".equals(data.getHost())
        ) {
            String status = data.getQueryParameter("status");
            if ("success".equals(status)) {
                transactionAuthorized = true;
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

        api.getDirections(o, d, key).enqueue(new Callback<DirectionsResponse>() {
            @Override public void onResponse(
                    Call<DirectionsResponse> call,
                    Response<DirectionsResponse> resp
            ) {
                if (!resp.isSuccessful() || resp.body() == null) return;
                DirectionsResponse result = resp.body();
                List<LatLng> routePoints = new ArrayList<>();
                for (DirectionsResponse.Leg leg : result.routes.get(0).legs) {
                    for (DirectionsResponse.Step step : leg.steps) {
                        routePoints.add(new LatLng(
                                step.end_location.lat, step.end_location.lng));
                    }
                }

                if (currentRoute != null) currentRoute.remove();
                currentRoute = mMap.addPolyline(
                        new PolylineOptions().addAll(routePoints).color(Color.RED).width(5)
                );
            }

            @Override public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(MainPage.this, "Error fetching route", Toast.LENGTH_SHORT).show();
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
        float distanceTraveled = results[0];  // distance in meters

        // Create a map to store the data
        Map<String, Object> rideData = new HashMap<>();
        rideData.put("distance_traveled", distanceTraveled);
        rideData.put("location", selectedEnd.latitude + "," + selectedEnd.longitude);
        rideData.put("timestamp", timestamp);

        // Get a reference to the "location_logs" -> "logs" node in Firebase
        DatabaseReference locationLogsRef = FirebaseDatabase.getInstance().getReference("location_logs").child("logs").push();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("ride_logs")
                .add(rideData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(MainPage.this, "Ride also logged to Firestore!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(MainPage.this, "Error logging to Firestore", Toast.LENGTH_SHORT).show()
                );
    }
}

