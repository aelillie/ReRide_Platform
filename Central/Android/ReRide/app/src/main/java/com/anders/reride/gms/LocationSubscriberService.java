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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Broadcasts location updates on a custom specified interval
 * source: https://developer.android.com/training/location/retrieve-current.html
 */

public class LocationSubscriberService extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult>,
        LocationListener{
    private static final String TAG = LocationSubscriberService.class.getCanonicalName();

    //Settings
    private static final int PREFERRED_INTERVAL = 5000; //5 seconds
    private static final int FASTEST_INTERVAL = 3000; //3 seconds

    //Request keys
    public static final int REQUEST_CHECK_SETTINGS = 1;
    public static final int REQUEST_CHECK_CONNECTION = 2;

    //Actions
    public static final String ACTION_CONNECTED =
            "com.anders.reride.gms.LocationSubscriberService.ACTION_CONNECTED";
    public static final String ACTION_UPDATE_AVAILABLE =
            "com.anders.reride.gms.LocationSubscriberService.ACTION_UPDATE_AVAILABLE";
    public static final String ACTION_CONNECTION_FAILED =
            "com.anders.reride.gms.LocationSubscriberService.ACTION_CONNECTION_FAILED";
    public static final String ACTION_SETTINGS_FAILED =
            "com.anders.reride.gms.LocationSubscriberService.ACTION_SETTINGS_FAILED";

    //Intent keys
    public static final String LAST_LOCATION_STRING_KEY =
            "com.anders.reride.gms.LocationSubscriberService.LAST_LOCATION_STRING_KEY";
    public static final String LAST_TIME_STRING_KEY =
            "com.anders.reride.gms.LocationSubscriberService.LAST_TIME_STRING_KEY";
    public static final String ERROR_STRING_KEY =
            "com.anders.reride.gms.LocationSubscriberService.ERROR_STRING_KEY";

    //Properties
    public static final int LONGITUDE_ID = 0;
    public static final int LATITUDE_ID = 1;

    //Location API
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;
    private LocationSettingsRequest mSettingsRequest;
    private LocationRequest mLocationRequest;

    private final IBinder binder = new LocalBinder();

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

    private void broadcastUpdate(final String action,
                                 Location location, String time) {
        final Intent intent = new Intent(action);
        double lon = location.getLongitude();
        double lat = location.getLatitude();
        double[] newLocation = new double[2];
        newLocation[LONGITUDE_ID] = lon;
        newLocation[LATITUDE_ID] = lat;
        intent.putExtra(LAST_LOCATION_STRING_KEY, newLocation);
        intent.putExtra(LAST_TIME_STRING_KEY, time);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public LocationSubscriberService getService() {
            return LocationSubscriberService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopUpdates();
        disconnect();
        Log.d(TAG, "Disconnected");
        return super.onUnbind(intent);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(PREFERRED_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setPriority(LocationRequest
                        .PRIORITY_BALANCED_POWER_ACCURACY); //precision within 100 meters
        Log.d(TAG, "Location request created");
    }

    private void addLocationRequest() {
        mSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .build();
        Log.d(TAG, "Location request added");
    }

    private void checkLocationSettings() {
        mLocationSettingsResult = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient, mSettingsRequest);
        mLocationSettingsResult.setResultCallback(this);
        Log.d(TAG, "Awaiting location request result");
    }

    public void connect() {
        mGoogleApiClient.connect();
        Log.d(TAG, "Connected");
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
        Log.d(TAG, "Disconnected");
    }


    public void requestUpdates() {
        createLocationRequest();
        addLocationRequest();
        checkLocationSettings();
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public void stopUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
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
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        if (status.isSuccess()) return;
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can
                // initialize location requests here.
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.d(TAG, "Resolution required");
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                broadcastUpdate(ACTION_SETTINGS_FAILED, result);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.d(TAG, "Settings change unavailable");
                // Location settings are not satisfied. However, we have no way
                // to fix the settings so we won't show the dialog.
                break;
        }
    }



    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Retrieved location update");
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+2"));
        String lastUpdateTime = now.get(Calendar.HOUR_OF_DAY) + ""
                + now.get(Calendar.MINUTE) + ""
                + now.get(Calendar.SECOND);
        broadcastUpdate(ACTION_UPDATE_AVAILABLE, location, lastUpdateTime);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
