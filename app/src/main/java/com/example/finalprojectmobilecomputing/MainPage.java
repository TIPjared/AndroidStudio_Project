package com.example.finalprojectmobilecomputing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
import androidx.core.graphics.ColorUtils;
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
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.location.GeofencingClient;


public class MainPage extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean isPhoneVisible = false;
    private String originalPhoneNumber = "";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private Marker bikeMarker; // This will be the rented bike marker
    private Marker userMarker; // User's own location marker
    private List<LatLng> ridePathPoints = new ArrayList<>();
    private Polyline ridePath;
    private long lastAlertTime = 0;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private NavigationView navigationView;
    private Button payButton;
    private Button stopButton;
    private GeofencingClient geofencingClient;
    private Map<String, LatLng> predefinedLocations = new HashMap<>();
    private List<LatLng> activeGeofencePoints = new ArrayList<>();

    private boolean transactionAuthorized = false;
    private RideStateManager rideStateManager;
    private BroadcastReceiver rideUpdateReceiver;
    
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

    // ALERTS WIGGLE
    MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private boolean isOutOfGeofence = false;
    private boolean alertScreenActive = false;
    private static final int ALERT_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        // --- Initialize services & views ---
        fusedLocationClient     = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient        = LocationServices.getGeofencingClient(this);
        rideStateManager        = RideStateManager.getInstance(this);

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
        bikesRef = FirebaseDatabase.getInstance().getReference("bikes"); // Available bikes GPS data
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
                startActivity(new Intent(MainPage.this, LogHistoryActivity.class));
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
                if (currentLocation != null && activeGeofencePoints != null && !activeGeofencePoints.isEmpty()) {

                    boolean isInside = PolyUtil.containsLocation(
                            new LatLng(currentLocation.latitude, currentLocation.longitude),
                            activeGeofencePoints,
                            true
                    );

                    if (isInside) {
                        // ✅ Only allow access if inside geofence
                        Intent intent = new Intent(MainPage.this, RideDurationActivity.class);
                        startActivity(intent);
                    } else {
                        // 🚫 Show warning if outside area
                        Toast.makeText(this, "You must be inside Antel Grand Village to start a ride.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // ⚠️ Location not yet available
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
        
        // --- Setup broadcast receiver for ride updates from service ---
        setupRideUpdateReceiver();
        
        // --- Restore ride state if there's an active ride ---
        restoreRideState();
    }
    
    /**
     * Setup broadcast receiver to receive updates from RideTimerService
     */
    private void setupRideUpdateReceiver() {
        rideUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (RideTimerService.ACTION_UPDATE_UI.equals(intent.getAction())) {
                    long elapsedMillis = intent.getLongExtra("elapsed_millis", 0);
                    float distanceTraveled = intent.getFloatExtra("total_distance", 0f);

                    // Update UI with elapsed time
                    int seconds = (int) (elapsedMillis / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

                    if (elapsedTimeTextView != null) {
                        elapsedTimeTextView.setText("Elapsed: " + timeStr);
                    }

                    // Update distance UI
                    float distanceKm = distanceTraveled / 1000f;
                    if (distanceTextView != null) {
                        distanceTextView.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
                    }

                    totalDistance = distanceTraveled;
                }
            }
        };

        IntentFilter filter = new IntentFilter(RideTimerService.ACTION_UPDATE_UI);

        // ✅ Fix: Handle Android 12+ properly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            registerReceiver(rideUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(rideUpdateReceiver, filter);
        }

        Log.d("MainPage", "Broadcast receiver registered for ride updates");
    }


    /**
     * Restore ride state from SharedPreferences if there's an active ride
     */
    private void restoreRideState() {
        if (rideStateManager.isRideActive()) {
            Log.d("MainPage", "Restoring active ride state");
            
            RideStateManager.RideState state = rideStateManager.getRideState();
            if (state != null) {
                // Restore ride variables
                transactionAuthorized = true;
                rideStartTime = state.rideStartTime;
                startTime = state.rideStartTime;
                totalDistance = state.totalDistance;
                rentedBikeId = state.bikeId;
                
                // Restore AppData
                AppData.getInstance().setBikeId(state.bikeId);
                AppData.getInstance().setQrCode(state.qrCode);
                AppData.getInstance().setUserId(state.userId);
                AppData.getInstance().setRideTime(state.rideTime);
                AppData.getInstance().setAmount(state.amount);
                
                // Update UI to show ride is active
                locationCard.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                payButton.setVisibility(View.GONE);
                timerTextView.setVisibility(View.VISIBLE);
                distanceTextView.setVisibility(View.VISIBLE);
                elapsedTimeTextView.setVisibility(View.VISIBLE);
                
                // Display current elapsed time
                long elapsed = System.currentTimeMillis() - state.rideStartTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                String timeStr = String.format(Locale.getDefault(), "Elapsed: %02d:%02d", minutes, seconds);
                elapsedTimeTextView.setText(timeStr);
                
                // Display current distance
                float distanceKm = state.totalDistance / 1000f;
                distanceTextView.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
                
                // Start the bike rental display
                if (state.bikeId != null && !state.bikeId.isEmpty()) {
                    startRental(state.bikeId);
                }
                
                // Ensure foreground service is running
                ensureServiceRunning();
                
                Log.d("MainPage", "Ride state restored successfully");
                Toast.makeText(this, "Continuing your active ride", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d("MainPage", "No active ride to restore");
        }
    }
    
    /**
     * Ensure RideTimerService is running for the active ride
     */
    private void ensureServiceRunning() {
        Intent serviceIntent = new Intent(this, RideTimerService.class);
        serviceIntent.setAction(RideTimerService.ACTION_START_RIDE);
        
        RideStateManager.RideState state = rideStateManager.getRideState();
        if (state != null) {
            serviceIntent.putExtra(RideTimerService.EXTRA_BIKE_ID, state.bikeId);
            serviceIntent.putExtra(RideTimerService.EXTRA_QR_CODE, state.qrCode);
            serviceIntent.putExtra(RideTimerService.EXTRA_USER_ID, state.userId);
            serviceIntent.putExtra(RideTimerService.EXTRA_RIDE_TIME, state.rideTime);
            serviceIntent.putExtra(RideTimerService.EXTRA_AMOUNT, state.amount);
            serviceIntent.putExtra(RideTimerService.EXTRA_START_TIME, state.rideStartTime);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d("MainPage", "Foreground service started/ensured");
    }
    
    /**
     * Start the foreground service for a new ride
     */
    private void startRideService() {
        String bikeId = AppData.getInstance().getBikeId();
        String qrCode = AppData.getInstance().getQrCode();
        String userId = AppData.getInstance().getUserId();
        String rideTime = AppData.getInstance().getRideTime();
        String amount = AppData.getInstance().getAmount();
        
        if (bikeId == null || userId == null) {
            Log.w("MainPage", "Cannot start service: missing bike ID or user ID");
            return;
        }
        
        Intent serviceIntent = new Intent(this, RideTimerService.class);
        serviceIntent.setAction(RideTimerService.ACTION_START_RIDE);
        serviceIntent.putExtra(RideTimerService.EXTRA_BIKE_ID, bikeId);
        serviceIntent.putExtra(RideTimerService.EXTRA_QR_CODE, qrCode);
        serviceIntent.putExtra(RideTimerService.EXTRA_USER_ID, userId);
        serviceIntent.putExtra(RideTimerService.EXTRA_RIDE_TIME, rideTime);
        serviceIntent.putExtra(RideTimerService.EXTRA_AMOUNT, amount);
        serviceIntent.putExtra(RideTimerService.EXTRA_START_TIME, System.currentTimeMillis());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d("MainPage", "Ride foreground service started");
    }


    private void stopRide() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (timerHandler != null && timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        transactionAuthorized = false;
        
        // Stop the foreground service
        Intent serviceIntent = new Intent(this, RideTimerService.class);
        serviceIntent.setAction(RideTimerService.ACTION_STOP_RIDE);
        startService(serviceIntent);
        Log.d("MainPage", "Foreground service stop requested");
        
        // Clear ride state from SharedPreferences
        rideStateManager.clearRideState();
        
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
            
            // Start foreground service for ride tracking
            startRideService();
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

        // ✅ Get selected duration from AppData
        String rideTime = AppData.getInstance().getRideTime();
        long durationMillis;
        switch (rideTime) {
            case "45 minutes":
                durationMillis = 45 * 60 * 1000;
                break;
            case "1 hour":
                durationMillis = 60 * 60 * 1000;
                break;
            default:
                durationMillis = 30 * 60 * 1000;
                break;
        }
        // ✅ Start dynamic timer
        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            boolean alertShown = false; // declare here — scoped to timer
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String time = String.format(Locale.getDefault(), "Time Left: %02d:%02d", minutes, seconds);
                timerTextView.setText(time);

                if (millisUntilFinished <= 2 * 60 * 1000 && !alertShown) {
                    alertShown = true;
                    Intent intent = new Intent(MainPage.this, RideTimeAlertActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
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
        if (activeGeofencePoints != null && !activeGeofencePoints.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : activeGeofencePoints) {
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

        // Draw Geofence
        loadActiveGeofenceFromFirestore();

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
        
        // Add a delayed check to ensure user marker appears even if location takes time
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (userMarker == null && currentLocation != null) {
                Log.d("MainPage", "Creating delayed user marker");
                createUserMarker();
            }
        }, 2000); // 2 second delay
    }

    private void loadActiveGeofenceFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("geofence")
                .whereEqualTo("is_active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String name = document.getString("name");
                        String colorCode = document.getString("color_code");
                        List<Map<String, Object>> points = (List<Map<String, Object>>) document.get("points");

                        if (points != null && !points.isEmpty()) {
                            activeGeofencePoints.clear();
                            for (Map<String, Object> point : points) {
                                GeoPoint geoPoint = (GeoPoint) point.get("location");
                                if (geoPoint != null) {
                                    LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                    activeGeofencePoints.add(latLng);
                                }
                            }

                            // Draw the geofence area on the map
                            mMap.addPolygon(new PolygonOptions()
                                    .addAll(activeGeofencePoints)
                                    .strokeColor(Color.parseColor(colorCode))
                                    .fillColor(ColorUtils.setAlphaComponent(Color.parseColor(colorCode), 80)));

                            // Add geofences dynamically
                            // for (LatLng latLng : activeGeofencePoints) {
                            //    addGeofence(latLng.latitude, latLng.longitude);
                            // }


                            Log.d("Geofence", "Active geofence loaded: " + name);
                        }
                    } else {
                        Log.w("Geofence", "No active geofence found in Firestore.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Geofence", "Error fetching geofence data", e));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted - enable location services
                enableMyLocation();
                Log.d("MainPage", "Location permission granted");
            } else {
                Toast.makeText(this, "Location permission denied. Please enable location access to use the app.", Toast.LENGTH_LONG).show();
                Log.e("MainPage", "Location permission denied");
            }
        } else if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted – you can post notifications now
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                if (loc.getAccuracy() > 100.0f) { // Only accept locations with accuracy better than 100 meters
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

        // Enable the built-in location layer first
        mMap.setMyLocationEnabled(true);
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        currentLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        
                        // Create user marker immediately when location is available
                        createUserMarker();
                        
                        // Start continuous location updates
                        startLocationUpdates();
                        
                        Log.d("MainPage", "Location obtained: " + currentLocation);
                    } else {
                        // If last location is null, start location updates anyway
                        Log.w("MainPage", "Last location is null, starting location updates");
                        startLocationUpdates();
                        
                        // Show a default location (Manila area) as fallback
                        currentLocation = new LatLng(14.5995, 120.9842); // Manila coordinates
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        createUserMarker();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainPage", "Failed to get location", e);
                    // Start location updates anyway as a fallback
                    startLocationUpdates();
                    
                    // Show a default location (Manila area) as fallback
                    currentLocation = new LatLng(14.5995, 120.9842); // Manila coordinates
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    createUserMarker();
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
                        String name = documentSnapshot.getString("name");
                            String phone = documentSnapshot.getString("phone");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        
                            Log.d("MainPage", "Retrieved data - name: " + name + ", phone: " + phone + ", imageUrl: " + profileImageUrl);
                            
                            // Cache the data
                            cachedUsername = name != null ? name : "";
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
                    Object value = snapshot.getValue();         // Read as Object
                    String status = String.valueOf(value);      // Convert safely to String
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
        
        // Unregister broadcast receiver
        if (rideUpdateReceiver != null) {
            try {
                unregisterReceiver(rideUpdateReceiver);
                Log.d("MainPage", "Broadcast receiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.w("MainPage", "Receiver already unregistered");
            }
        }
        
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

        Log.d("BikeRental", "Setting up listener for all bikes...");

        // Use ChildEventListener for more granular updates
        bikesRef.addChildEventListener(new com.google.firebase.database.ChildEventListener() {

            // Called when a new bike is added
            @Override
            public void onChildAdded(@NonNull com.google.firebase.database.DataSnapshot bikeSnapshot, @Nullable String previousChildName) {
                // Only process if no active rental
                if (rentedBikeId == null) {
                    updateOrAddBikeMarker(bikeSnapshot);
                }
            }

            // Called when a bike's data changes (e.g., location, status)
            @Override
            public void onChildChanged(@NonNull com.google.firebase.database.DataSnapshot bikeSnapshot, @Nullable String previousChildName) {
                // Only process if no active rental
                if (rentedBikeId == null) {
                    updateOrAddBikeMarker(bikeSnapshot);
                }
            }

            // Called when a bike is removed
            @Override
            public void onChildRemoved(@NonNull com.google.firebase.database.DataSnapshot bikeSnapshot) {
                String bikeId = bikeSnapshot.getKey();
                if (bikeId != null && allBikeMarkers.containsKey(bikeId)) {
                    Marker marker = allBikeMarkers.get(bikeId);
                    if (marker != null) {
                        marker.remove(); // Remove marker from the map
                    }
                    allBikeMarkers.remove(bikeId); // Remove from our tracking map
                    Log.d("BikeRental", "Removed bike marker: " + bikeId);
                }
            }

            // Called when a bike's order changes (less relevant here)
            @Override
            public void onChildMoved(@NonNull com.google.firebase.database.DataSnapshot snapshot, @Nullable String previousChildName) {
                // Can often be ignored for map markers unless order matters
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e("BikeRental", "Failed to load bikes: " + error.getMessage());
            }
        });
    }

    private void updateOrAddBikeMarker(com.google.firebase.database.DataSnapshot bikeSnapshot) {
        String bikeId = bikeSnapshot.getKey();
        if (bikeId == null) return;

        // --- Read location and status directly ---
        Object latObj = bikeSnapshot.child("latitude").getValue();
        Object lngObj = bikeSnapshot.child("longitude").getValue();
        Object statusObj = bikeSnapshot.child("status").getValue(); // Get status from RTDB

        if (latObj != null && lngObj != null) {
            try {
                double lat = Double.parseDouble(latObj.toString());
                double lng = Double.parseDouble(lngObj.toString());
                LatLng bikeLocation = new LatLng(lat, lng);
                String status = (statusObj != null) ? statusObj.toString() : "unknown"; // Default status

                // Determine availability based *only* on RTDB status for efficiency
                boolean isAvailable = "AVAILABLE".equalsIgnoreCase(status); // Use AVAILABLE status from RTDB

                // --- Update existing marker or create a new one ---
                Marker existingMarker = allBikeMarkers.get(bikeId);

                if (existingMarker != null) {
                    // Marker exists, update its position and potentially icon/snippet
                    existingMarker.setPosition(bikeLocation);
                    BitmapDescriptor newIcon = isAvailable ? createAvailableBikeMarker() : createRentedBikeMarker();
                    existingMarker.setIcon(newIcon);
                    existingMarker.setSnippet(isAvailable ? "Available" : "Rented"); // Update snippet too
                    Log.d("BikeRental", "Updated marker: " + bikeId);

                } else {
                    // Marker doesn't exist, create a new one
                    BitmapDescriptor markerIcon = isAvailable ? createAvailableBikeMarker() : createRentedBikeMarker();
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(bikeLocation)
                            .title("Bike " + bikeId)
                            .snippet(isAvailable ? "Available" : "Rented")
                            .icon(markerIcon)
                            .anchor(0.5f, 1.0f);

                    Marker newMarker = mMap.addMarker(markerOptions);
                    allBikeMarkers.put(bikeId, newMarker); // Add to tracking map
                    Log.d("BikeRental", "Added new marker: " + bikeId);
                }

            } catch (NumberFormatException e) {
                Log.e("BikeRental", "Failed to parse coordinates for bike: " + bikeId, e);
            }
        } else {
            // If location data is missing, remove the marker if it exists
            Marker existingMarker = allBikeMarkers.get(bikeId);
            if (existingMarker != null) {
                existingMarker.remove();
                allBikeMarkers.remove(bikeId);
                Log.d("BikeRental", "Removed marker due to missing location: " + bikeId);
            }
        }
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
        DatabaseReference rentedBikeRef = bikesRef.child(bikeId);
        
        rentedBikeRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!transactionAuthorized || !bikeId.equals(rentedBikeId)) {
                    return; // Rental ended or different bike
                }
                
                // Parse latitude and longitude as strings
                Object latStr = snapshot.child("latitude").getValue();
                Object lngStr = snapshot.child("longitude").getValue();
                String timestampStr = snapshot.child("timestamp").getValue(String.class);

                if (latStr != null && lngStr != null) {
                    try {
                        double lat = Double.parseDouble(latStr.toString());
                        double lng = Double.parseDouble(lngStr.toString());
                        LatLng newBikeLocation = new LatLng(lat, lng);

                        boolean inside = isWithinGeofence(newBikeLocation);

                        if (!inside) {
                            long now = System.currentTimeMillis();
                            if (!alertScreenActive && now - lastAlertTime > 2000) { // 2s debounce
                                lastAlertTime = now;
                                triggerAlertScreen();
                            }
                        } else if (inside && alertScreenActive) {
                            alertScreenActive = false;
                            stopAlertScreen();
                        }

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

    private boolean isWithinGeofence(LatLng location) {
        if (activeGeofencePoints == null || activeGeofencePoints.isEmpty()) return true;
        return PolyUtil.containsLocation(location, activeGeofencePoints, false);
    }

    private void triggerAlertScreen() {
        if (alertScreenActive) return; // Already active, don't start again
        alertScreenActive = true;
        Intent intent = new Intent(MainPage.this, AlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void stopAlertScreen() {
        // Send broadcast to stop AlertActivity
        Intent stopIntent = new Intent("STOP_ALERT");
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(stopIntent);
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

