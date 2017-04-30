package com.anders.reride.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for creating ReRide data JSON representations
 *
 * JSON schema:
 * {
    "title": "State",
    "type": "object",
    "properties": {
        "recorded": {
            "type": "object",
            "properties": {
                 "id": { "type" : "string" },
                 "time": { "type" : "string" },
                 "longitude": { "type" : "string" },
                 "latitude": { "type" : "string" },
                 "sensors":  {
                     "type": "array",
                     "items": {
                         "type": "object",
                         "properties": {
                             "name": { "type" : "string" },
                             "characteristic": { "type" : "string" },
                             "value": { "type" : "string" },
                             "unit": { "type" : "string" }
                         },
                        "required": ["sensorId", "value", "unit"]
                    },
                    "minItems": 1,
                    "uniqueItems": true
                 }
            },
            "required": ["id", "time", "sensors"]
        }
    }
 }

 example:
 {
     "state": {
         "recorded": {
             "id": "10",
             "time": "20170426123500",
             "sensors": [
                 {
                     "name": "flex sensor",
                     "characteristic": "Apparent Wind Direction",
                     "value": "45",
                     "unit": "degrees"
                 }
             ],
             "longitude": "12.324534",
             "latitude": "55.123124"
         }
     }
 }
 */

public class ReRideJSON {
    private JSONObject mState;
    private JSONObject mRecorded;
    private JSONObject mRiderProperties;
    private JSONArray mSensors;
    private Map<String, Integer> mSensorIndex;
    private int mCurrentIndex;

    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";
    static final String TIME = "time";
    static final String SENSORS = "sensors";
    static final String SENSOR_NAME = "name";
    static final String CHARACTERISTIC = "characteristic";
    static final String SENSOR_UNIT = "unit";
    static final String ID = "id";
    static final String RECORDED = "recorded";
    static final String STATE = "state";
    static final String VALUE = "value";

    private static ReRideJSON mReRideJSON;

    private ReRideJSON(String id) {
        mCurrentIndex = 0;
        mState = new JSONObject();
        mRecorded = new JSONObject();
        mRiderProperties = new JSONObject();
        mSensors = new JSONArray();
        mSensorIndex = new HashMap<>();
        try {
            mRiderProperties.put(SENSORS, mSensors);
            mRiderProperties.put(ID, id);
            mRecorded.put(RECORDED, mRiderProperties);
            mState.put(STATE, mRecorded);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean addSensor(String sensorName, String unit) {
        try {
            JSONObject sensor = new JSONObject();
            sensor.put(SENSOR_NAME, sensorName);
            sensor.put(SENSOR_UNIT, unit);
            mSensorIndex.put(sensorName, mCurrentIndex);
            mSensors.put(mCurrentIndex, sensor);
            mCurrentIndex++;
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Singleton getter
     * @return Singleton reference for this builder class
     */
    public static ReRideJSON getInstance(String id) {
        if (mReRideJSON == null) {
            mReRideJSON = new ReRideJSON(id);
        }
        return mReRideJSON;
    }

    public boolean putRiderProperties(String time, double lon, double lat) {
            try {
            mRiderProperties.put(TIME, time);
            mRiderProperties.put(LONGITUDE, lon);
            mRiderProperties.put(LATITUDE, lat);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean putSensorValue(String sensorName, String value, String characteristicUuid) {
        try {
            JSONObject sensor = mSensors.getJSONObject(mSensorIndex.get(sensorName));
            sensor.put(VALUE, value);
            sensor.put(CHARACTERISTIC, characteristicUuid);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONObject getState() {
        return mState;
    }

    public JSONObject getRiderProperties() {
        return mRiderProperties;
    }

    public void removeSensor(String sensorName) {
        mSensors.remove(mSensorIndex.get(sensorName));
    }

    public void clear() {
        for (String sensorName : mSensorIndex.keySet()) {
            removeSensor(sensorName);
        }
        mState.remove(STATE);
        mRecorded.remove(RECORDED);
        mRiderProperties.remove(ID);
        mRiderProperties.remove(SENSORS);
        mRiderProperties.remove(TIME);
        mRiderProperties.remove(LONGITUDE);
        mRiderProperties.remove(LATITUDE);
    }
}
