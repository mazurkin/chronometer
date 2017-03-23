package org.test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mock chronometer for test purposes. Allows to control both `tick` and `clock` sources.
 */
public class MockChronometer implements Chronometer {

    private static final long NS_IN_MS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final long MS_IN_SEC = TimeUnit.SECONDS.toMillis(1);

    private static final String TIMESTAMP_FORMAT = "uuuu-MM-dd HH:mm:ss.SSS z";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)
            .withLocale(Locale.US)
            .withChronology(IsoChronology.INSTANCE)
            .withDecimalStyle(DecimalStyle.STANDARD)
            .withResolverStyle(ResolverStyle.STRICT);

    private final AtomicReference<State> stateRef;

    private volatile Mode mode;

    /**
     * Constructs mock chronometer with predefined clock values
     * @param mode Chronometer mode
     * @param epochTimeMs Value of <em>wall clock</em> time in milliseconds since <em>epoch</em>
     * @param tickNs Value of <em>tick</em> time in nanoseconds 
     */
    public MockChronometer(Mode mode, long epochTimeMs, long tickNs) {
        this.stateRef = new AtomicReference<>(State.of(epochTimeMs, 0, tickNs));
        this.mode = mode;
    }

    /**
     * Constructs mock chronometer with current real time state
     * @param mode Chronometer mode
     */
    public MockChronometer(Mode mode) {
        this.stateRef = new AtomicReference<>(State.now());
        this.mode = mode;
    }

    /**
     * Constructs mock chronometer with current real time state
     */
    public MockChronometer() {
        this.stateRef = new AtomicReference<>(State.now());
        this.mode = Mode.TICKING;
    }

    /**
     * Set the state from system time sources
     */
    public void reset() {
        this.stateRef.set(State.now());
    }

    /**
     * Returns mocked <em>tick</em> time value.
     * @return Tick time in nanoseconds
     */
    @Override
    public long getTickNs() {
        State state = stateRef.get();

        switch (mode) {
            case FROZEN:
                return state.tickNs;
            case TICKING:
                return state.tickNs + state.getElapsedNs();
            case SYSTEM:
                return SystemChronometer.INSTANCE.getTickNs();
            default:
                throw new IllegalStateException("Unsupported mode: " + mode);
        }
    }

    /**
     * Returns mocked <em>wall clock</em> time value.
     * @return Wall clock time in milliseconds since <em>epoch</em>.
     */
    @Override
    public long getTimeMs() {
        State state = stateRef.get();

        switch (mode) {
            case FROZEN:
                return state.timeMs;
            case TICKING:
                return state.timeMs + Math.floorDiv(state.timeNs + state.getElapsedNs(), NS_IN_MS);
            case SYSTEM:
                return SystemChronometer.INSTANCE.getTimeMs();
            default:
                throw new IllegalStateException("Unsupported mode: " + mode);
        }
    }

    /**
     * Returns mocked <em>wall clock</em> time value
     * @return Wall clock time as {@link Instant}
     */
    @Override
    public Instant getInstant() {
        State state = stateRef.get();

        long epochSec = Math.floorDiv(state.timeMs, MS_IN_SEC);
        long epochMs = Math.floorMod(state.timeMs, MS_IN_SEC);

        switch (mode) {
            case FROZEN:
                return Instant.ofEpochSecond(epochSec, epochMs * NS_IN_MS + state.timeNs);
            case TICKING:
                return Instant.ofEpochSecond(epochSec, epochMs * NS_IN_MS + state.timeNs + state.getElapsedNs());
            case SYSTEM:
                return SystemChronometer.INSTANCE.getInstant();
            default:
                throw new IllegalStateException("Unsupported mode: " + mode);
        }
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times by specified value
     * @param deltaMs Shift value in milliseconds
     * @param deltaNs Additional shift delta value in nanoseconds
     */
    public void shiftBy(long deltaMs, long deltaNs) {
        while (true) {
            State curState = stateRef.get();

            State newState;
            switch (mode) {
                case FROZEN:
                    newState = curState.shiftBoth(deltaMs, deltaNs);
                    break;
                case TICKING:
                    newState = curState.actualize().shiftBoth(deltaMs, deltaNs);
                    break;
                default:
                    throw new IllegalStateException("Mode is not supported: " + mode);
            }

            if (stateRef.compareAndSet(curState, newState)) {
                break;
            }
        }
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times by specified value
     * @param deltaMs Shift delta value in milliseconds
     */
    public void shiftBy(long deltaMs) {
        shiftBy(deltaMs, 0);
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times by specified value
     * @param delta Shift delta value
     * @param deltaUnit Shift delta value time unit
     */
    public void shiftBy(long delta, TimeUnit deltaUnit) {
        if (deltaUnit.compareTo(TimeUnit.MILLISECONDS) < 0) {
            shiftBy(0, deltaUnit.toNanos(delta));
        } else {
            shiftBy(deltaUnit.toMillis(delta), 0);
        }
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param epochTimeMs Wall clock time value in millisecond since <em>epoch</em>
     * @param adjustmentNs Additional nanosecond part
     * @see System#currentTimeMillis()
     */
    public void shiftTo(long epochTimeMs, long adjustmentNs) {
        while (true) {
            State curState = stateRef.get();

            State newState;
            switch (mode) {
                case FROZEN:
                case TICKING:
                    newState = curState.shiftBoth(epochTimeMs - curState.timeMs, adjustmentNs - curState.timeNs);
                    break;
                default:
                    throw new IllegalStateException("Mode is not supported: " + mode);
            }

            if (stateRef.compareAndSet(curState, newState)) {
                break;
            }
        }
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param epochTimeMs Wall clock time value in millisecond since <em>epoch</em>
     * @see System#currentTimeMillis()
     */
    public void shiftTo(long epochTimeMs) {
        shiftTo(epochTimeMs, 0);
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param moment Wall clock time as {@link Date} instance
     */
    public void shiftTo(Date moment) {
        shiftTo(moment.getTime());
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param moment Wall clock time as {@link Instant} instance
     */
    public void shiftTo(Instant moment) {
        shiftTo(moment.toEpochMilli(), moment.get(ChronoField.NANO_OF_SECOND) % NS_IN_MS);
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param moment Wall clock time as {@link Timestamp} instance
     */
    public void shiftTo(Timestamp moment) {
        shiftTo(moment.toInstant());
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param moment Wall clock time as {@link ZonedDateTime} instance
     */
    public void shiftTo(ZonedDateTime moment) {
        shiftTo(moment.toInstant());
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param moment Wall clock time as {@link OffsetDateTime} instance
     */
    public void shiftTo(OffsetDateTime moment) {
        shiftTo(moment.toInstant());
    }

    /**
     * Shifts both <em>tick</em> and <em>wall clock</em> times to the specified time moment
     * @param moment Wall clock time in 'uuuu-MM-dd HH:mm:ss.SSS z' format
     */
    public void shiftTo(String moment) {
        ZonedDateTime t = ZonedDateTime.parse(moment, TIMESTAMP_FORMATTER);
        shiftTo(t);
    }

    /**
     * Shifts only <em>wall clock</em> time - same as NTP or manual time setting works
     * @param deltaMs Shift delta value in milliseconds
     * @param deltaNs Shift delta value in nanoseconds
     */
    public void correctBy(long deltaMs, long deltaNs) {
        while (true) {
            State curState = stateRef.get();

            State newState;
            switch (mode) {
                case FROZEN:
                    newState = curState.shiftTime(deltaMs, deltaNs);
                    break;
                case TICKING:
                    newState = curState.actualize().shiftTime(deltaMs, deltaNs);
                    break;
                default:
                    throw new IllegalStateException("Mode is not supported: " + mode);
            }

            if (stateRef.compareAndSet(curState, newState)) {
                break;
            }
        }
    }

    /**
     * Sets only <em>wall clock</em> time - same as NTP or manual time setting works
     * @param epochTimeMs Wall clock time in milliseconds since <em>epoch</em>
     * @param adjustmentNs Additional nanosecond part
     * @see System#currentTimeMillis()
     */
    public void correctTo(long epochTimeMs, long adjustmentNs) {
        while (true) {
            State curState = stateRef.get();

            State newState;
            switch (mode) {
                case FROZEN:
                    newState = curState.changeTime(epochTimeMs, adjustmentNs);
                    break;
                case TICKING:
                    newState = curState.actualize().changeTime(epochTimeMs, adjustmentNs);
                    break;
                default:
                    throw new IllegalStateException("Mode is not supported: " + mode);
            }

            if (stateRef.compareAndSet(curState, newState)) {
                break;
            }
        }
    }

    /**
     * Get mode of mock chronometer
     * @return Mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Set mock chronometer mode
     * @param mode Mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        this.reset();
    }

    @Override
    public String toString() {
        return mode.name() + ": " + getInstant();
    }

    /**
     * Chronometer mode
     */
    public enum Mode {

        /**
         * State of mock chronometer can only be changed with implicit method calls
         */
        FROZEN,

        /**
         * State of mock chronometer can be changed. Time is ticking after each change like normal system clock does.
         */
        TICKING,

        /**
         * Chronometer acts like system chronometer
         */
        SYSTEM

    }

    private static final class State {

        private final long timeMs;

        private final long timeNs;

        private final long tickNs;

        private final long createdNs;

        private State(long timeMs, long timeNs, long tickNs) {
            this.createdNs = SystemChronometer.INSTANCE.getTickNs();

            // Math.floorDiv(-123, 100) == -2
            // Math.floorDiv(+123, 100) == 1
            this.timeMs = timeMs + Math.floorDiv(timeNs, NS_IN_MS);

            // Math.floorMod(-123, 100) == 77
            // Math.floorMod(+123, 100) == 23
            this.timeNs = Math.floorMod(timeNs, NS_IN_MS);

            this.tickNs = tickNs;
        }

        private static State of(long timeMs, long timeNs, long tickNs) {
            return new State(timeMs, timeNs, tickNs);
        }

        private static State now() {
            long timeMs = SystemChronometer.INSTANCE.getTimeMs();
            long tickNs = SystemChronometer.INSTANCE.getTickNs();
            return State.of(timeMs, 0, tickNs);
        }

        private State shiftBoth(long deltaMs, long deltaNs) {
            return State.of(this.timeMs + deltaMs, this.timeNs + deltaNs, this.tickNs + deltaMs * NS_IN_MS + deltaNs);
        }

        private State shiftTime(long deltaMs, long deltaNs) {
            return State.of(this.timeMs + deltaMs, this.timeNs + deltaNs, this.tickNs);
        }

        private State changeTime(long timeMs, long timeNs) {
            return State.of(timeMs, timeNs, this.tickNs);
        }

        private State actualize() {
            return shiftBoth(0, getElapsedNs());
        }

        private long getElapsedNs() {
            return SystemChronometer.INSTANCE.getElapsedNs(this.createdNs);
        }

    }

}