package com.android.ranit.smartthermostat.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Created by: Ranit Raj Ganguly on 22/06/2021
 */
public class BleConnectivityService extends Service {
    private static final String TAG = BleConnectivityService.class.getSimpleName();

    // Intent Filter actions for Broadcast-Receiver
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "smart.thermostat.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "smart.thermostat.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "smart.thermostat.EXTRA_DATA";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private String mBluetoothDeviceAddress;

    /**
     * Implementing GATT callback events
     */
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange() called ");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt.discoverServices();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered() called");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead() called");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite() called");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged() called");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead() called");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite() called");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called");
        closeGattServer();
        return super.onUnbind(intent);
    }

    /**
     * Initialize reference to the Bluetooth Adapter (from Bluetooth Manager)
     *
     * @return boolean - Adapter initialization status
     */
    public boolean initializeBluetoothAdapter() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager == null) {
                Log.e(TAG, "initializeBluetoothAdapter: Bluetooth Manager couldn't be initialized");
                return false;
            }

            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "initializeBluetoothAdapter: Bluetooth Adapter couldn't be initialized");
                return false;
            }
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address - The device address of the destination device.
     * @return boolean - true if the connection is initiated successfully.
     */
    public boolean connectToBleDevice(String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.e(TAG, "connectToBleDevice: BluetoothAdapter not initialized or unspecified address");
            return false;
        }

        // Reconnecting to previously connected device
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "connectToBleDevice: Reconnecting to device");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "connectToBleDevice: Device not found");
            return false;
        }

        Log.d(TAG, "connectToBleDevice: Initiating Connection with device: "+device.getName());
        mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);

        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects from the existing connection or cancels a pending connection
     */
    public void disconnectFromBleDevice() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "disconnectFromBleDevice: BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * Called upon un-bind of Service to ensure proper release of resources
     */
    private void closeGattServer() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
