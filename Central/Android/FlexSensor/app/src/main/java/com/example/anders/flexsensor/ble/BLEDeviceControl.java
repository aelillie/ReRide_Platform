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

    private GATTCommunicator gattCommunicator;

    BLEDeviceControl(BluetoothDevice device) {
        this.device = device;

    }

    public void attach(GATTCommunicator gattCommunicator) {
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
                    gattCommunicator.connected(true);
                    break;
                }
                case BLEService.ACTION_GATT_DISCONNECTED: {
                    connected = false;
                    //UI info on disconnection
                    gattCommunicator.connected(false);
                    break;
                }
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED: {
                    searchGattServices(bleService.getSupportedGattServices());
                    break;
                }
                case BLEService.ACTION_DATA_AVAILABLE: {
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
        HashMap<String, String> gattServiceData = new HashMap<>();
        ArrayList<HashMap<String, String>> gattCharacteristicData =
                new ArrayList<>();
        String uuid;
        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            String serviceName = GattAttributes.lookup(uuid, "unknown");
            if (serviceName.equals("Battery Service")) {
                gattServiceData.put(LIST_NAME, serviceName);
                gattServiceData.put(LIST_UUID, uuid);

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<>();

                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<>();
                    uuid = gattCharacteristic.getUuid().toString();
                    String charaName = GattAttributes.lookup(
                            GattAttributes.BATTERY_LEVEL, "unknown");
                    if (charaName.equals("Battery Level")) {
                        currentCharaData.put(LIST_UUID, uuid);
                        currentCharaData.put(LIST_NAME, charaName);
                        gattCharacteristicData.add(currentCharaData);
                        break;
                    }
                }
                break;
            }
        }
    }

    BroadcastReceiver getGattUpdateReceiver() {
        return gattUpdateReceiver;
    }

    void reConnect() {
        if (bleService != null) {
            final boolean result = bleService.connect(device);
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    void connect(Context context) {
        Intent gattServiceIntent = new Intent(context, BLEService.class);
        context.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    void disconnect(Context context) {
        context.unbindService(serviceConnection);
        bleService = null;
    }

    interface GATTCommunicator {
        void connected(boolean isConnected);
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
