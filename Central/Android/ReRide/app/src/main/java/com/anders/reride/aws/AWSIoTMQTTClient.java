package com.anders.reride.aws;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttMessageDeliveryCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.anders.reride.ble.BLEDeviceControlService;
import com.anders.reride.data.ReRideJSON;

import java.util.UUID;


/**
 * Responsible for communication with AWS IoT through MQTT
 */

public class AWSIoTMQTTClient extends AWSIoTDataBroker{
    static final String LOG_TAG = AWSIoTMQTTClient.class.getCanonicalName();

    public static final boolean TEST_MODE = true;

    //AWS management
    private AWSIotMqttManager mqttManager;
    private AWSCredentials mAWSCredentials;
    private String clientId;

    //MQTT
    private static final String MQTT_PUBLISH = "ReRide/" + mId + "/pub";
    //private static final String MQTT_SUBSCRIBE = "ReRide/";

    //UI
    private TextView mStatus;
    private boolean mConnected;

    public AWSIoTMQTTClient(Context context, String userID) {
        super(context, userID);
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();

        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                mAWSCredentials = credentialsProvider.getCredentials();

            }
        }).start();*/

    }

    public boolean isConnected() {
        return mConnected;
    }

    public boolean connect() {
        Log.d(LOG_TAG, "clientId = " + clientId);
        if (TEST_MODE) {
            mConnected = true;
            return true;
        }
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
                            mConnected = true;
                            Log.d(LOG_TAG, "Connected");
                            break;
                        case ConnectionLost:
                            mConnected = false;
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

    public void publish(final ReRideJSON reRideJSON) {
        try {
            if (BLEDeviceControlService.TEST_GMS || TEST_MODE) {
                Log.d(LOG_TAG, "Publish success (test)");
                return;
            }
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
        } catch (AmazonClientException e) {
            Log.e(LOG_TAG, "Publish error.", e);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    publish(reRideJSON);
                }
            }, 1000);
            connect();
        }
    }

    public boolean disconnect() {
        if (!mConnected) return true;
        if (TEST_MODE) {
            mConnected = false;
            return true;
        }
        try {
            mqttManager.disconnect();
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
            return false;
        }
    }

}
