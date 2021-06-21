package com.android.ranit.smartthermostat.model;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Created by: Ranit Raj Ganguly on 21/06/2021
 *
 * Singleton Class used to cache and access non-persistent data throughout
 * the application lifecycle.
 */
public class DataManager {
    private static final String TAG = DataManager.class.getSimpleName();

    // Private instance variable
    private static DataManager INSTANCE;

    private MutableLiveData<BluetoothDevice> mBleDeviceMutableLiveData = new MutableLiveData<>();

    // Private Constructor
    private DataManager() {}

    // Public method to get instance of Singleton class
    public DataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DataManager();
        }
        return INSTANCE;
    }

    /**
     * Updates the Mutable-Live-Data based upon the current Connection state
     * received from ACL_Broadcast_Receiver
     *
     * @param bluetoothDevice - Bluetooth device object currently connected/disconnected
     */
    public void setBleDeviceLiveData(BluetoothDevice bluetoothDevice) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Main-Thread
            mBleDeviceMutableLiveData.setValue(bluetoothDevice);
        } else {
            // Background-Thread
            mBleDeviceMutableLiveData.postValue(bluetoothDevice);
        }
    }

    /**
     * Retrieves the Live-Data in order to be observed and perform necessary
     * UI operations accordingly from View (MainActivity)
     *
     * @return mBleDeviceMutableLiveData - live data
     */
    public LiveData<BluetoothDevice> getBleDeviceLiveData() {
        return mBleDeviceMutableLiveData;
    }

}
