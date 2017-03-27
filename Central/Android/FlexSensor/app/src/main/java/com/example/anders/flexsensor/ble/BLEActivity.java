package com.example.anders.flexsensor.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anders.flexsensor.R;

/**
 * Establishes and makes BLE management and connection
 * Source: https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 */

public class BLEActivity extends Activity
        implements BLEDeviceScanner.ScanResultCallback, BLEDeviceControl.GATTCommunicator {
    private static final String TAG = BLEActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private BluetoothAdapter bluetoothAdapter;
    private BLEDeviceScanner deviceScanner;
    private BluetoothDevice device;
    private BLEDeviceControl deviceControl;

    private TextView deviceInfo;
    private Button connectButton;
    private TextView connectionState;
    private TextView angleTextView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        final BluetoothManager bluetoothManager = (BluetoothManager)
                getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        Log.d(TAG, "BLE adapter found");
        enableBLE();
        deviceScanner = new BLEDeviceScanner(bluetoothAdapter);
        deviceScanner.attach(this);
        Log.d(TAG, "Scanner created");
        deviceControl = new BLEDeviceControl(device);
        deviceControl.attach(this);

        deviceInfo = (TextView) findViewById(R.id.deviceInfoText);
        connectionState = (TextView) findViewById(R.id.connectStateText);
        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initializeConnection();
            }
        });
        angleTextView = (TextView) findViewById(R.id.angleTextView);

        startScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(deviceControl.getGattUpdateReceiver(),
                BLEDeviceControl.makeGattUpdateIntentFilter());
        deviceControl.reConnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(deviceControl.getGattUpdateReceiver());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deviceControl.disconnect(this);
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
        deviceControl.connect(this);
    }

    @Override
    public void foundDevice(BluetoothDevice device) {
        deviceScanner.scanBLEDevice(false);
        this.device = device;
        String deviceName = device.getName();
        Toast.makeText(this, "Found: " + deviceName,
                Toast.LENGTH_SHORT).show();
        deviceInfo.setText(deviceName);
        connectButton.setEnabled(true);
    }

    @Override
    public void deviceNotFound() {
        Toast.makeText(this, "No device found!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connected(final boolean isConnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status = isConnected ? "Connected" : "Not connected";
                connectionState.setText(status);
            }
        });
    }

    @Override
    public void dataReceived(String data) {
        angleTextView.setText(data);
    }
}
