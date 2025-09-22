package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Set the privacy policy content
        TextView privacyContent = findViewById(R.id.privacyContent);
        privacyContent.setText(getPrivacyPolicyContent());
    }

    private String getPrivacyPolicyContent() {
        return "PRIVACY POLICY\n\n" +
                "Last updated: " + new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n" +
                
                "1. INTRODUCTION\n\n" +
                "Sikad is a smart bike rental system designed for residential communities. We are committed to protecting your privacy and ensuring the security of your personal information. This Privacy Policy explains how we collect, use, and safeguard your data when you use our mobile application and IoT-enabled bikes.\n\n" +
                
                "2. INFORMATION WE COLLECT\n\n" +
                "2.1 Personal Information:\n" +
                "• Name, email address, and phone number\n" +
                "• Age and gender (for demographic analytics)\n" +
                "• Payment information (processed securely through PayMongo)\n" +
                "• Emergency contact information\n\n" +
                
                "2.2 Usage Data:\n" +
                "• Bike rental history and ride duration\n" +
                "• GPS location data during rides\n" +
                "• Bike health and maintenance data\n" +
                "• App usage patterns and preferences\n\n" +
                
                "2.3 IoT Sensor Data:\n" +
                "• Accelerometer and gyroscope readings\n" +
                "• Motion patterns for theft detection\n" +
                "• Battery status and system health\n" +
                "• Geofencing boundary crossings\n\n" +
                
                "3. HOW WE USE YOUR INFORMATION\n\n" +
                "3.1 Core Services:\n" +
                "• Process bike rentals and payments\n" +
                "• Provide real-time GPS tracking\n" +
                "• Send notifications about rides and system updates\n" +
                "• Detect and prevent theft through AI algorithms\n\n" +
                
                "3.2 Safety and Security:\n" +
                "• Emergency contact notifications in case of incidents\n" +
                "• Geofencing enforcement for community safety\n" +
                "• Crash detection and automatic alerts\n" +
                "• Fraud prevention and account security\n\n" +
                
                "3.3 System Improvement:\n" +
                "• Analyze usage patterns to optimize bike placement\n" +
                "• Improve AI models for better theft detection\n" +
                "• Maintain bike health and performance\n" +
                "• Generate community mobility insights\n\n" +
                
                "4. DATA SHARING AND DISCLOSURE\n\n" +
                "4.1 We do NOT sell your personal data to third parties.\n\n" +
                "4.2 We may share data in these limited circumstances:\n" +
                "• With emergency contacts when safety is at risk\n" +
                "• With community administrators for system management\n" +
                "• With law enforcement if required by legal process\n" +
                "• With service providers (PayMongo, Firebase) under strict agreements\n\n" +
                
                "5. DATA SECURITY\n\n" +
                "5.1 Technical Safeguards:\n" +
                "• End-to-end encryption for all data transmission\n" +
                "• Secure token-based authentication\n" +
                "• Regular security audits and updates\n" +
                "• Firebase security rules and access controls\n\n" +
                
                "5.2 Physical Security:\n" +
                "• Encrypted data storage on IoT devices\n" +
                "• Secure server infrastructure on Render\n" +
                "• Regular backup and disaster recovery procedures\n\n" +
                
                "6. YOUR PRIVACY RIGHTS\n\n" +
                "6.1 You have the right to:\n" +
                "• Access your personal data\n" +
                "• Correct inaccurate information\n" +
                "• Delete your account and associated data\n" +
                "• Download your data in a portable format\n" +
                "• Opt-out of non-essential data collection\n\n" +
                
                "6.2 Privacy Controls:\n" +
                "• Adjust location sharing preferences\n" +
                "• Control notification settings\n" +
                "• Enable anonymous usage mode\n" +
                "• Manage emergency contact access\n\n" +
                
                "7. DATA RETENTION\n\n" +
                "7.1 We retain your data for:\n" +
                "• Active account: Until you delete your account\n" +
                "• Ride history: 2 years for analytics and support\n" +
                "• IoT sensor data: 30 days for system optimization\n" +
                "• Payment records: As required by financial regulations\n\n" +
                
                "8. CHILDREN'S PRIVACY\n\n" +
                "Sikad is designed for users aged 13 and above. We do not knowingly collect personal information from children under 13. If you believe a child has provided us with personal information, please contact us immediately.\n\n" +
                
                "9. COMMUNITY-SPECIFIC FEATURES\n\n" +
                "9.1 Geofencing:\n" +
                "• Location data is used to enforce community boundaries\n" +
                "• Automatic alerts when bikes leave designated areas\n" +
                "• Admin notifications for boundary violations\n\n" +
                
                "9.2 Safety Features:\n" +
                "• Crash detection using AI algorithms\n" +
                "• Emergency contact notifications\n" +
                "• Real-time location sharing during emergencies\n\n" +
                
                "10. UPDATES TO THIS POLICY\n\n" +
                "We may update this Privacy Policy periodically. We will notify you of any significant changes through the app or email. Continued use of Sikad after changes constitutes acceptance of the updated policy.\n\n" +
                
                "11. CONTACT INFORMATION\n\n" +
                "If you have questions about this Privacy Policy or your data, please contact us:\n" +
                "• Email: privacy@sikad.com\n" +
                "• Phone: +63-XXX-XXX-XXXX\n" +
                "• Address: Technological Institute of the Philippines, Manila\n\n" +
                
                "This Privacy Policy is designed specifically for the Sikad bike rental system and reflects our commitment to protecting your privacy while providing safe, efficient, and smart bike sharing services in your community.";
    }
}
