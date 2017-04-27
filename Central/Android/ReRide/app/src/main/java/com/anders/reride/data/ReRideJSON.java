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
                             "sensorId": { "type" : "string" },
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
                     "sensorId": "flex sensor",
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

    private static ReRideJSON mReRideJSON;

    private ReRideJSON(String id) {
        mCurrentIndex = 0;
        mState = new JSONObject();
        mRecorded = new JSONObject();
        mRiderProperties = new JSONObject();
        mSensors = new JSONArray();
        mSensorIndex = new HashMap<>();
        try {
            mRiderProperties.put("sensors", mSensors);
            mRiderProperties.put("id", id);
            mRecorded.put("reported", mRiderProperties);
            mState.put("state", mRecorded);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean addSensor(String sensorId, String unit) {
        try {
            JSONObject sensor = new JSONObject();
            sensor.put("sensorId", sensorId);
            sensor.put("unit", unit);
            mSensorIndex.put(sensorId, mCurrentIndex);
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
            mRiderProperties.put("time", time);
            mRiderProperties.put("longitude", lon);
            mRiderProperties.put("latitude", lat);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean putSensorValue(String sensorId, String value) {
        try {
            mSensors.getJSONObject(mSensorIndex.get(sensorId)).put("value", value);
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
}
