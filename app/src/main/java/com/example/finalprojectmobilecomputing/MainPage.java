package com.example.finalprojectmobilecomputing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.content.Context;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import com.google.android.material.button.MaterialButton;

import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;

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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private boolean isPhoneVisible = false;
    private String originalPhoneNumber = "";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private List<LatLng> antelBoundary;
    private Marker bikeMarker; // This will be the rented bike marker
    private Marker userMarker; // User's own location marker
    private List<LatLng> ridePathPoints = new ArrayList<>();
    private Polyline ridePath;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private NavigationView navigationView;
    private Button payButton;
    private Button stopButton;
    private GeofencingClient geofencingClient;
    private Map<String, LatLng> predefinedLocations = new HashMap<>();

    private boolean transactionAuthorized = false;
    
    // Bike rental GPS logic variables
    private Map<String, Marker> allBikeMarkers = new HashMap<>(); // All bike markers on map
    private String rentedBikeId = null; // ID of currently rented bike
    private LatLng rentedBikeLocation = null; // GPS location of rented bike
    private static final float PROXIMITY_THRESHOLD_METERS = 30f; // Distance to hide user marker
    private DatabaseReference bikesRef; // Reference to bikes in Firebase
    
    // Enhanced real-time tracking variables
    private LatLng lastUserLocation = null; // Last known user location for interpolation
    private LatLng lastBikeLocation = null; // Last known bike location for interpolation
    private long lastUserLocationTime = 0; // Timestamp of last user location update
    private long lastBikeLocationTime = 0; // Timestamp of last bike location update
    private Handler interpolationHandler = new Handler(Looper.getMainLooper()); // Handler for smooth interpolation

    // Firebase references - Realtime Database for GPS data, Firestore for user profiles
    private DatabaseReference rideLogsRef; // For GPS data in Realtime Database
    private FirebaseFirestore firestore; // For user profiles in Firestore
    private FirebaseUser currentUser;
    private String currentUserId;
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

    // Bike status tracking variables
    private com.google.firebase.database.ValueEventListener bikeListener;
    private com.google.firebase.database.DatabaseReference bikeStatusRef;

    // Emergency alert system
    private EmergencyAlertDialog emergencyAlert;


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
        
        // Red stop button with stop icon
        stopButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        stopButton.setText("Stop Ride");
        stopButton.setTextColor(getResources().getColor(android.R.color.white));
        stopButton.setTextSize(18);
        stopButton.setTypeface(null, android.graphics.Typeface.BOLD);
        stopButton.setElevation(8);
        
        // Add stop icon to the left
        Drawable stopIcon = getResources().getDrawable(R.drawable.baseline_stop_24);
        stopIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        stopButton.setCompoundDrawablesWithIntrinsicBounds(stopIcon, null, null, null);
        stopButton.setCompoundDrawablePadding(16);
        
        distanceTextView        = findViewById(R.id.distanceTextView);
        timerTextView           = findViewById(R.id.timerTextView);
        startTime               = System.currentTimeMillis();
        locationCard            = findViewById(R.id.locationCard);
        elapsedTimeTextView     = findViewById(R.id.elapsedTimeTextView);


        //startTimer();

        // XML layout already handles the icon positioning for proper centering

        // Firebase references - Realtime Database for GPS data, Firestore for user profiles
        rideLogsRef = FirebaseDatabase.getInstance().getReference("rideLogs"); // GPS data
        bikesRef = FirebaseDatabase.getInstance().getReference("gpsData"); // Available bikes GPS data
        firestore = FirebaseFirestore.getInstance(); // User profiles and feedback

        // Initialize current user for login tracking
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (currentUser != null) ? currentUser.getUid() : null;

        // Update last login timestamp
        updateLastLoginTimestamp();

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
            Intent intent;
            if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(MainPage.this, RideHistoryActivity.class));
            } else if (item.getItemId() == R.id.nav_support) {
                startActivity(new Intent(MainPage.this, SupportFeedbackActivity.class));
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
                stopRide();
            } else {
                if (currentLocation != null && antelBoundary != null && !antelBoundary.isEmpty()) {

                    // ===== Temporarily disabled geofence check =====
            /*
            if (PolyUtil.containsLocation(
                    new LatLng(currentLocation.latitude, currentLocation.longitude),
                    antelBoundary,
                    true
            )) {
                showRideDurationDialog();
            } else {
                Toast.makeText(this, "You must be inside Antel Grand Village to start a ride.", Toast.LENGTH_SHORT).show();
            }
            */

                    // Navigate to ride duration selection page
                    Intent intent = new Intent(MainPage.this, RideDurationActivity.class);
                    startActivity(intent);

                } else {
                    // Try to create user marker if location is available but marker doesn't exist
                    if (currentLocation != null && userMarker == null) {
                        createUserMarker();
                    }
                    Toast.makeText(this, "Waiting for current location data...", Toast.LENGTH_SHORT).show();
                }
            }
        });


        stopButton.setOnClickListener(v -> {
            // Retrieve scanned data from AppData
            String qrCode = AppData.getInstance().getQrCode();
            String currentUserId = AppData.getInstance().getUserId();

            if (qrCode == null || currentUserId == null) {
                Toast.makeText(MainPage.this, "No scanned bike or user found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Derive bikeId from QR code
            String bikeId = qrCode.split("_qr")[0];

            Map<String, String> endRidePayload = new HashMap<>();
            endRidePayload.put("bikeId", bikeId);
            endRidePayload.put("qrCode", qrCode);
            endRidePayload.put("userId", currentUserId);

            OkHttpClient client = new OkHttpClient();

            // --- Call /endRide ---
            RequestBody endRideBody = RequestBody.create(
                    new Gson().toJson(endRidePayload),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request endRideReq = new Request.Builder()
                    .url("https://sikad-server.onrender.com/endRide")
                    .post(endRideBody)
                    .build();

            client.newCall(endRideReq).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainPage.this, "Failed to end ride: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // --- Only after ending ride, lock the bike ---
                        Map<String, String> lockPayload = new HashMap<>();
                        lockPayload.put("bikeId", bikeId);

                        RequestBody lockBody = RequestBody.create(
                                new Gson().toJson(lockPayload),
                                MediaType.parse("application/json; charset=utf-8")
                        );

                        Request lockReq = new Request.Builder()
                                .url("https://sikad-server.onrender.com/lockBike")
                                .post(lockBody)
                                .build();

                        client.newCall(lockReq).enqueue(new okhttp3.Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() ->
                                        Toast.makeText(MainPage.this, "Failed to lock bike: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainPage.this, "Ride stopped and bike locked", Toast.LENGTH_SHORT).show();
                                    stopRide(); // Call stopRide() only after successful lock
                                });
                            }
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(MainPage.this, "Error ending ride", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });
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
        
        // End the rental and show all bikes again
        endRental();

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
            
            // Get the rented bike ID from AppData
            String bikeId = AppData.getInstance().getBikeId();
            if (bikeId != null && !bikeId.isEmpty()) {
                startRental(bikeId);
            }

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
            // Show red stop button
            stopButton.setVisibility(View.VISIBLE);
            stopButton.setText("Stop Ride");
            stopButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            stopButton.setTextColor(getResources().getColor(android.R.color.white));
            stopButton.setTextSize(18);
            stopButton.setTypeface(null, android.graphics.Typeface.BOLD);
            stopButton.setElevation(8);
            
            // Add stop icon to the left
            Drawable stopIcon = getResources().getDrawable(R.drawable.baseline_stop_24);
            stopIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            stopButton.setCompoundDrawablesWithIntrinsicBounds(stopIcon, null, null, null);
            stopButton.setCompoundDrawablePadding(16);
            
            // Hide Pay Now button during ride
            payButton.setVisibility(View.GONE);
            
                locationCard.setVisibility(View.VISIBLE);
            distanceTextView.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.VISIBLE);

            startRideTimer();
            startLocationUpdates();
            startTimer();
            } else {
            // Show Pay Now button
                payButton.setText("Pay Now");
                payButton.setVisibility(View.VISIBLE);
            payButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
            payButton.setTextColor(getResources().getColor(android.R.color.white));
            payButton.setTextSize(18);
            payButton.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // Hide stop button when not riding
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
        
        // Initialize ride tracking
        ridePathPoints.clear();
        if (bikeMarker != null) {
            bikeMarker.remove();
            bikeMarker = null;
        }
        if (ridePath != null) {
            ridePath.remove();
            ridePath = null;
        }

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

    private BitmapDescriptor fromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void loadProfileImageAndCreateMarker(LatLng position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // Load image using Picasso
                                new Thread(() -> {
                                    try {
                                        Bitmap profileBitmap = com.squareup.picasso.Picasso.get()
                                                .load(profileImageUrl)
                                                .resize(200, 200)
                                                .centerCrop()
                                                .get();
                                        
                                        runOnUiThread(() -> {
                                            BitmapDescriptor markerIcon = createMarkerWithProfileImage(profileBitmap);
                                            MarkerOptions markerOptions = new MarkerOptions()
                                                    .position(position)
                                                    .icon(markerIcon)
                                                    .anchor(0.5f, 1.0f)
                                                    .flat(false);
                                            bikeMarker = mMap.addMarker(markerOptions);
                                        });
                                    } catch (Exception e) {
                                        Log.e("ProfileMarker", "Error loading profile image", e);
                                        runOnUiThread(() -> createDefaultMarker(position));
                                    }
                                }).start();
                            } else {
                                createDefaultMarker(position);
                            }
                        } else {
                            createDefaultMarker(position);
                        }
                    })
                    .addOnFailureListener(e -> createDefaultMarker(position));
        } else {
            createDefaultMarker(position);
        }
    }

    private void createDefaultMarker(LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .icon(fromVector(MainPage.this, R.drawable.location_pin_marker))
                .anchor(0.5f, 1.0f)
                .flat(false);
        bikeMarker = mMap.addMarker(markerOptions);
    }

    private BitmapDescriptor createMarkerWithProfileImage(Bitmap profileBitmap) {
        // Larger marker size for better visibility (Life360 style)
        int markerSize = 140;
        int tailHeight = 40;
        Bitmap markerBitmap = Bitmap.createBitmap(markerSize, markerSize + tailHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centerX = markerSize / 2f;
        float centerY = markerSize / 2f;
        float radius = markerSize / 2f;

        // Create teardrop path
        Path teardropPath = new Path();
        teardropPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        teardropPath.moveTo(centerX - 15, centerY + radius - 10);
        teardropPath.lineTo(centerX, markerSize + tailHeight);
        teardropPath.lineTo(centerX + 15, centerY + radius - 10);
        teardropPath.close();

        // Draw green border (outer teardrop)
        paint.setColor(getResources().getColor(R.color.sikad_green));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(teardropPath, paint);

        // Draw profile image fitted inside the circular part of teardrop
        if (profileBitmap != null) {
            canvas.save();
            
            // Create circular clip path for profile image (slightly smaller than outer circle)
            Path clipPath = new Path();
            float imageRadius = radius - 8; // 8px border
            clipPath.addCircle(centerX, centerY, imageRadius, Path.Direction.CW);
            canvas.clipPath(clipPath);

            // Scale and draw profile image to fill the circle
            int imageSize = (int)(imageRadius * 2);
            Bitmap scaledProfile = Bitmap.createScaledBitmap(profileBitmap, imageSize, imageSize, true);
            
            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            imagePaint.setFilterBitmap(true);
            
            canvas.drawBitmap(scaledProfile, centerX - imageRadius, centerY - imageRadius, imagePaint);
            canvas.restore();
        }

        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
    }

    private void animateMarker(final Marker marker, final LatLng toPosition) {
        if (marker == null) return;
        
        final LatLng startLatLng = marker.getPosition();
        
        // Calculate distance to determine animation duration
        float[] results = new float[1];
        Location.distanceBetween(
                startLatLng.latitude, startLatLng.longitude,
                toPosition.latitude, toPosition.longitude,
                results
        );
        float distance = results[0];
        
        // Dynamic animation duration based on distance (smoother for short distances)
        long duration = Math.max(300, Math.min(1500, (long)(distance * 10))); // 300ms to 1500ms
        
        final Handler handler = new Handler(Looper.getMainLooper());
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    handler.postDelayed(this, 16); // 60fps for ultra-smooth animation
                }
            }
        });
    }

    /**
     * Ultra-smooth marker interpolation for real-time tracking
     * This method provides continuous smooth movement between location updates
     */
    private void interpolateMarkerSmoothly(final Marker marker, final LatLng targetPosition, final boolean isUserMarker) {
        if (marker == null) return;
        
        final LatLng startPosition = marker.getPosition();
        final long startTime = System.currentTimeMillis();
        final long duration = 1000; // 1 second interpolation
        
        interpolationHandler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(1.0f, (float) elapsed / duration);
                
                // Use ease-out interpolation for smoother movement
                float easedProgress = 1.0f - (1.0f - progress) * (1.0f - progress);
                
                double lat = startPosition.latitude + (targetPosition.latitude - startPosition.latitude) * easedProgress;
                double lng = startPosition.longitude + (targetPosition.longitude - startPosition.longitude) * easedProgress;
                
                marker.setPosition(new LatLng(lat, lng));
                
                if (progress < 1.0f) {
                    interpolationHandler.postDelayed(this, 16); // 60fps
                } else {
                    // Update tracking variables when interpolation completes
                    if (isUserMarker) {
                        lastUserLocation = targetPosition;
                        lastUserLocationTime = System.currentTimeMillis();
                    } else {
                        lastBikeLocation = targetPosition;
                        lastBikeLocationTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    /**
     * Enhanced marker animation with interpolation for ultra-smooth movement
     */
    private void animateMarkerWithInterpolation(final Marker marker, final LatLng toPosition, final boolean isUserMarker) {
        if (marker == null) return;
        
        final LatLng startLatLng = marker.getPosition();
        
        // Calculate distance to determine animation duration
        float[] results = new float[1];
        Location.distanceBetween(
                startLatLng.latitude, startLatLng.longitude,
                toPosition.latitude, toPosition.longitude,
                results
        );
        float distance = results[0];
        
        // Dynamic animation duration based on distance (smoother for short distances)
        long duration = Math.max(200, Math.min(1200, (long)(distance * 8))); // 200ms to 1200ms
        
        final Handler handler = new Handler(Looper.getMainLooper());
        final long start = SystemClock.uptimeMillis();
        
        // Use ease-in-out interpolator for more natural movement
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0f) {
                    handler.postDelayed(this, 16); // 60fps for ultra-smooth animation
                } else {
                    // Start continuous interpolation for smooth real-time tracking
                    interpolateMarkerSmoothly(marker, toPosition, isUserMarker);
                }
            }
        });
    }

    /**
     * Anti-stuck mechanism to ensure markers never freeze in place
     * This method provides fallback updates if markers haven't moved in a while
     */
    private void startAntiStuckMonitoring() {
        Handler antiStuckHandler = new Handler(Looper.getMainLooper());
        
        antiStuckHandler.post(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                
                // Check if user marker is stuck (no update for 5 seconds)
                if (userMarker != null && currentLocation != null && 
                    (currentTime - lastUserLocationTime) > 5000) {
                    Log.d("AntiStuck", "User marker appears stuck, forcing update");
                    animateMarkerWithInterpolation(userMarker, currentLocation, true);
                }
                
                // Check if bike marker is stuck (no update for 10 seconds)
                if (bikeMarker != null && rentedBikeLocation != null && 
                    (currentTime - lastBikeLocationTime) > 10000) {
                    Log.d("AntiStuck", "Bike marker appears stuck, forcing update");
                    animateMarkerWithInterpolation(bikeMarker, rentedBikeLocation, false);
                }
                
                // Schedule next check in 3 seconds
                antiStuckHandler.postDelayed(this, 3000);
            }
        });
    }

    /**
     * Enhanced marker creation with anti-stuck initialization
     */
    private void createUserMarkerWithAntiStuck() {
        if (currentLocation == null) return;
        
        createUserMarker();
        lastUserLocation = currentLocation;
        lastUserLocationTime = System.currentTimeMillis();
        
        // Start anti-stuck monitoring if not already started
        startAntiStuckMonitoring();
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

        // Clean up map elements
        if (bikeMarker != null) {
            bikeMarker.remove();
            bikeMarker = null;
        }
        if (ridePath != null) {
            ridePath.remove();
            ridePath = null;
        }
        ridePathPoints.clear();

        // Reset map camera to show the entire boundary
        if (antelBoundary != null && !antelBoundary.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : antelBoundary) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        requestLocationPermission();

        // Draw Antel Grand Village geofence
        listenToGeofenceChanges();

        // Load all available bikes on map
        loadAllBikes();

        // Remove any "destination" logic (markers are just visual now)
        mMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(this, "Marker: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
            return false;

        });
        
        // Ensure user marker is created if location is already available
        if (currentLocation != null && userMarker == null) {
            createUserMarker();
        }
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
                .setInterval(1000)  // Update every 1 second for smoother tracking
                .setFastestInterval(500)  // Fastest update every 0.5 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(1.0f); // Update if moved at least 1 meter

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location loc = locationResult.getLastLocation();
                if (loc == null) return;
                
                // Filter out inaccurate locations to prevent marker jumping
                if (loc.getAccuracy() > 50.0f) { // Only accept locations with accuracy better than 50 meters
                    Log.d("MainPage", "Location accuracy too low: " + loc.getAccuracy() + "m, skipping update");
                    return;
                }
                
                LatLng currentLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                
                // Check if location has changed significantly to avoid unnecessary updates
                if (currentLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            currentLocation.latitude, currentLocation.longitude,
                            currentLatLng.latitude, currentLatLng.longitude,
                            results
                    );
                    if (results[0] < 2.0f) { // Only update if moved more than 2 meters
                        return;
                    }
                }

                // Update current location
                currentLocation = currentLatLng;

                // Always update user marker position with enhanced smooth animation
                if (userMarker != null) {
                    animateMarkerWithInterpolation(userMarker, currentLatLng, true);
                } else {
                    createUserMarkerWithAntiStuck();
                }
                
                Log.d("MainPage", "Location updated: " + currentLatLng + " (accuracy: " + loc.getAccuracy() + "m), userMarker exists: " + (userMarker != null));

                // Update marker visibility based on proximity to rented bike
                if (transactionAuthorized && rentedBikeId != null) {
                    updateMarkerVisibility();
                }

                // Update ride path if ride is active
                if (transactionAuthorized) {
                    ridePathPoints.add(currentLatLng);
                    if (ridePath != null) {
                        ridePath.remove();
                    }
                    ridePath = mMap.addPolyline(new PolylineOptions()
                            .addAll(ridePathPoints)
                            .color(Color.BLUE)
                            .width(10));

                    // Move camera to follow the bike
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                }

                // Polygon check for precision
                if (antelBoundary != null && !antelBoundary.isEmpty()) {
                    if (!PolyUtil.containsLocation(currentLatLng, antelBoundary, true)) {
                        Toast.makeText(MainPage.this, "Outside village boundary!", Toast.LENGTH_LONG).show();
                    } else {
                        Log.d("GEOFENCE", "Inside village boundary");
                    }
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
                        
                        // Create user marker immediately when location is available
                        createUserMarker();
                        
                        // Start continuous location updates
                        startLocationUpdates();
                        
                        Log.d("MainPage", "Location obtained: " + currentLocation);
                    } else {
                        Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                        Log.e("MainPage", "Location is null");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainPage", "Failed to get location", e);
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * Create user marker on the map with profile picture
     */
    private void createUserMarker() {
        if (currentLocation == null) return;
        
        // Remove existing user marker if any
        if (userMarker != null) {
            userMarker.remove();
        }
        
        // Load profile image and create marker
        loadProfileImageAndCreateUserMarker(currentLocation);
        
        Log.d("MainPage", "User marker created at: " + currentLocation);
    }
    
    /**
     * Load profile image from Firebase and create user marker
     */
    private void loadProfileImageAndCreateUserMarker(LatLng position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // Load image using Picasso
                                new Thread(() -> {
                                    try {
                                        Bitmap profileBitmap = com.squareup.picasso.Picasso.get()
                                                .load(profileImageUrl)
                                                .resize(200, 200)
                                                .centerCrop()
                                                .get();
                                        
                                        runOnUiThread(() -> {
                                            BitmapDescriptor markerIcon = createUserMarkerWithProfileImage(profileBitmap);
                                            MarkerOptions markerOptions = new MarkerOptions()
                                                    .position(position)
                                                    .title("You")
                                                    .icon(markerIcon)
                                                    .anchor(0.5f, 1.0f);
                                            userMarker = mMap.addMarker(markerOptions);
                                        });
                                    } catch (Exception e) {
                                        Log.e("UserMarker", "Error loading profile image", e);
                                        runOnUiThread(() -> createDefaultUserMarker(position));
                                    }
                                }).start();
                            } else {
                                createDefaultUserMarker(position);
                            }
                        } else {
                            createDefaultUserMarker(position);
                        }
                    })
                    .addOnFailureListener(e -> createDefaultUserMarker(position));
        } else {
            createDefaultUserMarker(position);
        }
    }
    
    /**
     * Create default user marker (FULL BLUE teardrop)
     */
    private void createDefaultUserMarker(LatLng position) {
        // Create custom FULL BLUE teardrop marker
        BitmapDescriptor blueTeardropIcon = createSolidBlueTeardropMarker();
        
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title("You")
                .icon(blueTeardropIcon)
                .anchor(0.5f, 1.0f);
        userMarker = mMap.addMarker(markerOptions);
    }
    
    /**
     * Create solid blue teardrop marker (no white background)
     */
    private BitmapDescriptor createSolidBlueTeardropMarker() {
        int markerSize = 120;
        int tailHeight = 30;
        Bitmap markerBitmap = Bitmap.createBitmap(markerSize, markerSize + tailHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centerX = markerSize / 2f;
        float centerY = markerSize / 2f;
        float radius = markerSize / 2f;

        // Create teardrop path
        Path teardropPath = new Path();
        teardropPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        teardropPath.moveTo(centerX - 12, centerY + radius - 8);
        teardropPath.lineTo(centerX, markerSize + tailHeight);
        teardropPath.lineTo(centerX + 12, centerY + radius - 8);
        teardropPath.close();

        // Draw FULL BLUE teardrop (no white background)
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(teardropPath, paint);

        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
    }
    
    /**
     * Create user marker with profile image (FULL BLUE teardrop with profile picture)
     */
    private BitmapDescriptor createUserMarkerWithProfileImage(Bitmap profileBitmap) {
        // Larger marker size for better visibility
        int markerSize = 140;
        int tailHeight = 40;
        Bitmap markerBitmap = Bitmap.createBitmap(markerSize, markerSize + tailHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centerX = markerSize / 2f;
        float centerY = markerSize / 2f;
        float radius = markerSize / 2f;

        // Create teardrop path
        Path teardropPath = new Path();
        teardropPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        teardropPath.moveTo(centerX - 15, centerY + radius - 10);
        teardropPath.lineTo(centerX, markerSize + tailHeight);
        teardropPath.lineTo(centerX + 15, centerY + radius - 10);
        teardropPath.close();

        // Draw FULL BLUE teardrop (no white background) - USER MARKER IS FULL BLUE
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(teardropPath, paint);

        // Draw profile image fitted inside the circular part of teardrop
        if (profileBitmap != null) {
            canvas.save();
            
            // Create circular clip path for profile image (smaller than outer circle for blue border)
            Path clipPath = new Path();
            float imageRadius = radius - 12; // 12px blue border
            clipPath.addCircle(centerX, centerY, imageRadius, Path.Direction.CW);
            canvas.clipPath(clipPath);

            // Scale and draw profile image to fill the circle
            int imageSize = (int)(imageRadius * 2);
            Bitmap scaledProfile = Bitmap.createScaledBitmap(profileBitmap, imageSize, imageSize, true);
            
            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            imagePaint.setFilterBitmap(true);
            
            canvas.drawBitmap(scaledProfile, centerX - imageRadius, centerY - imageRadius, imagePaint);
            canvas.restore();
        }

        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
    }
    
    /**
     * Create available bike marker (FULL GREEN teardrop)
     */
    private BitmapDescriptor createAvailableBikeMarker() {
        int markerSize = 120;
        int tailHeight = 30;
        Bitmap markerBitmap = Bitmap.createBitmap(markerSize, markerSize + tailHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centerX = markerSize / 2f;
        float centerY = markerSize / 2f;
        float radius = markerSize / 2f;

        // Create teardrop path
        Path teardropPath = new Path();
        teardropPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        teardropPath.moveTo(centerX - 12, centerY + radius - 8);
        teardropPath.lineTo(centerX, markerSize + tailHeight);
        teardropPath.lineTo(centerX + 12, centerY + radius - 8);
        teardropPath.close();

        // Draw FULL GREEN teardrop (no white background) - AVAILABLE BIKE IS FULL GREEN
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(teardropPath, paint);

        // Draw white bike icon inside the green teardrop
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(radius * 0.6f);
        paint.setTextAlign(Paint.Align.CENTER);
        
        // Draw bike icon (simple bike symbol)
        canvas.drawText("🚲", centerX, centerY + radius * 0.2f, paint);

        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
    }
    
    /**
     * Create rented bike marker (white circle with bike design)
     */
    private BitmapDescriptor createRentedBikeMarker() {
        int markerSize = 100;
        Bitmap markerBitmap = Bitmap.createBitmap(markerSize, markerSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centerX = markerSize / 2f;
        float centerY = markerSize / 2f;
        float radius = markerSize / 2f;

        // Draw white circle background
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // Draw black border
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawCircle(centerX, centerY, radius - 2, paint);

        // Draw bike icon in the center
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(radius * 0.6f);
        paint.setTextAlign(Paint.Align.CENTER);
        
        // Draw bike icon (simple bike symbol)
        canvas.drawText("🚲", centerX, centerY + radius * 0.2f, paint);

        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
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

    // Cache user data to avoid repeated Firebase calls
    public static String cachedUsername = "";
    public static String cachedPhone = "";
    public static String cachedProfileImageUrl = "";
    public static String cachedEmail = "";
    public static boolean dataLoaded = false;

    // Convert phone number from +63 format to 09 format for display
    private String formatPhoneForDisplay(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "Phone not set";
        }
        // Convert +63XXXXXXXXXX to 09XXXXXXXXXX
        if (phone.startsWith("+63")) {
            return "0" + phone.substring(3);
        }
        // Convert 63XXXXXXXXXX to 09XXXXXXXXXX
        if (phone.startsWith("63") && phone.length() == 12) {
            return "0" + phone.substring(2);
        }
        // Already in correct format or unknown format
        return phone;
    }

    private void updatePhoneDisplay(TextView phoneView, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty() || phoneNumber.equals("Phone not set")) {
            phoneView.setText("Phone not set");
            return;
        }

        if (isPhoneVisible) {
            phoneView.setText(phoneNumber);
        } else {
            // Mask first 6 digits
            if (phoneNumber.length() > 6) {
                String masked = "******" + phoneNumber.substring(6);
                phoneView.setText(masked);
            } else {
                phoneView.setText("******");
            }
        }
    }

    private void loadUserProfileData(View headerView) {
        TextView profileName = headerView.findViewById(R.id.profileName);
        TextView profileEmail = headerView.findViewById(R.id.profileEmail);
        TextView profilePhone = headerView.findViewById(R.id.profilePhone);
        ImageView profileImage = headerView.findViewById(R.id.profileImage);
        ImageButton togglePhoneVisibility = headerView.findViewById(R.id.togglePhoneVisibility);
        
        // Get current Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("MainPage", "Loading user profile data. Current user: " + (currentUser != null ? currentUser.getUid() : "null"));
        
        if (currentUser != null && currentUser.getEmail() != null) {
            
            // Load cached data immediately for instant display
            if (dataLoaded) {
                profileEmail.setText(cachedEmail);
                profileName.setText(cachedUsername.isEmpty() ? "User Name" : cachedUsername);
                
                // Set up phone number masking - convert to 09 format for display
                originalPhoneNumber = formatPhoneForDisplay(cachedPhone);
                updatePhoneDisplay(profilePhone, originalPhoneNumber);
                
                // Set up visibility toggle
                if (togglePhoneVisibility != null) {
                    togglePhoneVisibility.setSelected(isPhoneVisible);
                    togglePhoneVisibility.setOnClickListener(v -> {
                        isPhoneVisible = !isPhoneVisible;
                        togglePhoneVisibility.setSelected(isPhoneVisible);
                        updatePhoneDisplay(profilePhone, originalPhoneNumber);
                    });
                }
                
                // Load cached profile image
                if (!cachedProfileImageUrl.isEmpty()) {
                    com.squareup.picasso.Picasso.get()
                        .load(cachedProfileImageUrl)
                        .placeholder(R.drawable.baseline_person_outline_24)
                        .error(R.drawable.baseline_person_outline_24)
                        .into(profileImage);
                }
            } else {
                // Set email from Firebase Auth immediately
                cachedEmail = currentUser.getEmail();
                profileEmail.setText(cachedEmail);
            }
            
            // Only fetch from Firebase if we don't have cached data or need to refresh
            if (!dataLoaded) {
                Log.d("MainPage", "Fetching user data from Firestore for user: " + currentUser.getUid());
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                        Log.d("MainPage", "Firestore query successful. Document exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                            String phone = documentSnapshot.getString("phone");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        
                            Log.d("MainPage", "Retrieved data - username: " + username + ", phone: " + phone + ", imageUrl: " + profileImageUrl);
                            
                            // Cache the data
                            cachedUsername = username != null ? username : "";
                            cachedPhone = phone != null ? phone : "";
                            cachedProfileImageUrl = profileImageUrl != null ? profileImageUrl : "";
                            dataLoaded = true;
                            
                            // Update UI with fresh data
                            profileName.setText(cachedUsername.isEmpty() ? "User Name" : cachedUsername);
                            
                            // Set up phone masking for fresh data - convert to 09 format for display
                            originalPhoneNumber = formatPhoneForDisplay(cachedPhone);
                            updatePhoneDisplay(profilePhone, originalPhoneNumber);
                            
                            // Set up visibility toggle for fresh data
                            if (togglePhoneVisibility != null) {
                                togglePhoneVisibility.setSelected(isPhoneVisible);
                                togglePhoneVisibility.setOnClickListener(v -> {
                                    isPhoneVisible = !isPhoneVisible;
                                    togglePhoneVisibility.setSelected(isPhoneVisible);
                                    updatePhoneDisplay(profilePhone, originalPhoneNumber);
                                });
                            }
                            
                            Log.d("MainPage", "Updated UI with cached data");
                            
                            // Load profile image
                            if (!cachedProfileImageUrl.isEmpty()) {
                            com.squareup.picasso.Picasso.get()
                                    .load(cachedProfileImageUrl)
                                .placeholder(R.drawable.baseline_person_outline_24)
                                .error(R.drawable.baseline_person_outline_24)
                                .into(profileImage);
                        }
                        } else {
                            Log.d("MainPage", "User document does not exist in Firestore");
                            dataLoaded = true; // Mark as loaded to prevent repeated attempts
                    }
                })
                .addOnFailureListener(e -> {
                        Log.e("MainPage", "Error fetching user data from Firestore", e);
                    // In case of error, leave default text
                        dataLoaded = true; // Mark as loaded to prevent repeated attempts
                    });
            } else {
                Log.d("MainPage", "Using cached data, dataLoaded: " + dataLoaded);
            }
        }
    }

    // Method to refresh cache when profile data is updated
    public static void refreshUserCache() {
        dataLoaded = false;
        cachedUsername = "";
        cachedPhone = "";
        cachedProfileImageUrl = "";
        cachedEmail = "";
    }

    // Method to force refresh navigation header data
    public void refreshNavigationHeader() {
        refreshUserCache();
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            loadUserProfileData(headerView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Force refresh navigation header data when returning to the page
        refreshNavigationHeader();
        
        // Refresh user profile data in navigation header when returning to this screen
        View headerView = navigationView.getHeaderView(0);
        loadUserProfileData(headerView);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        listenToBikeStatus();
    }

    // Listen to bike status changes in Firebase Realtime Database
    private void listenToBikeStatus() {
        if (currentUserId == null) {
            Log.d("MainPage", "No current user ID, skipping bike status listener");
            return;
        }
        
        // Listen to bike status for the current user
        bikeStatusRef = FirebaseDatabase.getInstance()
            .getReference("bikeStatus")
            .child(currentUserId);
            
        bikeListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    Log.d("MainPage", "Bike status updated: " + status);
                    // Handle bike status changes here if needed
                }
            }
            
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e("MainPage", "Bike status listener cancelled: " + error.getMessage());
            }
        };
        
        bikeStatusRef.addValueEventListener(bikeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bikeListener != null && bikeStatusRef != null) {
            bikeStatusRef.removeEventListener(bikeListener);
            bikeListener = null;
            bikeStatusRef = null;
        }
    }

    private void updateLastLoginTimestamp() {
        if (currentUserId != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("lastLoginTimestamp", System.currentTimeMillis());

            firestore.collection("users").document(currentUserId)
                    .update(update)
                    .addOnSuccessListener(aVoid -> Log.d("MainPage", "Last login timestamp updated"))
                    .addOnFailureListener(e -> Log.e("MainPage", "Failed to update last login timestamp", e));
        }
    }

    // ==================== BIKE RENTAL GPS LOGIC ====================
    
    /**
     * Load all available bikes from Firebase and display them on the map
     * Combines GPS data from Realtime Database with bike status from Firestore
     */
    private void loadAllBikes() {
        if (mMap == null) return;
        
        Log.d("BikeRental", "Loading all bikes from Firebase (gpsData + Firestore bikes)");
        
        // Listen to GPS data from Realtime Database
        bikesRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                // Only load bikes if no active rental
                if (rentedBikeId != null) {
                    Log.d("BikeRental", "Active rental exists, skipping bike load");
                    return;
                }
                
                // Clear existing bike markers
                for (Marker marker : allBikeMarkers.values()) {
                    marker.remove();
                }
                allBikeMarkers.clear();
                
                // Add marker for each bike in gpsData
                for (com.google.firebase.database.DataSnapshot bikeSnapshot : snapshot.getChildren()) {
                    String bikeId = bikeSnapshot.getKey();
                    
                    // Get bike location from gpsData/bike_xxx/latest/
                    com.google.firebase.database.DataSnapshot latestSnapshot = bikeSnapshot.child("latest");
                    
                    // Parse latitude and longitude as strings
                    String latStr = latestSnapshot.child("latitude").getValue(String.class);
                    String lngStr = latestSnapshot.child("longitude").getValue(String.class);
                    
                    if (latStr != null && lngStr != null && bikeId != null) {
                        try {
                            double lat = Double.parseDouble(latStr);
                            double lng = Double.parseDouble(lngStr);
                            LatLng bikeLocation = new LatLng(lat, lng);
                            
                            // Get bike status from Firestore bikes collection
                            final String finalBikeId = bikeId;
                            firestore.collection("bikes").document(bikeId)
                                    .get()
                                    .addOnSuccessListener(document -> {
                                        boolean isAvailable = true;
                                        boolean isActive = false;
                                        
                                        if (document.exists()) {
                                            String status = document.getString("status");
                                            Boolean active = document.getBoolean("isActive");
                                            String rentedBy = document.getString("rentedBy");
                                            
                                            // Bike is available if: status is "available" AND not currently rented
                                            isAvailable = "available".equals(status) && (rentedBy == null || rentedBy.isEmpty());
                                            isActive = active != null && active;
                                            
                                            Log.d("BikeRental", "Bike " + finalBikeId + " - status: " + status + 
                                                  ", isActive: " + isActive + ", rentedBy: " + rentedBy + ", available: " + isAvailable);
                                        } else {
                                            Log.w("BikeRental", "Bike document not found in Firestore: " + finalBikeId);
                                        }
                                        
                                        // Create bike marker with appropriate design
                                        BitmapDescriptor markerIcon;
                                        if (isAvailable) {
                                            // Green teardrop for available bikes
                                            markerIcon = createAvailableBikeMarker();
                                        } else {
                                            // White circle with bike design for rented bikes
                                            markerIcon = createRentedBikeMarker();
                                        }
                                        
                                        MarkerOptions markerOptions = new MarkerOptions()
                                                .position(bikeLocation)
                                                .title("Bike " + finalBikeId)
                                                .snippet(isAvailable ? "Available" : "Rented")
                                                .icon(markerIcon)
                                                .anchor(0.5f, 1.0f);
                                        
                                        Marker marker = mMap.addMarker(markerOptions);
                                        allBikeMarkers.put(finalBikeId, marker);
                                        
                                        Log.d("BikeRental", "✓ Added " + (isAvailable ? "available" : "rented") + 
                                              " bike marker: " + finalBikeId + " at " + bikeLocation);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("BikeRental", "Failed to get bike status for: " + finalBikeId, e);
                                        // Still add marker as unknown status (blue)
                                        MarkerOptions markerOptions = new MarkerOptions()
                                                .position(bikeLocation)
                                                .title("Bike " + finalBikeId)
                                                .snippet("Status unknown")
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                                .anchor(0.5f, 1.0f);
                                        
                                        Marker marker = mMap.addMarker(markerOptions);
                                        allBikeMarkers.put(finalBikeId, marker);
                                    });
                            
                        } catch (NumberFormatException e) {
                            Log.e("BikeRental", "Failed to parse coordinates for bike: " + bikeId, e);
                        }
                    }
                }
                
                Log.d("BikeRental", "Bikes loading completed from gpsData");
            }
            
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e("BikeRental", "Failed to load bikes from gpsData: " + error.getMessage());
            }
        });
    }
    
    /**
     * Start a bike rental - hide all other bikes, show only rented bike
     * @param bikeId The ID of the rented bike
     */
    private void startRental(String bikeId) {
        rentedBikeId = bikeId;
        Log.d("BikeRental", "Starting rental for bike: " + bikeId);
        
        // Update bike status in Firestore
        Map<String, Object> rentalUpdate = new HashMap<>();
        rentalUpdate.put("status", "rented");
        rentalUpdate.put("rentedBy", currentUserId);
        rentalUpdate.put("rentalStartTime", System.currentTimeMillis());
        
        firestore.collection("bikes").document(bikeId)
                .update(rentalUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d("BikeRental", "✓ Bike status updated to 'rented' in Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e("BikeRental", "Failed to update bike status in Firestore", e);
                });
        
        // Hide all bike markers
        hideAllBikeMarkers();
        
        // Listen to rented bike GPS updates
        listenToRentedBikeGPS(bikeId);
        
        Toast.makeText(this, "Rental started for bike " + bikeId, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * End the bike rental - show all bikes again
     */
    private void endRental() {
        if (rentedBikeId == null) {
            Log.w("BikeRental", "endRental called but no active rental");
            return;
        }
        
        Log.d("BikeRental", "Ending rental for bike: " + rentedBikeId);
        
        // Update bike status in Firestore back to available
        Map<String, Object> rentalEndUpdate = new HashMap<>();
        rentalEndUpdate.put("status", "available");
        rentalEndUpdate.put("rentedBy", null);
        rentalEndUpdate.put("rentalEndTime", System.currentTimeMillis());
        
        final String endedBikeId = rentedBikeId;
        firestore.collection("bikes").document(rentedBikeId)
                .update(rentalEndUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d("BikeRental", "✓ Bike " + endedBikeId + " status updated to 'available' in Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e("BikeRental", "Failed to update bike status in Firestore", e);
                });
        
        // Remove rented bike marker
        if (bikeMarker != null) {
            bikeMarker.remove();
            bikeMarker = null;
        }
        
        // Remove user marker
        if (userMarker != null) {
            userMarker.remove();
            userMarker = null;
        }
        
        rentedBikeId = null;
        rentedBikeLocation = null;
        
        // Show all bikes again
        loadAllBikes();
        
        Toast.makeText(this, "Rental ended. All bikes visible again.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Hide all bike markers during rental
     */
    private void hideAllBikeMarkers() {
        for (Marker marker : allBikeMarkers.values()) {
            marker.setVisible(false);
        }
        Log.d("BikeRental", "Hidden " + allBikeMarkers.size() + " bike markers");
    }
    
    /**
     * Listen to rented bike's GPS location from Firebase with enhanced real-time tracking
     * @param bikeId The rented bike's ID
     */
    private void listenToRentedBikeGPS(String bikeId) {
        // Listen to gpsData/bike_xxx/latest/
        DatabaseReference rentedBikeRef = bikesRef.child(bikeId).child("latest");
        
        rentedBikeRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!transactionAuthorized || !bikeId.equals(rentedBikeId)) {
                    return; // Rental ended or different bike
                }
                
                // Parse latitude and longitude as strings
                String latStr = snapshot.child("latitude").getValue(String.class);
                String lngStr = snapshot.child("longitude").getValue(String.class);
                String timestampStr = snapshot.child("timestamp").getValue(String.class);
                
                if (latStr != null && lngStr != null) {
                    try {
                        double lat = Double.parseDouble(latStr);
                        double lng = Double.parseDouble(lngStr);
                        LatLng newBikeLocation = new LatLng(lat, lng);
                        
                        // Check if bike location has changed significantly to prevent unnecessary updates
                        if (rentedBikeLocation != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    rentedBikeLocation.latitude, rentedBikeLocation.longitude,
                                    newBikeLocation.latitude, newBikeLocation.longitude,
                                    results
                            );
                            if (results[0] < 1.0f) { // Only update if moved more than 1 meter
                                return;
                            }
                        }
                        
                        rentedBikeLocation = newBikeLocation;
                        
                        // Update or create bike marker with enhanced smooth animation
                        if (bikeMarker == null) {
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(rentedBikeLocation)
                                    .title("Your Bike")
                                    .icon(createAvailableBikeMarker()) // Use green teardrop for rented bike
                                    .anchor(0.5f, 1.0f);
                            bikeMarker = mMap.addMarker(markerOptions);
                        } else {
                            animateMarkerWithInterpolation(bikeMarker, rentedBikeLocation, false);
                        }
                        
                        // Update marker visibility based on distance
                        updateMarkerVisibility();
                        
                        Log.d("BikeRental", "Rented bike location updated: " + rentedBikeLocation + 
                              (timestampStr != null ? " (timestamp: " + timestampStr + ")" : ""));
                    } catch (NumberFormatException e) {
                        Log.e("BikeRental", "Failed to parse bike coordinates", e);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e("BikeRental", "Failed to get rented bike location: " + error.getMessage());
            }
        });
    }
    
    /**
     * Calculate distance between user and rented bike
     * Show/hide user marker based on proximity
     */
    private void updateMarkerVisibility() {
        if (currentLocation == null || rentedBikeLocation == null || !transactionAuthorized) {
            return;
        }
        
        // Calculate distance between user and bike
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                rentedBikeLocation.latitude, rentedBikeLocation.longitude,
                results
        );
        float distanceMeters = results[0];
        
        Log.d("BikeRental", "Distance between user and bike: " + distanceMeters + "m");
        
        // If user is close to bike (< 30 meters), hide user marker
        if (distanceMeters < PROXIMITY_THRESHOLD_METERS) {
            // Hide user marker, show only bike
            if (userMarker != null) {
                userMarker.setVisible(false);
            }
            if (bikeMarker != null) {
                bikeMarker.setVisible(true);
            }
            Log.d("BikeRental", "User close to bike - hiding user marker");
        } else {
            // User is far from bike, show both markers
            if (userMarker == null) {
                // Create user marker if it doesn't exist
                createUserMarkerWithAntiStuck();
            } else {
                userMarker.setVisible(true);
                animateMarkerWithInterpolation(userMarker, currentLocation, true);
            }
            
            if (bikeMarker != null) {
                bikeMarker.setVisible(true);
            }
            Log.d("BikeRental", "User far from bike - showing both markers");
        }
    }
}

