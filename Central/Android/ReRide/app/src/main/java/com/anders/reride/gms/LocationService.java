package com.anders.reride.gms;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
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

public class LocationService extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = LocationSubscriberService.class.getCanonicalName();

    //Request keys
    public static final int REQUEST_CHECK_SETTINGS = 1;
    public static final int REQUEST_CHECK_CONNECTION = 2;

    //Actions
    public static final String ACTION_CONNECTED =
            "com.example.anders.flexsensor.gms.LocationSubscriberService.ACTION_CONNECTED";
    public static final String ACTION_CONNECTION_FAILED =
            "com.example.anders.flexsensor.gms.LocationSubscriberService.ACTION_CONNECTION_FAILED";

    //Intent keys
    public static final String ERROR_STRING_KEY =
            "com.example.anders.flexsensor.gms.LocationSubscriberService.ERROR_STRING_KEY";

    //Location API
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;

    private final IBinder binder = new LocationService.LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, Parcelable result) {
        final Intent intent = new Intent(action);
        intent.putExtra(ERROR_STRING_KEY, result);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        connect();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        Log.d(TAG, "Disconnected");
        return super.onUnbind(intent);
    }

    public void connect() {
        mGoogleApiClient.connect();
        Log.d(TAG, "Connected");
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
        Log.d(TAG, "Disconnected");
    }


    public Location getLocation() {
        //TODO: Return location
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
        broadcastUpdate(ACTION_CONNECTED);
    }

    @Override
    public void onConnectionSuspended(int cause) {
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
        Log.d(TAG, connectionResult.getErrorMessage());
        if (connectionResult.getErrorCode() == ConnectionResult.RESOLUTION_REQUIRED) {
            broadcastUpdate(ACTION_CONNECTION_FAILED, connectionResult);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
