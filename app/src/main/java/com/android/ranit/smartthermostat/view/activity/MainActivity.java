package com.android.ranit.smartthermostat.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.airbnb.lottie.LottieAnimationView;
import com.android.ranit.smartthermostat.R;
import com.android.ranit.smartthermostat.contract.MainActivityContract;
import com.android.ranit.smartthermostat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity
        implements MainActivityContract.View {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String ANIMATION_TEMPERATURE = "temperature.json";
    private static final String ANIMATION_LED_ON = "on.json";
    private static final String ANIMATION_LED_OFF = "off.json";

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        initializeUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    }

    @Override
    public void startAnimation(LottieAnimationView animationView, String animationName, boolean loop) {
        animationView.setAnimation(animationName);
        animationView.loop(loop);
        animationView.playAnimation();
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
    public void requestPermissions() {
        Log.d(TAG, "requestPermissions() called");
    }

    @Override
    public boolean checkPermissionsAtRuntime() {
        Log.d(TAG, "checkPermissionsAtRuntime() called");
        return false;
    }

    @Override
    public boolean checkBluetoothStatus() {
        Log.d(TAG, "checkBluetoothStatus() called");
        return false;
    }

    /**
     * Click listener for Connect button
     * Launches alert dialog which facilitates scanning and connection to BLE device.
     */
    private final View.OnClickListener connectButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // TODO: Launch alert dialog
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
}