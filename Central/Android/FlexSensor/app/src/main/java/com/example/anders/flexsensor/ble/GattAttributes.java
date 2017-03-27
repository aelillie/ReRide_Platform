package com.example.anders.flexsensor.ble;

import java.util.HashMap;

/**
 * Sample GATT attributes
 */

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap<>();
    static String BATTERY_LEVEL = "0x2A19";

    static {
        attributes.put("0x180F", "Battery Service");
        attributes.put(BATTERY_LEVEL, "Battery Level");
    }

    static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
