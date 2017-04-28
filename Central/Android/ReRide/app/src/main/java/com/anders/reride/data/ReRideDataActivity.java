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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.anders.reride.R;
import com.anders.reride.aws.AWSApiClient;
import com.anders.reride.ble.BLEDeviceControlService;
import com.anders.reride.model.ReRideDataItemsItemPayload;
import com.anders.reride.model.ReRideDataItemsItemPayloadSensorsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes care of visualization of data
 */

public class ReRideDataActivity extends AppCompatActivity {
    private static final String TAG = ReRideDataActivity.class.getSimpleName();
    public static final boolean DEBUG_MODE = true;

    public static final String EXTRAS_USER_ID =
            "com.anders.reride.data.EXTRAS_USER_ID";

    private AWSApiClient mAWSApiClient;
    private ViewAdapter mAdapter;
    private List<ReRideDataItemsItemPayloadSensorsItem> mRiderData;
    private Handler mHandler;

    private String mId;
    private String mTimeZone = "2"; //TODO: Necessary?
    private TextView mIdText;
    private TextView mTimeText;
    private TextView mLatText;
    private TextView mLonText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        Intent intent = getIntent();
        mId = intent.getStringExtra(EXTRAS_USER_ID);
        mAWSApiClient = new AWSApiClient();
        mHandler = new Handler();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.data_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRiderData = new ArrayList<>();
        mAdapter = new ViewAdapter(mRiderData);
        recyclerView.setAdapter(mAdapter);

        initializeUIComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getData(false);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    private void initializeUIComponents() {
        mIdText = (TextView) findViewById(R.id.id_value);
        mIdText.setText(mId);
        mTimeText = (TextView) findViewById(R.id.time_value);
        mLatText = (TextView) findViewById(R.id.location_lat_value);
        mLonText = (TextView) findViewById(R.id.location_lon_value);
    }

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void getData(final boolean enabled) {
        (new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                while (!Thread.interrupted() && enabled && isNetworkConnected())
                    try
                    {
                        final ReRideDataItemsItemPayload data =
                                mAWSApiClient.getDataLatest(mId, mTimeZone);
                        runOnUiThread(new Runnable() // start actions in UI thread
                        {

                            @Override
                            public void run()
                            {
                                getData(data); // this action have to be in UI thread
                            }
                        });
                        Thread.sleep(BLEDeviceControlService.UPDATE_FREQUENCY);
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
        mTimeText.setText(data.getTime());
        mLonText.setText(data.getLongitude());
        mLatText.setText(data.getLatitude());
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
