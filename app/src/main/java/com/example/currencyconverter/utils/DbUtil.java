package com.example.currencyconverter.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import nl.qbusict.cupboard.convert.EntityConverter;

import java.util.List;

import com.example.currencyconverter.entity.Currency;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class DbUtil extends SQLiteOpenHelper {
    private static final String TAG = "SQLHelper";

    private static final String DATABASE_NAME = "currencyconverter.db";
    private static final int DATABASE_VERSION = 1;

    private static DbUtil instance = null;

    static {
        cupboard().register(Currency.class);
    }

    private Context context;

    private DbUtil(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;

    }


    public static DbUtil getInstance(Context context) {
        if (instance == null) {
            instance = new DbUtil(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database");
        cupboard().withDatabase(db).createTables();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        cupboard().withDatabase(db).upgradeTables();
    }

    public void addCurrency(Currency currency) {
        cupboard().withDatabase(getWritableDatabase()).put(currency);
    }

    public void updateCurrency(Currency currency) {
        EntityConverter<Currency> currencyEntityConverter = cupboard().getEntityConverter(Currency.class);
        ContentValues values = new ContentValues();
        currencyEntityConverter.toValues(currency, values);
        cupboard().withDatabase(getReadableDatabase()).update(Currency.class, values,
                "WHERE name=? AND code=?", currency.getName(), currency.getCode());
    }

    public boolean isDatabaseEmpty() {
        int count = cupboard().withDatabase(getReadableDatabase()).query(Currency.class).list().size();
        Log.i(TAG, "Count: " + count);
        return (count <= 0);
    }


    public void addAllCurrencies(List<Currency> currencies) {
        Log.i(TAG, "Inserting all currencies");
        boolean isEmpty = isDatabaseEmpty();
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            for (Currency currency : currencies) {
                if (isEmpty) {
                    addCurrency(currency);
                } else {
                    updateCurrency(currency);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while inserting currencies " + e.getLocalizedMessage());
        } finally {
            db.endTransaction();
        }
    }


    public List<Currency> findAll() {
        List<Currency> currencies = null;
        try {
            currencies = cupboard().withDatabase(getReadableDatabase())
                    .query(Currency.class)
                    .list();
        } catch (Exception e) {
            Log.e(TAG, "Cannot return all currencies: " + e.getLocalizedMessage());
        }
        return currencies;
    }


}
