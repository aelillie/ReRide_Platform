package com.example.anders.flexsensor.BLE;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.example.anders.flexsensor.R;

/**
 * Controls operations on a GATT server
 */

class BLEDeviceControl {
    private final static String TAG = BLEDeviceControl.class.getSimpleName();
    static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private BLEService bleService;
    private BluetoothDevice device;
    private boolean connected;

    public BLEDeviceControl(BluetoothDevice device) {
        this.device = device;

    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BLEService.LocalBinder) service).getService();
            bleService.connect(device);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };


    public void connect(Context context) {
        Intent gattServiceIntent = new Intent(context, BLEService.class);
        context.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect(Context context) {
        context.unbindService(serviceConnection);
        bleService = null;
    }

    //TODO: Implement receiver for the BLEService
}
