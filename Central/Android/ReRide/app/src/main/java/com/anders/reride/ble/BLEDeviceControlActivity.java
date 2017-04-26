package com.anders.reride.ble;

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
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.R;
import com.anders.reride.aws.AWSIoTHTTPBroker;
import com.anders.reride.aws.AWSIoTMQTTBroker;
import com.anders.reride.data.ReRideJSON;
import com.anders.reride.gms.ReRideLocationManager;
import com.anders.reride.gms.ReRideTimeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controls operations on a GATT server
 */

public class BLEDeviceControlActivity extends AppCompatActivity {
    private final static String TAG = BLEDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESSES = "DEVICE_ADDRESSES";
    public static final String EXTRAS_USER_ID = "EXTRAS_USER_ID";
    public static final String EXTRAS_SENSOR_DATA = "SENSOR_DATA";
    public static final String EXTRAS_LOCATION_DATA = "LOCATION_DATA";
    public static final String EXTRAS_TIME_DATA = "TIME_DATA";

    private static final int SLEEP_TIME = 500; //ms
    private static final int UPDATE_FREQUENCY = 1000; //ms

    //Debug settings
    public static boolean TEST_GMS = false;

    //UI information
    private TextView connectionState;
    private TextView locationLongField;
    private TextView locationLatField;
    private TextView timeField;

    private String mUserId;
    private Location mLastLocation;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BLEService mBleService;
    private List<BluetoothDevice> mBluetoothDevices;
    private Map<BluetoothDevice, BluetoothGattCharacteristic> mGattCharacteristicMap;
    private int mConnectedDevices;
    private Handler mHandler;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mUserId = intent.getStringExtra(EXTRAS_USER_ID);
        setContentView(R.layout.activity_ble);
        //Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ble);
        toolbar.setTitle("Device control");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mHandler = new Handler();
        mReRideJSON = ReRideJSON.getInstance(mUserId);
        mBluetoothAdapter = ((BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mAWSIoTHTTPBroker = new AWSIoTHTTPBroker(this, mUserId);
        mAWSIoTMQTTBroker = new AWSIoTMQTTBroker(this, mUserId);
        mLocationManager = new ReRideLocationManager(this);

        //Receive devices info
        String[] deviceAddresses = intent.getStringArrayExtra(EXTRAS_DEVICE_ADDRESSES);
        mBluetoothDevices = new ArrayList<>(deviceAddresses.length);
        mGattCharacteristicMap = new HashMap<>(deviceAddresses.length);
        for (String deviceAddress : deviceAddresses) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
            mBluetoothDevices.add(device);
            mReRideJSON.addSensor(device.getName(), "degrees"); //TODO: Custom unit
        }

        initializeUIComponents();
        bindService(new Intent(this, BLEService.class),
                mBleServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private void updateConnectionState(final int connectedDevices) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText(connectedDevices);
            }
        });
    }
    private void initializeUIComponents() {
        ((TextView) findViewById(R.id.devices_number)).setText(mBluetoothDevices.size());
        connectionState = (TextView) findViewById(R.id.connection_state);
        locationLongField = (TextView) findViewById(R.id.location_long_value);
        locationLatField = (TextView) findViewById(R.id.location_lat_value);
        timeField = (TextView) findViewById(R.id.time_value);
    }

    private void streamData() {
        announce("Streaming data!");
        if (mGattCharacteristicMap.size() > 0) {
            for (final BluetoothDevice device : mGattCharacteristicMap.keySet()) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        readCharacteristic(device);
                    }
                }, UPDATE_FREQUENCY); //ms
            }
        } else {
            announce("No characteristic available");
        }
    }


    private void handleData(BluetoothDevice device, String data) {
        if (mLocationManager.requiresResolution()) {
            try {
                mLocationManager.getConnectionResult().startResolutionForResult(this,
                        ReRideLocationManager.REQUEST_CHECK_SETTINGS);
                mLocationManager.setRequiresResolution(false);
                return;
            } catch (IntentSender.SendIntentException e) {
                announce("Fixing GMS connection failed. Reconnecting...");
                e.printStackTrace();
                mLocationManager.reconnect();
                return;
            }
        }
        if (mLocationManager.isConnected()) {
            Location location = mLocationManager.getLocation();
            if (location != null) mLastLocation = location;
        }
        if (mLastLocation == null) {
            announce("Unable to find location");
            return;
        }
        double lon = mLastLocation.getLongitude();
        double lat = mLastLocation.getLatitude();
        locationLongField.setText(String.valueOf(lon));
        locationLatField.setText(String.valueOf(lat));
        String time = ReRideTimeManager.now("GMT+2");
        timeField.setText(time);
        if (TEST_GMS) {
            //mAngleData = "5"; //TODO: Handle multiple data
        }
        mReRideJSON.putSensorValue(device.getName(), data);
        mReRideJSON.putRiderProperties(time, lon, lat);
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBleService != null) {
            connectDevices();
        }
        if (mLocationManager != null) {
            mLocationManager.connect();
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
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mBleServiceConnection);
        mLocationManager.disconnect();
        mBleService = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == ReRideLocationManager.REQUEST_CHECK_SETTINGS)) {
            //the application should try to connect again.
            mLocationManager.connect();
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                    announce("Connected");
                    updateConnectionState(++mConnectedDevices);
                    if (mConnectedDevices == mBluetoothDevices.size()) {
                        discoverServices();
                    }
                    break;
                }
                case BLEService.ACTION_GATT_DISCONNECTED: {
                    announce("Disconnected");
                    updateConnectionState(--mConnectedDevices);
                    break;
                }
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED: {
                    announce("Services discovered");
                    if (++servicesDiscovered == mConnectedDevices) {
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
                    handleData(mBluetoothAdapter.getRemoteDevice(deviceAddress), data);
                    break;
                }
                case BLEService.ACTION_WRITE: {
                    announce("Descriptor written");
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
