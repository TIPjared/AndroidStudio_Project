package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ContactSupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_support);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Email contact button - Antel Grand Village
        TextView emailContact = findViewById(R.id.emailContact);
        emailContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:agvclubhouse@antel.com.ph"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Sikad App Support Request - Antel Grand Village");
            intent.putExtra(Intent.EXTRA_TEXT, "Hello Antel Grand Village Support Team,\n\nI need assistance with:\n\n");
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        // Phone contact button - Antel Grand Village Mobile Numbers
        TextView phoneContact = findViewById(R.id.phoneContact);
        phoneContact.setOnClickListener(v -> {
            // Show dialog with multiple contact options
            showContactOptionsDialog();
        });

        // Business hours info - Antel Grand Village
        TextView businessHours = findViewById(R.id.businessHours);
        businessHours.setText("Antel Grand Village - A Quiet Place in the City\n\nMonday - Friday: 8:00 AM - 6:00 PM\nSaturday: 9:00 AM - 5:00 PM\nSunday: Closed");
    }
    
    /**
     * Show contact options dialog with Antel Grand Village contact information
     */
    private void showContactOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contact Antel Grand Village");
        builder.setMessage("Choose how you'd like to contact us:");
        
        // Mobile numbers
        builder.setPositiveButton("📱 Mobile: 0917-704-0323", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+63-917-704-0323"));
            startActivity(intent);
        });
        
        // Second mobile number
        builder.setNeutralButton("📱 Mobile: 0917-708-0567", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+63-917-708-0567"));
            startActivity(intent);
        });
        
        // Office numbers
        builder.setNegativeButton("📞 Office Numbers", (dialog, which) -> {
            showOfficeNumbersDialog();
        });
        
        builder.show();
    }
    
    /**
     * Show office numbers dialog
     */
    private void showOfficeNumbersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Office Numbers");
        builder.setMessage("Choose the department you need:");
        
        // Accounting
        builder.setPositiveButton("📊 Accounting: 436-6026", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+63-2-436-6026"));
            startActivity(intent);
        });
        
        // Admin
        builder.setNegativeButton("👥 Admin: 436-6745", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+63-2-436-6745"));
            startActivity(intent);
        });
        
        builder.show();
    }
}
