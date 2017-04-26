package com.anders.reride.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anders.reride.aws.AWSIoTHTTPBroker;
import com.anders.reride.aws.AWSIoTMQTTBroker;
import com.anders.reride.data.ReRideJSON;
import com.anders.reride.gms.ReRideLocationManager;
import com.anders.reride.gms.ReRideTimeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Controls operations on a GATT server
 */

public class BLEDeviceControlService extends Service {
    private final static String TAG = BLEDeviceControlService.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESSES = "DEVICE_ADDRESSES";
    public static final String EXTRAS_USER_ID = "EXTRAS_USER_ID";
    public static final String EXTRAS_SENSOR_DATA = "SENSOR_DATA";
    public static final String EXTRAS_LOCATION_DATA = "LOCATION_DATA";
    public static final String EXTRAS_TIME_DATA = "TIME_DATA";

    private static final int SLEEP_TIME = 500; //ms
    private static final int UPDATE_FREQUENCY = 1000; //ms

    //Debug settings
    public static boolean TEST_GMS = true;

    private String mUserId;
    private Location mLastLocation;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BLEService mBleService;
    private List<BluetoothDevice> mBluetoothDevices;
    private Map<BluetoothDevice, BluetoothGattCharacteristic> mGattCharacteristicMap;
    private int mConnectedDevices;
    private Handler mHandler;

    private final IBinder binder = new LocalBinder();

