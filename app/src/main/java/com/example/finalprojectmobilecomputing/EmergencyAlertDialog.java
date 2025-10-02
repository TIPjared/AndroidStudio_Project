package com.example.finalprojectmobilecomputing;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmergencyAlertDialog {
    
    private Context context;
    private AlertDialog alertDialog;
    private Vibrator vibrator;
    
    public EmergencyAlertDialog(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    public void showGeofenceAlert(String alertType, String message) {
        // 🚨 NDRRMC-style Emergency Alert
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.emergency_alert_dialog, null);
        
        // Set up views
        TextView alertTitle = dialogView.findViewById(R.id.alertTitle);
        TextView alertMessage = dialogView.findViewById(R.id.alertMessage);
        TextView alertTime = dialogView.findViewById(R.id.alertTime);
        Button okButton = dialogView.findViewById(R.id.okButton);
        
        // Set NDRRMC-style alert content
        alertTitle.setText("🚨 SIKAD EMERGENCY ALERT 🚨");
        alertMessage.setText("GEOFENCE VIOLATION DETECTED!\n\n" + 
                           "You have left the designated bike rental area.\n" +
                           "Please return immediately to avoid automatic bike lock.\n\n" +
                           "Location: " + alertType + "\n" +
                           "Time: " + getCurrentTime());
        alertTime.setText("Alert Time: " + getCurrentTime());
        
        // Create alert dialog with NDRRMC-style urgency
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.EmergencyAlertTheme);
        builder.setView(dialogView);
        builder.setCancelable(false); // Cannot dismiss by tapping outside
        
        alertDialog = builder.create();
        
        // 🚨 Make it full-screen emergency alert like NDRRMC
        alertDialog.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        
        // Set urgent window type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        
        // Set up button click
        okButton.setOnClickListener(v -> {
            stopEmergencyEffects();
            alertDialog.dismiss();
        });
        
        // Show dialog
        alertDialog.show();
        
        // 🚨 Trigger NDRRMC-style emergency effects
        triggerEmergencyVibration();
        playEmergencySound();
        
        // Show toast notification
        Toast.makeText(context, "🚨 GEOFENCE VIOLATION - RETURN IMMEDIATELY 🚨", Toast.LENGTH_LONG).show();
    }
    
    // 🚨 NDRRMC-style Emergency Effects
    private void triggerEmergencyVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // NDRRMC-style emergency vibration pattern
                long[] pattern = {0, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200};
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
                vibrator.vibrate(effect);
            } else {
                // For older Android versions - emergency pattern
                long[] pattern = {0, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200, 100, 200};
                vibrator.vibrate(pattern, 0);
            }
        }
    }
    
    private void playEmergencySound() {
        try {
            // Play NDRRMC-style emergency alert sound
            Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone ringtone = RingtoneManager.getRingtone(context, alertSound);
            if (ringtone != null) {
                // Set volume to maximum for emergency
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
                }
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopEmergencyEffects() {
        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
        }
        
        // Stop any ongoing sounds
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
                    audioManager.getStreamVolume(AudioManager.STREAM_ALARM), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopAlert() {
        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
        }
        
        // Note: Ringtone stops automatically, no need to manually stop it
    }
    
    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a, dd MMM yy", 
                                                                       java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }
    
    public void dismiss() {
        if (alertDialog != null && alertDialog.isShowing()) {
            stopAlert();
            alertDialog.dismiss();
        }
    }
}
