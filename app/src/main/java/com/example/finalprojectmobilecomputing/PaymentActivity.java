package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class PaymentActivity extends AppCompatActivity {

    interface PayMongoApi {
        @Headers({
                "Content-Type: application/json"
        })
        @POST("sources")
        Call<Map<String, Object>> createSource(@Body Map<String, Object> body);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build the PayMongo API client
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String credential = Credentials.basic("pk_test_T53JbiXQ5r1FN3LNj1pu13t5", "");
                    return chain.proceed(chain.request().newBuilder()
                            .header("Authorization", credential)
                            .build());
                })
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.paymongo.com/v1/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PayMongoApi api = retrofit.create(PayMongoApi.class);

        // Create source body for GCash payment
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("amount", 10000); // amount in centavos (₱100)
        attributes.put("redirect", Map.of(
                "success", "https://sikad-static.onrender.com/",
                "failed", "https://sikad-static.onrender.com/payment-failed.html"
        ));
        attributes.put("type", "gcash");
        attributes.put("currency", "PHP");

        Map<String, Object> data = new HashMap<>();
        data.put("data", Map.of("attributes", attributes));

        api.createSource(data).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Extract checkout URL from response
                        Map<String, Object> sourceData = (Map<String, Object>) ((Map<String, Object>) response.body().get("data")).get("attributes");
                        String checkoutUrl = (String) ((Map<String, Object>) sourceData.get("redirect")).get("checkout_url");

                        // Redirect to PayMongo's checkout URL for GCash payment
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl)));
                        finish();
                    } catch (Exception e) {
                        Log.e("PAYMENT", "Parse error", e);
                    }
                } else {
                    Log.e("PAYMENT", "API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PAYMENT", "Network failure", t);
            }
        });
    }
}
