package com.anders.reride.ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * Sample GATT attributes
 */

class GattAttributes {
    private static final String baseBluetoothUuidPostfix = "0000-1000-8000-00805f9b34fb";
    private static HashMap<String, String> attributes = new HashMap<>();
    private static HashMap<String, String> units = new HashMap<>();
    //Services
    private static String ENVIRONMENTAL_SENSING = uuidSStringFromUInt16("181a");
    private static String BATTERY_SERVICE = uuidSStringFromUInt16("180f");
    private static String USER_DATA = uuidSStringFromUInt16("181c");
    private static String HEART_RATE_SERVICE = uuidSStringFromUInt16("180d");

    //Characteristics
    static String BATTERY_LEVEL = uuidSStringFromUInt16("2a19");
    static String APPARENT_WIND_DIRECTION = uuidSStringFromUInt16("2a73");
    static String AGE = uuidSStringFromUInt16("2a80");
    static String WEIGHT = uuidSStringFromUInt16("2a98");
    static String HEART_RATE_MEASUREMENT = uuidSStringFromUInt16("2a37");

    //Descriptors
    static String CLIENT_CHARACTERISTIC_CONFIGURATION = uuidSStringFromUInt16("2902");

    static {
        //Services
        attributes.put(ENVIRONMENTAL_SENSING, "Environmental Sensing");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(USER_DATA, "User Data");
        attributes.put(HEART_RATE_SERVICE, "Heart Rate Service");

        //Characteristics
        attributes.put(BATTERY_LEVEL, "Battery Level");
        units.put(BATTERY_LEVEL, "percentage");
        attributes.put(APPARENT_WIND_DIRECTION, "Apparent Wind Direction");
        units.put(APPARENT_WIND_DIRECTION, "degrees");
        attributes.put(AGE, "Age");
        units.put(AGE, "years");
        attributes.put(WEIGHT, "Weight");
        units.put(WEIGHT, "kilograms");
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        units.put(HEART_RATE_MEASUREMENT, "bpm");

        //Descriptors
        attributes.put(CLIENT_CHARACTERISTIC_CONFIGURATION, "Client Characteristic Configuration");
    }

    static String lookup(String uuid, String defaultName) {

        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    static String lookupUnit(String uuid, String defaultUnit) {
        String unit = units.get(uuid);
        return unit == null ? defaultUnit : unit;
    }

    private static String uuidSStringFromUInt16(String shortCode16) {
        return "0000" + shortCode16 + "-" + baseBluetoothUuidPostfix;
    }

    static boolean hasAttribute(String uuid) {
        return attributes.containsKey(uuid);
    }

    static String shortUuidString(UUID uuid) {
        return shortUuidString(uuid.toString());
    }

    static String shortUuidString(String uuid) {
        return uuid.substring(4, 8);
    }
}
