package com.android.ranit.smartthermostat.contract;

import android.widget.Button;

import com.airbnb.lottie.LottieAnimationView;

public interface MainActivityContract {
    // View
    interface View {
        void initializeUi();
        void startAnimation(LottieAnimationView animationView, String animationName, boolean loop);
        void displaySnackBar(String message);
        void changeVisibility(android.view.View view, int visibility);
        void switchButton(Button button, String text);

        void onConnectButtonClicked();
        void onDisconnectButtonClicked();
        void launchDeviceScanDialog();

        void startScanning();
        void stopScanning();

        void requestPermissions();
        boolean checkPermissionsAtRuntime();
        boolean checkBluetoothStatus();
    }
}
