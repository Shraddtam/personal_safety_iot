package com.example.myproject;

import android.app.Activity;
import android.widget.Toast;

public class SendHelper {
    public static void sendToAll(Activity activity, String location) {
        // Stub: Replace with actual SMS/Push logic
        Toast.makeText(activity, "Sending location: " + location, Toast.LENGTH_LONG).show();
    }
}
