package com.example.myproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 100;
    private BluetoothAdapter bluetoothAdapter;

    private final ArrayList<String> deviceNames = new ArrayList<>();
    private final HashMap<String, BluetoothDevice> deviceMap = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private TextView connectedDeviceStatus;
    private TextView bluetoothStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        ListView listView = findViewById(R.id.deviceListView);
        Button scanBtn = findViewById(R.id.scanBtn);
        bluetoothStatus = findViewById(R.id.bluetoothStatus);
        connectedDeviceStatus = findViewById(R.id.connectedDeviceStatus);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listView.setAdapter(adapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        updateBluetoothStatus();

        scanBtn.setOnClickListener(v -> {
            if (checkPermissions()) {
                startDiscovery();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = deviceNames.get(position);
            BluetoothDevice device = deviceMap.get(selected);
            if (device != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Missing Bluetooth permission", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, "Connecting to: " + device.getName(), Toast.LENGTH_SHORT).show();
                try {
                    BluetoothService btService = new BluetoothService(device, new Handler(msg -> {
                        String data = (String) msg.obj;
                        runOnUiThread(() -> {
                            Toast.makeText(DeviceListActivity.this, "Received: " + data, Toast.LENGTH_LONG).show();
                        });
                        return true;
                    }), this);
                    btService.connectAndListen();
                    runOnUiThread(() -> connectedDeviceStatus.setText("Connected to: " + device.getName()));
                } catch (IOException e) {
                    Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(bondStateReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothStatus.setText("Bluetooth is ON");
        } else {
            bluetoothStatus.setText("Bluetooth is OFF");
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scanPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connectPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            if (!scanPermission || !connectPermission) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_REQUEST);
                return false;
            }
        }
        return true;
    }

    private void startDiscovery() {
        deviceNames.clear();
        deviceMap.clear();
        adapter.notifyDataSetChanged();

        // Add already paired devices
        if (bluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (device.getName() != null) {
                    String info = device.getName() + "\n" + device.getAddress();
                    if (!deviceNames.contains(info)) {
                        deviceNames.add(info);
                        deviceMap.put(info, device);
                    }
                }
            }
            adapter.notifyDataSetChanged();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {
                    String info = device.getName() + "\n" + device.getAddress();
                    if (!deviceNames.contains(info)) {
                        deviceNames.add(info);
                        deviceMap.put(info, device);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                updateBluetoothStatus();
            }
        }
    };

    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(DeviceListActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (device != null && device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    String info = device.getName() + "\n" + device.getAddress();
                    if (!deviceNames.contains(info)) {
                        deviceNames.add(info);
                        deviceMap.put(info, device);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(bondStateReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (checkPermissions()) {
                startDiscovery();
            } else {
                Toast.makeText(this, "Bluetooth permissions denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
