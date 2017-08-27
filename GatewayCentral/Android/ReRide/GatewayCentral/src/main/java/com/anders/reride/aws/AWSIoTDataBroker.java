package com.anders.reride.aws;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

/**
 * Handles data transmission with an AWS endpoint
 */

abstract class AWSIoTDataBroker {
    // --- Constants to modify per your configuration ---

    // IoT endpoint
    protected static final String CUSTOMER_SPECIFIC_ENDPOINT =
            "ao87a9xyl0izr.iot.ap-southeast-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    protected static final String COGNITO_POOL_ID =
            "ap-southeast-1:ee11ced0-cfa8-4578-acfd-9df32f611d4b";
    // Region of AWS IoT
    protected static final Regions MY_REGION = Regions.AP_SOUTHEAST_1;

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
