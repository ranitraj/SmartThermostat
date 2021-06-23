package com.android.ranit.smartthermostat.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.ranit.smartthermostat.common.CharacteristicTypes;
import com.android.ranit.smartthermostat.common.GattAttributes;

import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;

/**
 * Created by: Ranit Raj Ganguly on 22/06/2021
 */
public class BleConnectivityService extends Service {
    private static final String TAG = BleConnectivityService.class.getSimpleName();

    // Intent Filter actions for Broadcast-Receiver
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "smart.thermostat.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "smart.thermostat.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "smart.thermostat.EXTRA_DATA";

    // Services
    private BluetoothGattService mEnvironmentSensingService;

    // Characteristics
    private BluetoothGattCharacteristic mTemperatureCharacteristic;

    // Descriptors
    private BluetoothGattDescriptor mTemperatureDescriptor;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private String mBluetoothDeviceAddress;
    private CharacteristicTypes mCurrentCharacteristicType = CharacteristicTypes.EMPTY_CHARACTERISTIC;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleConnectivityService getService() {
            return BleConnectivityService.this;
        }
    }

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
            } else {
                Log.e(TAG, "onConnectionStateChange(): GATT_FAILURE");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered() called");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                setupTemperatureCharacteristic(gatt);

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.e(TAG, "onServicesDiscovered(): GATT_FAILURE");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead() called");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, mCurrentCharacteristicType);
            } else {
                Log.e(TAG, "onCharacteristicRead(): GATT_FAILURE");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite() called");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mTemperatureCharacteristic.setValue(new byte[]{1, 1});
                gatt.writeCharacteristic(mTemperatureCharacteristic);
            } else {
                Log.e(TAG, "onDescriptorWrite(): GATT_FAILURE");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged() called ");

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, CharacteristicTypes.TEMPERATURE);
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
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called");
        closeGattServer();
        return super.onUnbind(intent);
    }

    /**
     * Broadcast to update UI from Service based on GATT callbacks
     *
     * @param action - Type of Action (for Intent-Filter)
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Broadcast to update UI and send values from characteristics of Services
     * based on GATT callbacks.
     *
     * @param action - Type of Action (for Intent-Filter)
     * @param characteristic - characteristic read from Service
     * @param characteristicToBeRead - facilitates parsing based on type of characteristic
     */
     private void broadcastUpdate(final String action, BluetoothGattCharacteristic characteristic,
                                 CharacteristicTypes characteristicToBeRead) {
        final Intent intent = new Intent(action);

        // Filter and broadcast Data based on 'CharacteristicType'
        if (characteristicToBeRead.equals(CharacteristicTypes.TEMPERATURE)) {
            float temperature =  (float) (characteristic.getIntValue(FORMAT_UINT16, 0) / 100);
            Log.d(TAG, "broadcastUpdate: Received Temperature: "+temperature);
            intent.putExtra(EXTRA_DATA, String.valueOf(temperature));
        }

         sendBroadcast(intent);
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
     * Read the characteristic
     *
     * @param characteristic - bluetoothGattCharacteristic which is to be read
     */
    private void readCharacteristicFromService(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "readCharacteristicFromService: BluetoothAdapter not initialized");
            return;
        }

        Log.d(TAG, "readBleCharacteristic() called");
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Write to characteristic with descriptor
     *
     * @param descriptor - bluetoothGattCharacteristic to be written to
     */
    private void writeToCharacteristicFromService(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "writeToCharacteristicFromService: BluetoothAdapter not initialized");
            return;
        }

        Log.d(TAG, "writeToCharacteristicFromService() called");
        mBluetoothGatt.writeDescriptor(descriptor);
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
     * Called from activity to specify characteristic to be read on
     * 'onServiceDiscovered()' depending upon the type
     *
     * @param type - CharacteristicType to be read
     */
    public void readCharacteristicValue(CharacteristicTypes type) {
        // Update global-variable
        mCurrentCharacteristicType = type;

        // Read appropriate Characteristics
        if (mCurrentCharacteristicType.equals(CharacteristicTypes.TEMPERATURE)) {
            readCharacteristicFromService(mTemperatureCharacteristic);
        }
    }

    /**
     * Called from activity to specify characteristic to be notified to in
     * 'onServiceDiscovered()' depending upon the type
     *
     * @param type - CharacteristicType to be read
     */
    public void notifyOnCharacteristicChanged(CharacteristicTypes type) {
        // Update global-variable
        mCurrentCharacteristicType = type;

        // Write to appropriate Characteristic
        if (mCurrentCharacteristicType.equals(CharacteristicTypes.TEMPERATURE)) {
            writeToCharacteristicFromService(mTemperatureDescriptor);
        }
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

    /**
     * Sets up all properties related to the Temperature characteristic in
     * the Environment Sensing Service.
     *
     * 1. Get the 'Temperature' characteristic from 'Environment Sensing' service
     * 2. Enable Notification for 'Temperature' characteristic (for 'Notify' only)
     *      a. Get the descriptor property (UUID) from the 'characteristic'
     *      b. Write 'notify' to the characteristics 'descriptor' value using the ENABLE_NOTIFICATION_VALUE
     *
     * @param gatt - Gatt from the onServiceDiscovered callback
     */
    private void setupTemperatureCharacteristic(BluetoothGatt gatt) {
        Log.d(TAG, "setupTemperatureCharacteristic() called ");
        mEnvironmentSensingService = gatt
                .getService(UUID.fromString(GattAttributes.ENVIRONMENTAL_SENSING_SERVICE_UUID));
        mTemperatureCharacteristic = mEnvironmentSensingService
                .getCharacteristic(UUID.fromString(GattAttributes.TEMPERATURE_CHARACTERISTIC_UUID));

        gatt.setCharacteristicNotification(mTemperatureCharacteristic, true);
        mTemperatureDescriptor = mTemperatureCharacteristic
                .getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        mTemperatureDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }
}