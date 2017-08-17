package org.test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation bases on time values from {@link java.lang.System} class
 */
public class SystemChronometer implements Chronometer {

    public static final SystemChronometer INSTANCE = new SystemChronometer();

    @Override
    public long getTickNs() {
        return System.nanoTime();
    }

    @Override
    public long getTimeMs() {
        return System.currentTimeMillis();
    }

    @Override
    public Instant getInstant() {
        return Instant.now();
    }

    @Override
    public void sleep(long pauseMs) throws InterruptedException {
        Thread.sleep(pauseMs);
    }

    @Override
    public void sleep(long pause, TimeUnit pauseUnit) throws InterruptedException {
        if (pauseUnit.compareTo(TimeUnit.MILLISECONDS) < 0) {
            long pauseMs = pauseUnit.toMillis(pause);

            long pauseNsTotal = pauseUnit.toNanos(pause);
            int pauseNsPart = (int) (pauseNsTotal - TimeUnit.MILLISECONDS.toNanos(pauseMs));

            Thread.sleep(pauseMs, pauseNsPart);
        } else {
            long pauseMs = pauseUnit.toMillis(pause);

            Thread.sleep(pauseMs);
        }
    }

    /**
     * Utility method chooses default chronometer instance if no any other chronometer is provided
     * @param chronometer Some provided chronometer
     * @return Provided or default chronometer
     */
    public static SystemChronometer defaultOr(SystemChronometer chronometer) {
        if (chronometer != null) {
            return chronometer;
        } else {
            return SystemChronometer.INSTANCE;
        }
    }

}
