package com.anders.reride.aws;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                             "value": { "type" : "string" },
                             "unit": { "type" : "string" }
                         },
                        "required": ["name", "value", "unit"]
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
             "time": "12:35:00",
             "sensors": [
                 {
                     "name": "flex sensor",
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

class ReRideJSON {
    private JSONObject mState;
    private JSONObject mRecorded;
    private JSONObject mRiderProperties;
    private JSONArray mSensors;
    private static ReRideJSON mReRideJSON;

    private ReRideJSON() {
        mState = new JSONObject();
        mRecorded = new JSONObject();
        mRiderProperties = new JSONObject();
        mSensors = new JSONArray();
        try {
            mRiderProperties.put("sensors", mSensors);
            mRecorded.put("reported", mRiderProperties);
            mState.put("state", mRecorded);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Singleton getter
     * @return Singleton reference for this builder class
     */
    public static ReRideJSON getInstance() {
        if (mReRideJSON == null) {
            mReRideJSON = new ReRideJSON();
        }
        return mReRideJSON;
    }

    public boolean putRiderProperties(String id, String time, String lon, String lat) {
        try {
            mRiderProperties.put("id", id);
            mRiderProperties.put("time", time);
            mRiderProperties.put("longitude", lon);
            mRiderProperties.put("latitude", lat);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean putSensorProperties(String name, String value, String unit) {
        try {
            JSONObject sensorProperties = new JSONObject();
            sensorProperties.put("name", name);
            sensorProperties.put("value", value);
            sensorProperties.put("unit", unit);
            mSensors.put(sensorProperties);
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
