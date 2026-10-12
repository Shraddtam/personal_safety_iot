package com.example.myproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "location_db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "locations";
    private static final String COL_ID = "id";
    private static final String COL_LAT = "latitude";
    private static final String COL_LON = "longitude";
    private static final String COL_TIMESTAMP = "timestamp";

    public LocationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LAT + " TEXT, " +
                COL_LON + " TEXT, " +
                COL_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertLocation(String latitude, String longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LAT, latitude);
        values.put(COL_LON, longitude);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id AS _id, latitude, longitude, timestamp FROM " + TABLE_NAME + " ORDER BY " + COL_TIMESTAMP + " DESC", null);
    }
}
