package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.example.anders.flexsensor.ble.BLEDeviceControlActivity;
import com.google.gson.Gson;

import java.nio.ByteBuffer;

/**
 * Responsible for communication with AWS IoT through WebSocket
 */

public class AWSIoTManager {

    private static final String LOG_TAG = AWSIoTManager.class.getSimpleName();

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    private static final String CUSTOMER_SPECIFIC_ENDPOINT =
            "a3mcftlm8wc1g9.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID =
            "eu-central-1:b6eda114-0f5c-456d-8128-c1cd2c0aa73d";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_CENTRAL_1;

    private AWSIotDataClient iotDataClient;

    private static final String THING_NAME = "FlexSensor";

    public AWSIoTManager(Context context) {
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        iotDataClient = new AWSIotDataClient(credentialsProvider);
        iotDataClient.setEndpoint(CUSTOMER_SPECIFIC_ENDPOINT);
    }

    public void update(Bundle data) {
        String newAngle = data.getString(BLEDeviceControlActivity.EXTRAS_ANGLE_DATA);
        double[] newLocation = data.getDoubleArray(BLEDeviceControlActivity.EXTRAS_LOCATION_DATA);
        String newTime = data.getString(BLEDeviceControlActivity.EXTRAS_TIME_DATA);
        //TODO: Use location
        Log.i(LOG_TAG, "New angle:" + newAngle);
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName(THING_NAME);
        String newState = String.format("{\"state\":{\"reported\":{\"angle\":%s}}}", newAngle);
        Log.i(LOG_TAG, newState);
        updateShadowTask.setState(newState);
        updateShadowTask.execute();
    }

    public void getShadow() {
        new GetShadowTask(THING_NAME).execute();
    }

    private void flexSensorStatusUpdated(String flexSensorStatusState) {
        Gson gson = new Gson();
        FlexSensorStatus ts = gson.fromJson(flexSensorStatusState, FlexSensorStatus.class);

        Log.i(LOG_TAG, String.format("angle:  %d", ts.state.desired.angle));
        Log.i(LOG_TAG, String.format("curState: %s", ts.state.desired.curState));
    }

    private class GetShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private final String thingName;

        GetShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                GetThingShadowRequest getThingShadowRequest = new GetThingShadowRequest()
                        .withThingName(thingName);
                GetThingShadowResult result = iotDataClient.getThingShadow(getThingShadowRequest);
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<>(resultString);
            } catch (Exception e) {
                Log.e("E", "getShadowTask", e);
                return new AsyncTaskResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(GetShadowTask.class.getCanonicalName(), result.getResult());
                flexSensorStatusUpdated(result.getResult());
            } else {
                Log.e(GetShadowTask.class.getCanonicalName(), "getShadowTask", result.getError());
            }
        }
    }

    private class UpdateShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private String thingName;
        private String updateState;

        void setThingName(String name) {
            thingName = name;
        }

        void setState(String state) {
            updateState = state;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                UpdateThingShadowRequest request = new UpdateThingShadowRequest();
                request.setThingName(thingName);

                ByteBuffer payloadBuffer = ByteBuffer.wrap(updateState.getBytes());
                request.setPayload(payloadBuffer);

                UpdateThingShadowResult result = iotDataClient.updateThingShadow(request);

                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<>(resultString);
            } catch (Exception e) {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "updateShadowTask", e);
                return new AsyncTaskResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(UpdateShadowTask.class.getCanonicalName(), result.getResult());
            } else {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "Error in Update Shadow",
                        result.getError());
            }
        }
    }


}
