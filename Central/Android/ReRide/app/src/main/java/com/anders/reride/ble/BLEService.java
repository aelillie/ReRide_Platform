package com.anders.reride.ble;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
    private static final UUID UUID_APPARENT_WIND_DIRECTION =
            UUID.fromString(GattAttributes.APPARENT_WIND_DIRECTION);

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
    public static final String ACTION_WRITE =
            "com.example.anders.flexsensor.ACTION_WRITE";

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        /*
        Callback indicating when GATT client has
        connected/disconnected to/from a remote GATT server.
         */
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

        /*
        Callback invoked when the list of remote services, characteristics and descriptors
        for the remote device have been updated, ie new services have been discovered.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        //Callback reporting the result of a characteristic read operation.
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }


        // Callback triggered as a result of a remote characteristic notification.
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_WRITE);
            }else {
                Log.w(TAG, "onDescriptorWrite received: " + status);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (UUID_APPARENT_WIND_DIRECTION.equals(characteristic.getUuid())) {
            try {
                int format = BluetoothGattCharacteristic.FORMAT_UINT16;
                final int windDir = characteristic.getIntValue(format, 0);
                Log.d(TAG, String.format("Apparent wind direction: %d", windDir));
                intent.putExtra(EXTRA_DATA, String.valueOf(windDir));
            } catch (NullPointerException e) {
                intent.putExtra(EXTRA_DATA, readUnknownData(characteristic));
            }
        } else {
            intent.putExtra(EXTRA_DATA, readUnknownData(characteristic));
        }
        sendBroadcast(intent);
    }

    private String readUnknownData(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        String dataString = "";
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            dataString = new String(data) + "\n" + stringBuilder.toString();
        }
        return dataString;
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

    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (bluetoothDeviceAddress != null && bluetoothGatt != null
            && bluetoothDevice.getAddress().equals(bluetoothDeviceAddress)) {
            Log.d(TAG, "Trying to use an existing bluetooth gatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            } else return false;
        }
        //No existing GATT connection established. Find new
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = bluetoothDevice.getAddress();
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

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        return bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }
}
