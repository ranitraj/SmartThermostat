package com.android.ranit.smartthermostat.broadcastReceiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by: Ranit Raj Ganguly on 21/06/2021
 *
 * Broadcast Receiver used to receive broadcasts and update LiveData
 * for different bluetooth connection states.
 *
 * NOTE: ACL events are triggered for both Bluetooth-Classic and LE type devices.
 * Therefore, for the scope of this project, we filter LE devices type.
 */
public class AclBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = AclBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice bluetoothDevice;
        String action = intent.getAction();

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                Log.d(TAG, "onReceive() called with: ACTION_ACL_CONNECTED");

            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                Log.e(TAG, "onReceive() called with: ACTION_ACL_DISCONNECTED");

            }
        }
    }
}
