package com.anders.reride.ble;

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
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.R;
import com.anders.reride.aws.AWSIoTHTTPBroker;
import com.anders.reride.aws.AWSIoTMQTTBroker;
import com.anders.reride.data.ReRideJSON;
import com.anders.reride.gms.LocationSubscriberService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationSettingsResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls operations on a GATT server
 */

public class BLEDeviceControlActivity extends AppCompatActivity {
    private final static String TAG = BLEDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_ANGLE_DATA = "ANGLE_DATA";
    public static final String EXTRAS_LOCATION_DATA = "LOCATION_DATA";
    public static final String EXTRAS_TIME_DATA = "TIME_DATA";

    //Debug settings
    public static boolean TEST_GMS = false;

    //UI information
    private boolean connected;
    private TextView connectionState;
    private TextView dataField;
    private TextView locationLongField;
    private TextView locationLatField;
    private TextView timeField;

    private Button getDataButton;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private BLEService bleService;
    private LocationSubscriberService mLocationSubscriberService;
    private List<BluetoothDevice> mBluetoothDevices;
    private int mConnectedDevices;
    private boolean mReadyToConnect;

    private ReRideJSON mReRideJSON;

    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BLEService.LocalBinder) service).getService();
            connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    private final ServiceConnection mGmsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mLocationSubscriberService = ((LocationSubscriberService.LocalBinder) service).getService();
            mLocationSubscriberService.connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mLocationSubscriberService = null;
        }
    };

    private final BroadcastReceiver mLocationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case LocationSubscriberService.ACTION_UPDATE_AVAILABLE:
                    double[] location = intent.getDoubleArrayExtra(LocationSubscriberService.LAST_LOCATION_STRING_KEY);
                    if (location != null) mLocation = location;
                     mTime = intent.getStringExtra(LocationSubscriberService.LAST_TIME_STRING_KEY);
                    handleData();
                    break;
                case LocationSubscriberService.ACTION_CONNECTED:
                    announce("Location services connected");
                    break;
                case LocationSubscriberService.ACTION_CONNECTION_FAILED:
                    ConnectionResult cr = intent.getParcelableExtra(
                            LocationSubscriberService.ERROR_STRING_KEY);
                    try {
                        cr.startResolutionForResult(getParent(),
                                LocationSubscriberService.REQUEST_CHECK_CONNECTION);
                    } catch (IntentSender.SendIntentException e) {
                        Log.d(TAG, e.getMessage());
                    } break;
                case LocationSubscriberService.ACTION_SETTINGS_FAILED:
                    LocationSettingsResult lsr = intent.getParcelableExtra(
                            LocationSubscriberService.ERROR_STRING_KEY);
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        lsr.getStatus().startResolutionForResult(getParent(),
                                LocationSubscriberService.REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        Log.d(TAG, e.getMessage());
                    } break;
                default: Log.d(TAG, "Action not implemented"); break;
            }

        }
    };
    private double[] mLocation;
    private String mTime;

    private String mAngleData;
    private final GattBroadcastReceiver gattUpdateReceiver = new GattBroadcastReceiver();
    private BluetoothGattCharacteristic mGattCharacteristic;
    private AWSIoTHTTPBroker mAWSIoTHTTPBroker;
    private AWSIoTMQTTBroker mAWSIoTMQTTBroker;

    private void updateUI() {
        getDataButton.setEnabled(true);
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


    private void clearUI() {
        dataField.setText(R.string.no_data);
        getDataButton.setEnabled(false);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ble);
        toolbar.setTitle("Device control");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        String[] deviceAddresses = intent.getStringArrayExtra(EXTRAS_DEVICE_ADDRESS);
        if (deviceAddresses.length > 0) {
            mBluetoothDevices = new ArrayList<>(deviceAddresses.length);
            BluetoothAdapter bluetoothAdapter = ((BluetoothManager)
                    getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
            for (String deviceAddress : deviceAddresses) {
                mBluetoothDevices.add(bluetoothAdapter.getRemoteDevice(deviceAddress));
            }
            mReadyToConnect = true;
        }

        ((TextView) findViewById(R.id.devices_number)).setText(mBluetoothDevices.size());
        connectionState = (TextView) findViewById(R.id.connection_state);
        //dataField = (TextView) findViewById(R.id.data_value);
        locationLongField = (TextView) findViewById(R.id.location_long_value);
        locationLatField = (TextView) findViewById(R.id.location_lat_value);
        timeField = (TextView) findViewById(R.id.time_value);
        /*getDataButton = (Button) findViewById(R.id.get_data_button);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDataButton.setEnabled(false);
                streamData();
            }
        });

        if (TEST_GMS) {
            getDataButton.setEnabled(true);
        }*/

        mReRideJSON = ReRideJSON.getInstance("223344"); //TODO: ID
        mAWSIoTHTTPBroker = new AWSIoTHTTPBroker(this, "223344");
        mAWSIoTMQTTBroker = new AWSIoTMQTTBroker(this, "223344");

        if (mBluetoothDevices != null) {
            Intent gattServiceIntent = new Intent(this, BLEService.class);
            bindService(gattServiceIntent, mBleServiceConnection, Context.BIND_AUTO_CREATE);
        }
        Intent gmsServiceIntent = new Intent(this, LocationSubscriberService.class);
        bindService(gmsServiceIntent, mGmsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void streamData() {
        announce("Streaming data!");
        if (mGattCharacteristic != null) {
            readCharacteristic(mGattCharacteristic);
        } else {
            announce("No characteristic available");
        }
        mLocationSubscriberService.requestUpdates();
    }


    private void handleData() {
        locationLongField.setText(String.valueOf(mLocation[LocationSubscriberService.LONGITUDE_ID]));
        locationLatField.setText(String.valueOf(mLocation[LocationSubscriberService.LATITUDE_ID]));
        timeField.setText(mTime);
        if (TEST_GMS) {
            mAngleData = "5";
        }
        dataField.setText(mAngleData);
        //TODO: Create JSON object
        mAWSIoTMQTTBroker.publish(mReRideJSON);
    }

    private void searchGattServices(List<BluetoothGattService> supportedGattServices) {
        if (supportedGattServices == null) return;
        String uuid;
        for (BluetoothGattService gattService : supportedGattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals(GattAttributes.ENVIRONMENTAL_SENSING)) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (uuid.equals(GattAttributes.APPARENT_WIND_DIRECTION)) {
                        mGattCharacteristic = gattCharacteristic;
                        List<BluetoothGattDescriptor> descriptors =
                                gattCharacteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            uuid = descriptor.getUuid().toString();
                            if (uuid.equals(GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION)){
                                descriptor.setValue(
                                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                if (!bleService.writeDescriptor(descriptor)) {
                                    Log.d(TAG, "Unable to write descriptor");
                                }
                                break;
                            }
                        }
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
        registerReceiver(mLocationUpdateReceiver, makeLocationUpdateFilter());
        if (mLocationSubscriberService != null) {
            mLocationSubscriberService.connect();
        }
        connect();
    }

    private void connect() {
        if (bleService != null && mReadyToConnect) {
            Handler handler = new Handler();
            for (final BluetoothDevice device : mBluetoothDevices) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final boolean result = bleService.connect(device);
                        Log.d(TAG, "Connect request result=" + result);
                        if (result) getDataButton.setEnabled(true);
                    }
                }, 500); //ms
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mBleServiceConnection);
        unbindService(mGmsServiceConnection);
        bleService = null;
        mLocationSubscriberService = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == LocationSubscriberService.REQUEST_CHECK_CONNECTION
                        || requestCode == LocationSubscriberService.REQUEST_CHECK_SETTINGS)) {
            //the application should try to connect again.
            mLocationSubscriberService.connect();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();
//        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//            // If there is an active notification on a characteristic, clear
//            // it first so it doesn't publish the data field on the user interface.
//            if (notifyCharacteristic != null) {
//                bleService.setCharacteristicNotification(
//                        notifyCharacteristic, false);
//                notifyCharacteristic = null;
//            }
//            bleService.readCharacteristic(characteristic);
//        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            notifyCharacteristic = characteristic;
            if (!bleService.setCharacteristicNotification(characteristic, true)) {
                Toast.makeText(this, "Enable notifications failed",
                        Toast.LENGTH_SHORT).show();
            }
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

    static IntentFilter makeLocationUpdateFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationSubscriberService.ACTION_CONNECTED);
        intentFilter.addAction(LocationSubscriberService.ACTION_UPDATE_AVAILABLE);
        intentFilter.addAction(LocationSubscriberService.ACTION_CONNECTION_FAILED);
        intentFilter.addAction(LocationSubscriberService.ACTION_SETTINGS_FAILED);
        return intentFilter;
    }

    private class GattBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BLEService.ACTION_GATT_CONNECTED: {
                    connected = true;
                    //UI info on connection
                    announce("Connected");
                    updateConnectionState(++mConnectedDevices);
                    invalidateOptionsMenu();
                    updateUI();
                    break;
                }
                case BLEService.ACTION_GATT_DISCONNECTED: {
                    connected = false;
                    //UI info on disconnection
                    announce("Disconnected");
                    updateConnectionState(--mConnectedDevices);
                    invalidateOptionsMenu();
                    clearUI();
                    break;
                }
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED: {
                    announce("Services discovered");
                    searchGattServices(bleService.getSupportedGattServices());
                    break;
                }
                case BLEService.ACTION_DATA_AVAILABLE: {
                    mAngleData = intent.getStringExtra(BLEService.EXTRA_DATA);
                    handleData();
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
}
