package com.example.anders.flexsensor.ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * Sample GATT attributes
 */

class GattAttributes {
    private static final String baseBluetoothUuidPostfix = "0000-1000-8000-00805F9B34FB";
    private static HashMap<String, String> attributes = new HashMap<>();
    static String BATTERY_LEVEL = uuidSStringFromUInt16("2A19");

    static {
        attributes.put(uuidSStringFromUInt16("180F"), "Battery Service");
        attributes.put(BATTERY_LEVEL, "Battery Level");
    }

    static String lookup(String uuid, String defaultName) {

        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    private static String uuidSStringFromUInt16(String shortCode16) {
        return "0000" + shortCode16 + "-" + baseBluetoothUuidPostfix;
    }
}
