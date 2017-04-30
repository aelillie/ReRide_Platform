package com.anders.reride.data;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.List;

/**
 * Takes care of visualization of data
 */

public class ReRideHistoryDataActivity extends AppCompatActivity {
    private static final String TAG = ReRideHistoryDataActivity.class.getSimpleName();
    public static final boolean DEBUG_MODE = false;

    private AWSApiClient mAWSApiClient;
    private ViewAdapter mAdapter;
    private List<ReRideDataItemsItemPayloadSensorsItem> mRiderData;

    private String mId = ReRideUserData.USER_ID;
    private String mTimeZone = ReRideTimeManager.TIMEZONE;

    private boolean mEnabled;
    private TextView mIdText;
    private TextView mTimeText;
    private TextView mLatText;
    private TextView mLonText;
    private Button mDataButton;

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
                        final ReRideDataItemsItemPayload data =
                                mAWSApiClient.getDataLatest(mId, mTimeZone);
                        if (data == null) continue;
                        Log.d(TAG, "Got latest data");
                        runOnUiThread(new Runnable() // start actions in UI thread
                        {

                            @Override
                            public void run()
                            {
                                getData(data); // this action have to be in UI thread
                            }
                        });
                    }
                    catch (InterruptedException e)
                    {
                        announce("Operation halted");
                    }
            }
        })).start(); // the while thread will start in BG thread
    }

    private void getData(ReRideDataItemsItemPayload data) {
        if (!data.getId().equals(mId)) {
            announce("ID not identical");
            return;
        }
        mRiderData.clear();
        mRiderData.addAll(data.getSensors());
        mAdapter.notifyDataSetChanged();
        mIdText.setText(mId);
        mTimeText.setText(formatTime(data.getTime()));
        mLonText.setText(data.getLongitude());
        mLatText.setText(data.getLatitude());
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
        private List<ReRideDataItemsItemPayloadSensorsItem> data;
        ViewAdapter(List<ReRideDataItemsItemPayloadSensorsItem> data) {
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
            final ReRideDataItemsItemPayloadSensorsItem sensorData = data.get(position);
            holder.sensorName.setText(sensorData.getSensorId());
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
