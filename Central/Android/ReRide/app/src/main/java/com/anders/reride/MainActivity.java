package com.anders.reride;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.anders.reride.ble.BLEDeviceControlService;
import com.anders.reride.data.ReRideDataActivity;
import com.anders.reride.gms.ReRideLocationManager;

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

    private List<BluetoothDevice> mScannedDevices;
    private List<String> mDeviceAddresses;

    private ViewAdapter adapter;
    private boolean scanning;
    private BluetoothLeScanner scanner;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private BLECallback callback;

    private Button mScanButton;
    private ProgressBar mProgressBar;
    private Button mConnectButton;

    private BluetoothAdapter bluetoothAdapter;
    private Handler mHandler;
    private Intent mDeviceIntent;
    private ReRideLocationManager mLocationManager;

    //User sign-up settings
    private String mUserId = "1234";

    private BLEDeviceControlService mBleDeviceService;
    private final ServiceConnection mBleDeviceServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleDeviceService = ((BLEDeviceControlService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleDeviceService = null;
        }
    };
    private DeviceBroadcastReceiver mDeviceBroadcastReceiver;
    private boolean userConfirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle(R.string.device_scan_title);
        setSupportActionBar(toolbar);
        singIn();
        final Intent startIntent = new Intent(getApplicationContext(), ReRideDataActivity.class);
        startIntent.putExtra(ReRideDataActivity.EXTRAS_USER_ID, mUserId);
        if (ReRideDataActivity.DEBUG_MODE) {
            startActivity(startIntent);
            finish();
        }
        mHandler = new Handler();
        mDeviceIntent = new Intent(getApplicationContext(),
                BLEDeviceControlService.class);
        mDeviceIntent.putExtra(BLEDeviceControlService.EXTRAS_USER_ID,
                mUserId); //TODO: Get from dialog box at startup
        askForLocationPermission();
        mLocationManager = ReRideLocationManager.getInstance(this);
        mLocationManager.connect();
        mDeviceBroadcastReceiver = new DeviceBroadcastReceiver();
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
                    mDeviceIntent.putStringArrayListExtra(
                            BLEDeviceControlService.EXTRAS_DEVICE_ADDRESSES,
                            (ArrayList<String>) mDeviceAddresses);
                    if (scanning) {
                        scanner.stopScan(callback);
                        scanning = false;
                    }
                }
                bindService(mDeviceIntent,
                        mBleDeviceServiceConnection, Context.BIND_AUTO_CREATE);

                startActivity(startIntent);
            }
        });
    }

    private void singIn() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_signin);
        builder.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mUserId = ((EditText) findViewById(R.id.username)).getText().toString();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
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
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        callback = new BLECallback();
        /*ScanFilter filter = new ScanFilter.Builder().build();
        filters = new ArrayList<>();
        filters.add(filter);
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        }
        settings = settingsBuilder.build();*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }
        scanBLEDevice(false);
        resetScanResult();
        unregisterReceiver(mDeviceBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBleDeviceService != null) {
            unbindService(mBleDeviceServiceConnection);
            Log.d(TAG, "Unbound service!");
        }
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
        registerReceiver(mDeviceBroadcastReceiver, makeDeviceUpdateIntentFilter());
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
        if (resultCode == RESULT_OK &&
                (requestCode == ReRideLocationManager.REQUEST_CHECK_SETTINGS)) {
            //the application should try to connect again.
            mLocationManager.connect();
            announce("Trying to connect again. Try again in a moment.");
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

    static IntentFilter makeDeviceUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        return intentFilter;
    }

    private class DeviceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
            }
        }
    }
}
