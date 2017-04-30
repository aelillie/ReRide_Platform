package com.anders.reride;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.anders.reride.ble.BLEScanActivity;
import com.anders.reride.data.MultiRiderActivity;
import com.anders.reride.data.ReRideHistoryDataActivity;
import com.anders.reride.data.ReRideUserData;

/**
 * Main activity and start page
 */

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCE_USER_ID =
            "com.anders.reride.PREFERENCE_USER_ID";
    private static final String PREFERENCE_TIMEZONE =
            "com.anders.reride.PREFERENCE_TIMEZONE";
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        initializeUIComponents();
        mPreferences = this.getPreferences(Context.MODE_PRIVATE);
        if (mPreferences.contains(PREFERENCE_USER_ID) &&
                mPreferences.contains(PREFERENCE_TIMEZONE)) {
            ReRideUserData.USER_ID = mPreferences.getString(PREFERENCE_USER_ID,
                    getResources().getString(R.string.preference_user_id_default));
            ReRideUserData.TIMEZONE = mPreferences.getString(PREFERENCE_TIMEZONE,
                    getResources().getString(R.string.preference_timezone_default));
        } else {
            signIn();
        }


    }

    private void initializeUIComponents() {
        Button rideButton = (Button) findViewById(R.id.button_start_ride);
        rideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),
                        BLEScanActivity.class));
            }
        });
        Button multiRideButton = (Button) findViewById(R.id.button_multi_rider);
        multiRideButton.setEnabled(false); //TODO: Implement activity
        multiRideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),
                        MultiRiderActivity.class));
            }
        });
        Button dataButton = (Button) findViewById(R.id.button_rider_data);
        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),
                        ReRideHistoryDataActivity.class));
            }
        });
        Button settingsButton = (Button) findViewById(R.id.button_change_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }


    private void signIn() {
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signin, null);
        final String[] userId = new String[1];
        final String[] timeZone = new String[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                userId[0] = ((EditText) dialogView.findViewById(R.id.username))
                        .getText().toString();
                timeZone[0] = ((EditText) dialogView.findViewById(R.id.timeZone))
                        .getText().toString();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREFERENCE_USER_ID, userId[0]);
        editor.putString(PREFERENCE_TIMEZONE, timeZone[0]);
        editor.apply(); //In background
    }
}
