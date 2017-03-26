package com.example.anders.flexsensor.BLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.anders.flexsensor.R;

/**
 * Controls operations on a GATT server
 */

public class BLEDeviceControl {
    private boolean connected;

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
            }
        }
    };

    private void updateConnectionState(final int resourceId) {
        //TODO: Handle this
    }
}
