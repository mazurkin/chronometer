package org.test;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
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
 * <li><em>Tick</em> time that is provided by CPU or built-time hardware timers. Default source is {@link System#nanoTime()}</li>
 * <li><em>Wall clock</em> time that represents time since some <em>epoch</em> moment. Default source is {@link System#currentTimeMillis()}</li>
 * </ul>
 * 
 * <p>Support class {@link ChronometerUtils} has some advanced method and algorithms based on Chronometer abstraction.</p>
 *
 * <p>In tests use subclass {@link MockChronometer} that allows to control time flow.</p>
 *
 * <p>In production use subclass {@link SystemChronometer} that gets time from {@link java.lang.System} class</p>
 *
 * @see MockChronometer
 * @see SystemChronometer
 * @see ChronometerUtils
 * @see Clock
 */
public interface Chronometer {

    /**
     * Returns current <em>tick</em> time in nanoseconds. Doesn't depend on what <em>wall</em> clock time is it now. Doesn't depends
     * on NTP shifts. Almost monotonic. Mostly used for timers and duration measurements.
     * @return Tick time in nanoseconds
     * @see <a href="http://stas-blogspot.blogspot.ru/2012/02/what-is-behind-systemnanotime.html">System.nanotTime() in details</a>
     */
    long getTickNs();

    /**
     * Returns <em>wall clock</em> time in milliseconds since <em>epoch</em> (midnight, January 1, 1970 UTC)
     * @return Milliseconds between the current time and <em>epoch</em> moment
     * @see <a href="http://infiniteundo.com/post/25326999628/falsehoods-programmers-believe-about-time">Facts about time #1</a>
     * @see <a href="http://infiniteundo.com/post/25509354022/more-falsehoods-programmers-believe-about-time">Facts about time #2</a>
     */
    long getTimeMs();

    /**
     * Current <em>wall clock</em> as {@link Date}
     * @return Current time
     */
    default Date getDate() {
        return new Date(getTimeMs());
    }

    /**
     * Current <em>wall clock</em> as {@link Calendar}
     * @return Current time
     */
    default Calendar getCalendar() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(getTimeMs());
        return calendar;
    }

    /**
     * Current <em>wall clock</em> as {@link Calendar}
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
     * @return Current time
     */
    Instant getInstant();

    /**
     * Current <em>wall clock</em> as {@link Timestamp}
     * @return Current time
     */
    default Timestamp getTimestamp() {
        return Timestamp.from(getInstant());
    }

    /**
     * Current <em>wall clock</em> as {@link ZonedDateTime}
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default ZonedDateTime getZonedDateTime(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(getInstant(), zoneId);
    }

    /**
     * Current <em>wall clock</em> as {@link OffsetDateTime}
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default OffsetDateTime getOffsetDateTime(ZoneId zoneId) {
        return OffsetDateTime.ofInstant(getInstant(), zoneId);
    }

    /**
     * Current <em>wall clock</em> as {@link LocalDateTime}
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default LocalDateTime getLocalDateTime(ZoneId zoneId) {
        return LocalDateTime.ofInstant(getInstant(), zoneId);
    }

    /**
     * Current <em>wall clock</em> as {@link LocalTime}
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default LocalTime getLocalTime(ZoneId zoneId) {
        return getLocalDateTime(zoneId).toLocalTime();
    }

    /**
     * Current <em>wall clock</em> as {@link LocalDate}
     * @param zoneId Timezone for time container
     * @return Current time
     */
    default LocalDate getLocalDate(ZoneId zoneId) {
        return getLocalDateTime(zoneId).toLocalDate();
    }

    /**
     * Calculates elapsed time in nanoseconds
     * @param tickNs Value returned by {@link Chronometer#getTickNs()} call at the start of measured operation
     * @return Elapsed time, nanoseconds
     * @see Chronometer#getTickNs()
     */
    default long getElapsedNs(long tickNs) {
        long elapsedNs = getTickNs() - tickNs;

        if (elapsedNs >= 0) {
            return elapsedNs;
        } else if (elapsedNs >= -1_000_000) {
            // System.nanoTime() could be different on CPU cores on Linux - so we allow some jitter (1ms back)
            return 0;
        } else {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Calculates elapsed time in specified time unit
     * @param tickNs Value returned by {@link Chronometer#getTickNs()} call at the start of measured operation
     * @param timeUnit Time unit for result
     * @return Elapsed time in selected time unit
     * @see Chronometer#getTickNs()
     */
    default long getElapsed(long tickNs, TimeUnit timeUnit) {
        long elapsedNs = getTickNs() - tickNs;

        if (elapsedNs >= 0) {
            return timeUnit.convert(elapsedNs, TimeUnit.NANOSECONDS);
        } else if (elapsedNs >= -1_000_000) {
            // System.nanoTime() could be different on CPU cores on Linux - so we allow some jitter (1ms back)
            return 0;
        } else {
            if (timeUnit.compareTo(TimeUnit.NANOSECONDS) > 0) {
                // handle long type overflow for higher time units
                long elapsedMcs = elapsedNs / 1_000;
                elapsedMcs -= Long.MIN_VALUE / 1_000; 
                elapsedMcs += Long.MAX_VALUE / 1_000;
                return timeUnit.convert(elapsedMcs, TimeUnit.MICROSECONDS);
            } else {
                return Long.MAX_VALUE;
            }
        }
    }

}
