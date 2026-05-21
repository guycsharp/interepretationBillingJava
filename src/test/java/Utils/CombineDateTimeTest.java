package Utils;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

public class CombineDateTimeTest {

    // ---------------------------------------------------------
    // mergeDateAndTime
    // ---------------------------------------------------------
    @Test
    void testMergeDateAndTime() {
        Calendar dateCal = Calendar.getInstance();
        dateCal.set(2024, Calendar.MARCH, 10, 0, 0, 0);

        Calendar timeCal = Calendar.getInstance();
        timeCal.set(1970, Calendar.JANUARY, 1, 14, 30, 0);

        java.util.Date result = CombineDateTime.mergeDateAndTime(
                dateCal.getTime(),
                timeCal.getTime()
        );

        Calendar merged = Calendar.getInstance();
        merged.setTime(result);

        assertEquals(2024, merged.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, merged.get(Calendar.MONTH));
        assertEquals(10, merged.get(Calendar.DAY_OF_MONTH));
        assertEquals(14, merged.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, merged.get(Calendar.MINUTE));
        assertEquals(0, merged.get(Calendar.SECOND));
    }

    // ---------------------------------------------------------
    // calcDuration(Date, Date)
    // ---------------------------------------------------------
    @Test
    void testCalcDuration_Date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1, 10, 0, 0);
        java.util.Date start = cal.getTime();

        cal.set(2024, Calendar.JANUARY, 1, 11, 15, 0);
        java.util.Date end = cal.getTime();

        double mins = CombineDateTime.calcDuration(start, end);
        assertEquals(75.0, mins, 0.001);
    }

    // ---------------------------------------------------------
    // calcDuration(Timestamp, Timestamp, String)
    // ---------------------------------------------------------
    @Test
    void testCalcDuration_Timestamp_NoOverride() {
        Timestamp start = Timestamp.valueOf("2024-01-01 10:00:00");
        Timestamp end   = Timestamp.valueOf("2024-01-01 11:00:00");

        double mins = CombineDateTime.calcDuration(start, end, "");
        assertEquals(60.0, mins, 0.001);
    }

    @Test
    void testCalcDuration_Timestamp_WithOverride() {
        Timestamp start = Timestamp.valueOf("2024-01-01 10:00:00");
        Timestamp end   = Timestamp.valueOf("2024-01-01 11:00:00");

        double mins = CombineDateTime.calcDuration(start, end, "42.5");
        assertEquals(42.5, mins, 0.001);
    }

    // ---------------------------------------------------------
    // getDayOfWeek
    // ---------------------------------------------------------
    @Test
    void testGetDayOfWeek() {
        Date d = Date.valueOf("2024-03-10"); // This is a Sunday
        String day = CombineDateTime.getDayOfWeek(d);
        assertEquals("Sunday", day);
    }

    // ---------------------------------------------------------
    // DateFormatter(Date, pattern)
    // ---------------------------------------------------------
    @Test
    void testDateFormatter_Date() {
        Date d = Date.valueOf("2024-03-10");
        String formatted = CombineDateTime.DateFormatter(d, "dd-MM-yyyy");
        assertEquals("10-03-2024", formatted);
    }

    // ---------------------------------------------------------
    // DateFormatter(pattern, Timestamp)
    // ---------------------------------------------------------
    @Test
    void testDateFormatter_Timestamp() {
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2024, 3, 10, 14, 0));
        String formatted = CombineDateTime.DateFormatter("yyyy-MMM-dd HH", ts);
        assertEquals("2024-Mar-10 14", formatted);
    }
}
