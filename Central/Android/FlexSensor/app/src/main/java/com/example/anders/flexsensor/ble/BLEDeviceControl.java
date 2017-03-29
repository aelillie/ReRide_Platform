package com.example.anders.flexsensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Controls operations on a GATT server
 */

class BLEDeviceControl {
    private final static String TAG = BLEDeviceControl.class.getSimpleName();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private BLEService bleService;
    private BluetoothDevice device;
    private boolean connected;
    private boolean serviceBound;

    private Context context;
    private GATTCommunicator gattCommunicator;
    private BluetoothGattCharacteristic notifyCharacteristic;

    BLEDeviceControl(Context context) {
        this.context = context;

    }

    public void attachCallback(GATTCommunicator gattCommunicator) {
        this.gattCommunicator = gattCommunicator;
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

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BLEService.ACTION_GATT_CONNECTED: {
                    connected = true;
                    //UI info on connection
                    gattCommunicator.announceStatus("Connected");
                    break;
                }
                case BLEService.ACTION_GATT_DISCONNECTED: {
                    connected = false;
                    //UI info on disconnection
                    gattCommunicator.announceStatus("Not announceStatus");
                    break;
                }
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED: {
                    gattCommunicator.announceStatus("Services discovered");
                    searchGattServices(bleService.getSupportedGattServices());
                    break;
                }
                case BLEService.ACTION_DATA_AVAILABLE: {
                    gattCommunicator.announceStatus("Data available");
                    handleData(intent.getStringExtra(BLEService.EXTRA_DATA));
                    break;
                }
            }
        }
    };

    private void handleData(String stringExtra) {
        gattCommunicator.dataReceived(stringExtra);
    }

    private void searchGattServices(List<BluetoothGattService> supportedGattServices) {
        if (supportedGattServices == null) return;
        String uuid;
        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            String serviceName = GattAttributes.lookup(uuid, "unknown");
            if (serviceName.equals("Battery Service")) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    String charaName = GattAttributes.lookup(uuid, "unknown");
                    if (charaName.equals("Battery Level")) {
                        readCharacteristic(gattCharacteristic); //TODO: Stream this
                        break;
                    }
                }
                break;
            }
        }
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (notifyCharacteristic != null) {
                bleService.setCharacteristicNotification(
                        notifyCharacteristic, false);
                notifyCharacteristic = null;
            }
            bleService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            notifyCharacteristic = characteristic;
            bleService.setCharacteristicNotification(
                    characteristic, true);
        }
    }

    BroadcastReceiver getGattUpdateReceiver() {
        return gattUpdateReceiver;
    }

    void reConnect() {
        if (bleService != null) {
            final boolean result = bleService.connect(device);
            serviceBound = true;
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    void connect(BluetoothDevice device) {
        this.device = device;
        Intent gattServiceIntent = new Intent(context, BLEService.class);
        context.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        serviceBound = true;
    }

    void disconnect(Context context) {
        if (serviceBound) return; //TODO: This does not work
        context.unbindService(serviceConnection);
        bleService = null;
        serviceBound = false;
    }

    interface GATTCommunicator {
        void announceStatus(String status);
        void dataReceived(String data);
    }

    static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