    private ReRideJSON mReRideJSON;

    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleService = ((BLEService.LocalBinder) service).getService();
            connectDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
        }
    };

    private final GattBroadcastReceiver gattUpdateReceiver = new GattBroadcastReceiver();
    private AWSIoTHTTPBroker mAWSIoTHTTPBroker;
    private AWSIoTMQTTBroker mAWSIoTMQTTBroker;
    private BluetoothAdapter mBluetoothAdapter;
    private ReRideLocationManager mLocationManager;
    private Random mRandomGenerator; //For testing


    private void streamData() {
        Log.d(TAG, "Ready to stream data");
        if (TEST_GMS) {
            handleData("Flex sensor", String.valueOf(mRandomGenerator.nextInt(180)));
            while(mAWSIoTMQTTBroker.isConnected()) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Handle!");
                        //handleData(null, String.valueOf(mRandomGenerator.nextInt(180)));
                    }
                }, UPDATE_FREQUENCY);
            }
        } else {
            if (mGattCharacteristicMap.size() > 0) {
                while (mAWSIoTMQTTBroker.isConnected()) {
                    for (final BluetoothDevice device : mGattCharacteristicMap.keySet()) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                readCharacteristic(device);
                            }
                        }, UPDATE_FREQUENCY); //ms
                    }
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public BLEDeviceControlService getService() {
            return BLEDeviceControlService.this;
        }
    }

    private void handleData(String deviceName, String data) {
        Log.d(TAG, "Handling data");
        if (mLocationManager.isConnected()) {
            Location location = mLocationManager.getLocation();
            if (location != null) mLastLocation = location;
        }
        if (mLastLocation == null) {
            return;
        }
        double lon = mLastLocation.getLongitude();
        double lat = mLastLocation.getLatitude();
        String time = ReRideTimeManager.now("GMT+2");
        mReRideJSON.putSensorValue(deviceName, data);
        mReRideJSON.putRiderProperties(time, lon, lat);
        Log.d(TAG, "Sending data package!");
        mAWSIoTMQTTBroker.publish(mReRideJSON);
    }

    private void searchGattServices(BluetoothDevice bluetoothDevice) {
        List<BluetoothGattService> supportedGattServices =
                mBleService.getSupportedGattServices(bluetoothDevice);
        if (supportedGattServices == null) return;
        String uuid;
        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals(GattAttributes.ENVIRONMENTAL_SENSING)) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (uuid.equals(GattAttributes.APPARENT_WIND_DIRECTION)) { //TODO: Fix
                        mGattCharacteristicMap.put(bluetoothDevice, gattCharacteristic);
                        /*List<BluetoothGattDescriptor> descriptors =
                                gattCharacteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            uuid = descriptor.getUuid().toString();
                            if (uuid.equals(GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION)){
                                descriptor.setValue(
                                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                if (!mBleService.writeDescriptor(bluetoothDevice, descriptor)) {
                                    Log.d(TAG, "Unable to write descriptor");
                                }
                                break;
                            }
                        }*/
                        break;
                    }
                }
                break;
            }
        }
    }


    private void connectDevices() {
        if (mBleService != null) {
            for (final BluetoothDevice device : mBluetoothDevices) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final boolean result = mBleService.connect(device);
                        Log.d(TAG, "Connect request result=" + result);
                        //if (result) getDataButton.setEnabled(true);
                    }
                }, SLEEP_TIME); //ms
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!TEST_GMS) unbindService(mBleServiceConnection);
        mLocationManager.disconnect();
        mAWSIoTMQTTBroker.disconnect();
        mBleService = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mHandler = new Handler();
        mBluetoothAdapter = ((BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mUserId = intent.getStringExtra(EXTRAS_USER_ID);
        mReRideJSON = ReRideJSON.getInstance(mUserId);
        //mAWSIoTHTTPBroker = new AWSIoTHTTPBroker(this, mUserId);
        mAWSIoTMQTTBroker = new AWSIoTMQTTBroker(this, mUserId);
        mAWSIoTMQTTBroker.connect();
        mLocationManager = ReRideLocationManager.getInstance(this);

        if (TEST_GMS) {
            mRandomGenerator = new Random();
            mReRideJSON.addSensor("Flex sensor", "degrees");
        } else {
            //Receive devices info
            String[] deviceAddresses = intent.getStringArrayExtra(EXTRAS_DEVICE_ADDRESSES);
            mBluetoothDevices = new ArrayList<>(deviceAddresses.length);
            mGattCharacteristicMap = new HashMap<>(deviceAddresses.length);
            for (String deviceAddress : deviceAddresses) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                mBluetoothDevices.add(device);
                mReRideJSON.addSensor(device.getName(), "degrees"); //TODO: Custom unit
            }
            bindService(new Intent(this, BLEService.class),
                    mBleServiceConnection, Context.BIND_AUTO_CREATE);
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        if (mBleService != null) {
            connectDevices();
        }
        if (mLocationManager != null) {
            mLocationManager.connect();
            if (TEST_GMS) {
                streamData();
            }
        }
        return binder;
    }



    private void readCharacteristic(BluetoothDevice device) {
        BluetoothGattCharacteristic characteristic = mGattCharacteristicMap.get(device);
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't publish the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBleService.setCharacteristicNotification(device,
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBleService.readCharacteristic(device, characteristic);
        }
        /*if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            if (!mBleService.setCharacteristicNotification(device, characteristic, true)) {
                Toast.makeText(this, "Enable notifications failed",
                        Toast.LENGTH_SHORT).show();
            }
        }*/
    }

    static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private class GattBroadcastReceiver extends BroadcastReceiver {
        private int servicesDiscovered;
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BLEService.ACTION_GATT_CONNECTED: {
                    ++mConnectedDevices;
                    if (mConnectedDevices == mBluetoothDevices.size()) {
                        Log.d(TAG, "All connected");
                        discoverServices();
                    }
                    break;
                }
                case BLEService.ACTION_GATT_DISCONNECTED: {
                    --mConnectedDevices;
                    Log.d(TAG, "GATT disconnected");
                    break;
                }
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED: {
                    if (++servicesDiscovered == mConnectedDevices) {
                        Log.d(TAG, "All services discovered");
                        for (final BluetoothDevice device : mBluetoothDevices) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    searchGattServices(device);
                                }
                            }, SLEEP_TIME); //ms
                        }
                        streamData();
                    }
                    break;
                }
                case BLEService.ACTION_DATA_AVAILABLE: {
                    String data = intent.getStringExtra(BLEService.EXTRA_DATA);
                    String deviceAddress = intent.getStringExtra(BLEService.EXTRA_DEVICE_ADDRESS);
                    handleData(mBluetoothAdapter.getRemoteDevice(deviceAddress).getName(), data);
                    break;
                }
                case BLEService.ACTION_WRITE: {
                    Log.d(TAG, "Descriptor written");
                    break;
                }
                default: Log.d(TAG, "Action not implemented"); break;
            }
        }
    }

    private void discoverServices() {
        if (mBleService != null) {
            for (final BluetoothDevice device : mBluetoothDevices) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBleService.startGattServicesDiscovery(device);
                    }
                }, SLEEP_TIME); //ms
            }
        }
    }
}
