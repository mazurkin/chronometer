package org.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class SystemChronometerTest {

    private SystemChronometer chronometer;

    @Before
    public void setUp() throws Exception {
        chronometer = new SystemChronometer();
    }

    @Test
    public void testElapsedMs() throws Exception {
        long markerNs = chronometer.getTickNs();
        Thread.sleep(5);
        long elapsedNs = chronometer.getElapsedNs(markerNs);
        Assert.assertTrue(elapsedNs > 5_000_000);
    }

    @Test
    public void testElapsed() throws Exception {
        long markerNs = chronometer.getTickNs();
        Thread.sleep(5);
        long elapsedNs = chronometer.getElapsed(markerNs, TimeUnit.NANOSECONDS);
        Assert.assertTrue(elapsedNs > 5_000_000);
    }

    @Test
    public void testLocalTime() throws Exception {
        LocalTime v = chronometer.getLocalTime(ZoneId.of("America/New_York"));
        Assert.assertNotNull(v);
    }

    @Test
    public void testLocalDate() throws Exception {
        LocalDate v = chronometer.getLocalDate(ZoneId.of("America/New_York"));
        Assert.assertNotNull(v);
    }
}
