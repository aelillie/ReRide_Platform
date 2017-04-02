package com.example.anders.flexsensor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anders.flexsensor.ble.BLEDeviceControlActivity;
import com.example.anders.flexsensor.ble.BLEDeviceScanner;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements FlexSensorManager.CallBack, BLEDeviceScanner.ScanResultCallback{

    public static final String TAG = MainActivity.class.getCanonicalName();
    public static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private FlexSensorManager flexSensorManager;
    private BLEDeviceScanner bleDeviceScanner;
    private List<BluetoothDevice> devices;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.device_scan_title);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.devices_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        devices = new ArrayList<>();
        adapter = new ViewAdapter(devices);
        recyclerView.setAdapter(adapter);

        final Button scanButton = (Button) findViewById(R.id.scan_button);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.scan_progress);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bleDeviceScanner.isScanning()) {
                    progressBar.setVisibility(View.INVISIBLE);
                    bleDeviceScanner.scanBLEDevice(false);
                    scanButton.setText(R.string.scan);
                } else { //Initiate scan
                    progressBar.setVisibility(View.VISIBLE);
                    bleDeviceScanner.scanBLEDevice(true);
                    scanButton.setText(R.string.stop);
                }
            }
        });


        checkForBLESupport();
        askForLocationPermission();
        flexSensorManager = new FlexSensorManager(this);
        bleDeviceScanner = new BLEDeviceScanner(this);
        bleDeviceScanner.attachCallback(this);

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
    public void updatedState(int newState) {
//        angleText.setText(newState);
    }

    @Override
    public void foundDevice(BluetoothDevice device) {
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        devices.add(device);
        adapter.notifyItemChanged(devices.indexOf(device));
    }

    @Override
    public void deviceNotFound() {
        Toast.makeText(this, "No device found!", Toast.LENGTH_SHORT).show();
    }

    private class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ItemViewHolder> {
        private List<BluetoothDevice> devices;
        ViewAdapter(List<BluetoothDevice> devices) {
            this.devices = devices;
        }

        @Override
        public ViewAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView v = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            final BluetoothDevice device = devices.get(position);
            holder.deviceName.setText(device.getName());
            holder.connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent = new Intent(getApplicationContext(),
                            BLEDeviceControlActivity.class);
                    intent.putExtra(BLEDeviceControlActivity.EXTRAS_DEVICE_ADDRESS,
                            device.getAddress());
                    if (bleDeviceScanner.isScanning()) {
                        bleDeviceScanner.scanBLEDevice(false);
                    }
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView deviceName;
            Button connectButton;

            ItemViewHolder(View itemView) {
                super(itemView);
                deviceName = (TextView) itemView.findViewById(R.id.item_text);
                connectButton = (Button) itemView.findViewById(R.id.connect_button);
            }
        }
    }
}
