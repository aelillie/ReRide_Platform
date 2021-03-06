package com.anders.reride.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
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

import com.anders.reride.R;
import com.anders.reride.data.ReRideDataActivity;
import com.anders.reride.gms.ReRideLocationManager;

import java.util.ArrayList;
import java.util.List;


public class BLEScanActivity extends AppCompatActivity{

    private static final String TAG = BLEScanActivity.class.getCanonicalName();
    private static final int REQUEST_ENABLE_BT = 1; //Request code for BLE Intent
    private static final int PERMISSION_REQUEST_LOCATION = 2;
    private static final String[] LOCATION_PERMISSIONS =
            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.



    private List<BluetoothDevice> mScannedDevices;
    private List<String> mDeviceAddresses;

    private ViewAdapter adapter;
    private boolean scanning;
    private BluetoothLeScanner scanner;
    private BLECallback callback;

    private Button mScanButton;
    private ProgressBar mProgressBar;
    private Button mConnectButton;

    private BluetoothAdapter bluetoothAdapter;
    private Handler mHandler;
    private Intent mDeviceControl;
    private ReRideLocationManager mLocationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ble_scan);
        toolbar.setTitle(R.string.device_scan_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        final Intent reRideData = new Intent(getApplicationContext(),
                ReRideDataActivity.class);
        mDeviceControl = new Intent(getApplicationContext(),
                BLEDeviceControlService.class);
        if (ReRideDataActivity.DEBUG_MODE) {
            startActivity(reRideData);
            finish();
        }
        mHandler = new Handler();
        mLocationManager = ReRideLocationManager.getInstance(this);
        if (locationAccessPermitted()) {
            mLocationManager.connect();
        }
        checkForBLESupport();
        setupScanSettings();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.devices_recycler_view);
        //recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mScannedDevices = new ArrayList<>();
        mDeviceAddresses = new ArrayList<>();
        adapter = new ViewAdapter(mScannedDevices);
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
        mConnectButton = (Button) findViewById(R.id.connect_button);
        if (BLEDeviceControlService.TEST_GMS) mConnectButton.setEnabled(true);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mLocationManager.isConnected()) mLocationManager.connect();
                if (mLocationManager.requiresResolution()) {
                    try {
                        mLocationManager.getConnectionResult().startResolutionForResult(getParent(),
                                ReRideLocationManager.REQUEST_CHECK_SETTINGS);
                        mLocationManager.setRequiresResolution(false);
                        announce("Fixing location settings");
                        return;
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                        mLocationManager.reconnect();
                        return;
                    }
                }
                if (!BLEDeviceControlService.TEST_GMS) {
                    if (mDeviceAddresses.size() == 0) return;
                    mDeviceControl.putStringArrayListExtra(
                            BLEDeviceControlService.EXTRAS_DEVICE_ADDRESSES,
                            (ArrayList<String>) mDeviceAddresses);
                    if (scanning) {
                        scanner.stopScan(callback);
                        scanning = false;
                    }
                }
                startService(mDeviceControl);
                startActivity(reRideData);
            }
        });
    }



    private void announce(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void onScanStop() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mScanButton.setText(R.string.scan);
    }

    private void setupScanSettings() {
        final BluetoothManager bluetoothManager = (BluetoothManager)
                getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        Log.d(TAG, "BLE adapter found");
        callback = new BLECallback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }
        scanBLEDevice(false);
        resetScanResult();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.disconnect();
        stopService(new Intent(this, BLEDeviceControlService.class));
        Log.d(TAG, "Stopped service!");
    }

    private void resetScanResult() {
        if (mScannedDevices.isEmpty()) return;
        mScannedDevices.clear();
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
        if (mLocationManager != null && !mLocationManager.isConnected()) {
            mLocationManager.connect();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish();
            return;
        }
        if (resultCode == RESULT_OK &&
                (requestCode == ReRideLocationManager.REQUEST_CHECK_SETTINGS)) {
            //the application should try to connect again.
            mLocationManager.connect();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean locationAccessPermitted() {
        // Prompt for permissions
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location access not granted!");
            ActivityCompat.requestPermissions(this,
                    LOCATION_PERMISSIONS,
                    PERMISSION_REQUEST_LOCATION);
            return false;
        } else return true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION
                && (grantResults[0] == PackageManager.PERMISSION_DENIED ||
                    grantResults[1] == PackageManager.PERMISSION_DENIED)) {
            announce("Location permission not granted");
        } else {
            mLocationManager.connect();
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
                    .inflate(R.layout.devices_list_item, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, int position) {
            final BluetoothDevice device = devices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                holder.deviceName.setText(device.getName());
            } else {
                holder.deviceName.setText(R.string.unknown_device);
            }
            holder.selectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDeviceAddresses.add(device.getAddress());
                    holder.selectButton.setEnabled(false);
                    if (!mConnectButton.isEnabled()) mConnectButton.setEnabled(true);
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
            Button selectButton;

            ItemViewHolder(View itemView) {
                super(itemView);
                deviceName = (TextView) itemView.findViewById(R.id.item_text);
                selectButton = (Button) itemView.findViewById(R.id.select_button);
            }
        }
    }

    /**
     * Start or stop a scan for BLE mScannedDevices
     * @param enable If true, enables a scan, which will stop after
     *               SCAN_PERIOD, otherwise stops a scan
     */
    private void scanBLEDevice(final boolean enable) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "BT not enabled",
                    Toast.LENGTH_LONG).show();
            return;
        } else {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
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
            BluetoothDevice bleDevice = result.getDevice();
            Log.d(TAG, "Scanned: " + bleDevice.getName() + ": " + bleDevice.getAddress());
            if (mScannedDevices.contains(bleDevice)) return;
            mScannedDevices.add(bleDevice);
            adapter.notifyItemChanged(mScannedDevices.indexOf(bleDevice));
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
