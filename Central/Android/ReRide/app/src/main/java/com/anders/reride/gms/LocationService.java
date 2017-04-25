package com.anders.reride.gms;

import android.app.Service;
import android.content.Context;
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

public class LocationService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = LocationSubscriberService.class.getCanonicalName();

    private boolean mRequiresResolution;
    private boolean mConnected;

    //Location API
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;


    public LocationService(Context context) {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

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
        mConnected = true;
        mRequiresResolution = false;
    }

    @Override
    public void onConnectionSuspended(int cause) {
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
        mConnected = false;
        Log.d(TAG, connectionResult.getErrorMessage());
        if (connectionResult.getErrorCode() == ConnectionResult.RESOLUTION_REQUIRED) {
            mRequiresResolution = true;
        }
    }

    public boolean isRequiresResolution() {
        return mRequiresResolution;
    }

    public boolean isConnected() {
        return mConnected;
    }
}
