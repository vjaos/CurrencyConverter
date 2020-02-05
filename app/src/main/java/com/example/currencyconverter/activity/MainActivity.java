package com.example.currencyconverter.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyconverter.R;
import com.example.currencyconverter.adapter.CurrencyAdapter;
import com.example.currencyconverter.api.CurrencyApi;
import com.example.currencyconverter.entity.Currency;
import com.example.currencyconverter.entity.RateResponse;
import com.example.currencyconverter.fragment.AboutDialogFragment;
import com.example.currencyconverter.fragment.ErrorDialogFragment;
import com.example.currencyconverter.utils.DbUtil;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    public static final String KEY_TIMESTAMP = "key_timestamp";
    private CurrencyApi currencyAPI;
    private DbUtil dbUtil;
    private CurrencyAdapter currencyAdapter;

    private HashMap<String, String> currencyMappings;
    private List<Currency> currencies;
    private RateResponse aResponse;

    private EditText currencyEditText;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private Button convertBtn;
    private ProgressBar progressBar;


    private String key;
    private Currency fromCurrency;
    private Currency toCurrency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        key = getString(R.string.key);

        currencyEditText = findViewById(R.id.edit_amount);
        progressBar = findViewById(R.id.progress_loading);
        fromSpinner = findViewById(R.id.spinner_from);
        toSpinner = findViewById(R.id.spinner_to);
        convertBtn = findViewById(R.id.btn_convert);

        initAdapter();
        initSpinnerOnSelect();
        initBtnOnClick();

        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                downloadData();
                return true;
            case R.id.action_about:
                AboutDialogFragment aboutDialogFragment = AboutDialogFragment.newInstance();
                aboutDialogFragment.show(getFragmentManager(), "FRAGMENT_ABOUT");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initAdapter() {
        final RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("https://openexchangerates.org/api")
                .build();

        currencyAPI = adapter.create(CurrencyApi.class);
        dbUtil = DbUtil.getInstance(this);

        if (dbUtil.isDatabaseEmpty()) {
            Log.i(TAG, "Downloading data");
            downloadData();

        } else {
            Log.i(TAG, "Loading data from db");
            currencyAdapter = new CurrencyAdapter(this);
            fromSpinner.setAdapter(currencyAdapter);
            toSpinner.setAdapter(currencyAdapter);
        }
    }

    private void initSpinnerOnSelect() {
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
                fromCurrency = (Currency) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toCurrency = (Currency) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initBtnOnClick() {
        convertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currencyEditText.getText().toString().isEmpty()) {
                    createAlertDialog("Amount can't be empty", "Please enter Amount");
                } else if (!currencyEditText.getText().toString().matches("\\d+|\\d+.\\d+")) {
                    createAlertDialog("Invalid input", "Please make sure the data is correct");
                } else if (fromSpinner.getSelectedItemPosition() == 0) {
                    createAlertDialog("Base currency can't be empty", "Choose a currency from which you wan to convert");
                } else if (toSpinner.getSelectedItemPosition() == 0) {
                    createAlertDialog("Convert currency can't be empty", "Choose a currency to which you want to convert");
                } else {
                    Double amount = Double.parseDouble(currencyEditText.getText().toString());
                    double calculatedAmount = Double.parseDouble(new DecimalFormat("##.###")
                            .format(toCurrency.getRate() * (1 / fromCurrency.getRate()) * amount)
                            .replace(',', '.'));
                    String resultStr = amount + " " + fromCurrency.getCode() + " = " + calculatedAmount + " " + toCurrency.getCode();
                    TextView result = findViewById(R.id.result);
                    result.setText(resultStr);
                }
            }

        });
    }

    public void downloadData() {

        if (!isNetworkConnected()) {
            ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(
                    getString(R.string.title_error_no_network),
                    getString(R.string.message_error_no_network)
            );
            fragment.show(getFragmentManager(), "FRAGMENT_ERROR");
        } else {
            progressBar.setVisibility(View.VISIBLE);
            convertBtn.setEnabled(false);

            currencyAPI.getCurrencyMappings(key, new Callback<HashMap<String, String>>() {
                @Override
                public void success(HashMap<String, String> stringStringHashMap, Response response) {
                    Log.i(TAG, "Got rates: " + stringStringHashMap.toString());
                    currencyMappings = stringStringHashMap;
                    currencyAPI.getRates(key, new Callback<RateResponse>() {
                        @Override
                        public void success(RateResponse rateResponse, Response response) {
                            Log.i(TAG, "Got names: " + rateResponse.getRates().toString());
                            aResponse = rateResponse;
                            Log.i(TAG, "Timestamp: " + rateResponse.getTimestamp());
                            SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
                            preferences.edit()
                                    .putLong(KEY_TIMESTAMP, rateResponse.getTimestamp())
                                    .apply();
                            if (currencyMappings != null) {
                                currencies = Currency.generateCurrencies(currencyMappings, aResponse);

                                Log.i(TAG, "Generated Currencies: " + Arrays.toString(currencies.toArray()));
                                dbUtil.addAllCurrencies(currencies);
                                initAdapter();
                                toSpinner.setAdapter(currencyAdapter);
                                progressBar.setVisibility(View.GONE);
                                convertBtn.setEnabled(true);

                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.e(TAG, error.getLocalizedMessage());
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, error.getLocalizedMessage());
                }
            });
        }
    }

    private void createAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        builder.show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}
