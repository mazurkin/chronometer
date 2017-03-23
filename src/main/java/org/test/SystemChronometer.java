package org.test;

import java.time.Instant;

/**
 * Default implementation bases on time values from {@link java.lang.System} class
 */
public class SystemChronometer implements Chronometer {

    public static final SystemChronometer INSTANCE = new SystemChronometer();

    /**
     * @see System#nanoTime()
     */
    public long getTickNs() {
        return System.nanoTime();
    }

    /**
     * @see System#currentTimeMillis()
     */
    public long getTimeMs() {
        return System.currentTimeMillis();
    }

    /**
     * @see Instant
     */
    public Instant getInstant() {
        return Instant.now();
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
