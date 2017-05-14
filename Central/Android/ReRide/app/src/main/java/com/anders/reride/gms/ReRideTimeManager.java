package com.anders.reride.gms;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Get current time information
 */

public class ReRideTimeManager {

    public static final String TIMEZONE =
            String.valueOf(
                    TimeZone.getDefault().getOffset(getNow().getTimeInMillis())/1000/60/60
            );

    /**
     * Get current date and time
     * @return Truncated datetime string without spaces in the format (YYYYMMDDHHMMSS)
     */
    public static String now() {
        Calendar now = getNow();
        return getDateString(now)+getTimeString(now);
    }

    /**
     * Get current time
     * @return Truncated time string without spaces in the format (HHMMSS)
     */
    private static String getTimeString(Calendar now) {
        return format(now.get(Calendar.HOUR_OF_DAY))
                + format(now.get(Calendar.MINUTE))
                + format(now.get(Calendar.SECOND));
    }

    /**
     * Get current date
     * @return Truncated date string without spaces in the format (YYYYMMDD)
     */
    private static String getDateString(Calendar now) {
        return now.get(Calendar.YEAR)
                + format(now.get(Calendar.MONTH)+1)
                + format(now.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar getNow() {
        return Calendar.getInstance(TimeZone.getDefault()); //Like GMT+2
    }

    /**
     * Prefix one-digit integers with a zero
     * @param time Specific time value
     * @return Two-digit format
     */
    private static String format(int time) {
        return time < 10 ? "0"+time : ""+time;
    }

}
