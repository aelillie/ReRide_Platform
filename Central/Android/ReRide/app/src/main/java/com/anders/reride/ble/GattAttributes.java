package com.anders.reride.ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * Sample GATT attributes
 */

class GattAttributes {
    private static final String baseBluetoothUuidPostfix = "0000-1000-8000-00805f9b34fb";
    private static HashMap<String, String> attributes = new HashMap<>();
    //Services
    static String ENVIRONMENTAL_SENSING = uuidSStringFromUInt16("181a");
    static String BATTERY_SERVICE = uuidSStringFromUInt16("180f");
    static String USER_DATA = uuidSStringFromUInt16("181c");

    //Characteristics
    static String BATTERY_LEVEL = uuidSStringFromUInt16("2a19");
    static String APPARENT_WIND_DIRECTION = uuidSStringFromUInt16("2a73");
    static String AGE = uuidSStringFromUInt16("2a80");
    static String WEIGHT = uuidSStringFromUInt16("2a98");

    //Descriptors
    static String CLIENT_CHARACTERISTIC_CONFIGURATION = uuidSStringFromUInt16("2902");

    static {
        //Services
        attributes.put(ENVIRONMENTAL_SENSING, "Environmental Sensing");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(USER_DATA, "User Data");

        //Characteristics
        attributes.put(BATTERY_LEVEL, "Battery Level");
        attributes.put(APPARENT_WIND_DIRECTION, "Apparent Wind Direction");
        attributes.put(AGE, "Age");
        attributes.put(WEIGHT, "Weight");

        //Descriptors
        attributes.put(CLIENT_CHARACTERISTIC_CONFIGURATION, "Client Characteristic Configuration");
    }

    static String lookup(String uuid, String defaultName) {

        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    private static String uuidSStringFromUInt16(String shortCode16) {
        return "0000" + shortCode16 + "-" + baseBluetoothUuidPostfix;
    }

    static boolean hasAttribute(String uuid) {
        return attributes.containsKey(uuid);
    }
}
