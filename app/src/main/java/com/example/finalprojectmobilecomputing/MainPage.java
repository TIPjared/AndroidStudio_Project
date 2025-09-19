package com.example.finalprojectmobilecomputing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.PolyUtil;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.google.android.gms.maps.model.PolygonOptions;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.gms.location.GeofencingClient;


public class MainPage extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private List<LatLng> antelBoundary;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private NavigationView navigationView;
    private Button payButton;
    private Button stopButton;
    private GeofencingClient geofencingClient;
    private Map<String, LatLng> predefinedLocations = new HashMap<>();

    private boolean transactionAuthorized = false;

    // Firebase Database reference
    private DatabaseReference rideLogsRef;
    private TextView distanceTextView, timerTextView, elapsedTimeTextView;;
    private float totalDistance = 0f;
    private Location previousLocation;
    private long startTime;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long rideStartTime;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private CountDownTimer countDownTimer;
    private CardView locationCard;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        // --- Initialize services & views ---
        fusedLocationClient     = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient        = LocationServices.getGeofencingClient(this);

        drawerLayout            = findViewById(R.id.drawerLayout);
        menuButton              = findViewById(R.id.menuButton);
        navigationView          = findViewById(R.id.navigationView);
        payButton               = findViewById(R.id.payButton);
        stopButton              = findViewById(R.id.stopButton);
        distanceTextView        = findViewById(R.id.distanceTextView);
        timerTextView           = findViewById(R.id.timerTextView);
        startTime               = System.currentTimeMillis();
        locationCard            = findViewById(R.id.locationCard);
        elapsedTimeTextView     = findViewById(R.id.elapsedTimeTextView);


        //startTimer();

        // XML layout already handles the icon positioning for proper centering

        // Firebase reference for logging rides
        rideLogsRef = FirebaseDatabase.getInstance().getReference("rideLogs");

        // Initially hide route controls
        stopButton.setVisibility(View.GONE);

        locationCard.setVisibility(View.GONE); // Hide at app start

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

        // Asking for Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101); // 101 is the request code
            }
        }

        //Location Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, 1001);
        }

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

        payButton.setOnClickListener(v -> {
            if ("Stop Ride".equals(payButton.getText().toString())) {
                // If button says Stop Ride → call stopRide()
                stopRide();
            } else {
                // Otherwise → normal Pay Now flow
                if (currentLocation != null /* && antelBoundary != null && !antelBoundary.isEmpty() */) {
                    // Geofence check commented out → bypass restriction
            /*
            if (PolyUtil.containsLocation(
                    new LatLng(currentLocation.latitude, currentLocation.longitude),
                    antelBoundary,
                    true
            )) {
                Intent intent = new Intent(MainPage.this, QRScannerActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "You must be inside Antel Grand Village to start a ride.", Toast.LENGTH_SHORT).show();
            }
            */
                    // Directly start QR scanner
                    Intent intent = new Intent(MainPage.this, QRScannerActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Waiting for current location or geofence data...", Toast.LENGTH_SHORT).show();
                }
            }
        });


        stopButton.setOnClickListener(v -> {
            if (currentLocation != null && antelBoundary != null && !antelBoundary.isEmpty()) {
                if (PolyUtil.containsLocation(
                        new LatLng(currentLocation.latitude, currentLocation.longitude),
                        antelBoundary,
                        true
                )) {
                    Toast.makeText(this, "Ride ended successfully.", Toast.LENGTH_LONG).show();
                    stopRide();
                } else {
                    Toast.makeText(this, "You must return inside the area to end your ride.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Waiting for current location or geofence data...", Toast.LENGTH_SHORT).show();
            }
        });


        // --- Map fragment init ---
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }


    private void stopRide() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (timerHandler != null && timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        transactionAuthorized = false;

        long rideDuration = System.currentTimeMillis() - startTime;
        int seconds = (int) (rideDuration / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String time = String.format(Locale.getDefault(), "You rode for %02d:%02d", minutes, seconds);
        elapsedTimeTextView.setText(time);
        elapsedTimeTextView.setVisibility(View.VISIBLE);

        distanceTextView.setVisibility(View.VISIBLE); // make sure distance stays visible
        locationCard.setVisibility(View.VISIBLE);     // keep card visible
        stopButton.setVisibility(View.GONE);
        payButton.setVisibility(View.VISIBLE);
        payButton.setText("Pay Now");


        showCompletionDialog();
        String bikeId = getIntent().getStringExtra("bikeId");
        if (bikeId != null) {
            revertQRCodeStatus(bikeId);
        }
        updateTransactionUI();
        resetRideUI();
    }

    private void showCompletionDialog() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String timeRode = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        String distance = distanceTextView.getText().toString(); // Already updated

        AlertDialog.Builder builder = new AlertDialog.Builder(MainPage.this);
        builder.setTitle("Ride Finished");
        builder.setMessage("Time Rode: " + timeRode + "\nDistance Travelled: " + distance);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
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
        boolean paymentSuccess = false;

        // 1️⃣ Check Intent extra (for new server redirect)
        String paymentStatus = getIntent().getStringExtra("payment_status");
        if ("success".equals(paymentStatus)) {
            paymentSuccess = true;
        }

        // 2️⃣ Check Intent URI (covers old myapp:// scheme and HTTP redirect)
        Uri data = getIntent().getData();
        if (data != null) {
            // Old myapp:// scheme
            if ("myapp".equals(data.getScheme()) && "main".equals(data.getHost())) {
                String status = data.getQueryParameter("payment_status"); // updated to match server
                if ("success".equals(status)) {
                    paymentSuccess = true;
                }
            }
            // HTTP redirect from sikad-static.onrender.com
            else if (data.toString().contains("sikad-static.onrender.com")) {
                String paymentParam = data.getQueryParameter("payment");
                if ("success".equals(paymentParam)) {
                    paymentSuccess = true;
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // 3️⃣ If payment succeeded, update UI and start timers
        if (paymentSuccess) {
            transactionAuthorized = true;

            CardView locationCard = findViewById(R.id.locationCard);
            locationCard.setVisibility(View.VISIBLE);

            timerTextView.setVisibility(View.VISIBLE);

            updateTransactionUI();  // updates buttons, visibility
        }
    }

    private void revertQRCodeStatus(String bikeId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference qrRef = db.collection("qr_codes").document(bikeId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", false);       // back to default
        updates.put("status", "available");   // or whatever the original value is

        qrRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d("QR_CODE", "QR code reset after ride"))
                .addOnFailureListener(e -> Log.e("QR_CODE", "Failed to reset QR code", e));
    }

    private void updateTransactionUI() {
        if (transactionAuthorized) {
            payButton.setText("Stop Ride");
            locationCard.setVisibility(View.VISIBLE);
            distanceTextView.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.VISIBLE);

            startRideTimer();
            startLocationUpdates();
            startTimer();
        } else {
            payButton.setText("Pay Now");
            stopButton.setVisibility(View.GONE);
        }
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedMillis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String time = String.format(Locale.getDefault(), "Elapsed: %02d:%02d", minutes, seconds);
                elapsedTimeTextView.setText(time);

                timerHandler.postDelayed(this, 1000); // Repeat every second
            }
        };
        timerHandler.post(timerRunnable);
    }


    private void startRideTimer() {
        rideStartTime = System.currentTimeMillis();

        countDownTimer = new CountDownTimer(30 * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String time = String.format(Locale.getDefault(), "Time Left: %02d:%02d", minutes, seconds);
                timerTextView.setText(time);
            }

            public void onFinish() {
                Toast.makeText(MainPage.this, "Ride time is over!", Toast.LENGTH_SHORT).show();
                stopRide(); // Automatically stop ride
            }
        }.start();
    }



    private void stopRideTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        long rideDuration = System.currentTimeMillis() - rideStartTime;
        long minutes = rideDuration / 60000;
        Toast.makeText(this, "Ride time: " + minutes + " minutes", Toast.LENGTH_SHORT).show();
    }

    private void resetRideUI() {
        payButton.setText("Pay Now");
        payButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);

        distanceTextView.setText("");
        distanceTextView.setVisibility(View.GONE);

        timerTextView.setText("");
        timerTextView.setVisibility(View.GONE);

        if (elapsedTimeTextView != null) {
            elapsedTimeTextView.setText("");
            elapsedTimeTextView.setVisibility(View.GONE);
        }

        locationCard.setVisibility(View.GONE);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        requestLocationPermission();

        // Draw Antel Grand Village geofence
        listenToGeofenceChanges();


        // Remove any "destination" logic (markers are just visual now)
        mMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(this, "Marker: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
            return false;

        });
    }

    private void addPredefinedMarker(String title, LatLng coords) {
        mMap.addMarker(new MarkerOptions().position(coords).title(title));
        predefinedLocations.put(title, coords);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted – you can post notifications now
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private LatLng getPolygonCenter(List<LatLng> points) {
        double lat = 0, lng = 0;
        for (LatLng p : points) {
            lat += p.latitude;
            lng += p.longitude;
        }
        return new LatLng(lat / points.size(), lng / points.size());
    }
    private void listenToGeofenceChanges() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("geofence").document("antel");

        docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.w("Firestore", "Listen failed.", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                List<Map<String, Object>> points = (List<Map<String, Object>>) snapshot.get("points");
                if (points != null) {
                    antelBoundary = new ArrayList<>();
                    for (Map<String, Object> point : points) {
                        GeoPoint geoPoint = (GeoPoint) point.get("location");
                        if (geoPoint != null) {
                            antelBoundary.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                        }
                    }

                    // Draw village polygon on map
                    drawGeofencePolygon();

                    // Add just ONE circular geofence that surrounds the whole village
                    LatLng center = getPolygonCenter(antelBoundary);
                    float radiusMeters = getMaxDistanceFromCenter(center, antelBoundary) + 50; // +50m buffer
                    addWakeUpGeofence(center, radiusMeters);
                }
            } else {
                Log.d("Firestore", "No current data in geofence document.");
            }
        });
    }
    private void drawGeofencePolygon() {
        // Clear previous polygons if needed
        mMap.clear();

        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(antelBoundary)
                .strokeColor(Color.RED)
                .fillColor(0x2200FF00)
                .strokeWidth(5);

        mMap.addPolygon(polygonOptions);

        // Redraw predefined markers (if you cleared map)
        addPredefinedMarker("Point W1", new LatLng(14.4035, 120.8928));
        addPredefinedMarker("Point W2", new LatLng(14.4020, 120.8935));
        addPredefinedMarker("Point W3", new LatLng(14.4015, 120.8920));
        addPredefinedMarker("Point W4", new LatLng(14.4028, 120.8908));
        addPredefinedMarker("Point W5", new LatLng(14.4040, 120.8918));

        // Automatically zoom map to show entire geofence
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : antelBoundary) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 100; // offset from edges in pixels

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void addWakeUpGeofence(LatLng center, float radiusMeters) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId("village-wakeup")
                .setCircularRegion(center.latitude, center.longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(this, GeofenceBroadcastReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d("GEOFENCE", "Wake-up geofence added"))
                    .addOnFailureListener(e -> Log.e("GEOFENCE", "Failed to add wake-up geofence", e));
        }
    }
    private float getMaxDistanceFromCenter(LatLng center, List<LatLng> points) {
        float maxDistance = 0;
        Location centerLoc = new Location("");
        centerLoc.setLatitude(center.latitude);
        centerLoc.setLongitude(center.longitude);

        for (LatLng p : points) {
            Location pointLoc = new Location("");
            pointLoc.setLatitude(p.latitude);
            pointLoc.setLongitude(p.longitude);
            float distance = centerLoc.distanceTo(pointLoc);
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        return maxDistance;
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



    //Location Updates

    @SuppressWarnings("deprecation")
    private void startLocationUpdates() {
        locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location loc = locationResult.getLastLocation();
                LatLng currentLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                // Polygon check for precision
                if (!PolyUtil.containsLocation(currentLatLng, antelBoundary, true)) {
                    Toast.makeText(MainPage.this, "Outside village boundary!", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("GEOFENCE", "Inside village boundary");
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
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
                    } else {
                        Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void logRideToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Log.e("RideLog", "User is null - not logged in");
            return;
        }

        String userId = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "unknown";
        long rideEndTime = System.currentTimeMillis();

        // Make sure start time is valid
        if (rideStartTime == 0 || rideEndTime < rideStartTime) {
            Toast.makeText(this, "Invalid ride time", Toast.LENGTH_SHORT).show();
            Log.e("RideLog", "Invalid rideStartTime: " + rideStartTime);
            return;
        }

        // Compute duration
        long rideDurationMillis = rideEndTime - rideStartTime;
        int rideDurationMinutes = (int) (rideDurationMillis / 60000);

        // Ensure distance is valid
        if (Double.isNaN(totalDistance) || totalDistance < 0) {
            totalDistance = 0.0F;
            Log.w("RideLog", "Distance invalid. Set to 0.");
        }

        // Timestamps
        Timestamp start = new Timestamp(new Date(rideStartTime));
        Timestamp end = new Timestamp(new Date(rideEndTime));

        // Ride log entry
        Map<String, Object> rideLog = new HashMap<>();
        rideLog.put("user_id", userId);
        rideLog.put("user_email", email);
        rideLog.put("start_time", start);
        rideLog.put("end_time", end);
        rideLog.put("duration_minutes", rideDurationMinutes);
        rideLog.put("distance_traveled", totalDistance);
        rideLog.put("status", "Completed");

        // Upload to Firestore
        FirebaseFirestore.getInstance()
                .collection("ride_logs")
                .add(rideLog)
                .addOnSuccessListener(docRef -> {
                    Log.d("RideLog", "Ride log saved: " + docRef.getId());
                    Toast.makeText(this, "Ride logged successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("RideLog", "Failed to save ride log", e);
                    Toast.makeText(this, "Failed to log ride", Toast.LENGTH_LONG).show();
                });
    }



    private void updateUserStats(String userId, double distanceInKm) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.collection("users").document(userId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            int totalRides = 0;
            double totalDistance = 0;

            if (snapshot.exists()) {
                String totalRidesStr = snapshot.getString("totalRides");
                String totalDistanceStr = snapshot.getString("totalDistance");

                try {
                    totalRides = totalRidesStr != null ? Integer.parseInt(totalRidesStr) : 0;
                    totalDistance = totalDistanceStr != null ? Double.parseDouble(totalDistanceStr.replace(" km", "")) : 0;
                } catch (NumberFormatException ignored) {}
            }

            totalRides++;
            totalDistance += distanceInKm;

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

