package com.anders.reride.gms;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResult;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Class for retrieving current GPS position
 */

public class ReRideLocationManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = ReRideLocationManager.class.getCanonicalName();
    public static final int REQUEST_CHECK_SETTINGS = 1;

    private boolean mRequiresResolution;
    private boolean mConnected;

    //Location API
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;
    private ConnectionResult mConnectionResult;
    private static ReRideLocationManager mLocationManager;


    private ReRideLocationManager(Context context) {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    public static ReRideLocationManager getInstance(Context context) {
        if (mLocationManager == null) {
            mLocationManager = new ReRideLocationManager(context);
        }
        return mLocationManager;
    }

    public void connect() {
        if (!mConnected) {
            mGoogleApiClient.connect();
        }
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    public void reconnect() {
        mGoogleApiClient.reconnect();
        Log.d(TAG, "Reconnecting...");
    }


    public Location getLocation() {
        try {
             Location location = FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            Log.d(TAG, "Retrieved location info");
            return location;
        } catch (SecurityException e) {
            return null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected");
        mConnected = true;
        mRequiresResolution = false;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Disconnected");
        mConnected = false;
        switch (cause) {
            case CAUSE_NETWORK_LOST:
                //TODO: Try to reconnect network
                break;
            case CAUSE_SERVICE_DISCONNECTED:
                //TODO: Try to reconnect service
                break;
            default:
                Log.d(TAG, "Unknown cause on connection suspended");
                break;
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Disconnected");
        mConnected = false;
        Log.d(TAG, connectionResult.getErrorMessage());
        if (connectionResult.getErrorCode() == ConnectionResult.RESOLUTION_REQUIRED) {
            mRequiresResolution = true;
            mConnectionResult = connectionResult;
        }
    }

    public ConnectionResult getConnectionResult() {
        return mConnectionResult;
    }

    public boolean requiresResolution() {
        return mRequiresResolution;
    }

    public void setRequiresResolution(boolean enabled) {
        mRequiresResolution = enabled;
    }

    public boolean isConnected() {
        return mConnected;
    }
}
