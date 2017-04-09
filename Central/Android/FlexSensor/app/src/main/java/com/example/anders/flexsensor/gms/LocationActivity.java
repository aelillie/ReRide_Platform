package com.example.anders.flexsensor.gms;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

public class LocationActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult>,
        LocationListener{
    private static final String TAG = LocationActivity.class.getCanonicalName();

    //Settings
    private static final int PREFERRED_INTERVAL = 10000; //10 seconds
    private static final int FASTEST_INTERVAL = 5000; //5 seconds

    //Request keys
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int REQUEST_CHECK_CONNECTION = 2;
    private static final String REQUESTING_LOCATION_UPDATES_KEY =
            "com.example.anders.flexsensor.gms.LocationActivity.REQUESTING_LOCATION_UPDATES_KEY";
    private static final String LOCATION_KEY =
            "com.example.anders.flexsensor.gms.LocationActivity.LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY =
            "com.example.anders.flexsensor.gms.LocationActivity.LAST_UPDATED_TIME_STRING_KEY";

    //Location API
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;
    private LocationSettingsRequest mSettingsRequest;
    private LocationRequest mLocationRequest;

    //Properties
    private String mLastUpdateTime;
    private boolean mLocationSettingsSuccess;
    private boolean mRequestingLocationUpdates;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValuesFromBundle(savedInstanceState);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();
        mLocationSettingsResult.setResultCallback(this);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocation is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, mCurrentLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(outState);
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

    private void connect() {
        mGoogleApiClient.connect();
        Log.d(TAG, "Connected");
    }

    @Override
    protected void onStart() {
        super.onStart();
        connect();
        addLocationRequest();
        checkLocationSettings();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        Log.d(TAG, "Disconnected");
    }

    public void requestUpdates() {
        mRequestingLocationUpdates = true;
    }

    public void stopUpdates() {
        mRequestingLocationUpdates = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            mCurrentLocation = FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            Log.d(TAG, "Retrieved location info");
            if (mRequestingLocationUpdates) {
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, mLocationRequest, this);
            }
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
            finish();
        }
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
            try {
                connectionResult.startResolutionForResult(this, REQUEST_CHECK_CONNECTION);
            } catch (IntentSender.SendIntentException e) {
                Log.d(TAG, e.getMessage());
                finish();
            }
        } else {
            finish();
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
                 mLocationSettingsSuccess = true;
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.d(TAG, "Resolution required");
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    mLocationSettingsSuccess = false;
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.d(TAG, "Settings change unavailable");
                // Location settings are not satisfied. However, we have no way
                // to fix the settings so we won't show the dialog.
                mLocationSettingsSuccess = false;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == REQUEST_CHECK_CONNECTION
                || requestCode == REQUEST_CHECK_SETTINGS)) {
            //the application should try to connect again.
            connect();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Retrieved location update");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    }
}
