package com.example.currencyconverter.api;

import java.util.HashMap;

import com.example.currencyconverter.entity.RateResponse;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface CurrencyApi {
    @GET("/latest.json")
    void getRates(@Query("app_id") String key, Callback<RateResponse> callback);

    @GET("/currencies.json")
    void getCurrencyMappings(@Query("app_id") String key, Callback<HashMap<String, String>> callback);
}
