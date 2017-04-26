package com.anders.reride.aws;

import android.content.Context;
import android.os.Bundle;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.anders.reride.ble.BLEDeviceControlService;

/**
 * Handles data transmission with an AWS endpoint
 */

abstract class AWSIoTDataBroker {
    // --- Constants to modify per your configuration ---

    // IoT endpoint
    protected static final String CUSTOMER_SPECIFIC_ENDPOINT =
            "a3mcftlm8wc1g9.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    protected static final String COGNITO_POOL_ID =
            "eu-central-1:3f7b1bf2-d066-4976-97ea-f89c6ffbab60";
    // Region of AWS IoT
    protected static final Regions MY_REGION = Regions.EU_CENTRAL_1;

    protected final Context mContext;

    protected static String mId;

    protected CognitoCachingCredentialsProvider credentialsProvider;


    public AWSIoTDataBroker(Context context, String userID) {
        mId = userID;
        mContext = context;
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );
    }
}
