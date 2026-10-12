package com.example.myproject;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;

public class SavedLocationsActivity extends AppCompatActivity {

    ListView locationList;
    LocationDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_locations);

        locationList = findViewById(R.id.locationList);
        dbHelper = new LocationDatabaseHelper(this);

        loadLocations();
    }

    private void loadLocations() {
        Cursor cursor = dbHelper.getAllLocations();
        String[] from = new String[]{"latitude", "longitude", "timestamp"};
        int[] to = new int[]{R.id.latText, R.id.lonText, R.id.timeText};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.location_item, cursor, from, to, 0);
        locationList.setAdapter(adapter);
    }
}
