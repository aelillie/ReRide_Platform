package com.example.anders.flexsensor.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for scanning for BLE devices
 */

class BLEDeviceScanner {
    private static final String TAG = BLEDeviceScanner.class.getSimpleName();

    private boolean scanning;
    private BluetoothLeScanner scanner;
    private List<BluetoothDevice> devices;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private BLECallback callback;
    private ScanResultCallback resultCallback;
    private boolean deviceFound;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;

    BLEDeviceScanner(BluetoothAdapter bluetoothAdapter) {
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        devices = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceName("ReRide");
        filters = new ArrayList<>();
        filters.add(builder.build());
        callback = new BLECallback();
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        int scanMode = ScanSettings.SCAN_MODE_BALANCED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        }
        settingsBuilder.setScanMode(scanMode);
        settings = settingsBuilder.build();
    }

    void attach(ScanResultCallback callback) {
        resultCallback = callback;
    }

    /**
     * Start or stop a scan for BLE devices
     * @param enable If true, enables a scan, which will stop after
     *               SCAN_PERIOD, otherwise stops a scan
     */
    void scanBLEDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    scanner.stopScan(callback);
                    if (!deviceFound) {
                        resultCallback.deviceNotFound();
                    }
                }
            }, SCAN_PERIOD);

            scanning = true;
            scanner.startScan(filters, settings, callback); //Filters could be used
        } else {
            scanning = false;
            scanner.stopScan(callback);
        }
    }

    private class BLECallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            deviceFound = true;
            BluetoothDevice bleDevice = result.getDevice();
            Log.d(TAG, "Found device: " + bleDevice.getName());
            //devices.add(bleDevice);
            resultCallback.foundDevice(bleDevice);
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "already started"); break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.d(TAG, "cannot be registered"); break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "power optimized scan not supported"); break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "internal error"); break;
            }
        }
    }

    interface ScanResultCallback {
        void foundDevice(BluetoothDevice device);
        void deviceNotFound();
    }
}
