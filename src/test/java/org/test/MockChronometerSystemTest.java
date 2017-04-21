package org.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

public class MockChronometerSystemTest {

    private MockChronometer chronometer;

    @Before
    public void setUp() throws Exception {
        chronometer = new MockChronometer(MockChronometer.Mode.SYSTEM);
    }

    @Test
    public void test() throws Exception {
        long time1 = chronometer.getTimeMs();
        long tick1 = chronometer.getTickNs();
        Instant instant1 = chronometer.getInstant();

        Thread.sleep(10);

        long time2 = chronometer.getTimeMs();
        long tick2 = chronometer.getTickNs();
        Instant instant2 = chronometer.getInstant();

        Assert.assertTrue(time2 - time1 >= 10);
        Assert.assertTrue(tick2 - tick1 >= 10_000_000);
        Assert.assertTrue(Duration.between(instant1, instant2).toNanos() >= 10_000_000);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoShiftTo() throws Exception {
        chronometer.shiftTo(System.currentTimeMillis());
    }

    @Test(expected = IllegalStateException.class)
    public void testNoShiftBy() throws Exception {
        chronometer.shiftBy(10, 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoCorrectTo() throws Exception {
        chronometer.correctTimeTo(System.currentTimeMillis(), 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoCorrectBy() throws Exception {
        chronometer.correctTimeBy(10, 0);
    }

    @Test
    public void testFactories() throws Exception {
        MockChronometer c = MockChronometer.createSystem();
        Assert.assertNotNull(c);
        Assert.assertEquals(MockChronometer.Mode.SYSTEM, c.getMode());
    }
}