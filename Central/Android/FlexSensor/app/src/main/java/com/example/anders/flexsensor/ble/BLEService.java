package com.example.anders.flexsensor.ble;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Manages connection and operations on a GATT server
 */

public class BLEService extends Service{
    private static final String TAG = BLEService.class.getSimpleName();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private final IBinder binder = new LocalBinder();

    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private int connectionState = STATE_DISCONNECTED;

    //GATT actions
    public static final String ACTION_GATT_CONNECTED =
            "com.example.anders.flexsensor.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED =
            "com.example.anders.flexsensor.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.anders.flexsensor.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE =
            "com.example.anders.flexsensor.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA =
            "com.example.anders.flexsensor.EXTRA_DATA";

    public final static UUID UUID_BATTERY_LEVEL =
            UUID.fromString(GattAttributes.BATTERY_LEVEL);

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery" +
                        bluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) { //TODO: Not sure about this
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Battery service format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Battery service format UINT8.");
            }
            final int angle = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received angle: %d", angle));
            intent.putExtra(EXTRA_DATA, String.valueOf(angle));
        } else {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public boolean connect(BluetoothDevice device) {
        if (bluetoothDeviceAddress != null &&
                device.getAddress().equals(bluetoothDeviceAddress)) {
            Log.d(TAG, "Trying to use an existing bluetooth gatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            } else return false;
        }
        //No existing GATT connection established. Find new
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = device.getAddress();
        connectionState = STATE_CONNECTING;
        return true;

    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        //TODO: This should never be called??
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }
}
