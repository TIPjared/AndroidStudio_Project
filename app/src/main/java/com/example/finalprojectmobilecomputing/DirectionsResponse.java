package com.example.finalprojectmobilecomputing;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for Google Directions API
 * All classes are public to ensure they're accessible throughout the app
 */
public class DirectionsResponse {
    // Default constructor required by Gson
    public DirectionsResponse() {
        routes = new ArrayList<>();
    }
    
    public List<Route> routes;
    
    /**
     * Helper method to convert API route to a list of map points
     */
    public List<com.google.android.gms.maps.model.LatLng> getRoutePoints() {
        List<com.google.android.gms.maps.model.LatLng> routePoints = new ArrayList<>();
        if (routes == null || routes.isEmpty()) {
            return routePoints;
        }
        
        Route route = routes.get(0);
        if (route.legs == null || route.legs.isEmpty()) {
            return routePoints;
        }
        
        for (Leg leg : route.legs) {
            if (leg.steps == null) continue;
            
            for (Step step : leg.steps) {
                if (step.polyline != null && step.polyline.points != null) {
                    // Add all points from the polyline
                    List<LatLng> stepPoints = decodePoly(step.polyline.points);
                    for (LatLng point : stepPoints) {
                        routePoints.add(new com.google.android.gms.maps.model.LatLng(
                            point.lat, point.lng));
                    }
                }
            }
        }
        
        return routePoints;
    }
    
    /**
     * Method to decode polyline points from Google's encoded format
     * @param encoded The encoded polyline string
     * @return List of decoded LatLng points
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            
            LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }
        
        return poly;
    }

    public static class Route {
        public Route() {
            // Empty constructor for Gson
            legs = new ArrayList<>();
        }
        
        public List<Leg> legs;
        public PolylineOverview overview_polyline;
    }
    
    public static class PolylineOverview {
        public PolylineOverview() {
            points = "";
        }
        
        public String points;
    }

    public static class Leg {
        public Leg() {
            // Empty constructor for Gson
            steps = new ArrayList<>();
        }
        
        public List<Step> steps;
        public Distance distance;
        public Duration duration;
    }
    
    public static class Distance {
        public Distance() {
            text = "";
            value = 0;
        }
        
        public String text;
        public int value; // distance in meters
    }
    
    public static class Duration {
        public Duration() {
            text = "";
            value = 0;
        }
        
        public String text;
        public int value; // duration in seconds
    }

    public static class Step {
        public Step() {
            // Empty constructor for Gson
            polyline = new Polyline();
        }
        
        public LatLng start_location;
        public LatLng end_location;
        public Polyline polyline;
        public String html_instructions;
        public Distance distance;
        public Duration duration;
    }

    public static class Polyline {
        public Polyline() {
            // Empty constructor for Gson
            points = "";
        }
        
        public String points;
    }

    public static class LatLng {
        public LatLng() {
            // Empty constructor for Gson
            lat = 0;
            lng = 0;
        }
        
        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
        
        public double lat;
        public double lng;
    }
}
