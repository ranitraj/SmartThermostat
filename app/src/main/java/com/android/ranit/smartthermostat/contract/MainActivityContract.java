package com.android.ranit.smartthermostat.contract;

import com.airbnb.lottie.LottieAnimationView;

public interface MainActivityContract {
    // View
    interface View {
        void initializeUi();
        void startAnimation(LottieAnimationView animationView, String animationName, boolean loop);

        void onConnectButtonClicked();
        void onDisconnectButtonClicked();

        void requestPermissions();
        boolean checkPermissionsAtRuntime();
        boolean checkBluetoothStatus();
    }
}