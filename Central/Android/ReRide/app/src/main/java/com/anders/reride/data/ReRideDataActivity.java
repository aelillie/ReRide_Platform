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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.MainActivity;
import com.anders.reride.R;
import com.anders.reride.ble.BLEDeviceControlService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows current data
 */

public class ReRideDataActivity extends AppCompatActivity {
    private static final String TAG = ReRideDataActivity.class.getSimpleName();
    public static final boolean DEBUG_MODE = false;

    private BLEDeviceControlService mBleDeviceService;
    private ReRideJSON mReRideJson;

    private TextView idField;
    private TextView locationLonField;
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
                    updateUI();
                    break;
                }
                default: Log.d(TAG, "Action not implemented"); break;
            }
        }
    };
    private ViewAdapter mAdapter;
    private JSONObject mRiderProperties;
    private String mId;

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    locationLatField.setText(mRiderProperties.getString(ReRideJSON.LATITUDE));
                    locationLonField.setText(mRiderProperties.getString(ReRideJSON.LONGITUDE));
                    timeField.setText(formatTime(mRiderProperties.getString(ReRideJSON.TIME)));
                    mAdapter.notifyDataSetChanged(); //Update sensor values
                } catch (JSONException e) {
                    announce("Data could not be fetched!");
                    e.printStackTrace();
                }
            }
        });
    }

    private String formatTime(String simpleTime) {
        String time =
                simpleTime.substring(0, 4) + "." + //Year
                simpleTime.substring(4, 6) + "." + //Month
                simpleTime.substring(6, 8) + " " + //Day
                simpleTime.substring(8, 10) + ":" + //Hour
                simpleTime.substring(10, 12) + ":" + //Minutes
                simpleTime.substring(12, 14); //Seconds
        return time; //YYYY.MM.DD HH:MM:SS
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ble_data);
        toolbar.setTitle(R.string.streaming_data);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mId = intent.getStringExtra(MainActivity.EXTRAS_USER_ID);

        mReRideJson = ReRideJSON.getInstance(mId);
        mRiderProperties = mReRideJson.getRiderProperties();

        idField = (TextView) findViewById(R.id.id__value);
        idField.setText(mId);
        locationLonField = (TextView) findViewById(R.id.location_lon_value);
        locationLatField = (TextView) findViewById(R.id.location_lat_value);
        timeField = (TextView) findViewById(R.id.time_value);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.data_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        try {
            mAdapter = new ViewAdapter(mRiderProperties.getJSONArray(ReRideJSON.SENSORS));
            recyclerView.setAdapter(mAdapter);
        } catch (JSONException e) {
            announce("No available sensor data");
            e.printStackTrace();
        }

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

    private class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ItemViewHolder> {
        private JSONArray data;
        ViewAdapter(JSONArray data) {
            this.data = data;
        }

        @Override
        public ViewAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.data_list_item, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, int position) {
            try {
                JSONObject sensorData = data.getJSONObject(position);
                holder.sensorName.setText(sensorData.getString(ReRideJSON.SENSOR_ID));
                holder.sensorUnit.setText(sensorData.getString(ReRideJSON.SENSOR_UNIT));
                holder.sensorValue.setText(sensorData.getString(ReRideJSON.VALUE));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return data.length();
        }

        void clear() {
            data = null;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView sensorName;
            TextView sensorUnit;
            TextView sensorValue;


            ItemViewHolder(View itemView) {
                super(itemView);
                sensorName = (TextView) itemView.findViewById(R.id.sensor_name);
                sensorUnit = (TextView) itemView.findViewById(R.id.data_unit);
                sensorValue = (TextView) itemView.findViewById(R.id.data_value);
            }
        }
    }

    static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEDeviceControlService.ACTION_DATA_UPDATE);
        return intentFilter;
    }
}
