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
import android.widget.TextView;
import android.widget.Toast;

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
    private SharedPreferences mPreferences;
    private Button multiRideButton;
    private Button dataButton;
    private Button settingsButton;
    private Button rideButton;
    private TextView idText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initializeUIComponents();
        mPreferences = this.getPreferences(Context.MODE_PRIVATE);
        if (mPreferences.contains(PREFERENCE_USER_ID)) {
            String userId = mPreferences.getString(PREFERENCE_USER_ID,
                    getResources().getString(R.string.preference_user_id_default));
            idText.setText(userId);
            updateUserSettings(userId);
        } else {
            signIn();
        }


    }

    private void updateUserSettings(String userId) {
        ReRideUserData.USER_ID = userId;
    }

    private void initializeUIComponents() {
        idText = (TextView) findViewById(R.id.id_text);
        rideButton = (Button) findViewById(R.id.button_start_ride);
        rideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),
                        BLEScanActivity.class));
            }
        });
        multiRideButton = (Button) findViewById(R.id.button_multi_rider);
        multiRideButton.setEnabled(false); //TODO: Implement activity
        multiRideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),
                        MultiRiderActivity.class));
            }
        });
        dataButton = (Button) findViewById(R.id.button_rider_data);
        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),
                        ReRideHistoryDataActivity.class));
            }
        });
        settingsButton = (Button) findViewById(R.id.button_change_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void enableButtons(boolean enable) {
        rideButton.setEnabled(enable);
        //multiRideButton.setEnabled(enable);
        dataButton.setEnabled(enable);
    }


    private void signIn() {
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signin, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String userId = ((EditText) dialogView.findViewById(R.id.username))
                        .getText().toString();
                boolean success = saveSettings(userId);
                if (!success) {
                    builder.create().show();
                } else {
                    announce("Success");
                    enableButtons(true);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                enableButtons(false);
            }
        });
        builder.create().show();

    }

    private boolean saveSettings(String userId) {
        SharedPreferences.Editor editor = mPreferences.edit();
        if (userId.isEmpty()) {
            announce("Please enter all settings");
            return false;
        }
        editor.putString(PREFERENCE_USER_ID, userId);
        editor.apply(); //In background
        idText.setText(userId);
        updateUserSettings(userId);
        return true;
    }
}
