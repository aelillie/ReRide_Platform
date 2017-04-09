package com.example.anders.flexsensor.ble;

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
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anders.flexsensor.aws.AWSIoTManager;
import com.example.anders.flexsensor.R;
import com.example.anders.flexsensor.aws.PubSubFragment;
import com.example.anders.flexsensor.gms.LocationService;

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

    private PubSubFragment mPubSubFragment;

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
    private LocationService mLocationService;
    private BluetoothDevice bluetoothDevice;

    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BLEService.LocalBinder) service).getService();
            bleService.connect(bluetoothDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    private final ServiceConnection mGmsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mLocationService = ((LocationService.LocalBinder) service).getService();
            mLocationService.connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mLocationService = null;
        }
    };

    private final BroadcastReceiver mLocationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case LocationService.ACTION_UPDATE_AVAILABLE:
                    double[] location = intent.getDoubleArrayExtra(LocationService.LAST_LOCATION_STRING_KEY);
                    if (location != null) mLocation = location;
                     mTime = intent.getStringExtra(LocationService.LAST_TIME_STRING_KEY);
                    break;
                case LocationService.ACTION_CONNECTED:
                    announce("Location services connected");
                    break;
                default: Log.d(TAG, "Action not implemented"); break;
            }

        }
    };

    private double[] mLocation;
    private String mTime;
    private String mAngleData;

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BLEService.ACTION_GATT_CONNECTED: {
                    connected = true;
                    //UI info on connection
                    announce("Connected");
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                    updateUI();
                    break;
                }
                case BLEService.ACTION_GATT_DISCONNECTED: {
                    connected = false;
                    //UI info on disconnection
                    announce("Disconnected");
                    updateConnectionState(R.string.disconnected);
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
    };
    private BluetoothGattCharacteristic mGattCharacteristic;
    private AWSIoTManager mAWSManager;

    private void updateUI() {
        getDataButton.setEnabled(true);
    }


    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText(resourceId);
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

        mAWSManager = new AWSIoTManager(this);
        final Intent intent = getIntent();
        String deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        BluetoothAdapter bluetoothAdapter = ((BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        ((TextView) findViewById(R.id.device_address)).setText(deviceAddress);
        connectionState = (TextView) findViewById(R.id.connection_state);
        dataField = (TextView) findViewById(R.id.data_value);
        locationLongField = (TextView) findViewById(R.id.location_long_value);
        locationLatField = (TextView) findViewById(R.id.location_lat_value);
        timeField = (TextView) findViewById(R.id.time_value);
        getDataButton = (Button) findViewById(R.id.get_data_button);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDataButton.setEnabled(false);
                streamData();
            }
        });

//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction trans = fragmentManager.beginTransaction();
//        mPubSubFragment = new PubSubFragment();
//        trans.add(R.id.aws_fragment, mPubSubFragment);
//        trans.commit();

        toolbar.setTitle(bluetoothDevice.getName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mBleServiceConnection, Context.BIND_AUTO_CREATE);
        Intent gmsServiceIntent = new Intent(this, LocationService.class);
        bindService(gmsServiceIntent, mGmsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void streamData() {
        announce("Streaming data!");
        if (mGattCharacteristic != null) {
            readCharacteristic(mGattCharacteristic);
        } else {
            announce("No characteristic available");
        }
        mLocationService.requestUpdates();
    }


    private void handleData() {
        dataField.setText(mAngleData);
        locationLongField.setText(String.valueOf(mLocation[LocationService.LONGITUDE_ID]));
        locationLatField.setText(String.valueOf(mLocation[LocationService.LATITUDE_ID]));
        timeField.setText(mTime);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRAS_ANGLE_DATA, mAngleData);
        bundle.putDoubleArray(EXTRAS_LOCATION_DATA, mLocation);
        bundle.putString(EXTRAS_TIME_DATA, mTime);
        mAWSManager.update(bundle);
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
        if (bleService != null) {
            final boolean result = bleService.connect(bluetoothDevice);
            Log.d(TAG, "Connect request result=" + result);
            if (result) getDataButton.setEnabled(true);
        }
        if (mLocationService != null) {
            mLocationService.connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mBleServiceConnection);
        unbindService(mGmsServiceConnection);
        bleService = null;
        mLocationService = null;
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();
//        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//            // If there is an active notification on a characteristic, clear
//            // it first so it doesn't update the data field on the user interface.
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
        intentFilter.addAction(LocationService.ACTION_CONNECTED);
        intentFilter.addAction(LocationService.ACTION_UPDATE_AVAILABLE);
        return intentFilter;
    }
}
