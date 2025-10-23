package com.example.finalprojectmobilecomputing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AlertActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Play sound - use system alert sound
        try {
            mediaPlayer = MediaPlayer.create(this, android.R.raw.alarm_klaxon);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // Fallback to system notification sound
            try {
                mediaPlayer = MediaPlayer.create(this, android.R.raw.notification);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                }
            } catch (Exception ex) {
                // No sound if both fail
                Log.e("AlertActivity", "Could not play alert sound", ex);
            }
        }

        // Vibrate
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, 0));
            } else {
                vibrator.vibrate(new long[]{0, 500, 200, 500}, 0);
            }
        }

        // Listen for broadcast to stop alert
        LocalBroadcastManager.getInstance(this).registerReceiver(stopReceiver,
                new IntentFilter("STOP_ALERT"));
    }

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopAlertsAndFinish();
        }
    };

    private void stopAlertsAndFinish() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (vibrator != null) vibrator.cancel();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlertsAndFinish();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopReceiver);
    }
}
