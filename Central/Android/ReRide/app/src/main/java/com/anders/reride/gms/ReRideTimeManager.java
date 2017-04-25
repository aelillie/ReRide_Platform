package com.anders.reride.gms;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by Anders on 25-Apr-17.
 */

public class ReRideTimeManager {
    public static String getTimeString(String gmtTimeZone) { //TODO: Add date!!!
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone(gmtTimeZone)); //Like GMT+2
        return now.get(Calendar.HOUR_OF_DAY) + ""
                + now.get(Calendar.MINUTE) + ""
                + now.get(Calendar.SECOND);
    }
}
