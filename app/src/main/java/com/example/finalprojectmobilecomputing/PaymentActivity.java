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
        @Headers({ "Content-Type: application/json" })
        @POST("sources")
        Call<Map<String, Object>> createSource(@Body Map<String, Object> body);
    }

    private String bikeId;
    private String qrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get extras from QRScannerActivity
        Intent intent = getIntent();
        bikeId = intent.getStringExtra("bike_id");
        qrCode = intent.getStringExtra("qr_code");

        if (bikeId == null || qrCode == null) {
            Log.e("PAYMENT", "Missing QR or Bike ID");
            finish();
            return;
        }

        // 1️⃣ Get one-time token from server, include bikeId + qrCode
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit serverRetrofit = new Retrofit.Builder()
                .baseUrl("https://sikad-server.onrender.com/") // your server URL
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ServerApi serverApi = serverRetrofit.create(ServerApi.class);

        serverApi.getToken(bikeId, qrCode).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().get("token");
                    createPayMongoSource(token);
                } else {
                    Log.e("PAYMENT", "Failed to get token");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e("PAYMENT", "Network failure", t);
            }
        });
    }

    private void createPayMongoSource(String token) {
        // 2️⃣ Build PayMongo API client
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String credential = okhttp3.Credentials.basic("pk_test_T53JbiXQ5r1FN3LNj1pu13t5", "");
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

        // 3️⃣ Prepare payment request
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("amount", 10000); // amount in centavos
        attributes.put("currency", "PHP");
        attributes.put("type", "gcash");

        Map<String, String> redirect = new HashMap<>();
        redirect.put("success", "https://sikad-server.onrender.com/success?token=" + token + "&bikeId=" + bikeId + "&qrCode=" + qrCode);
        redirect.put("failed", "https://sikad-static.onrender.com/?payment=failed");
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
