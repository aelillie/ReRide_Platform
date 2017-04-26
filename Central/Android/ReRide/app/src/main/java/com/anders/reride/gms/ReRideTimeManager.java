package com.anders.reride.gms;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Get current time information
 */

public class ReRideTimeManager {

    /**
     * Get current date and time
     * @param gmtTimeZone The current time zone (GMT)
     * @return Truncated datetime string without spaces in the format (YYYYMMDDHHMMSS)
     */
    public static String now(String gmtTimeZone) {
        return getDateString(gmtTimeZone)+getTimeString(gmtTimeZone);
    }

    /**
     * Get current time
     * @param gmtTimeZone The current time zone (GMT)
     * @return Truncated time string without spaces in the format (HHMMSS)
     */
    private static String getTimeString(String gmtTimeZone) {
        Calendar now = getNow(gmtTimeZone);
        return format(now.get(Calendar.HOUR_OF_DAY))
                + format(now.get(Calendar.MINUTE))
                + format(now.get(Calendar.SECOND));
    }

    /**
     * Get current date
     * @param gmtTimeZone The current time zone (GMT)
     * @return Truncated date string without spaces in the format (YYYYMMDD)
     */
    private static String getDateString(String gmtTimeZone) {
        Calendar now = getNow(gmtTimeZone);
        return now.get(Calendar.YEAR)
                + format(now.get(Calendar.MONTH)+1)
                + format(now.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar getNow(String gmtTimeZone) {
        return Calendar.getInstance(TimeZone.getTimeZone(gmtTimeZone)); //Like GMT+2
    }

    private static String format(int time) {
        return time < 10 ? "0"+time : ""+time;
    }

}
