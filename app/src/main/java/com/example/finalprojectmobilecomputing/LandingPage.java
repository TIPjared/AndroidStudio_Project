package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LandingPage extends AppCompatActivity {

    Button loginButton, signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing_page);

        try {
            loginButton = findViewById(R.id.loginlanding);
            signupButton = findViewById(R.id.signuplanding);

            if (loginButton == null || signupButton == null) {
                Log.e("LandingPage", "Buttons not found in layout");
                Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the user is already logged in and verified
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Check Firestore to see if phone is verified
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Boolean phoneVerified = document.getBoolean("phoneVerified");
                            
                            Log.d("LandingPage", "User found - phoneVerified: " + phoneVerified);
                            
                            if (phoneVerified != null && phoneVerified) {
                                // Phone is verified, go directly to MainPage
                                Log.d("LandingPage", "Phone verified, going to MainPage");
                                startActivity(new Intent(LandingPage.this, MainPage.class));
                                finish();
                            } else {
                                // Phone not verified, logout and show welcome page
                                Log.d("LandingPage", "Phone not verified, logging out and showing welcome");
                                FirebaseAuth.getInstance().signOut();
                                // Stay on landing page to show login/signup options
                            }
                        } else {
                            // No user record, logout
                            Log.d("LandingPage", "No user record found, logging out");
                            FirebaseAuth.getInstance().signOut();
                            // Stay on landing page
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LandingPage", "Error checking verification status", e);
                        // Stay on landing page on error
                    });
                return; // Exit early from onCreate method
            }

            // Continue with regular LandingPage flow if no user is logged in
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Set up the login button to go to MainActivity if clicked
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent loginIntent = new Intent(LandingPage.this, MainActivity.class);
                        startActivity(loginIntent);
                    } catch (Exception e) {
                        Log.e("LandingPage", "Error starting MainActivity", e);
                        Toast.makeText(LandingPage.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Set up the signup button to go to SignUpActivity if clicked
            signupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent signupIntent = new Intent(LandingPage.this, SignUpActivity.class);
                        startActivity(signupIntent);
                    } catch (Exception e) {
                        Log.e("LandingPage", "Error starting SignUpActivity", e);
                        Toast.makeText(LandingPage.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Log.e("LandingPage", "Error in onCreate", e);
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
