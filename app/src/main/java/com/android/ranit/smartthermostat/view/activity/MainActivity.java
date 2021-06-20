package com.android.ranit.smartthermostat.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.airbnb.lottie.LottieAnimationView;
import com.android.ranit.smartthermostat.R;
import com.android.ranit.smartthermostat.common.ConnectionStates;
import com.android.ranit.smartthermostat.common.Constants;
import com.android.ranit.smartthermostat.contract.MainActivityContract;
import com.android.ranit.smartthermostat.databinding.ActivityMainBinding;
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
    private static final String SCANNING = "scanning.json";

    private ConnectionStates mCurrentState = ConnectionStates.DISCONNECTED;

    private final String[] PERMISSIONS = {
            // Note: Only 'ACCESS_FINE_LOCATION' permission is needed from user at run-time
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private BluetoothAdapter mBluetoothAdapter;

    private ActivityMainBinding mBinding;
    private View mCustomAlertView;
    private RecyclerView mRecyclerView;
    private LottieAnimationView mScanningLottieView;
    private BleDeviceAdapter mRvAdapter;

    private final List<BluetoothDevice> mBleDeviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        initializeUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void initializeUi() {
        Log.d(TAG, "initializeUi() called");

        // Prepare initial animations
        startAnimation(mBinding.lottieViewTemperature, ANIMATION_TEMPERATURE, false);
        startAnimation(mBinding.lottieViewLight, ANIMATION_LED_OFF, false);

        onConnectButtonClicked();
    }

    @Override
    public void startAnimation(LottieAnimationView animationView, String animationName, boolean loop) {
        animationView.setAnimation(animationName);
        animationView.loop(loop);
        animationView.playAnimation();
    }

    @Override
    public void displaySnackBar(String message) {
        Snackbar.make(mBinding.layoutMain, message, Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void changeVisibility(View view, int visibility) {

    }

    @Override
    public void switchButton(Button button, String text) {

    }

    @Override
    public void onConnectButtonClicked() {
        Log.d(TAG, "onConnectButtonClicked() called");
        mBinding.btnStartScanning.setOnClickListener(connectButtonClickListener);
    }

    @Override
    public void onDisconnectButtonClicked() {
        Log.d(TAG, "onDisconnectButtonClicked() called");
        mBinding.btnStartScanning.setOnClickListener(disconnectButtonClickListener);
    }

    @Override
    public void launchAlertDialog() {
        Log.d(TAG, "launchAlertDialog() called");
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(mCustomAlertView)
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.dialog_positive_button, null)
                .setNegativeButton(R.string.dialog_negative_button, null)
                .setNeutralButton(R.string.dialog_neutral_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // TODO: Stop Scanning
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
                        // TODO: Start Scanning
                    }
                });

                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Stop button clicked");
                        // TODO: Stop Scanning
                    }
                });
            }
        });
        dialog.show();
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
            // Launch Custom Alert-Dialog
            prepareAlertDialog();

            // Start Scanning prior to launch
            // TODO: Start Scan
            launchAlertDialog();
        }
    };

    /**
     * Click listener for disconnect button
     * Initiates disconnection request to GATT server in order to disconnect from BLE device.
     */
    private final View.OnClickListener disconnectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // TODO: Disconnect from device & modify UI appropriately
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        // Set-up adapter
        mRvAdapter = new BleDeviceAdapter(this, mBleDeviceList,
                new BleDeviceAdapter.DeviceItemClickListener() {
                    @Override
                    public void onHrmDeviceClicked(int position) {
                        if (mCurrentState == ConnectionStates.CONNECTED) {
                            // Disconnect
                            Log.d(TAG, "Disconnecting from Device: "+mBleDeviceList.get(position).getName());

                            // TODO: Disconnect from BLE device (via service)

                            mCurrentState = ConnectionStates.DISCONNECTING;
                            updateAdapterConnectionState(position);
                        } else if (mCurrentState == ConnectionStates.DISCONNECTED) {
                            // Connect
                            Log.d(TAG, "Connecting to Device: "+mBleDeviceList.get(position).getName());

                            // TODO: Connect to BLE device (via service)

                            mCurrentState = ConnectionStates.CONNECTING;
                            updateAdapterConnectionState(position);
                        }
                    }
                });

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mRvAdapter);
    }

    /**
     * Update Current connection state to Adapter
     */
    private void updateAdapterConnectionState(int position) {
        mRvAdapter.setCurrentDeviceState(mCurrentState, position);
        mRvAdapter.notifyDataSetChanged();
    }
}