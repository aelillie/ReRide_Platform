package com.example.anders.flexsensor;

import com.example.anders.flexsensor.ble.BLEDeviceControl;
import com.example.anders.flexsensor.ble.BLEDeviceScanner;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity
        implements FlexSensorManager.CallBack, BLEDeviceScanner.ScanResultCallback{

    public static final String TAG = MainActivity.class.getCanonicalName();
    public static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private FlexSensorManager flexSensorManager;
    private BLEDeviceScanner bleDeviceScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.app_name);

        checkForBLESupport();
        askForLocationPermission();
        flexSensorManager = new FlexSensorManager(this);
        bleDeviceScanner = new BLEDeviceScanner(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bleDeviceScanner.bleIsEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        bleDeviceScanner.scanBLEDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void askForLocationPermission() {
        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Location access not granted!");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_FINE_LOCATION);
            }
        }
    }

    private void checkForBLESupport() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (bleDeviceScanner.isScanning()) {
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
        } else {
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                bleDeviceScanner.scanBLEDevice(true);
                break;
            case R.id.menu_stop:
                bleDeviceScanner.scanBLEDevice(false);
                break;
        }
        return true;
    }


    @Override
    public void updatedState(int newState) {
//        angleText.setText(newState);
    }

    @Override
    public void foundDevice(String deviceName, String deviceAddress) {
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        final Intent intent = new Intent(this, BLEDeviceControl.class);
        intent.putExtra(BLEDeviceControl.EXTRAS_DEVICE_NAME, deviceName);
        intent.putExtra(BLEDeviceControl.EXTRAS_DEVICE_ADDRESS, deviceAddress);
        if (bleDeviceScanner.isScanning()) {
            bleDeviceScanner.scanBLEDevice(false);
        }
        startActivity(intent);
    }

    @Override
    public void deviceNotFound() {
        Toast.makeText(this, "No device found!", Toast.LENGTH_SHORT).show();
    }
}
