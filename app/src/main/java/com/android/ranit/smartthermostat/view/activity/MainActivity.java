package com.android.ranit.smartthermostat.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.airbnb.lottie.LottieAnimationView;
import com.android.ranit.smartthermostat.R;
import com.android.ranit.smartthermostat.common.CharacteristicTypes;
import com.android.ranit.smartthermostat.common.ConnectionStates;
import com.android.ranit.smartthermostat.common.Constants;
import com.android.ranit.smartthermostat.contract.MainActivityContract;
import com.android.ranit.smartthermostat.data.BleDeviceDataObject;
import com.android.ranit.smartthermostat.databinding.ActivityMainBinding;
import com.android.ranit.smartthermostat.model.DataManager;
import com.android.ranit.smartthermostat.service.BleConnectivityService;
import com.android.ranit.smartthermostat.view.adapter.BleDeviceAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements MainActivityContract.View {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String ANIMATION_TEMPERATURE = "temperature.json";
    private static final String ANIMATION_LED_ON = "on.json";
    private static final String ANIMATION_LED_OFF = "off.json";
    private static final String ANIMATION_SCANNING = "scanning.json";
    private static final String ANIMATION_STOPPED = "stopped.json";

    private ConnectionStates mCurrentState = ConnectionStates.DISCONNECTED;

    private final String[] PERMISSIONS = {
            // Note: Only 'ACCESS_FINE_LOCATION' permission is needed from user at run-time
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BleConnectivityService mService;

    private ActivityMainBinding mBinding;
    private View mCustomAlertView;
    private RecyclerView mRecyclerView;
    private LottieAnimationView mScanningLottieView;
    private BleDeviceAdapter mRvAdapter;

    private final List<BluetoothDevice> mBleDeviceList = new ArrayList<>();

    private Intent mServiceIntent;
    private boolean mIsLedButtonClicked = false;

    /**
     * Manage Service life-cycles
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = ((BleConnectivityService.LocalBinder) service).getService();
            if (!mService.initializeBluetoothAdapter()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected: ");
            mService = null;
        }
    };

    /**
     * Broadcast Receiver to communicate and update UI components from Service
     */
    private final BroadcastReceiver mGattStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BleConnectivityService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "onReceive: ACTION_GATT_SERVICES_DISCOVERED");

            } else if (BleConnectivityService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive: ACTION_DATA_AVAILABLE");

                // Receive data via broadcast intent and display in UI
                int currentType = intent.getIntExtra(BleConnectivityService.DATA_TYPE, -1);
                if (currentType == 0) {
                    String temperature = intent.getStringExtra(BleConnectivityService.EXTRA_DATA);
                    mBinding.tvTemperature.setText(temperature);
                } else if (currentType == 1) {
                    String ledState = intent.getStringExtra(BleConnectivityService.EXTRA_DATA);
                    onLedBroadcastEventReceived(ledState);
                }
            }
        }
    };

    /**
     * Observer for current-connection-state of BLE device
     */
    private final Observer<BleDeviceDataObject> mDeviceConnectionStateObserver = new Observer<BleDeviceDataObject>() {
        @Override
        public void onChanged(BleDeviceDataObject bleDeviceDataObject) {
            Log.d(TAG, "onChanged() called with: connectionState = [" + bleDeviceDataObject.getCurrentConnectionState() + "]");
            mCurrentState = bleDeviceDataObject.getCurrentConnectionState();

            if (mCurrentState.equals(ConnectionStates.CONNECTED)) {
                if (bleDeviceDataObject.getBluetoothDevice() != null) {
                    onConnectedBroadcastReceived(bleDeviceDataObject.getBluetoothDevice());
                }
            } else if (mCurrentState.equals(ConnectionStates.DISCONNECTED)) {
                onDisconnectedBroadcastReceived();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        initializeComponents();
        initializeUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerToGattBroadcastReceiver();
        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissionsAtRuntime()) {
            if (!checkBluetoothStatus()) {
                enableBluetoothRequest();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFromGattBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearComponents();
    }

    @Override
    public void initializeComponents() {
        Log.d(TAG, "initializeComponents() called");

        // Initialize and Bind to Service
        mServiceIntent = new Intent(this, BleConnectivityService.class);
        bindToService();

        // Set initial state of BLE device in Live-Data as DISCONNECTED and bluetoothDevice as 'null'
        DataManager.getInstance()
                .setBleDeviceLiveData(new BleDeviceDataObject(ConnectionStates.DISCONNECTED, null));

        // Attach observer for live-data
        DataManager.getInstance().getBleDeviceLiveData().observeForever(mDeviceConnectionStateObserver);
    }

    @Override
    public void initializeUi() {
        Log.d(TAG, "initializeUi() called");

        // Prepare initial animations
        startAnimation(mBinding.lottieViewTemperature, ANIMATION_TEMPERATURE, false);
        startAnimation(mBinding.lottieViewLight, ANIMATION_LED_OFF, false);

        prepareConnectButton();
        prepareReadTemperatureButton();
        prepareNotifyTemperatureButton();
        prepareLedToggleButton();
    }

    @Override
    public void clearComponents() {
        Log.d(TAG, "clearComponents() called");
        DataManager.getInstance().getBleDeviceLiveData().removeObserver(mDeviceConnectionStateObserver);
        unbindFromService();
    }

    @Override
    public void startAnimation(LottieAnimationView animationView, String animationName, boolean loop) {
        animationView.setAnimation(animationName);
        animationView.loop(loop);
        animationView.playAnimation();
    }

    @Override
    public void stopAnimation(LottieAnimationView animationView) {
        animationView.cancelAnimation();
    }

    @Override
    public void displaySnackBar(String message) {
        Snackbar.make(mBinding.layoutMain, message, Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void changeVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }

    @Override
    public void switchButtonText(Button button, String text) {
        button.setText(text);
    }

    @Override
    public void disableButtons() {
        Log.d(TAG, "disableButtons() called");
        mBinding.btnReadTemperature.setClickable(false);
        mBinding.btnEnableNotify.setClickable(false);
        mBinding.btnToggleLed.setClickable(false);

        mBinding.btnReadTemperature.setAlpha(0.4f);
        mBinding.btnEnableNotify.setAlpha(0.4f);
        mBinding.btnToggleLed.setAlpha(0.4f);
    }

    @Override
    public void enableButtons() {
        Log.d(TAG, "enableButtons() called");
        mBinding.btnReadTemperature.setClickable(true);
        mBinding.btnEnableNotify.setClickable(true);
        mBinding.btnToggleLed.setClickable(true);

        mBinding.btnReadTemperature.setAlpha(1f);
        mBinding.btnEnableNotify.setAlpha(1f);
        mBinding.btnToggleLed.setAlpha(1f);
    }

    @Override
    public void prepareConnectButton() {
        Log.d(TAG, "prepareConnectButton() called");
        mBinding.btnStartScanning.setOnClickListener(connectButtonClickListener);
    }

    @Override
    public void prepareDisconnectButton() {
        Log.d(TAG, "prepareDisconnectButton() called");
        mBinding.btnStartScanning.setOnClickListener(disconnectButtonClickListener);
    }

    @Override
    public void launchDeviceScanDialog() {
        Log.d(TAG, "launchDeviceScanDialog() called");
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(mCustomAlertView)
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.dialog_positive_button, null)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setNeutralButton(R.string.dialog_neutral_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        stopScanning();
                        dialogInterface.dismiss();
                    }
                })
                .setCancelable(false)
                .create();

        // Implemented in order to avoid auto-dismiss upon click of a dialog button
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);

                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Start button clicked");
                        startScanning();
                    }
                });

                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Stop button clicked");
                        stopScanning();
                    }
                });
            }
        });
        dialog.show();
    }

    @Override
    public void prepareReadTemperatureButton() {
        Log.d(TAG, "prepareReadTemperatureButton() called");
        mBinding.btnReadTemperature.setOnClickListener(readTemperatureButtonClickListener);
    }

    @Override
    public void prepareNotifyTemperatureButton() {
        Log.d(TAG, "prepareNotifyTemperatureButton() called");
        mBinding.btnEnableNotify.setOnClickListener(notifyTemperatureButtonClickListener);
    }

    @Override
    public void prepareLedToggleButton() {
        Log.d(TAG, "prepareLedButton() called");
        mBinding.btnToggleLed.setOnClickListener(toggleLedButtonClickListener);
    }

    /**
     * Start Scanning for BLE Devices
     *
     * Scanning requires 3 parameters to 'Start Scanning':
     * a) ScanFilter (pass 'null' in case no-specific filtering is required)
     * b) ScanSettings
     * c) ScanCallback
     */
    @Override
    public void startScanning() {
        Log.d(TAG, "startScanning() called");
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mScanningLottieView.getVisibility() != View.VISIBLE) {
            changeVisibility(mScanningLottieView, View.VISIBLE);
        }
        playDialogAnimation(ANIMATION_SCANNING);

        // Begin Scan
        Log.d(TAG, "Started Scanning for BLE devices");
        mBluetoothLeScanner.startScan(null, bluetoothLeScanSettings, bluetoothLeScanCallback);
    }

    /**
     * Scanning consumes a lot of battery resource.
     * Hence, stopScan is mandatory
     *
     * 'stopScan' requires one 1 parameter (i.e) 'ScanCallback'
     */
    @Override
    public void stopScanning() {
        Log.d(TAG, "stopScanning() called");

        if (mScanningLottieView.getVisibility() != View.VISIBLE) {
            changeVisibility(mScanningLottieView, View.VISIBLE);
            changeVisibility(mRecyclerView, View.GONE);
        }
        playDialogAnimation(ANIMATION_STOPPED);

        mBluetoothLeScanner.stopScan(bluetoothLeScanCallback);
        mBleDeviceList.clear();
        mRvAdapter.notifyDataSetChanged();
    }

    /**
     * Update UI when CONNECTED broadcast is received
     */
    @Override
    public void onConnectedBroadcastReceived(BluetoothDevice device) {
        Log.d(TAG, "onConnectedBroadcastReceived() called");

        String deviceName = device.getName();
        String mDeviceAddress = device.getAddress();

        enableButtons();
        prepareDisconnectButton();
        updateAdapterConnectionState(-1);

        mBinding.tvDeviceName.setText(deviceName);
        mBinding.tvConnectivityStatus.setText(R.string.connected);
        mBinding.tvConnectivityStatus.setTextColor(getResources().getColor(R.color.green_500));
        switchButtonText(mBinding.btnStartScanning, getResources().getString(R.string.disconnect));
    }

    /**
     * Update UI when DISCONNECTED broadcast is received
     */
    @Override
    public void onDisconnectedBroadcastReceived() {
        Log.d(TAG, "onDisconnectedBroadcastReceived() called");

        disableButtons();
        prepareConnectButton();

        mBinding.tvDeviceName.setText(getString(R.string.no_sensor_connected));
        mBinding.tvConnectivityStatus.setText(R.string.no_sensor_connected);
        mBinding.tvTemperature.setText("0");
        stopAnimation(mBinding.lottieViewTemperature);
        mBinding.tvConnectivityStatus.setTextColor(getResources().getColor(R.color.red_500));
        switchButtonText(mBinding.btnStartScanning, getResources().getString(R.string.connect));
    }

    @Override
    public void onLedBroadcastEventReceived(String ledState) {
        Log.d(TAG, "onLedBroadcastEventReceived() called with: ledState = [" + ledState + "]");

        if (ledState.equals(Constants.ON)) {
            startAnimation(mBinding.lottieViewLight, ANIMATION_LED_ON, false);
            switchButtonText(mBinding.btnToggleLed, getString(R.string.led_on));
            mBinding.btnToggleLed.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_on, null));
        } else {
            startAnimation(mBinding.lottieViewLight, ANIMATION_LED_OFF, false);
            switchButtonText(mBinding.btnToggleLed, getString(R.string.led_off));
            mBinding.btnToggleLed.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_off, null));
        }
    }

    @Override
    public void bindToService() {
        Log.d(TAG, "bindToService() called");
        bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void unbindFromService() {
        Log.d(TAG, "unbindFromService() called");
        unbindService(mServiceConnection);
    }

    @Override
    public void registerToGattBroadcastReceiver() {
        Log.d(TAG, "registerToGattBroadcastReceiver() called");
        registerReceiver(mGattStatusBroadcastReceiver, gattIntentFilters());
    }

    @Override
    public void unregisterFromGattBroadcastReceiver() {
        Log.d(TAG, "unregisterFromGattBroadcastReceiver() called");
        unregisterReceiver(mGattStatusBroadcastReceiver);
    }

    @Override
    public void connectToDevice(String address) {
        Log.d(TAG, "connectToDevice() called with: address = [" + address + "]");
        mService.connectToBleDevice(address);
    }

    @Override
    public void disconnectFromDevice() {
        Log.d(TAG, "disconnectFromDevice() called");
        mService.disconnectFromBleDevice();
    }

    @Override
    public void requestPermissions() {
        Log.d(TAG, "requestPermissions() called");
        ActivityCompat.requestPermissions(this, PERMISSIONS, Constants.REQUEST_PERMISSION_ALL);
    }

    @Override
    public boolean checkPermissionsAtRuntime() {
        Log.d(TAG, "checkPermissionsAtRuntime() called");
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkBluetoothStatus() {
        Log.d(TAG, "checkBluetoothStatus() called");
        if (mBluetoothAdapter != null) {
            // Return Bluetooth Enable Status
            return mBluetoothAdapter.isEnabled();
        } else {
            displaySnackBar("This device doesn't support Bluetooth");
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean isAlertDialogInflated = false;
        if (requestCode == Constants.REQUEST_PERMISSION_ALL) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    boolean showRationale = shouldShowRequestPermissionRationale(permission);
                    if (!showRationale) {
                        // Called when user selects 'NEVER ASK AGAIN'
                        isAlertDialogInflated = true;

                    } else {
                        // Called when user selects 'DENY'
                        displaySnackBar("Enable permission");
                    }
                }
            }
            inflateEnablePermissionDialog(isAlertDialogInflated);
        }
    }

    /**
     * Shows Alert Dialog when User denies permission permanently
     *
     * @param isTrue - true when user selects on never-ask-again
     */
    private void inflateEnablePermissionDialog(boolean isTrue) {
        if (isTrue) {
            // Inflate Alert Dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permissions Mandatory")
                    .setMessage("Kindly enable all permissions through Settings")
                    .setPositiveButton("OKAY", (dialogInterface, i) -> {
                        launchAppSettings();
                        dialogInterface.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Launch Enable Bluetooth Request
     */
    private void enableBluetoothRequest() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BLUETOOTH);
    }

    /**
     * Launch App-Settings Screen
     */
    private void launchAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, Constants.REQUEST_PERMISSION_SETTING);
    }

    /**
     * Click listener for Connect button
     * Launches alert dialog which facilitates scanning and connection to BLE device.
     */
    private final View.OnClickListener connectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "connectButton() clicked");
            // Launch Custom Alert-Dialog
            prepareAlertDialog();

            // Start Scanning prior to launch
            startScanning();
            launchDeviceScanDialog();
        }
    };

    /**
     * Click listener for disconnect button
     * Initiates disconnection request to GATT server in order to disconnect from BLE device.
     */
    private final View.OnClickListener disconnectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "disconnectButton() clicked");
            disconnectFromDevice();
        }
    };

    /**
     * Click listener for Read-temperature button
     * Initiates read-temperature request to BleConnectivityService for Temperature characteristic.
     *
     * Once service is discovered, we filter out the necessary characteristic in 'onServiceDiscovered'
     * callback of the BleGatt and read the same using 'readCharacteristic'.
     *
     * Once characteristic is read, 'onCharacteristicRead' callback of BleGatt is triggered.
     *
     * Note: A characteristic is read only once.
     */
    private final View.OnClickListener readTemperatureButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "readTemperatureButton() clicked");
            mService.readCharacteristicValue(CharacteristicTypes.TEMPERATURE);
        }
    };

    /**
     * Click listener for Notify-Temperature button
     *
     * To set the notification value, we need to tell the sensor to enables us this notification mode.
     * We will write to the characteristic’s descriptor to set the right value: Notify or Indicate.
     */
    private final View.OnClickListener notifyTemperatureButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "notifyTemperatureButton() clicked");
            mService.notifyOnCharacteristicChanged(CharacteristicTypes.TEMPERATURE);
            startAnimation(mBinding.lottieViewTemperature, ANIMATION_TEMPERATURE, true);
        }
    };

    /**
     * Click listener for toggle LED button
     */
    private final View.OnClickListener toggleLedButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "toggleLedButton() clicked with state: "+mIsLedButtonClicked);
            byte ledStatus;
            if (mIsLedButtonClicked) {
                ledStatus = 0;
                mIsLedButtonClicked = false;
            } else {
                ledStatus = 1;
                mIsLedButtonClicked = true;
            }
            mService.writeToLedCharacteristic(ledStatus);
        }
    };

    /**
     * Initializing 'ScanSettings' parameter for 'BLE device Scanning' via Builder Pattern
     */
    private final ScanSettings bluetoothLeScanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build();

    /**
     * Initializing 'ScanCallback' parameter for 'BLE device Scanning'
     *
     * NOTE: onScanResult is triggered whenever a BLE device, matching the
     *       ScanFilter and ScanSettings is found.
     *       In this callback, we get access to the BluetoothDevice and RSSI
     *       objects through the ScanResult
     */
    private final ScanCallback bluetoothLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();

            // Append device to Scanned devices list
            if (bluetoothDevice.getName() != null) {
                if (!mBleDeviceList.contains(bluetoothDevice)) {
                    Log.d(TAG, "onScanResult: Adding "+bluetoothDevice.getName()+" to list");
                    mBleDeviceList.add(bluetoothDevice);

                    changeVisibility(mRecyclerView, View.VISIBLE);
                    changeVisibility(mScanningLottieView, View.GONE);

                    mRvAdapter.setDeviceList(mBleDeviceList);
                    mRvAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed() called with: errorCode = [" + errorCode + "]");
        }
    };

    /**
     * Prepare Custom Alert-Dialog
     */
    private void prepareAlertDialog() {
        // Inflate custom layout
        mCustomAlertView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_device_scan, null, false);

        mRecyclerView = mCustomAlertView.findViewById(R.id.rvScannedDevices);
        mScanningLottieView = mCustomAlertView.findViewById(R.id.lottieViewScanning);

        displayDataInRecyclerView(mRecyclerView);
    }

    /**
     * Prepare RecyclerView adapter
     *
     * @param recyclerView - to display Ble devices matching the scan filters and parameters
     */
    private void displayDataInRecyclerView(RecyclerView recyclerView) {
        Log.d(TAG, "displayDataInRecyclerView() called ");
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        // Set-up adapter
        mRvAdapter = new BleDeviceAdapter(this, mBleDeviceList,
                new BleDeviceAdapter.DeviceItemClickListener() {
                    @Override
                    public void onDeviceClicked(int position) {
                        if (mCurrentState == ConnectionStates.CONNECTED) {
                            // Disconnect
                            Log.d(TAG, "Disconnecting from Device: "+mBleDeviceList.get(position).getName());

                            disconnectFromDevice();

                            mCurrentState = ConnectionStates.DISCONNECTING;
                            updateAdapterConnectionState(position);
                        } else if (mCurrentState == ConnectionStates.DISCONNECTED) {
                            // Connect
                            Log.d(TAG, "Connecting to Device: "+mBleDeviceList.get(position).getName());

                            connectToDevice(mBleDeviceList.get(position).getAddress());

                            mCurrentState = ConnectionStates.CONNECTING;
                            updateAdapterConnectionState(position);
                        }
                    }
                });

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mRvAdapter);
    }

    /**
     * Play lottie-animations within alert-dialog
     *
     * @param animationName - animation to be played
     */
    private void playDialogAnimation(String animationName) {
        mScanningLottieView.setAnimation(animationName);
        mScanningLottieView.playAnimation();
    }

    /**
     * Update Current connection state to Adapter
     */
    private void updateAdapterConnectionState(int position) {
        mRvAdapter.setCurrentDeviceState(mCurrentState, position);
        mRvAdapter.notifyDataSetChanged();
    }

    /**
     * Prepare Intent Filters for GATT Update Status
     *
     * @return intentFilter
     */
    private IntentFilter gattIntentFilters() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleConnectivityService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleConnectivityService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}