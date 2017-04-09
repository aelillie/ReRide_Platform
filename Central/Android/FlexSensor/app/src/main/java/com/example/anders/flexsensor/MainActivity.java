package com.example.anders.flexsensor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity{

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private static final int PERMISSION_REQUEST_LOCATION = 2;
    private static final String[] LOCATION_PERMISSIONS =
            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
    private static final String DEVICE_NAME = "ReRide";

    private List<BluetoothDevice> devices;

    private ViewAdapter adapter;
    private boolean scanning;
    private BluetoothLeScanner scanner;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private BLECallback callback;

    private Button mScanButton;
    private ProgressBar mProgressBar;

    private BluetoothAdapter bluetoothAdapter;
    private boolean mDeviceFound;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.device_scan_title);
        setSupportActionBar(toolbar);
        mHandler = new Handler();

        askForLocationPermission();
        checkForBLESupport();
        setupScanSettings();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.devices_recycler_view);
        //recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        devices = new ArrayList<>();
        adapter = new ViewAdapter(devices);
        recyclerView.setAdapter(adapter);

        mScanButton = (Button) findViewById(R.id.scan_button);
        mProgressBar = (ProgressBar) findViewById(R.id.scan_progress);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanning) {
                    scanBLEDevice(false);
                    onScanStop();
                } else { //Initiate scan
                    resetScanResult();
                    mProgressBar.setVisibility(View.VISIBLE);
                    scanBLEDevice(true);
                    mScanButton.setText(R.string.stop);
                }
            }
        });
    }

    private void onScanStop() {
        if (!mDeviceFound) {
            Toast.makeText(getApplicationContext(),
                    DEVICE_NAME + " not found", Toast.LENGTH_SHORT).show();
        }
        mProgressBar.setVisibility(View.INVISIBLE);
        mScanButton.setText(R.string.scan);
    }

    private void setupScanSettings() {
        final BluetoothManager bluetoothManager = (BluetoothManager)
                getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        Log.d(TAG, "BLE adapter found");
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName(DEVICE_NAME)
                .build();
        filters = new ArrayList<>();
        filters.add(filter);
        callback = new BLECallback();
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        }
        settings = settingsBuilder.build();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanBLEDevice(false);
        resetScanResult();
    }

    private void resetScanResult() {
        if (devices.isEmpty()) return;
        devices.clear();
        adapter.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
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
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location access not granted!");
            ActivityCompat.requestPermissions(this,
                    LOCATION_PERMISSIONS,
                    PERMISSION_REQUEST_LOCATION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            finish();
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkForBLESupport() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ItemViewHolder> {
        private List<BluetoothDevice> devices;
        ViewAdapter(List<BluetoothDevice> devices) {
            this.devices = devices;
        }

        @Override
        public ViewAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            final BluetoothDevice device = devices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                holder.deviceName.setText(device.getName());
            } else {
                holder.deviceName.setText(R.string.unknown_device);
            }
            holder.connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent = new Intent(getApplicationContext(),
                            BLEDeviceControlActivity.class);
                    intent.putExtra(BLEDeviceControlActivity.EXTRAS_DEVICE_ADDRESS,
                            device.getAddress());
                    if (scanning) {
                        scanner.stopScan(callback);
                        scanning = false;
                    }
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        void clear() {
            devices.clear();
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

    /**
     * Start or stop a scan for BLE devices
     * @param enable If true, enables a scan, which will stop after
     *               SCAN_PERIOD, otherwise stops a scan
     */
    private void scanBLEDevice(final boolean enable) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "BT not enabled",
                    Toast.LENGTH_LONG).show();
        }
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    scanner.stopScan(callback);
                    onScanStop();
                }
            }, SCAN_PERIOD);

            scanning = true;
            scanner.startScan(callback); //filters, settings
        } else {
            scanning = false;
            scanner.stopScan(callback);
        }
    }

    private class BLECallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mDeviceFound = true;
            BluetoothDevice bleDevice = result.getDevice();
            Log.d(TAG, "Found device");
            if (devices.contains(bleDevice)) return;
            devices.add(bleDevice);
            adapter.notifyItemChanged(devices.indexOf(bleDevice));
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "already started"); break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.d(TAG, "cannot be registered"); break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "power optimized scan not supported"); break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "internal error"); break;
                default: Log.d(TAG, "unrecognized error"); break;
            }
            Toast.makeText(getApplicationContext(),
                    "Scan failed!", Toast.LENGTH_SHORT).show();
        }
    }
}
