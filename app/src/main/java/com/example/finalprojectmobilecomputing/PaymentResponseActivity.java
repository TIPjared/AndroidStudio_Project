package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentResponseActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get the URI data
        Uri data = getIntent().getData();
        
        if (data != null) {
            String status = null;
            
            // Extract payment status based on scheme
            if ("myapp".equals(data.getScheme())) {
                // Handle myapp:// scheme
                status = data.getQueryParameter("status");
            } else if (data.toString().contains("sikad-static.onrender.com")) {
                // Handle HTTP scheme 
                status = data.getQueryParameter("payment");
            }
            
            // Create intent to return to MainPage with payment status
            Intent intent = new Intent(this, MainPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // Add the payment status
            if (status != null) {
                intent.putExtra("payment_status", status);
                
                if ("success".equals(status)) {
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Payment failed.", Toast.LENGTH_SHORT).show();
                }
            }
            
            // Start MainPage activity
            startActivity(intent);
        }
        
        // Close this activity
        finish();
    }
} 