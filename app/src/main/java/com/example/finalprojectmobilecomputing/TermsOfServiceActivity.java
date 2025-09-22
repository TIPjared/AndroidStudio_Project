package com.example.finalprojectmobilecomputing;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TermsOfServiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_of_service);

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Set the terms of service content
        TextView termsContent = findViewById(R.id.termsContent);
        termsContent.setText(getTermsOfServiceContent());
    }

    private String getTermsOfServiceContent() {
        return "TERMS OF SERVICE\n\n" +
                "Last updated: " + new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n" +
                
                "1. AGREEMENT TO TERMS\n\n" +
                "By using the Sikad bike rental system, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use our service.\n\n" +
                
                "2. DESCRIPTION OF SERVICE\n\n" +
                "Sikad is a smart bike rental system that provides:\n" +
                "• IoT-enabled bicycles for rent in residential communities\n" +
                "• Real-time GPS tracking and geofencing\n" +
                "• Mobile application for bike discovery and rental\n" +
                "• AI-powered theft detection and safety features\n" +
                "• Community-based bike sharing infrastructure\n\n" +
                
                "3. ELIGIBILITY\n\n" +
                "3.1 Age Requirements:\n" +
                "• Users must be at least 13 years old\n" +
                "• Users under 18 must have parental consent\n" +
                "• Valid identification may be required\n\n" +
                
                "3.2 Account Requirements:\n" +
                "• Valid email address and phone number\n" +
                "• Payment method (GCash, credit/debit card)\n" +
                "• Emergency contact information\n" +
                "• Agreement to community rules and regulations\n\n" +
                
                "4. BIKE RENTAL TERMS\n\n" +
                "4.1 Rental Process:\n" +
                "• Scan QR code to initiate rental\n" +
                "• Complete payment through PayMongo\n" +
                "• Two-step verification for bike unlocking\n" +
                "• Rental starts upon successful payment\n\n" +
                
                "4.2 Usage Rules:\n" +
                "• Stay within designated community boundaries\n" +
                "• Follow traffic laws and safety regulations\n" +
                "• Do not tamper with IoT devices or bike components\n" +
                "• Return bike to designated areas after use\n" +
                "• Report any issues or damages immediately\n\n" +
                
                "4.3 Prohibited Activities:\n" +
                "• Riding outside community boundaries\n" +
                "• Attempting to disable or damage IoT sensors\n" +
                "• Using bikes for illegal activities\n" +
                "• Sharing account credentials with others\n" +
                "• Unauthorized bike modifications\n\n" +
                
                "5. PAYMENT AND FEES\n\n" +
                "5.1 Rental Fees:\n" +
                "• Pay-per-ride pricing model\n" +
                "• Fees charged through PayMongo payment gateway\n" +
                "• All fees are non-refundable unless service failure\n\n" +
                
                "5.2 Additional Charges:\n" +
                "• Late return fees if bike not returned on time\n" +
                "• Damage fees for bike or IoT equipment damage\n" +
                "• Boundary violation penalties\n" +
                "• Emergency response fees for false alarms\n\n" +
                
                "6. SAFETY AND SECURITY\n\n" +
                "6.1 User Safety:\n" +
                "• Helmets recommended for all riders\n" +
                "• Follow community traffic rules\n" +
                "• Be aware of weather conditions\n" +
                "• Emergency contacts will be notified in case of incidents\n\n" +
                
                "6.2 Theft Prevention:\n" +
                "• AI-powered motion detection monitors for theft\n" +
                "• Geofencing alerts when bikes leave boundaries\n" +
                "• Automatic braking system for security violations\n" +
                "• SMS alerts to administrators for suspicious activity\n\n" +
                
                "7. LIABILITY AND INSURANCE\n\n" +
                "7.1 User Responsibility:\n" +
                "• Users ride at their own risk\n" +
                "• Personal insurance recommended\n" +
                "• Responsible for bike damage due to misuse\n" +
                "• Must report accidents or incidents immediately\n\n" +
                
                "7.2 System Limitations:\n" +
                "• GPS accuracy may vary in certain areas\n" +
                "• IoT sensors may malfunction due to weather\n" +
                "• Network connectivity required for full functionality\n" +
                "• Emergency response times may vary\n\n" +
                
                "8. DATA COLLECTION AND PRIVACY\n\n" +
                "8.1 Data Collection:\n" +
                "• Location data for GPS tracking and geofencing\n" +
                "• Motion sensor data for theft detection\n" +
                "• Usage patterns for system optimization\n" +
                "• Payment information through secure gateways\n\n" +
                
                "8.2 Privacy Rights:\n" +
                "• Control over data sharing preferences\n" +
                "• Right to access and delete personal data\n" +
                "• Anonymous usage mode available\n" +
                "• Detailed privacy policy available in app\n\n" +
                
                "9. SYSTEM AVAILABILITY\n\n" +
                "9.1 Service Availability:\n" +
                "• System available 24/7 with maintenance windows\n" +
                "• Weather-related service interruptions possible\n" +
                "• Network outages may affect functionality\n" +
                "• Emergency maintenance may be required\n\n" +
                
                "9.2 Technical Support:\n" +
                "• Support available through app and email\n" +
                "• Response times: 24-48 hours for non-emergencies\n" +
                "• Emergency support for safety-related issues\n" +
                "• Community administrator assistance available\n\n" +
                
                "10. COMMUNITY RULES\n\n" +
                "10.1 Respectful Use:\n" +
                "• Respect other community members\n" +
                "• Follow community-specific guidelines\n" +
                "• Report misuse or violations\n" +
                "• Maintain bike cleanliness and condition\n\n" +
                
                "10.2 Environmental Responsibility:\n" +
                "• Support eco-friendly transportation\n" +
                "• Reduce carbon footprint in community\n" +
                "• Promote sustainable mobility practices\n" +
                "• Participate in community improvement initiatives\n\n" +
                
                "11. ACCOUNT SUSPENSION AND TERMINATION\n\n" +
                "11.1 Grounds for Suspension:\n" +
                "• Violation of community boundaries\n" +
                "• Repeated damage to bikes or equipment\n" +
                "• Fraudulent payment activities\n" +
                "• Harassment of other users\n" +
                "• Tampering with IoT devices\n\n" +
                
                "11.2 Account Termination:\n" +
                "• Severe or repeated violations\n" +
                "• Illegal activities using the service\n" +
                "• Failure to pay fees or damages\n" +
                "• Violation of community safety rules\n\n" +
                
                "12. INTELLECTUAL PROPERTY\n\n" +
                "12.1 Sikad Technology:\n" +
                "• All IoT hardware and software is proprietary\n" +
                "• AI algorithms and theft detection systems\n" +
                "• Mobile application and backend systems\n" +
                "• Community management and analytics tools\n\n" +
                
                "12.2 User Content:\n" +
                "• Users retain rights to personal data\n" +
                "• Feedback and suggestions may be used for improvement\n" +
                "• No compensation for user-contributed ideas\n" +
                "• Respect for intellectual property of others\n\n" +
                
                "13. DISPUTE RESOLUTION\n\n" +
                "13.1 Dispute Process:\n" +
                "• Contact customer support first\n" +
                "• Community administrator mediation\n" +
                "• Formal complaint process available\n" +
                "• Legal action as last resort\n\n" +
                
                "13.2 Governing Law:\n" +
                "• Philippine law governs these terms\n" +
                "• Manila courts have jurisdiction\n" +
                "• Community-specific regulations apply\n" +
                "• International users subject to local laws\n\n" +
                
                "14. CHANGES TO TERMS\n\n" +
                "We may update these Terms of Service periodically. Users will be notified of significant changes through the app. Continued use after changes constitutes acceptance of updated terms.\n\n" +
                
                "15. CONTACT INFORMATION\n\n" +
                "For questions about these Terms of Service:\n" +
                "• Email: support@sikad.com\n" +
                "• Phone: +63-XXX-XXX-XXXX\n" +
                "• Address: Technological Institute of the Philippines, Manila\n" +
                "• Community Administrator: Available through app\n\n" +
                
                "These Terms of Service are designed specifically for the Sikad community bike rental system and reflect our commitment to providing safe, efficient, and sustainable transportation services.";
    }
}
