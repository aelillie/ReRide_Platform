package com.anders.reride.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.R;
import com.anders.reride.aws.AWSApiClient;
import com.anders.reride.ble.BLEDeviceControlService;
import com.anders.reride.gms.ReRideTimeManager;
import com.anders.reride.model.ReRideDataItemsItemPayload;
import com.anders.reride.model.ReRideDataItemsItemPayloadSensorsItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes care of visualization of data
 */

public class ReRideHistoryDataActivity extends AppCompatActivity {
    private static final String TAG = ReRideHistoryDataActivity.class.getSimpleName();
    public static final boolean DEBUG_MODE = false;

    private AWSApiClient mAWSApiClient;
    private ViewAdapter mAdapter;
    private List<SensorHistoryData> mRiderData;
    private ReRideDataItemsItemPayloadSensorsItem sampleSensor;

    private String mId = ReRideUserData.USER_ID;
    private String mTimeZone = ReRideTimeManager.TIMEZONE;

    private boolean mEnabled;
    private TextView mIdText;
    private TextView mTimeText;
    private TextView mLatText;
    private TextView mLonText;
    private Button mDataButton;
    private String startTime, endTime, startLon, startLat, endLon, endLat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_historical_data);
        toolbar.setTitle(R.string.historical_data);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mAWSApiClient = new AWSApiClient();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.historical_data_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRiderData = new ArrayList<>();
        mAdapter = new ViewAdapter(mRiderData);
        recyclerView.setAdapter(mAdapter);

        initializeUIComponents();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mEnabled) {
            mEnabled = false;
            mDataButton.setText(R.string.get_data_button_text);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    private void initializeUIComponents() {
        mIdText = (TextView) findViewById(R.id.id_history_value);
        mIdText.setText(mId);
        mTimeText = (TextView) findViewById(R.id.time_history_value);
        mLatText = (TextView) findViewById(R.id.location_lat_history_value);
        mLonText = (TextView) findViewById(R.id.location_lon_history_value);
        mDataButton = (Button) findViewById(R.id.start_button);
        mDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEnabled) {
                    mEnabled = false;
                    mDataButton.setText(R.string.get_data_button_text);
                } else {
                    mEnabled = true;
                    mDataButton.setText(R.string.stop);
                    getData();
                }
            }
        });
    }

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void getData() {
        (new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                while (!Thread.interrupted() && mEnabled && isNetworkConnected())
                    try
                    {
                        Thread.sleep(BLEDeviceControlService.UPDATE_FREQUENCY);
                        final List<ReRideDataItemsItemPayload> data =
                                mAWSApiClient.getData(mId, 15, 0, mTimeZone);
                        if (data == null) continue;
                        Log.d(TAG, "Got data");
                        try {
                            compute(data);
                        } catch (final IllegalArgumentException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    announce(e.getMessage());
                                }
                            });
                        }
                    }
                    catch (InterruptedException e)
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                announce("Operation halted");
                            }
                        });
                    }
            }
        })).start(); // the while thread will start in BG thread
    }

    private void compute(List<ReRideDataItemsItemPayload> data)
            throws IllegalArgumentException{
        ReRideDataItemsItemPayload sampleItem = data.get(0);
        String id = sampleItem.getId();
        if (!id.equals(mId)) {
            throw new IllegalArgumentException("IDs do not match");
        }

        //Start position
        startLon = sampleItem.getLongitude();
        startLat = sampleItem.getLatitude();
        startTime = sampleItem.getTime();
        //End position
        int end = data.size()-1;
        endLon = data.get(end).getLongitude();
        endLat = data.get(end).getLatitude();
        endTime = data.get(end).getTime();


        mRiderData.clear();
        //Find average values
        for (ReRideDataItemsItemPayload payload : data) { //Go through all data
            List<Integer> sensorValues = new ArrayList<>();
            List<ReRideDataItemsItemPayloadSensorsItem> sensors = payload.getSensors();
            for (int i = 0; i < sensors.size(); i++) {
                int oldVal = sensorValues.get(j);
                sensorValues.set(j, oldVal + Integer.parseInt(sensor.getValue()));

                mRiderData.add(new SensorHistoryData(sensor.getCharacteristic(),
                        sensor.getName(), sensor.getUnit(), ))
            }
        }



        runOnUiThread(new Runnable() // start actions in UI thread
        {

            @Override
            public void run()
            {
                showData(); // this action have to be in UI thread
            }
        });
    }

    private class SensorHistoryData {
        private String sensorCharacteristic;
        private String sensorName;
        private String sensorUnit;
        private String sensorValue;

        public SensorHistoryData(String sensorCharacteristic, String sensorName, String sensorUnit,
                                 String sensorValue) {
            this.sensorCharacteristic = sensorCharacteristic;
            this.sensorName = sensorName;
            this.sensorUnit = sensorUnit;
            this.sensorValue = sensorValue;
        }

        String getSensorCharacteristic() {
            return sensorCharacteristic;
        }

        String getSensorName() {
            return sensorName;
        }

        String getSensorUnit() {
            return sensorUnit;
        }

        String getSensorValue() {
            return sensorValue;
        }
    }


    private void showData() {
        mAdapter.notifyDataSetChanged();
        mIdText.setText(mId);
        mTimeText.setText(startTime);
        mLonText.setText(startLon);
        mLatText.setText(startLat);
        Log.d(TAG, "Updated UI with data");
    }

    private String formatTime(String simpleTime) {
        return simpleTime.substring(0, 4) + "." + //Year
                simpleTime.substring(4, 6) + "." + //Month
                simpleTime.substring(6, 8) + " " + //Day
                simpleTime.substring(8, 10) + ":" + //Hour
                simpleTime.substring(10, 12) + ":" + //Minutes
                simpleTime.substring(12, 14); //YYYY.MM.DD HH:MM:SS
    }

    private class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ItemViewHolder> {
        private List<SensorHistoryData> data;
        ViewAdapter(List<SensorHistoryData> data) {
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
            final SensorHistoryData sensorData = data.get(position);
            holder.sensorName.setText(sensorData.getName());
            holder.sensorUnit.setText(sensorData.getUnit());
            holder.sensorValue.setText(sensorData.getValue());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        void clear() {
            data.clear();
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
}
