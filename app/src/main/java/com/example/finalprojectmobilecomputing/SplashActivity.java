package com.example.finalprojectmobilecomputing;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000; // 2 seconds
    private static final String TAG = "SplashActivity";

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Handler().postDelayed(() -> {
            // Check if user is already logged in
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            
            Intent intent;
            if (currentUser != null) {
                // User is logged in, go directly to MainPage
                Log.d(TAG, "User already logged in: " + currentUser.getEmail());
                intent = new Intent(SplashActivity.this, MainPage.class);
            } else {
                // User not logged in, go to landing page
                Log.d(TAG, "No user logged in, going to landing page");
                intent = new Intent(SplashActivity.this, LandingPage.class);
            }
            
            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);

    }
}