package com.example.anders.flexsensor.gms;

import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
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

import java.text.DateFormat;
import java.util.Date;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Provides a way to get location information
 * source: https://developer.android.com/training/location/retrieve-current.html
 */

public class LocationService extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult>,
        LocationListener{
    private static final String TAG = LocationService.class.getCanonicalName();

    //Settings
    private static final int PREFERRED_INTERVAL = 5000; //5 seconds
    private static final int FASTEST_INTERVAL = 3000; //3 seconds

    //Request keys
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int REQUEST_CHECK_CONNECTION = 2;
    private static final String REQUESTING_LOCATION_UPDATES_KEY =
            "com.example.anders.flexsensor.gms.LocationService.REQUESTING_LOCATION_UPDATES_KEY";
    private static final String LOCATION_KEY =
            "com.example.anders.flexsensor.gms.LocationService.LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY =
            "com.example.anders.flexsensor.gms.LocationService.LAST_UPDATED_TIME_STRING_KEY";

    public static final String ACTION_CONNECTED =
            "com.example.anders.flexsensor.gms.LocationService.ACTION_CONNECTED";
    public static final String ACTION_UPDATE_AVAILABLE =
            "com.example.anders.flexsensor.gms.LocationService.ACTION_UPDATE_AVAILABLE";
    public static final String LAST_LOCATION_STRING_KEY =
            "com.example.anders.flexsensor.gms.LocationService.LAST_LOCATION_STRING_KEY";
    public static final String LAST_TIME_STRING_KEY =
            "com.example.anders.flexsensor.gms.LocationService.LAST_TIME_STRING_KEY";
    public static final int LONGITUDE_ID = 0;
    public static final int LATITUDE_ID = 1;

    //Location API
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;
    private LocationSettingsRequest mSettingsRequest;
    private LocationRequest mLocationRequest;

    //Properties
    private boolean mConnected;
    private boolean mLocationSettingsSuccess;

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

        mLocationSettingsResult.setResultCallback(this);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
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
        public LocationService getService() {
            return LocationService.this;
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
        mConnected = true;
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
        /*if (connectionResult.getErrorCode() == ConnectionResult.RESOLUTION_REQUIRED) {
            try {
                connectionResult.startResolutionForResult(get, REQUEST_CHECK_CONNECTION);
            } catch (IntentSender.SendIntentException e) {
                Log.d(TAG, e.getMessage());
            }
        }*/
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        if (status.isSuccess()) return;
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can
                // initialize location requests here.
                 mLocationSettingsSuccess = true;
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.d(TAG, "Resolution required");
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                /*try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    mLocationSettingsSuccess = false;
                }*/
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.d(TAG, "Settings change unavailable");
                // Location settings are not satisfied. However, we have no way
                // to fix the settings so we won't show the dialog.
                mLocationSettingsSuccess = false;
                break;
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == REQUEST_CHECK_CONNECTION
                || requestCode == REQUEST_CHECK_SETTINGS)) {
            //the application should try to connect again.
            connect();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }*/

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Retrieved location update");
        String lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        broadcastUpdate(ACTION_UPDATE_AVAILABLE, location, lastUpdateTime);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
