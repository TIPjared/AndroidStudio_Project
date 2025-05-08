package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainPage extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private NavigationView navigationView;
    private Button selectRouteButton, payButton;
    private TextView startLocationTextView, endLocationTextView;
    private Map<String, LatLng> predefinedLocations;
    private LatLng selectedStart, selectedEnd;
    private String selectedStartName, selectedEndName;
    private Polyline currentRoute;
    private boolean selectingStart = false, selectingEnd = false;

    private boolean transactionAuthorized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        drawerLayout = findViewById(R.id.drawerLayout);
        menuButton = findViewById(R.id.menuButton);
        navigationView = findViewById(R.id.navigationView);
        selectRouteButton = findViewById(R.id.selectRouteButton);
        payButton = findViewById(R.id.payButton);

        startLocationTextView = findViewById(R.id.startLocationTextView);
        endLocationTextView = findViewById(R.id.endLocationTextView);

        selectRouteButton.setText("Choose Location");
        selectRouteButton.setVisibility(View.GONE);

        setupPaymentCallback();

        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        payButton.setOnClickListener(v -> {
            if (!transactionAuthorized) {
                startActivity(new Intent(this, PaymentActivity.class));
            } else {
                cancelTransaction();
            }
        });

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainPage.this, LandingPage.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        selectRouteButton.setOnClickListener(v -> {
            resetSelectionsAndRoute();
            showStartSelectionPopup();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupPaymentCallback() {
        Uri data = getIntent().getData();
        if (data != null && "myapp".equals(data.getScheme()) && "main".equals(data.getHost())) {
            String status = data.getQueryParameter("status");
            if ("success".equals(status)) {
                transactionAuthorized = true;
                updateTransactionUI();
            }
        } else {
            updateTransactionUI(); // ensure UI is correct if app resumes from background
        }
    }

    private void updateTransactionUI() {
        if (transactionAuthorized) {
            payButton.setText("Cancel Transaction");
            selectRouteButton.setVisibility(View.VISIBLE);
        } else {
            payButton.setText("Pay Now");
            selectRouteButton.setVisibility(View.GONE);
            resetSelectionsAndRoute();
        }
    }

    private void cancelTransaction() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Transaction")
                .setMessage("Are you sure you want to cancel the transaction?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    transactionAuthorized = false;
                    updateTransactionUI();
                    Toast.makeText(this, "Transaction cancelled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        predefinedLocations = new HashMap<>();

        addMarker("TIP Arlegui", new LatLng(14.5947, 120.9876));
        addMarker("Manila City Hall", new LatLng(14.5896, 120.9814));
        addMarker("Rizal Park", new LatLng(14.5820, 120.9789));
        addMarker("Fort Santiago", new LatLng(14.5942, 120.9706));
        addMarker("Manila Cathedral", new LatLng(14.5916, 120.9739));
        addMarker("National Museum", new LatLng(14.5889, 120.9810));
        addMarker("Quiapo Church", new LatLng(14.5992, 120.9838));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(14.5947, 120.9845), 14));

        mMap.setOnMarkerClickListener(marker -> {
            if (selectingStart) {
                selectedStart = marker.getPosition();
                selectedStartName = marker.getTitle();
                startLocationTextView.setText("Start: " + selectedStartName);
                selectingStart = false;
                showEndSelectionPopup();
            } else if (selectingEnd) {
                selectedEnd = marker.getPosition();
                selectedEndName = marker.getTitle();
                endLocationTextView.setText("End: " + selectedEndName);
                selectingEnd = false;
                getRouteFromAPI(selectedStart, selectedEnd);
            }
            return false;
        });
    }

    private void resetSelectionsAndRoute() {
        selectedStart = null;
        selectedEnd = null;
        selectedStartName = null;
        selectedEndName = null;
        selectingStart = false;
        selectingEnd = false;

        startLocationTextView.setText("Start: Not selected");
        endLocationTextView.setText("End: Not selected");

        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }
    }

    private void showStartSelectionPopup() {
        new AlertDialog.Builder(this)
                .setTitle("Start Point")
                .setMessage("Select starting destination pin on map.")
                .setPositiveButton("OK", (dialog, which) -> selectingStart = true)
                .setCancelable(false)
                .show();
    }

    private void showEndSelectionPopup() {
        new AlertDialog.Builder(this)
                .setTitle("End Point")
                .setMessage("Select end destination pin on map.")
                .setPositiveButton("OK", (dialog, which) -> selectingEnd = true)
                .setCancelable(false)
                .show();
    }

    private void addMarker(String title, LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(title));
        predefinedLocations.put(title, latLng);
    }

    private void getRouteFromAPI(LatLng start, LatLng end) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DirectionsAPI directionsAPI = retrofit.create(DirectionsAPI.class);

        String origin = start.latitude + "," + start.longitude;
        String destination = end.latitude + "," + end.longitude;
        String key = getString(R.string.google_maps_key);

        Call<DirectionsResponse> call = directionsAPI.getDirections(origin, destination, key);
        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().routes != null && !response.body().routes.isEmpty()) {
                    List<LatLng> route = parseRoute(response.body());
                    if (!route.isEmpty()) {
                        drawRoute(route);
                    } else {
                        Toast.makeText(MainPage.this, "No route found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainPage.this, "Failed to fetch route", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(MainPage.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<LatLng> parseRoute(DirectionsResponse response) {
        List<LatLng> route = new ArrayList<>();
        if (response.routes != null && !response.routes.isEmpty()) {
            for (DirectionsResponse.Leg leg : response.routes.get(0).legs) {
                for (DirectionsResponse.Step step : leg.steps) {
                    route.addAll(decodePoly(step.polyline.points));
                }
            }
        }
        return route;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            polyline.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return polyline;
    }

    private void drawRoute(List<LatLng> route) {
        if (currentRoute != null) {
            currentRoute.remove();
        }

        currentRoute = mMap.addPolyline(new PolylineOptions()
                .addAll(route)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.get(0), 15));
        selectRouteButton.setText("Change Location");
    }
}
