package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.util.UUID;


/**
 * Responsible for communication with AWS IoT through MQTT
 */

class AWSIoTMQTTBroker extends AWSIoTDataBroker{
    static final String LOG_TAG = AWSIoTMQTTBroker.class.getCanonicalName();

    //AWS management
    private AWSIotMqttManager mqttManager;
    private AWSCredentials mAWSCredentials;
    private String clientId;

    //MQTT
    private static final String MQTT_PUBLISH = "ReRide/"; //TODO: Possible post-fix?
    private static final String MQTT_SUBSCRIBE = "ReRide/"; //TODO: Possible post-fix?

    //UI
    private TextView mStatus;

    public AWSIoTMQTTBroker(Context context) {
        super(context);
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

    @Override
    public boolean connect() {
        Log.d(LOG_TAG, "clientId = " + clientId);
        try {
            mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    /*getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (status) {
                                case Connecting:
                                    mStatus.setText(R.string.connecting); break;
                                case Connected:
                                    mStatus.setText(R.string.connected); break;
                                case ConnectionLost:
                                    if (throwable != null)
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    mStatus.setText(R.string.disconnected);
                                    break;
                                case Reconnecting:
                                    if (throwable != null)
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    mStatus.setText(R.string.reconnecting);
                                    break;
                                default: mStatus.setText(R.string.disconnected); break;
                            }
                        }
                    });*/
                }
            });
            return true;
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            return false;
        }
    }

    @Override
    public void subscribe() {
        throw new UnsupportedOperationException();
        /*try {
            mqttManager.subscribeToTopic(MQTT_SUBSCRIBE, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            *//*getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);

                                        //tvLastMessage.setText(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }
                                }
                            });*//*
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }*/
    }

    @Override
    public Bundle getShadow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShadow(Bundle state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publish(Bundle data) {
        super.publish(data);
        try {
            mqttManager.publishString(mJState.toString(), MQTT_SUBSCRIBE, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    @Override
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
