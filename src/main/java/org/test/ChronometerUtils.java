package org.test;

import java.util.concurrent.TimeUnit;

/**
 * Support class for {@link Chronometer} abstraction
 */
public final class ChronometerUtils {

    private ChronometerUtils() {
    }

    public static long runElapsedNs(Chronometer chronometer, Runnable runnable) {
        long markerNs = chronometer.getTickNs();
        runnable.run();
        return chronometer.getElapsedNs(markerNs);
    }

    public static long runElapsed(Chronometer chronometer, TimeUnit timeUnit, Runnable runnable) {
        long markerNs = chronometer.getTickNs();
        runnable.run();
        return chronometer.getElapsed(markerNs, timeUnit);
    }

}
