package com.example.anders.flexsensor.ble;

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

import com.example.anders.flexsensor.FlexSensorManager;
import com.example.anders.flexsensor.R;

import java.util.List;

/**
 * Controls operations on a GATT server
 */

public class BLEDeviceControlActivity extends AppCompatActivity {
    private final static String TAG = BLEDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    //UI information
    private boolean connected;
    private TextView connectionState;
    private TextView dataField;
    private Button getDataButton;

    private BluetoothGattCharacteristic notifyCharacteristic;
    private BLEService bleService;
    private BluetoothDevice bluetoothDevice;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
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
                    handleData(intent.getStringExtra(BLEService.EXTRA_DATA));
                    break;
                }
            }
        }
    };
    private BluetoothGattCharacteristic mGattCharacteristic;
    private FlexSensorManager mAWSManager;

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

        mAWSManager = new FlexSensorManager(this);
        final Intent intent = getIntent();
        String deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        BluetoothAdapter bluetoothAdapter = ((BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        ((TextView) findViewById(R.id.device_address)).setText(deviceAddress);
        connectionState = (TextView) findViewById(R.id.connection_state);
        dataField = (TextView) findViewById(R.id.data_value);
        getDataButton = (Button) findViewById(R.id.get_data_button);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDataButton.setEnabled(false);
                streamData();
            }
        });

        toolbar.setTitle(bluetoothDevice.getName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void streamData() {
        announce("Streaming data!");
        if (mGattCharacteristic != null) {
            readCharacteristic(mGattCharacteristic);
        } else {
            announce("No characteristic available");
        }
    }


    private void handleData(String data) {
        dataField.setText(data);
        mAWSManager.update(data);
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
        if (bleService != null) {
            final boolean result = bleService.connect(bluetoothDevice);
            Log.d(TAG, "Connect request result=" + result);
            if (result) getDataButton.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bleService.disconnect();
        unregisterReceiver(gattUpdateReceiver);
        getDataButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
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

    static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
