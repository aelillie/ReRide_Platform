package com.anders.reride.aws;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.anders.reride.data.ReRideJSON;

import java.util.UUID;


/**
 * Responsible for communication with AWS IoT through MQTT
 */

public class AWSIoTMQTTBroker extends AWSIoTDataBroker{
    static final String LOG_TAG = AWSIoTMQTTBroker.class.getCanonicalName();

    //AWS management
    private AWSIotMqttManager mqttManager;
    private AWSCredentials mAWSCredentials;
    private String clientId;

    //MQTT
    private static final String MQTT_PUBLISH = "ReRide/" + mId + "/pub";
    //private static final String MQTT_SUBSCRIBE = "ReRide/";

    //UI
    private TextView mStatus;

    public AWSIoTMQTTBroker(Context context, String userID) {
        super(context, userID);
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();

        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAWSCredentials = credentialsProvider.getCredentials();

            }
        }).start();

    }

    public boolean connect() {
        Log.d(LOG_TAG, "clientId = " + clientId);
        try {
            mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    switch (status) {
                        case Connecting:
                            Log.d(LOG_TAG, "Connecting"); break;
                        case Connected:
                            Log.d(LOG_TAG, "Connected"); break;
                        case ConnectionLost:
                            if (throwable != null)
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            break;
                        case Reconnecting:
                            if (throwable != null)
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            break;
                        default: Log.d(LOG_TAG, "Disconnected"); break;
                    }
                }
            });
            return true;
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            return false;
        }
    }

    public void subscribe() {
        throw new UnsupportedOperationException();
        /*try {
            mqttManager.subscribeToTopic(MQTT_SUBSCRIBE, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(String topic, byte[] data) {

                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }*/
    }

    public void publish(ReRideJSON reRideJSON) {
        try {
            mqttManager.publishString(reRideJSON.getRiderProperties().toString(),
                    MQTT_PUBLISH, AWSIotMqttQos.QOS0,
                    new AWSIotMqttMessageDeliveryCallback() {
                        @Override
                        public void statusChanged(MessageDeliveryStatus status, Object userData) {
                            switch (status) {
                                case Success: Log.d(LOG_TAG, "Publish success!"); break;
                                case Fail: Log.d(LOG_TAG, "Publish fail!"); break;
                            }
                        }
                    }, null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public boolean disconnect() {
        try {
            mqttManager.disconnect();
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
            return false;
        }
    }

}
