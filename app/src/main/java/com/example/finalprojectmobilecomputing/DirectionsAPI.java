package com.example.finalprojectmobilecomputing;

import com.example.finalprojectmobilecomputing.DirectionsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsAPI {

    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("mode") String mode,
            @Query("alternatives") boolean alternatives,
            @Query("key") String apiKey
    );
}
