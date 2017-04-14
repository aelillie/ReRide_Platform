package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.Bundle;

/**
 * Interface for handling data management
 */

public class AWSIoTManager implements AWSIoTOperations{
    protected AWSIoTDataBroker mDataBroker;

    public AWSIoTManager(Context context, PROTOCOL protocol) {
        switch (protocol) {
            case HTTP:
                mDataBroker = new AWSIoTHTTPBroker(context);
                break;
            case MQTT:
                mDataBroker = new AWSIoTMQTTBroker(context);
                break;
        }
    }

    @Override
    public void publish(Bundle data) {
        mDataBroker.publish(data);
    }

    @Override
    public void subscribe() {
        mDataBroker.subscribe();
    }

    @Override
    public Bundle getShadow() {
        return mDataBroker.getShadow();
    }

    @Override
    public void updateShadow(Bundle state) {
        mDataBroker.updateShadow(state);
    }

    @Override
    public boolean connect() {
        return mDataBroker.connect();
    }

    @Override
    public boolean disconnect() {
        return mDataBroker.disconnect();
    }
}
