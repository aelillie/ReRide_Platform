package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.Bundle;

/**
 * Relay manager for data management
 */

public class AWSIoTManager extends AWSIoTDataManagement{

    public AWSIoTManager(Context context, PROTOCOL protocol) {
        super(context, protocol);
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
    public Bundle getData() {
        return null;
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
