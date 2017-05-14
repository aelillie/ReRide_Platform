package com.anders.reride.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.R;
import com.anders.reride.aws.AWSApiClient;
import com.anders.reride.gms.ReRideTimeManager;
import com.anders.reride.model.ReRideDataItemsItemPayload;
import com.anders.reride.model.ReRideDataItemsItemPayloadSensorsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes care of visualization of data
 */

public class ReRideHistoryDataActivity extends AppCompatActivity {
    private static final String TAG = ReRideHistoryDataActivity.class.getSimpleName();
    public static final boolean DEBUG_MODE = false;
    private static final int MIN_PER_HOUR = 60;

    private AWSApiClient mAWSApiClient;
    private ViewAdapter mAdapter;
    private List<SensorHistoryData> mRiderData;
    private List<SensorHistoryData> newSensors;

    private String mId = ReRideUserData.USER_ID;

    private String mTimeZone = ReRideTimeManager.TIMEZONE;
    private boolean mEnabled;
    private TextView mIdText;
    private TextView mTimeStart;
    private TextView mLatStart;
    private TextView mLonStart;
    private Button mDataButton;
    private String startTime, endTime, startLon, startLat, endLon, endLat;
    private TextView mTimeEnd;
    private TextView mLatEnd;
    private TextView mLonEnd;
    private EditText mFromTime;
    private EditText mEndTime;

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
        mFromTime = (EditText) findViewById(R.id.from_text);
        mEndTime = (EditText) findViewById(R.id.end_text);
        mIdText = (TextView) findViewById(R.id.id_history_value);
        mIdText.setText(mId);
        mTimeStart = (TextView) findViewById(R.id.time_start_value);
        mTimeEnd = (TextView) findViewById(R.id.time_end_value);
        mLatStart = (TextView) findViewById(R.id.location_lat_start_value);
        mLatEnd = (TextView) findViewById(R.id.location_lat_end_value);
        mLonStart = (TextView) findViewById(R.id.location_lon_start_value);
        mLonEnd = (TextView) findViewById(R.id.location_lon_end_value);
        mDataButton = (Button) findViewById(R.id.start_button);
        mDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if no view has focus:
                View v = getCurrentFocus();
                if (v != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                Editable from = mFromTime.getText();
                Editable to = mEndTime.getText();
                if (from.length() > 0 && to.length() > 0) {
                    int f = Integer.parseInt(from.toString())*MIN_PER_HOUR;
                    int t = Integer.parseInt(to.toString())*MIN_PER_HOUR;
                    if (t > f) {
                        announce("Start time must be greater than end time");
                    } else {
                        getData(f,t);
                    }
                } else {
                    announce("No range specified - fetching all available data");
                    getData(-1, 0);
                }
            }
        });
    }

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void getData(final int fromTime, final int endTime) {
        (new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                if (!Thread.interrupted() && isNetworkConnected()) {
                    final List<ReRideDataItemsItemPayload> data =
                            mAWSApiClient.getData(mId, fromTime, endTime, mTimeZone);
                    if (data.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                announce("No data");
                                clearUI();
                            }
                        });
                        return;
                    }
                    Log.d(TAG, "Got data");
                    try {
                        compute(data);
                    } catch (final IllegalArgumentException e) {
                        Log.d(TAG, e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                announce(e.getMessage());
                            }
                        });
                    }

                }
            }
        })).start(); // the while thread will start in BG thread
    }

    private void clearUI() {
        mRiderData.clear();
        CharSequence noDataText = getResources().getText(R.string.no_data);
        mAdapter.notifyDataSetChanged();
        mIdText.setText(noDataText);
        mTimeStart.setText(noDataText);
        mTimeEnd.setText(noDataText);
        mLonStart.setText(noDataText);
        mLonEnd.setText(noDataText);
        mLatStart.setText(noDataText);
        mLatEnd.setText(noDataText);
        Log.d(TAG, "Cleared UI");
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

        List<ReRideDataItemsItemPayloadSensorsItem> sampleSensors = sampleItem.getSensors();
        int sensorsSize = sampleSensors.size();
        int[] sensorValues = new int[sensorsSize];
        //Find average values
        for (ReRideDataItemsItemPayload payload : data) { //Go through all data
            List<ReRideDataItemsItemPayloadSensorsItem> sensors = //Sensors for one item
                    payload.getSensors();
            for (int i = 0; i < sensorsSize; i++) {
                int sensorVal = Integer.parseInt(sensors.get(i).getValue());
                try {
                    if (sensorValues[i] != 0) {
                        int oldVal = sensorValues[i]/data.size();
                        sensorValues[i] = oldVal + sensorVal;
                    } else {
                        sensorValues[i] = sensorVal;
                    }
                } catch (IndexOutOfBoundsException e) {
                    Log.d(TAG, e.getMessage());
                    return;
                }

            }
        }

        newSensors = new ArrayList<>(sensorsSize);
        for(int i = 0; i < sensorsSize; i++) {
            ReRideDataItemsItemPayloadSensorsItem sensor = sampleSensors.get(i);
            newSensors.add(new SensorHistoryData(sensor.getCharacteristic(),
                    sensor.getName(), sensor.getUnit(),
                    String.valueOf(sensorValues[i])));
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

        SensorHistoryData(String sensorCharacteristic, String sensorName, String sensorUnit,
                          String sensorValue) {
            this.sensorCharacteristic = sensorCharacteristic;
            this.sensorName = sensorName;
            this.sensorUnit = sensorUnit;
            this.sensorValue = sensorValue;
        }

        String getCharacteristic() {
            return sensorCharacteristic;
        }

        String getName() {
            return sensorName;
        }

        String getUnit() {
            return sensorUnit;
        }

        String getValue() {
            return sensorValue;
        }
    }


    private void showData() {
        mRiderData.clear();
        mRiderData.addAll(newSensors);
        mAdapter.notifyDataSetChanged();
        mIdText.setText(mId);
        mTimeStart.setText(formatTime(startTime));
        mTimeEnd.setText(formatTime(endTime));
        mLonStart.setText(startLon);
        mLonEnd.setText(endLon);
        mLatStart.setText(startLat);
        mLatEnd.setText(endLat);
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
            holder.sensorCharacteristic.setText(sensorData.getCharacteristic());
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
            TextView sensorCharacteristic;


            ItemViewHolder(View itemView) {
                super(itemView);
                sensorName = (TextView) itemView.findViewById(R.id.sensor_name);
                sensorUnit = (TextView) itemView.findViewById(R.id.data_unit);
                sensorValue = (TextView) itemView.findViewById(R.id.data_value);
                sensorCharacteristic = (TextView) itemView.findViewById(R.id.characteristic_name);
            }
        }
    }
}
