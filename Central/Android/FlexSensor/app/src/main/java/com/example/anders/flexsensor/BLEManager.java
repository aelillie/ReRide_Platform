package com.example.anders.flexsensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import java.util.List;

/**
 * Establishes and makes BLE management and connection
 * Source: https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 */

public class BLEManager {
    public static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private DeviceScanner deviceScanner;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BLEManager(Context context) {
        this.context = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        deviceScanner = new DeviceScanner();
    }

    public Intent enableBLE() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean isBLESupported(Context context) {
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class DeviceScanner {
        private boolean scanning;
        private BluetoothLeScanner scanner;
        private List<ScanFilter> filters;
        private ScanSettings settings;
        private BLECallback callback;

        // Stops scanning after 10 seconds.
        private static final long SCAN_PERIOD = 10000;

        DeviceScanner() {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setDeviceName("ReRide");
            filters.add(builder.build());
            callback = new BLECallback();
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
            settings = settingsBuilder.build();
        }

        /**
         * Start or stop a scan for BLE devices
         * @param enable If true, enables a scan, which will stop after
         *               SCAN_PERIOD, otherwise stops a scan
         */
        public void scanBLEDevice(final boolean enable) {
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

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private class BLECallback extends ScanCallback {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                //TODO: Deliver scan results backer
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                //TODO: Handle a failure
            }
        }
    }
}
