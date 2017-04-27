package com.anders.reride.data;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.anders.reride.R;
import com.anders.reride.aws.AWSApiClient;
import com.anders.reride.model.ReRideDataItemsItemPayload;
import com.anders.reride.model.ReRideDataItemsItemPayloadSensorsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes care of visualization of data
 */

public class ReRideDataActivity extends AppCompatActivity {

    private AWSApiClient mAWSApiClient;
    private ViewAdapter mAdapter;
    private List<ReRideDataItemsItemPayloadSensorsItem> mRiderData;

    private String mId;
    private String mTimeZone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        mAWSApiClient = new AWSApiClient();
        //TODO: Get user id from MainActivity
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.data_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRiderData = new ArrayList<>();
        mAdapter = new ViewAdapter(mRiderData);
        recyclerView.setAdapter(mAdapter);

        //TODO: Initialize other UI components
    }

    private void getData() {
        while (true) {
            ReRideDataItemsItemPayload data = mAWSApiClient.getDataLatest(mId, mTimeZone);
            mRiderData = data.getSensors();
            mAdapter.notifyDataSetChanged();
            //Also set other UI components
        }
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
