package com.example.finalprojectmobilecomputing;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class RideTimeAlertActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private final int ALERT_DURATION = 10000; // 10 seconds (you can adjust this)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_time_alert);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Play alert sound
        mediaPlayer = MediaPlayer.create(this, R.raw.red_alert);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // Vibrate
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, 0));
            } else {
                vibrator.vibrate(new long[]{0, 500, 200, 500}, 0);
            }
        }

        // Automatically stop after ALERT_DURATION ms
        new Handler().postDelayed(this::stopAlertsAndFinish, ALERT_DURATION);
    }

    private void stopAlertsAndFinish() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) vibrator.cancel();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlertsAndFinish();
    }
}
