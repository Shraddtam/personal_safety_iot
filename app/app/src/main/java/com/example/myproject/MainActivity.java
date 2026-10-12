package com.example.myproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private static final int REQUEST_SMS_PERMISSION = 1002;

    private BluetoothService bluetoothService;
    private LocationDatabaseHelper dbHelper;
    private ContactDatabaseHelper contactDbHelper;

    private final Handler bluetoothHandler = new Handler(msg -> {
        if (msg.what == 0) {
            String received = (String) msg.obj;

            if (received.startsWith("COORDS:")) {
                String coordsOnly = received.substring(7); // Remove "COORDS:"
                String[] parts = coordsOnly.split(",");

                if (parts.length == 2) {
                    String latitude = parts[0].trim();
                    String longitude = parts[1].trim();

                    Log.d("SMS_Debug", "Received valid coordinates: " + latitude + "," + longitude);

                    dbHelper.insertLocation(latitude, longitude);
                    Toast.makeText(this, "Location saved: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        sendSMSToAllContacts(latitude, longitude);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
                    }
                } else {
                    Toast.makeText(this, "Received malformed location: " + received, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Received message: " + received, Toast.LENGTH_SHORT).show();
            }
        } else if (msg.what == 1) {
            Toast.makeText(this, "Error: " + msg.obj, Toast.LENGTH_LONG).show();
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new LocationDatabaseHelper(this);
        contactDbHelper = new ContactDatabaseHelper(this);

        checkBluetoothPermissions();

        findViewById(R.id.btnAddContact).setOnClickListener(view ->
                startActivity(new Intent(this, AddContactActivity.class)));

        findViewById(R.id.btnViewContacts).setOnClickListener(view ->
                startActivity(new Intent(this, ViewContactsActivity.class)));

        findViewById(R.id.btnConnectKeychain).setOnClickListener(view ->
                startActivity(new Intent(this, DeviceListActivity.class)));

        findViewById(R.id.btnReceivedLocations).setOnClickListener(view ->
                startActivity(new Intent(this, SavedLocationsActivity.class)));
    }

    private void checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice targetDevice = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if ("ESP32_GPS_Tracker".equals(device.getName())) {
                targetDevice = device;
                break;
            }
        }

        if (targetDevice == null) {
            Toast.makeText(this, "ESP32_GPS_Tracker not paired", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            bluetoothService = new BluetoothService(targetDevice, bluetoothHandler, this);
            bluetoothService.connectAndListen();
            Toast.makeText(this, "Connecting to ESP32...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendSMSToAllContacts(String latitude, String longitude) {
        Toast.makeText(this, "Preparing to send SMS...", Toast.LENGTH_SHORT).show();

        List<Contact> contacts = contactDbHelper.getAllContacts();

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        String cleanLat = latitude.replaceAll("[°\\s+NSEW]", "");
        String cleanLon = longitude.replaceAll("[°\\s+NSEW]", "");
        String message = "Emergency! Location: https://maps.google.com/?q=" + cleanLat + "," + cleanLon;

        for (Contact contact : contacts) {
            try {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(contact.getPhone(), null, parts, null, null);
                Toast.makeText(this, "SMS sent to: " + contact.getPhone(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS to: " + contact.getPhone(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothService();
            } else {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted. Try again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
