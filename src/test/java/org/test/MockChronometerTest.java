package org.test;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class MockChronometerTest {

    @Test
    public void testSwitching() throws Exception {
        MockChronometer c = new MockChronometer(MockChronometer.Mode.SYSTEM);

        c.setMode(MockChronometer.Mode.TICKING);
        c.setMode(MockChronometer.Mode.FROZEN);
        c.setMode(MockChronometer.Mode.SYSTEM);
        c.setMode(MockChronometer.Mode.FROZEN);
        c.setMode(MockChronometer.Mode.TICKING);
    }

    @Test
    public void testChaining() throws Exception {
        Instant instant1 = MockChronometer.createFrozen("2010-04-30 10:00:00.000 UTC", 0)
                .shiftTo("2010-03-01 10:00:00.000 UTC")
                .reset("2010-01-20 10:00:00.000 UTC", 0)
                .shiftBy(1, TimeUnit.HOURS)
                .getInstant();

        Instant instant2 = MockChronometer.createFrozen("2010-01-20 11:00:00.000 UTC", 0)
                .getInstant();

        Assert.assertEquals(instant2, instant1);
    }

}
