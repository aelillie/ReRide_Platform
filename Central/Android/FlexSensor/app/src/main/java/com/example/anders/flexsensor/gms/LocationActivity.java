package com.example.anders.flexsensor.gms;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.example.anders.flexsensor.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Provides a way to get location information
 * source: https://developer.android.com/training/location/retrieve-current.html
 */

public class LocationActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult>{
    private static final String TAG = LocationActivity.class.getCanonicalName();

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private boolean mLocationSettingsSuccess;

    private final int PREFERRED_INTERVAL = 10000; //10 seconds
    private final int FASTEST_INTERVAL = 5000; //5 seconds
    private final int REQUEST_CHECK_SETTINGS = 1;
    private PendingResult<LocationSettingsResult> mLocationSettingsResult;
    private LocationSettingsRequest mSettingsRequest;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            mLastLocation = FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
                mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            }
            Log.d(TAG, "Retrieved location info");
        } catch (SecurityException e) {
            mLatitudeText.setText(R.string.error);
            mLongitudeText.setText(R.string.error);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
        if (requestCode == REQUEST_CHECK_SETTINGS
                && resultCode == RESULT_OK) {
            //the application should try to connect again.
            connect();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
