package com.example.anders.flexsensor.aws;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;
import com.example.anders.flexsensor.R;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


/**
 * Responsible for communication with AWS IoT through MQTT
 */

public class PubSubFragment {
    static final String LOG_TAG = PubSubFragment.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3mcftlm8wc1g9.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-central-1:b6eda114-0f5c-456d-8128-c1cd2c0aa73d";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_CENTRAL_1;
    private final Context mContext;

    //AWS management
    AWSIotMqttManager mqttManager;
    AWSCredentials mAWSCredentials;
    String clientId;
    CognitoCachingCredentialsProvider credentialsProvider;

    //MQTT
    private static final String MQTT_UPDATE = "$aws/things/FlexSensor/shadow/update";
    private static final String MQTT_GET = "$aws/things/FlexSensor/shadow/get";

    //UI
    private TextView mStatus;

    public PubSubFragment(Context context) {
        mContext = context;
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context.getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAWSCredentials = credentialsProvider.getCredentials();

            }
        }).start();

    }

    public void connect() {
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
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }
    }

    public void subscribe() {
        try {
            mqttManager.subscribeToTopic(MQTT_GET, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            /*getActivity().runOnUiThread(new Runnable() {
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
                            });*/
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    public void publish(String msg) {

        try {
            mqttManager.publishString(msg, MQTT_UPDATE, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public void disconnect() {
        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }

}
