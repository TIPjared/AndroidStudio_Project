package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.util.Log;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (currentUser != null) {
                String uid = currentUser.getUid();
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("phoneVerified"))) {
                                Log.d(TAG, "User verified, going to MainPage");
                                startActivity(new Intent(SplashActivity.this, MainPage.class));
                            } else {
                                Log.d(TAG, "User not verified, redirecting to OTPVerification");
                                // optional: sign out to force re-login
                                auth.signOut();
                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                intent.putExtra("requireOTP", true);
                                startActivity(intent);
                            }
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error checking Firestore: " + e.getMessage());
                            auth.signOut();
                            startActivity(new Intent(SplashActivity.this, LandingPage.class));
                            finish();
                        });
            } else {
                Log.d(TAG, "No user logged in, going to LandingPage");
                startActivity(new Intent(SplashActivity.this, LandingPage.class));
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
