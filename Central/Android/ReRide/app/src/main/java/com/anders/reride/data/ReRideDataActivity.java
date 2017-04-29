package com.anders.reride.data;

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
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.R;
import com.anders.reride.ble.BLEDeviceControlService;
import com.anders.reride.ble.BLEService;

/**
 * Shows current data
 */

public class ReRideDataActivity extends AppCompatActivity {
    private static final String TAG = ReRideDataActivity.class.getSimpleName();

    private BLEDeviceControlService mBleDeviceService;

    private TextView connectionState;
    private TextView dataField;
    private TextView locationLongField;
    private TextView locationLatField;
    private TextView timeField;

    private boolean mConnected;

    private final ServiceConnection mBleDeviceServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleDeviceService = ((BLEDeviceControlService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleDeviceService = null;
        }
    };
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BLEDeviceControlService.ACTION_DATA_UPDATE: {
                    break;
                }
                default: Log.d(TAG, "Action not implemented"); break;
            }
        }
    };

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Update
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.streaming_data);
        setSupportActionBar(toolbar);

        connectionState = (TextView) findViewById(R.id.connection_state);
        dataField = (TextView) findViewById(R.id.data_value);
        locationLongField = (TextView) findViewById(R.id.location_lon_value);
        locationLatField = (TextView) findViewById(R.id.location_lat_value);
        timeField = (TextView) findViewById(R.id.time_value);

        bindService(new Intent(this, BLEDeviceControlService.class),
                mBleDeviceServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mBleDeviceServiceConnection);
        mBleDeviceService = null;
    }

    static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEDeviceControlService.ACTION_DATA_UPDATE);
        return intentFilter;
    }
}
