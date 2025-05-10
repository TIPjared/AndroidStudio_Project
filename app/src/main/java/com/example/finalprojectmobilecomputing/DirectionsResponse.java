package com.example.finalprojectmobilecomputing;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
    }

    public static class Leg {
        public List<Step> steps;
    }

    public static class Step {
        public LatLng end_location; // Adding end_location to Step class
        public Polyline polyline;
    }

    public static class Polyline {
        public String points;
    }

    public static class LatLng {
        public double lat;
        public double lng;

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
}
