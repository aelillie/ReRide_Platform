package com.example.anders.flexsensor.aws;

import android.content.Context;

/**
 * Interface for handling data management
 */

public abstract class AWSIoTDataManagement implements AWSIoTOperations{
    protected AWSIoTDataBroker mDataBroker;

    public AWSIoTDataManagement(Context context, PROTOCOL protocol) {
        switch (protocol) {
            case HTTP:
                mDataBroker = new AWSIoTHTTPBroker(context);
                break;
            case MQTT:
                mDataBroker = new AWSIoTMQTTBroker(context);
                break;
        }
    }
}
