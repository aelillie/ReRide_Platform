package com.example.anders.flexsensor.BLE;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anders.flexsensor.R;

/**
 * Establishes and makes BLE management and connection
 * Source: https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 */

public class BLEActivity extends Activity implements BLEDeviceScanner.ScanResultCallback {
    public static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private BluetoothAdapter bluetoothAdapter;
    private BLEDeviceScanner deviceScanner;
    private BluetoothDevice device;

    private TextView deviceInfo;
    private Button connectButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final BluetoothManager bluetoothManager = (BluetoothManager)
                getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        enableBLE();
        deviceScanner = new BLEDeviceScanner(bluetoothAdapter);
        deviceScanner.attach(this);

        deviceInfo = (TextView) findViewById(R.id.deviceInfoText);
        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initializeConnection();
            }
        });

        startScan();
    }

    private void enableBLE() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBLEIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBLEIntent, BLEActivity.REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLEActivity.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "BLE enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "BLE activation failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static boolean isBLESupported(Context context) {
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    private void startScan() {
        deviceScanner.scanBLEDevice(true);
    }

    private void initializeConnection() {
        Intent connectService = new Intent(this, BLEService.class);
        startService(connectService);
    }

    @Override
    public void foundDevice(BluetoothDevice device) {
        deviceScanner.scanBLEDevice(false);
        this.device = device;
        Toast.makeText(this, "Found device!", Toast.LENGTH_SHORT).show();
        String deviceName = device.getName();
        deviceInfo.setText(deviceName);
        connectButton.setEnabled(true);
    }

    @Override
    public void deviceNotFound() {
        Toast.makeText(this, "No device found!", Toast.LENGTH_SHORT).show();
    }
}
