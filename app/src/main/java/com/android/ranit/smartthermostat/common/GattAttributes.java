package com.android.ranit.smartthermostat.common;

import java.util.HashMap;

/**
 * Created by: Ranit Raj Ganguly on 22/06/2021
 */
public class GattAttributes {
    // Temperature
    public static String ENVIRONMENTAL_SENSING_SERVICE_UUID = "0000181a-0000-1000-8000-00805f9b34fb";
    public static String TEMPERATURE_CHARACTERISTIC_UUID = "00002a6e-0000-1000-8000-00805f9b34fb";

    // LED
    public static String LED_SERVICE_UUID = "277eaf48-6698-4da9-8329-335d05343490";
    public static String LED_CHARACTERISTIC_UUID = "277eaf49-6698-4da9-8329-335d05343490";

    // Client Characteristic
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static final HashMap<String, String> gattAttributes = new HashMap();

    static {
        // Services
        gattAttributes.put(ENVIRONMENTAL_SENSING_SERVICE_UUID, "Environmental Sensing Service");
        gattAttributes.put(LED_SERVICE_UUID, "LED Service");

        // Characteristics
        gattAttributes.put(TEMPERATURE_CHARACTERISTIC_UUID, "Temperature Measurement");
        gattAttributes.put(LED_CHARACTERISTIC_UUID, "LED Characteristic");
    }
}
