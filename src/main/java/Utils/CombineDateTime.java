package Utils;

import java.util.Calendar;
import java.util.Date;

public class CombineDateTime {
    /**
     * Combines a date-only value (yyyy-MM-dd) with a time-only value (HH:mm:ss)
     * into a full java.util.Date with both date and time preserved.
     *
     * @param dateOnly the date part (usually from a spinner or calendar)
     * @param timeOnly the time part (usually from a time picker)
     * @return combined Date instance with full datetime
     */

    public static Date mergeDateAndTime(Date dateOnly, Date timeOnly){
//        Date datePart = (Date) dateWorkedSpinner.getValue();      // Gives yyyy-MM-dd
//        Date timePart = (Date) startTimeSpinner.getValue();       // Gives HH:mm:ss

        Calendar calDate = Calendar.getInstance();
        calDate.setTime(dateOnly);

        Calendar calTime = Calendar.getInstance();
        calTime.setTime(timeOnly);

// Apply time to date
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE,      calTime.get(Calendar.MINUTE));
        calDate.set(Calendar.SECOND,      calTime.get(Calendar.SECOND));
        calDate.set(Calendar.MILLISECOND, calTime.get(Calendar.MILLISECOND));

// ✅ Combined datetime
        return calDate.getTime();

    }

    /**
     * Calculates the difference between two Date objects in minutes.
     *
     * @param startDate the start timestamp
     * @param endDate   the end timestamp
     * @return elapsed time in minutes (fractional)
     */
    public static double calcDuration(Date startDate, Date endDate) {
        long diffMillis = endDate.getTime() - startDate.getTime();
        return diffMillis / (1000.0 * 60.0);
    }

}
