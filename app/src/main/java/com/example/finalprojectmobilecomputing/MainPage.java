package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

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
    private Button selectRouteButton;
    private Map<String, LatLng> predefinedLocations;
    private LatLng selectedStart, selectedEnd;
    private final String BASE_URL = "https://maps.googleapis.com/maps/api/directions/";
    private Polyline currentRoute;
    private boolean selectingStart = false, selectingEnd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        drawerLayout = findViewById(R.id.drawerLayout);
        menuButton = findViewById(R.id.menuButton);
        navigationView = findViewById(R.id.navigationView);

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        selectRouteButton = findViewById(R.id.selectRouteButton);
        selectRouteButton.setVisibility(View.GONE);

        // Check if the URI contains parameters indicating a successful payment
        Intent intent2 = getIntent();
        Uri data = intent2.getData();

        if (data != null && "myapp".equals(data.getScheme()) && "main".equals(data.getHost())) {
            String status = data.getQueryParameter("status");
            if ("success".equals(status)) {
                selectRouteButton.setVisibility(View.VISIBLE);
            }
        }

        Button payButton = findViewById(R.id.payButton);
        payButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainPage.this, PaymentActivity.class);
            startActivity(intent);
        });

        TextView helloTextView = findViewById(R.id.helloTextView);
        helloTextView.setText("Hello, You have successfully Logged In!");

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainPage.this, LandingPage.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        selectRouteButton.setOnClickListener(v -> {
            Toast.makeText(this, "Tap on map markers to choose start and end points", Toast.LENGTH_LONG).show();
            selectingStart = true;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
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
                selectingStart = false;
                selectingEnd = true;
                Toast.makeText(MainPage.this, "Start point selected. Now tap on end point.", Toast.LENGTH_SHORT).show();
            } else if (selectingEnd) {
                selectedEnd = marker.getPosition();
                selectingEnd = false;
                getRouteFromAPI(selectedStart, selectedEnd);
            }
            return false;
        });
    }

    private void addMarker(String title, LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(title));
        predefinedLocations.put(title, latLng);
    }

    private void getRouteFromAPI(LatLng start, LatLng end) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")  // Correct base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DirectionsAPI directionsAPI = retrofit.create(DirectionsAPI.class);

        String origin = start.latitude + "," + start.longitude;
        String destination = end.latitude + "," + end.longitude;
        String key = getString(R.string.google_maps_key);

        Log.d("API_REQUEST", "Origin: " + origin + " | Destination: " + destination + " | Key: " + key);

        Call<DirectionsResponse> call = directionsAPI.getDirections(origin, destination, key);

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Log the full response body for debugging
                    Log.d("API_RESPONSE", "Response: " + new Gson().toJson(response.body()));

                    // Check if routes are available
                    if (response.body().routes != null && !response.body().routes.isEmpty()) {
                        List<LatLng> route = parseRoute(response.body());
                        if (route.isEmpty()) {
                            Toast.makeText(MainPage.this, "No route found", Toast.LENGTH_SHORT).show();
                        } else {
                            drawRoute(route);
                        }
                    } else {
                        // If no routes were found, log the status
                        Log.e("API_RESPONSE", "No routes found. Status: " + response.body());
                        Toast.makeText(MainPage.this, "No route found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Log the error message if the API response is unsuccessful
                    Log.e("API_ERROR", "Directions API response error: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            String errorResponse = response.errorBody().string();
                            Log.e("API_ERROR", "Error Response: " + errorResponse);
                        } catch (Exception e) {
                            Log.e("API_ERROR", "Error while reading error body: " + e.getMessage());
                        }
                    }
                    Toast.makeText(MainPage.this, "No API route response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e("Retrofit Error", "Failed to get route: " + t.getMessage());
                Toast.makeText(MainPage.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<LatLng> parseRoute(DirectionsResponse directionsResponse) {
        List<LatLng> route = new ArrayList<>();
        if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
            for (DirectionsResponse.Leg leg : directionsResponse.routes.get(0).legs) {
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
            int dLat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lng += dLng;

            polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }

        return polyline;
    }

    private void drawRoute(List<LatLng> route) {
        if (route == null || route.isEmpty()) {
            Toast.makeText(this, "No route found", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentRoute != null) {
            currentRoute.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(route)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true);

        currentRoute = mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.get(0), 15));
    }
}
