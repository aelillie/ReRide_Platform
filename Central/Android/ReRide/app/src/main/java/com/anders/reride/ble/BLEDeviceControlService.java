package com.anders.reride.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.support.annotation.Nullable;
import android.util.Log;

import com.anders.reride.aws.AWSIoTShadowClient;
import com.anders.reride.aws.AWSIoTMQTTClient;
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

    public static final String EXTRAS_DEVICE_ADDRESSES =
            "com.anders.reride.ble.DEVICE_ADDRESSES";
    public static final String EXTRAS_USER_ID =
            "com.anders.reride.ble.EXTRAS_USER_ID";

    public static final String ACTION_DATA_UPDATE =
            "com.anders.reride.ble.ACTION_DATA_UPDATE";

    private static final int SLEEP_TIME = 500; //ms
    public static final int UPDATE_FREQUENCY = 1000; //ms

    //Debug settings
    public static boolean TEST_GMS = false;

    private String mUserId;
    private Location mLastLocation;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BLEService mBleService;
    private List<BluetoothDevice> mBluetoothDevices;
    private Map<BluetoothDevice, List<BluetoothGattCharacteristic>> mGattCharacteristicMap;
    private int mConnectedDevices;
    private Handler mHandler;

    private final IBinder binder = new LocalBinder();

    private ReRideJSON mReRideJSON;

    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleService = ((BLEService.LocalBinder) service).getService();
            if (!TEST_GMS) {

                connectDevices();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
        }
    };

    private final GattBroadcastReceiver gattUpdateReceiver = new GattBroadcastReceiver();
    private AWSIoTShadowClient mAWSIoTShadowClient;
    private AWSIoTMQTTClient mAWSIoTMQTTClient;
    private BluetoothAdapter mBluetoothAdapter;
    private ReRideLocationManager mLocationManager;
    private Random mRandomGenerator; //For testing


    private void streamData() {
        Log.d(TAG, "Streaming data");
        if (TEST_GMS) {
            handleData("TEST NAME", String.valueOf(mRandomGenerator.nextInt(180)));
        } else {
            if (mGattCharacteristicMap.size() > 0 && mAWSIoTMQTTClient.isConnected()) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        streamData();
                    }
                }, UPDATE_FREQUENCY);
                for (final BluetoothDevice device : mGattCharacteristicMap.keySet()) {
                    readCharacteristics(device);
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
        Intent intent = new Intent(ACTION_DATA_UPDATE);
        if (mLocationManager.isConnected()) {
            Location location = mLocationManager.getLocation();
            if (location != null) mLastLocation = location;
        }
        if (mLastLocation == null) {
            return;
        }
        double lon = mLastLocation.getLongitude();
        double lat = mLastLocation.getLatitude();
        String time = ReRideTimeManager.now("GMT+2"); //TODO: Custom time zone
        mReRideJSON.putSensorValue(deviceName, data);
        mReRideJSON.putRiderProperties(time, lon, lat);
        Log.d(TAG, "Sending data package!");
        mAWSIoTMQTTClient.publish(mReRideJSON);
        sendBroadcast(intent);
    }

    private void searchGattServices(BluetoothDevice bluetoothDevice) {
        List<BluetoothGattService> supportedGattServices =
                mBleService.getSupportedGattServices(bluetoothDevice);
        if (supportedGattServices == null) return;
        String uuid;
        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            if (GattAttributes.hasAttribute(uuid)) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (GattAttributes.hasAttribute(uuid)) {
                        List<BluetoothGattCharacteristic> characteristics =
                                mGattCharacteristicMap.get(bluetoothDevice);
                        if (characteristics == null) {
                            characteristics = new ArrayList<>();
                        }
                        characteristics.add(gattCharacteristic);
                        mGattCharacteristicMap.put(bluetoothDevice, characteristics);
                        String unit = GattAttributes.lookupUnit(
                                gattCharacteristic.getUuid().toString(),
                                "generic");
                        mReRideJSON.addSensor(bluetoothDevice.getName(), unit);
                    }
                }
            }
        }
    }


    private void connectDevices() {
        if (mBleService != null) {
            for (final BluetoothDevice device : mBluetoothDevices) {
                final boolean result = mBleService.connect(device);
                Log.d(TAG, "Connect request result=" + result);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Killing service");
        if (!TEST_GMS) {
            unregisterReceiver(gattUpdateReceiver);
            unbindService(mBleServiceConnection);
        }
        mLocationManager.disconnect();
        mAWSIoTMQTTClient.disconnect();
        mBleService = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mBluetoothAdapter = ((BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        //mAWSIoTShadowClient = new AWSIoTShadowClient(this, mUserId);
        mAWSIoTMQTTClient = new AWSIoTMQTTClient(this, mUserId);
        mAWSIoTMQTTClient.connect();
        mLocationManager = ReRideLocationManager.getInstance(this);
        bindService(new Intent(this, BLEService.class),
                mBleServiceConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mUserId = intent.getStringExtra(EXTRAS_USER_ID);
        mReRideJSON = ReRideJSON.getInstance(mUserId);

        if (TEST_GMS) {
            mRandomGenerator = new Random();
            mReRideJSON.addSensor("Flex sensor", "degrees");
        } else {
            //Receive devices info
            List<String> deviceAddresses = intent.getStringArrayListExtra(EXTRAS_DEVICE_ADDRESSES);
            mBluetoothDevices = new ArrayList<>(deviceAddresses.size());
            mGattCharacteristicMap = new HashMap<>(deviceAddresses.size());
            for (String deviceAddress : deviceAddresses) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                mBluetoothDevices.add(device);
            }

        }

        if (mBleService != null) {
            connectDevices();
        }
        if (mLocationManager != null && !mLocationManager.isConnected()) {
            mLocationManager.connect();
            if (TEST_GMS) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        streamData();

                    }
                }, 1000);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }






    private void enableNotification(BluetoothDevice device,
                                 BluetoothGattCharacteristic gattCharacteristic) {
        List<BluetoothGattDescriptor> descriptors =
                                gattCharacteristic.getDescriptors();
        String uuid;
        for (BluetoothGattDescriptor descriptor : descriptors) {
            uuid = descriptor.getUuid().toString();
            if (uuid.equals(GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION)){
                descriptor.setValue(
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!mBleService.writeDescriptor(device, descriptor)) {
                    Log.d(TAG, "Unable to write descriptor");
                }
                break;
            }
        }
    }

    private void readCharacteristics(BluetoothDevice device) {
        for (BluetoothGattCharacteristic characteristic : mGattCharacteristicMap.get(device)) {
            readCharacteristic(device, characteristic);
        }
    }

    private void readCharacteristic(BluetoothDevice device,
                                    BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear it
            if (mNotifyCharacteristic != null) {
                mBleService.setCharacteristicNotification(device,
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBleService.readCharacteristic(device, characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBleService.setCharacteristicNotification(device, characteristic, true);
        }
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
                    Log.d(TAG, "GATT disconnected");
                    --mConnectedDevices;
                    String address = intent.getStringExtra(BLEService.EXTRA_DEVICE_ADDRESS);
                    for (BluetoothDevice device : mGattCharacteristicMap.keySet()) {
                        if (device.getAddress().equals(address)) {
                            mGattCharacteristicMap.remove(device);
                            mReRideJSON.removeSensor(device.getName());
                            break;
                        }
                    }
                    break;
                }
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED: {
                    if (++servicesDiscovered == mConnectedDevices) {
                        Log.d(TAG, "All services discovered");
                        for (final BluetoothDevice device : mBluetoothDevices) {
                            searchGattServices(device);
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                streamData();
                            }
                        }, SLEEP_TIME); //ms
                    }
                    break;
                }
                case BLEService.ACTION_DATA_AVAILABLE: {
                    String data = intent.getStringExtra(BLEService.EXTRA_DATA);
                    Log.d(TAG, "Data: " + data);
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
                mBleService.startGattServicesDiscovery(device);
            }
        }
    }
}
