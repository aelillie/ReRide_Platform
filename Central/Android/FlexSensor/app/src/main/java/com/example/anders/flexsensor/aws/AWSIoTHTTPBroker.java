package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.google.gson.Gson;

import java.nio.ByteBuffer;

/**
 * Responsible for communication with AWS IoT through HTTP
 */

class AWSIoTHTTPBroker extends AWSIoTDataBroker{
    private static final String LOG_TAG = AWSIoTHTTPBroker.class.getSimpleName();


    private AWSIotDataClient iotDataClient;

    public AWSIoTHTTPBroker(Context context, String userID) {
        super(context, userID);
    }

    @Override
    public void publish(Bundle data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShadow(Bundle state) {
        super.updateShadow(state);
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName(mId);
        Log.i(LOG_TAG, mJState.toString());
        updateShadowTask.setState(mJState.toString());
        updateShadowTask.execute();
    }

    private void flexSensorStatusUpdated(String flexSensorStatusState) {
        Gson gson = new Gson();
        FlexSensorStatus ts = gson.fromJson(flexSensorStatusState, FlexSensorStatus.class);

        Log.i(LOG_TAG, String.format("angle:  %d", ts.state.desired.angle));
        Log.i(LOG_TAG, String.format("curState: %s", ts.state.desired.curState));
    }

    @Override
    public void subscribe() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getShadow() {
        //new GetShadowTask(mId).execute();
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean connect() {
        iotDataClient = new AWSIotDataClient(credentialsProvider);
        iotDataClient.setEndpoint(CUSTOMER_SPECIFIC_ENDPOINT);
        return true; //TODO: Try-catch
    }

    @Override
    public boolean disconnect() {
        iotDataClient.shutdown(); //TODO: What is this?
        iotDataClient = null;
        return true; //TODO: Try-catch
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
