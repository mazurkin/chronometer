package org.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class MockChronometerTickingTest {

    private MockChronometer chronometer;

    @Before
    public void setUp() throws Exception {
        chronometer = new MockChronometer();

        // TICKING must be default mode for mock chronometer
        Assert.assertEquals(MockChronometer.Mode.TICKING, chronometer.getMode());
    }

    @Test
    public void testSleepTo() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC"); // point zero

        long time1 = chronometer.getTimeMs();
        long tick1 = chronometer.getTickNs();
        Instant instant1 = chronometer.getInstant();

        Thread.sleep(10); // this will be ignored as the next shift is absolute 

        chronometer.shiftTo("2017-03-13 02:12:30.768 UTC"); // +5ms

        Thread.sleep(5);

        long time2 = chronometer.getTimeMs();
        long tick2 = chronometer.getTickNs();
        Instant instant2 = chronometer.getInstant();

        Assert.assertTrue(time2 - time1 >= 10);
        Assert.assertTrue(tick2 - tick1 >= 10_000_000);
        Assert.assertTrue(Duration.between(instant1, instant2).toNanos() >= 10_000_000);
    }

    @Test
    public void testShiftBy() throws Exception {
        chronometer.shiftTo("2017-03-13 02:12:30.763 UTC"); // point zero

        long time1 = chronometer.getTimeMs();
        long tick1 = chronometer.getTickNs();
        Instant instant1 = chronometer.getInstant();

        chronometer.shiftBy(2, TimeUnit.MILLISECONDS);

        Thread.sleep(5);

        chronometer.shiftBy(3, TimeUnit.MILLISECONDS);

        long time2 = chronometer.getTimeMs();
        long tick2 = chronometer.getTickNs();
        Instant instant2 = chronometer.getInstant();

        Assert.assertTrue(time2 - time1 >= 10);
        Assert.assertTrue(tick2 - tick1 >= 10_000_000);
        Assert.assertTrue(Duration.between(instant1, instant2).toNanos() >= 10_000_000);
    }
}