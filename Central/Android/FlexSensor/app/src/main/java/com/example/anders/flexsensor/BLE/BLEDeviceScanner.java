package com.example.anders.flexsensor.BLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;

import java.util.List;

/**
 * Responsible for scanning for BLE devices
 */

class BLEDeviceScanner {
    private boolean scanning;
    private BluetoothLeScanner scanner;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private BLECallback callback;
    private ScanResultCallback resultCallback;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    BLEDeviceScanner(BluetoothAdapter bluetoothAdapter) {
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceName("ReRide");
        filters.add(builder.build());
        callback = new BLECallback();
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
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
                }
            }, SCAN_PERIOD);

            scanning = true;
            scanner.startScan(filters, settings, callback);
        } else {
            scanning = false;
            scanner.stopScan(callback);
        }
    }

    private class BLECallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice bleDevice = result.getDevice();
            resultCallback.foundDevice(bleDevice);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            //TODO: Handle a failure
        }
    }

    interface ScanResultCallback {
        void foundDevice(BluetoothDevice device);
    }
}
