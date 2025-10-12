package com.example.finalprojectmobilecomputing;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

/**
 * Foreground service to keep ride timer and location tracking active
 * even when app is in background or closed
 */
public class RideTimerService extends Service {
    
    private static final String TAG = "RideTimerService";
    private static final String CHANNEL_ID = "RideTimerChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    public static final String ACTION_START_RIDE = "com.example.finalprojectmobilecomputing.START_RIDE";
    public static final String ACTION_STOP_RIDE = "com.example.finalprojectmobilecomputing.STOP_RIDE";
    public static final String ACTION_UPDATE_UI = "com.example.finalprojectmobilecomputing.UPDATE_RIDE_UI";
    
    public static final String EXTRA_BIKE_ID = "bike_id";
    public static final String EXTRA_QR_CODE = "qr_code";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_RIDE_TIME = "ride_time";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_START_TIME = "start_time";
    
    private RideStateManager rideStateManager;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long rideStartTime;
    private float totalDistance = 0f;
    private Location previousLocation;
    
    // Location tracking
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        rideStateManager = RideStateManager.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        timerHandler = new Handler(Looper.getMainLooper());
        
        createNotificationChannel();
        
        // Setup location tracking
        setupLocationTracking();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, "Service started with null intent, checking for existing ride state");
            
            // Check if there's an existing ride state to restore
            if (rideStateManager.isRideActive()) {
                Log.d(TAG, "Restoring ride from saved state");
                rideStartTime = rideStateManager.getRideStartTime();
                totalDistance = rideStateManager.getTotalDistance();
                
                // Start foreground with notification
                startForeground(NOTIFICATION_ID, createNotification());
                startTimer();
                startLocationUpdates();
                
                return START_STICKY;
            } else {
                Log.w(TAG, "No active ride found, stopping service");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        
        String action = intent.getAction();
        Log.d(TAG, "Service started with action: " + action);
        
        if (ACTION_START_RIDE.equals(action)) {
            handleStartRide(intent);
        } else if (ACTION_STOP_RIDE.equals(action)) {
            handleStopRide();
        }
        
        // START_STICKY ensures service restarts if killed by system
        return START_STICKY;
    }
    
    private void handleStartRide(Intent intent) {
        String bikeId = intent.getStringExtra(EXTRA_BIKE_ID);
        String qrCode = intent.getStringExtra(EXTRA_QR_CODE);
        String userId = intent.getStringExtra(EXTRA_USER_ID);
        String rideTime = intent.getStringExtra(EXTRA_RIDE_TIME);
        String amount = intent.getStringExtra(EXTRA_AMOUNT);
        rideStartTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis());
        
        Log.d(TAG, "Starting ride: bikeId=" + bikeId + ", startTime=" + rideStartTime);
        
        // Save ride state
        rideStateManager.saveRideState(bikeId, qrCode, userId, rideTime, amount, rideStartTime);
        
        // Update AppData for immediate access
        AppData.getInstance().setBikeId(bikeId);
        AppData.getInstance().setQrCode(qrCode);
        AppData.getInstance().setUserId(userId);
        AppData.getInstance().setRideTime(rideTime);
        AppData.getInstance().setAmount(amount);
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start timer and location tracking
        startTimer();
        startLocationUpdates();
    }
    
    private void handleStopRide() {
        Log.d(TAG, "Stopping ride");
        
        // Stop timer and location updates
        stopTimer();
        stopLocationUpdates();
        
        // Clear ride state
        rideStateManager.clearRideState();
        totalDistance = 0f;
        previousLocation = null;
        
        // Stop foreground service
        stopForeground(true);
        stopSelf();
    }
    
    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - rideStartTime;
                
                // Update ride progress in shared preferences
                rideStateManager.updateRideProgress(totalDistance, elapsed);
                
                // Update notification
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, createNotification());
                }
                
                // Broadcast update to UI
                broadcastRideUpdate(elapsed);
                
                // Schedule next update
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        };
        
        timerHandler.post(timerRunnable);
        Log.d(TAG, "Timer started");
    }
    
    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            Log.d(TAG, "Timer stopped");
        }
    }
    
    private void setupLocationTracking() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) return;
                
                // Calculate distance traveled
                if (previousLocation != null) {
                    float distanceInMeters = previousLocation.distanceTo(location);
                    totalDistance += distanceInMeters;
                    
                    // Update ride state
                    long elapsed = System.currentTimeMillis() - rideStartTime;
                    rideStateManager.updateRideProgress(totalDistance, elapsed);
                    
                    Log.d(TAG, "Distance updated: " + totalDistance + "m");
                }
                
                previousLocation = location;
            }
        };
    }
    
    @SuppressWarnings("deprecation")
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
            .setInterval(2000)  // Update every 2 seconds
            .setFastestInterval(1000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setSmallestDisplacement(2.0f); // Update if moved at least 2 meters
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, 
                locationCallback, 
                Looper.getMainLooper()
            );
            Log.d(TAG, "Location updates started");
        } else {
            Log.w(TAG, "Location permission not granted");
        }
    }
    
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        }
    }
    
    private void broadcastRideUpdate(long elapsedMillis) {
        Intent intent = new Intent(ACTION_UPDATE_UI);
        intent.putExtra("elapsed_millis", elapsedMillis);
        intent.putExtra("total_distance", totalDistance);
        sendBroadcast(intent);
    }
    
    private Notification createNotification() {
        // Create intent to open MainPage when notification is tapped
        Intent intent = new Intent(this, MainPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Calculate elapsed time
        long elapsed = System.currentTimeMillis() - rideStartTime;
        int seconds = (int) (elapsed / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        
        // Calculate distance in km
        float distanceKm = totalDistance / 1000f;
        String distanceStr = String.format(Locale.getDefault(), "%.2f km", distanceKm);
        
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_directions_bike_24)
            .setContentTitle("Ride in Progress")
            .setContentText("Time: " + timeStr + " • Distance: " + distanceStr)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE);
        
        return builder.build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ride Timer",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows ongoing ride timer and distance");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        stopTimer();
        stopLocationUpdates();
    }
}

