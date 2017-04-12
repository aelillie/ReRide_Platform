package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.Bundle;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.example.anders.flexsensor.ble.BLEDeviceControlActivity;
import com.example.anders.flexsensor.gms.LocationService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles data transmission with an AWS endpoint
 */

abstract class AWSIoTDataBroker implements AWSIoTOperations {
    // --- Constants to modify per your configuration ---

    // IoT endpoint
    protected static final String CUSTOMER_SPECIFIC_ENDPOINT =
            "a3mcftlm8wc1g9.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    protected static final String COGNITO_POOL_ID =
            "eu-central-1:b6eda114-0f5c-456d-8128-c1cd2c0aa73d";
    // Region of AWS IoT
    protected static final Regions MY_REGION = Regions.EU_CENTRAL_1;

    protected final Context mContext;

    protected static final String THING_NAME = "FlexSensor";

    protected CognitoCachingCredentialsProvider credentialsProvider;

    protected final JSONObject mJState;
    private final JSONObject mJElement;
    private final JSONObject mJData;


    public AWSIoTDataBroker(Context context) {
        mContext = context;
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        mJState = new JSONObject();
        mJElement = new JSONObject();
        mJData = new JSONObject();
        try {
            mJElement.put("reported", mJData);
            mJState.put("state", mJElement);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(Bundle data) {
        String newAngle = data.getString(BLEDeviceControlActivity.EXTRAS_ANGLE_DATA);
        double[] newLocation = data.getDoubleArray(BLEDeviceControlActivity.EXTRAS_LOCATION_DATA);
        if (newLocation == null) throw new IllegalArgumentException();
        String newTime = data.getString(BLEDeviceControlActivity.EXTRAS_TIME_DATA);
        createJSON(newAngle,
                newLocation[LocationService.LONGITUDE_ID],
                newLocation[LocationService.LATITUDE_ID],
                newTime);
    }

    private void createJSON(String newAngle, double lon, double lat, String newTime){
        try {
            mJData.put("angle", newAngle);
            mJData.put("longitude", lon);
            mJData.put("latitude", lat);
            mJData.put("time", newTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
