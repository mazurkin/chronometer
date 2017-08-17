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
        chronometer = new MockChronometer(MockChronometer.Mode.TICKING);

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

    @Test
    public void testFactories() throws Exception {
        MockChronometer c1 = MockChronometer.createTicking();
        Assert.assertNotNull(c1);
        Assert.assertEquals(MockChronometer.Mode.TICKING, c1.getMode());

        MockChronometer c2 = MockChronometer.createTicking(System.currentTimeMillis(), System.nanoTime());
        Assert.assertNotNull(c2);
        Assert.assertEquals(MockChronometer.Mode.TICKING, c2.getMode());

        MockChronometer c3 = MockChronometer.createTicking("2017-04-21 14:22:12.000 Europe/Moscow", System.nanoTime());
        Assert.assertNotNull(c3);
        Assert.assertEquals(MockChronometer.Mode.TICKING, c3.getMode());
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