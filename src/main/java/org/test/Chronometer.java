package org.test;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * <p>Some advanced factory abstraction on current time source. It's like {@link Clock} but more abstract,
 * has more methods and supports both old and new Java time APIs.</p>
 *
 * <p>The main (and possible the only) idea behind this abstraction is to decouple code from native time flow. That
 * gives a possibility to properly test all time-related issues like timeouts and waits.</p>
 *
 * <p>This abstraction has two sources:</p>
 * <ul>
 * <li><em>Tick</em> time that is provided by CPU or built-time hardware timers. Default source
 * is {@link System#nanoTime()}</li>
 * <li><em>Wall clock</em> time that represents time since some <em>epoch</em> moment. Default source
 * is {@link System#currentTimeMillis()}</li>
 * </ul>
 *
 * <p>In tests use subclass {@link MockChronometer} that allows to control time flow.</p>
 *
 * <p>In production use subclass {@link SystemChronometer} that gets time from {@link java.lang.System} class</p>
 *
 * @see MockChronometer
 * @see SystemChronometer
 * @see Clock
 */
public interface Chronometer {

    long ALLOWED_TICK_JITTER_NS = 1_000_000;

    long NS_IN_MCS = TimeUnit.MICROSECONDS.toNanos(1);

    /**
     * Returns current <em>tick</em> time in nanoseconds. Doesn't depend on what <em>wall</em> clock time is it now.
     * Doesn't depends on NTP shifts. Almost monotonic. Mostly used for timers and duration measurements.
     *
     * @return Tick time in nanoseconds
     *
     * @see <a href="http://stas-blogspot.blogspot.ru/2012/02/what-is-behind-systemnanotime.html">
     *     System.nanotTime() in details</a>
     * @see <a href="https://www.kapsi.de/blog/a-big-flaw-in-javas-nanotime">
     *     Negative values</a>
     */
    long getTickNs();

    /**
     * Returns <em>wall clock</em> time in milliseconds since <em>epoch</em> (midnight, January 1, 1970 UTC)
     *
     * @return Milliseconds between the current time and <em>epoch</em> moment
     *
     * @see <a href="http://infiniteundo.com/post/25326999628/falsehoods-programmers-believe-about-time">
     *     Facts about time #1</a>
     * @see <a href="http://infiniteundo.com/post/25509354022/more-falsehoods-programmers-believe-about-time">
     *     Facts about time #2</a>
     */
    long getTimeMs();

    /**
     * Current <em>wall clock</em> as {@link Date}
     *
     * @return Current time
     */
    default Date getDate() {
        return new Date(getTimeMs());
    }

    /**
     * Current <em>wall clock</em> as {@link Calendar}
     *
     * @return Current time
     */
    default Calendar getCalendar() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(getTimeMs());
        return calendar;
    }

    /**
     * Current <em>wall clock</em> as {@link Calendar}
     *
     * @param timezone Timezone for calendar
     * @return Current time
     */
    default Calendar getCalendar(TimeZone timezone) {
        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setTimeInMillis(getTimeMs());
        return calendar;
    }

    /**
     * Current <em>wall clock</em> as {@link Calendar}
     *
     * @param timezone Timezone for calendar
     * @param locale Locale for calendar
     * @return Current time
     */
    default Calendar getCalendar(TimeZone timezone, Locale locale) {
        Calendar calendar = new GregorianCalendar(timezone, locale);
        calendar.setTimeInMillis(getTimeMs());
        return calendar;
    }

    /**
     * Current <em>wall clock</em> as {@link Instant}
     *
     * @return Current time
     */
    Instant getInstant();

    /**
     * Current <em>wall clock</em> as {@link Timestamp}
     *
     * @return Current time
     */
    default Timestamp getTimestamp() {
        return Timestamp.from(getInstant());
    }

    /**
     * Current <em>wall clock</em> as {@link ZonedDateTime}
     *
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default ZonedDateTime getZonedDateTime(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(getInstant(), zoneId);
    }

    /**
     * Current <em>wall clock</em> as {@link OffsetDateTime}
     *
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default OffsetDateTime getOffsetDateTime(ZoneId zoneId) {
        return OffsetDateTime.ofInstant(getInstant(), zoneId);
    }

    /**
     * Current <em>wall clock</em> as {@link LocalDateTime}
     *
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default LocalDateTime getLocalDateTime(ZoneId zoneId) {
        return LocalDateTime.ofInstant(getInstant(), zoneId);
    }

    /**
     * Current <em>wall clock</em> as {@link LocalTime}
     *
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default LocalTime getLocalTime(ZoneId zoneId) {
        return getLocalDateTime(zoneId).toLocalTime();
    }

    /**
     * Current <em>wall clock</em> as {@link LocalDate}
     *
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default LocalDate getLocalDate(ZoneId zoneId) {
        return getLocalDateTime(zoneId).toLocalDate();
    }

    /**
     * Calculates elapsed time in nanoseconds
     *
     * @param tickNs Value returned by {@link Chronometer#getTickNs()} call at the start of measured operation
     * @return Elapsed time, nanoseconds
     *
     * @see Chronometer#getTickNs()
     */
    default long getElapsedNs(long tickNs) {
        long elapsedNs = getTickNs() - tickNs;

        if (elapsedNs >= 0) {
            return elapsedNs;
        } else if (elapsedNs >= -ALLOWED_TICK_JITTER_NS) {
            // System.nanoTime() could be different on CPU cores on Linux - so we allow some jitter (1ms back)
            return 0;
        } else {
            // large negative value - overflow
            return Long.MAX_VALUE;
        }
    }

    /**
     * Calculates elapsed time in specified time unit
     *
     * @param tickNs Value returned by {@link Chronometer#getTickNs()} call at the start of measured operation
     * @param timeUnit Time unit for result
     * @return Elapsed time in selected time unit
     *
     * @see Chronometer#getTickNs()
     */
    default long getElapsed(long tickNs, TimeUnit timeUnit) {
        return getElapsed(tickNs, getTickNs(), timeUnit);
    }

    /**
     * Calculates difference between two timestamps in specified time unit
     *
     * @param tickNs1 The first value returned by {@link Chronometer#getTickNs()} call at the start of an operation
     * @param tickNs2 The second value returned by {@link Chronometer#getTickNs()} call at the end of an operation
     * @param timeUnit Time unit for result
     * @return Elapsed time in selected time unit
     *
     * @see Chronometer#getTickNs()
     */
    default long getElapsed(long tickNs1, long tickNs2, TimeUnit timeUnit) {
        long elapsedNs = tickNs2 - tickNs1;

        if (elapsedNs >= 0) {
            return timeUnit.convert(elapsedNs, TimeUnit.NANOSECONDS);
        } else if (elapsedNs >= -ALLOWED_TICK_JITTER_NS) {
            // System.nanoTime() could be different on CPU cores on Linux - so we allow some jitter (1ms back)
            return 0;
        } else {
            // large negative value - overflow
            if (timeUnit.compareTo(TimeUnit.NANOSECONDS) > 0) {
                // handle long type overflow for higher time units
                long elapsedMcs = elapsedNs / NS_IN_MCS;
                elapsedMcs -= Long.MIN_VALUE / NS_IN_MCS;
                elapsedMcs += Long.MAX_VALUE / NS_IN_MCS;
                return timeUnit.convert(elapsedMcs, TimeUnit.MICROSECONDS);
            } else {
                return Long.MAX_VALUE;
            }
        }
    }

    /**
     * Pause current thread on specified time
     *
     * @param pauseMs Pause duration in milliseconds
     * @throws InterruptedException Thrown if the thread is interrupted
     */
    void sleep(long pauseMs) throws InterruptedException;

    /**
     * Pause current thread on specified time
     *
     * @param pause Pause duration
     * @param pauseUnit Time unit for time duration
     * @throws InterruptedException Thrown if the thread is interrupted
     */
    void sleep(long pause, TimeUnit pauseUnit) throws InterruptedException;

    /**
     * Pause current thread on specified time without throwing an interrupted exception
     *
     * @param pause Pause duration
     * @param pauseUnit Time unit for time duration
     */
    default void sleepUninterruptibly(long pause, TimeUnit pauseUnit) {
        boolean interrupted = false;

        try {
            long remainingNs = pauseUnit.toNanos(pause);
            long deadlineNs = getTickNs() + remainingNs;

            while (true) {
                try {
                    sleep(remainingNs, TimeUnit.NANOSECONDS);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNs = deadlineNs - getTickNs();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
