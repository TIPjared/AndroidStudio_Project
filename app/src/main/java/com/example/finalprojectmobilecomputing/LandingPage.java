package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

        loginButton = findViewById(R.id.loginlanding);
        signupButton = findViewById(R.id.signuplanding);

        // Check if the user is already logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // If the user is logged in, check if their phone number exists (OTP verified)
            if (user.getPhoneNumber() != null) {
                // If phone number exists, the user is verified, go to MainPage
                startActivity(new Intent(LandingPage.this, MainPage.class));
            } else {
                // If phone number does not exist, the user has not completed OTP, go to OTPVerification
                startActivity(new Intent(LandingPage.this, OTPVerification.class));
            }
            finish(); // Close the LandingPage activity so the user cannot go back to it
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
                Intent loginIntent = new Intent(LandingPage.this, MainActivity.class);
                startActivity(loginIntent);
            }
        });

        // Set up the signup button to go to SignUpActivity if clicked
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signupIntent = new Intent(LandingPage.this, SignUpActivity.class);
                startActivity(signupIntent);
            }
        });
    }
}
