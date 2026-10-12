package com.example.myproject;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private final BluetoothSocket socket;
    private InputStream inputStream;
    private final Handler handler;
    private final Context context;
    private Thread listenThread;

    public BluetoothService(BluetoothDevice device, Handler handler, Context context) throws IOException {
        this.handler = handler;
        this.context = context;

        UUID uuid = (device.getUuids() != null && device.getUuids().length > 0)
                ? device.getUuids()[0].getUuid()
                : UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Missing BLUETOOTH_CONNECT permission");
        }

        socket = device.createRfcommSocketToServiceRecord(uuid);
    }

    public void connectAndListen() {
        listenThread = new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission denied for BLUETOOTH_CONNECT");
                    return;
                }

                socket.connect();
                inputStream = socket.getInputStream();
                Log.d(TAG, "Connected to device");

                byte[] buffer = new byte[1024];
                int bytes;
                StringBuilder messageBuffer = new StringBuilder();

                while ((bytes = inputStream.read(buffer)) != -1) {
                    String part = new String(buffer, 0, bytes);
                    messageBuffer.append(part);

                    // Process complete lines
                    int end;
                    while ((end = messageBuffer.indexOf("\n")) != -1) {
                        String line = messageBuffer.substring(0, end).trim();
                        messageBuffer.delete(0, end + 1);

                        Log.d(TAG, "Received line: " + line);
                        handler.obtainMessage(0, line).sendToTarget();

                        // Check for GPS coordinates
                        if (line.contains("Latitude:") && line.contains("Longitude:")) {
                            try {
                                String latitude = line.substring(line.indexOf("Latitude:") + 9, line.indexOf("Longitude:")).trim();
                                String longitude = line.substring(line.indexOf("Longitude:") + 10).trim();

                                Log.d(TAG, "Parsed Latitude: " + latitude + ", Longitude: " + longitude);

                                // Save to DB
                                LocationDatabaseHelper dbHelper = new LocationDatabaseHelper(context);
                                dbHelper.insertLocation(latitude, longitude);
                                Log.d(TAG, "Location saved to database");

                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse and save coordinates", e);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);
                handler.obtainMessage(1, "Connection error: " + e.getMessage()).sendToTarget();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket", e);
                }
            }
        });
        listenThread.start();
    }

    public void stop() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
            }
            if (listenThread != null && listenThread.isAlive()) {
                listenThread.interrupt();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error stopping Bluetooth service", e);
        }
    }
}
