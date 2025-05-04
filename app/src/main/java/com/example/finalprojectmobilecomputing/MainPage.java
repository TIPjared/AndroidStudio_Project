package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainPage extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        drawerLayout = findViewById(R.id.drawerLayout);
        menuButton = findViewById(R.id.menuButton);
        navigationView = findViewById(R.id.navigationView);

        // Open the drawer when menu button is clicked
        menuButton.setOnClickListener(v -> {
            drawerLayout.openDrawer(Gravity.LEFT);
        });

        Button payButton = findViewById(R.id.payButton);
        payButton.setOnClickListener(v -> {
            startActivity(new Intent(MainPage.this, PaymentActivity.class));
        });

        // Set the welcome message
        TextView helloTextView = findViewById(R.id.helloTextView);
        helloTextView.setText("Hello, You have successfully Logged In!");

        // Navigation drawer item clicks
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.nav_history) {
                //startActivity(new Intent(MainPage.this, HistoryActivity.class));
            } else if (id == R.id.nav_saved_places) {
                //startActivity(new Intent(MainPage.this, SavedPlacesActivity.class));
            } else if (id == R.id.nav_support) {
                //startActivity(new Intent(MainPage.this, SupportActivity.class));
            } else if (id == R.id.nav_feedback) {
                //startActivity(new Intent(MainPage.this, FeedbackActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainPage.this, LandingPage.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // Access the header view to find profileLink
        TextView profileLink = navigationView.getHeaderView(0).findViewById(R.id.profileLink);
        profileLink.setOnClickListener(v -> {
            startActivity(new Intent(MainPage.this, Profile.class));
        });

        // Initialize Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Sample marker in Manila
        LatLng manila = new LatLng(14.5995, 120.9842);
        mMap.addMarker(new MarkerOptions().position(manila).title("Marker in Manila"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 12));
    }
}
