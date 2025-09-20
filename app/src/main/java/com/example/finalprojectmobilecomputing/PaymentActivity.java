package com.example.finalprojectmobilecomputing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class PaymentActivity extends AppCompatActivity {

    interface ServerApi {
        @GET("generate-token")
        Call<Map<String, String>> getToken(@Query("bikeId") String bikeId,
                                           @Query("qrCode") String qrCode);
    }

    interface PayMongoApi {

        @Headers({"Content-Type: application/json"})

        @POST("sources")
        Call<Map<String, Object>> createSource(@Body Map<String, Object> body);
    }

    interface ServerApi {
        @GET("/generate-token")
        Call<Map<String, Object>> generateToken(
                @Query("bikeId") String bikeId,
                @Query("qrCode") String qrCode,
                @Query("userId") String userId
        );
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get QR code and userId passed from QRScannerActivity
        String qrCode = getIntent().getStringExtra("qr_code");
        String userId = getIntent().getStringExtra("user_id");

        if (qrCode == null || userId == null) {
            Log.e("PAYMENT", "Missing qrCode or userId");

            finish();
            return;
        }

        // Derive bikeId from qrCode (e.g. bike_001_qr → bike_001)
        String bikeId = qrCode.split("_qr")[0];

        // Build logging client
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient baseClient = new OkHttpClient.Builder()

                .addInterceptor(interceptor)
                .build();

        // Retrofit for your backend server
        Retrofit serverRetrofit = new Retrofit.Builder()
                .baseUrl("https://sikad-server.onrender.com/") // <-- your server
                .client(baseClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ServerApi serverApi = serverRetrofit.create(ServerApi.class);

        // Step 1: Ask server for token
        serverApi.generateToken(bikeId, qrCode, userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = (String) response.body().get("token");
                    Log.d("PAYMENT", "Got token: " + token);

                    // Step 2: Proceed with PayMongo flow (same as your working code)
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
                    attributes.put("amount", 10000); // ₱100.00 in centavos
                    attributes.put("type", "gcash");
                    attributes.put("currency", "PHP");

                    // Redirect URLs now include bikeId, qrCode, userId, and token
                    Map<String, String> redirect = new HashMap<>();
                    redirect.put("success",
                            "https://sikad-server.onrender.com/success" +
                                    "?bikeId=" + bikeId +
                                    "&qrCode=" + qrCode +
                                    "&userId=" + userId +
                                    "&token=" + token);
                    redirect.put("failed", "https://sikad-server.onrender.com/failed");
                    attributes.put("redirect", redirect);

                    Map<String, Object> dataAttributes = new HashMap<>();
                    dataAttributes.put("attributes", attributes);

                    Map<String, Object> data = new HashMap<>();
                    data.put("data", dataAttributes);

                    api.createSource(data).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> responseData = (Map<String, Object>) response.body().get("data");

                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> sourceData = (Map<String, Object>) responseData.get("attributes");

                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> redirectData = (Map<String, Object>) sourceData.get("redirect");

                                    String checkoutUrl = (String) redirectData.get("checkout_url");

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
                } else {
                    Log.e("PAYMENT", "Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("PAYMENT", "Server Network failure", t);
            }
        });
    }
}
