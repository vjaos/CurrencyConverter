package com.example.currencyconverter.entity;

import java.util.HashMap;

public class RateResponse {

    private long timestamp;
    private HashMap<String, Double> rates;

    public long getTimestamp() {
        return timestamp;
    }

    public HashMap<String, Double> getRates() {
        return rates;
    }
}
