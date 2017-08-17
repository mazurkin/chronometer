package org.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MockChronometerFrozenTest {

    private static final long NS_IN_MS = TimeUnit.MILLISECONDS.toNanos(1);

    private MockChronometer chronometer;

    @Before
    public void setUp() throws Exception {
        chronometer = new MockChronometer(MockChronometer.Mode.FROZEN);
    }

    @Test(expected = DateTimeParseException.class)
    public void testClockNoTimezone() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763");
    }

    @Test(expected = DateTimeParseException.class)
    public void testClockNoMillis() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30 UTC");
    }

    @Test
    public void testClock() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC");

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(chronometer.getTimeMs());

        Assert.assertEquals(2017, calendar.get(Calendar.YEAR));
        Assert.assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH));
        Assert.assertEquals(13, calendar.get(Calendar.DAY_OF_MONTH));

        Assert.assertEquals(2, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(12, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(30, calendar.get(Calendar.SECOND));
        
        Assert.assertEquals(763, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testShift() {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC");
        chronometer.shiftBy(0, 100);

        long markerNs = chronometer.getTickNs();

        chronometer.shiftTo("2017-03-13 02:12:32.363 UTC");
        chronometer.shiftBy(0, 400);

        long elapsedNs = chronometer.getElapsed(markerNs, TimeUnit.NANOSECONDS);
        Assert.assertEquals(1_600_000_300, elapsedNs);

        long elapsedMs = chronometer.getElapsed(markerNs, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1_600, elapsedMs);
    }

    @Test
    public void testInstant() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC");
        chronometer.shiftBy(123_345, TimeUnit.NANOSECONDS);

        ZonedDateTime v = chronometer.getZonedDateTime(ZoneId.of("UTC"));
        
        Assert.assertEquals(2017, v.get(ChronoField.YEAR));
        Assert.assertEquals(3, v.get(ChronoField.MONTH_OF_YEAR));
        Assert.assertEquals(13, v.get(ChronoField.DAY_OF_MONTH));

        Assert.assertEquals(2, v.get(ChronoField.HOUR_OF_DAY));
        Assert.assertEquals(12, v.get(ChronoField.MINUTE_OF_HOUR));
        Assert.assertEquals(30, v.get(ChronoField.SECOND_OF_MINUTE));
        
        Assert.assertEquals(763_123_345, v.get(ChronoField.NANO_OF_SECOND));
    }

    @Test
    public void testCorrectBy() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC");
        long time1 = chronometer.getTimeMs();
        long tick1 = chronometer.getTickNs();

        chronometer.correctTimeBy(100, 400);
        long time2 = chronometer.getTimeMs();
        long tick2 = chronometer.getTickNs();

        Assert.assertEquals(time2, time1 + 100);
        Assert.assertEquals(tick2, tick1);
    }

    @Test
    public void testCorrectTo() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC");
        long time1 = chronometer.getTimeMs();
        long tick1 = chronometer.getTickNs();

        chronometer.correctTimeTo(time1 + 100, 400);
        long time2 = chronometer.getTimeMs();
        long tick2 = chronometer.getTickNs();

        Assert.assertEquals(time2, time1 + 100);
        Assert.assertEquals(tick2, tick1);
    }

    @Test
    public void testElapsedOverflow1() throws Exception {
        // tick goes through Long.MAX_VALUE and Long.MIN_VALUE
        MockChronometer chronometer = MockChronometer.createFrozen(
                System.currentTimeMillis(), Long.MAX_VALUE - 1_000_000);

        long tickNs = chronometer.getTickNs();

        chronometer.shiftBy(2_000_000, TimeUnit.NANOSECONDS);

        long elapsedNs1 = chronometer.getElapsedNs(tickNs);
        Assert.assertEquals(2_000_000, elapsedNs1);

        long elapsedNs2 = chronometer.getElapsed(tickNs, TimeUnit.NANOSECONDS);
        Assert.assertEquals(2_000_000, elapsedNs2);

        long elapsedMcs = chronometer.getElapsed(tickNs, TimeUnit.MICROSECONDS);
        Assert.assertEquals(2_000, elapsedMcs);

        long elapsedMs = chronometer.getElapsed(tickNs, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, elapsedMs);
    }

    @Test
    public void testElapsedOverflow2() throws Exception {
        long negNs = Long.MIN_VALUE + 1_000_000;
        long posNs = negNs + Long.MAX_VALUE + 5_000_000;

        // difference is more than Long.MAX_VALUE
        MockChronometer chronometer = MockChronometer.createFrozen(
                System.currentTimeMillis(), negNs);

        long tickNs = chronometer.getTickNs();

        chronometer.shiftBy(-negNs, TimeUnit.NANOSECONDS);
        chronometer.shiftBy(posNs, TimeUnit.NANOSECONDS);

        long elapsedNs1 = chronometer.getElapsedNs(tickNs);
        Assert.assertEquals(Long.MAX_VALUE, elapsedNs1);

        long elapsedNs2 = chronometer.getElapsed(tickNs, TimeUnit.NANOSECONDS);
        Assert.assertEquals(Long.MAX_VALUE, elapsedNs2);

        long elapsedMs = chronometer.getElapsed(tickNs, TimeUnit.MILLISECONDS);
        long negMs = negNs / NS_IN_MS;
        long posMs = posNs / NS_IN_MS;
        long rstMs = ((posNs - posMs * NS_IN_MS) - (negNs - negMs * NS_IN_MS)) / NS_IN_MS;
        Assert.assertEquals((posMs - negMs) + rstMs, elapsedMs);
    }

    @Test
    public void testFactories() throws Exception {
        MockChronometer c1 = MockChronometer.createFrozen();
        Assert.assertNotNull(c1);
        Assert.assertEquals(MockChronometer.Mode.FROZEN, c1.getMode());

        MockChronometer c2 = MockChronometer.createFrozen(System.currentTimeMillis(), System.nanoTime());
        Assert.assertNotNull(c2);
        Assert.assertEquals(MockChronometer.Mode.FROZEN, c2.getMode());

        MockChronometer c3 = MockChronometer.createFrozen("2017-04-21 14:22:12.000 Europe/Moscow", System.nanoTime());
        Assert.assertNotNull(c3);
        Assert.assertEquals(MockChronometer.Mode.FROZEN, c3.getMode());
    }

    @Test
    public void testSleep1() throws Exception {
        long m = chronometer.getTickNs();
        chronometer.sleep(500);
        long elapsedMs = chronometer.getElapsed(m, TimeUnit.MILLISECONDS);
        Assert.assertTrue(Math.abs(elapsedMs - 500) < 20);
    }

    @Test
    public void testSleep2() throws Exception {
        long m = chronometer.getTickNs();
        chronometer.sleep(500_000_100, TimeUnit.NANOSECONDS);
        long elapsedMs = chronometer.getElapsed(m, TimeUnit.MILLISECONDS);
        Assert.assertTrue(Math.abs(elapsedMs - 500) < 20);
    }
}

