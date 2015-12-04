package io.totemo.nerdlag;

import java.lang.reflect.Field;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;

// ----------------------------------------------------------------------------
/**
 * Extends TimedRegisteredListener to keep track of the maximum duration of an
 * event handler invocation and report events that exceed a threshold.
 *
 * Unfortunately, TimedRegisteredListener and it's base, RegisteredListener,
 * don't anticipate subclassing; all their fields are private. So this class
 * must duplicate the fields and implementation of TimedRegisteredListener.
 * MaxTimedRegisteredListener is derived from TimedRegisteredListener so that
 * the /timings command continues to function correctly.
 */
public class MaxTimedRegisteredListener extends TimedRegisteredListener {
    // ------------------------------------------------------------------------
    /**
     * Callback interface that notifies an object that an event took longer to
     * execute than the reporting threshold.
     *
     * @param reg the {@link MaxTimedRegisteredListener} dispatching the event.
     * @param elapsedNanos the duration of the event handler's callback in
     *        nanoseconds.
     */
    public interface EventDurationHandler {
        public void reportDuration(MaxTimedRegisteredListener reg, long durationNanos);
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * A MaxTimedRegisteredListener is constructed from an original
     * TimedRegisteredListener that it replaces.
     *
     * @param original the RegisteredListener this instance will replace.
     * @param report if true, event handler invocations exceeding
     *        reportThresholdNanos.
     * @param reportThresholdNanos the event handler duration in nanoseconds,
     *        above which the event will be reported to
     *        {@link NerdLag#report(MaxTimedRegisteredListener, long)}.
     * @param handler receives callbacks about event executions whose durations
     *        exceed the reporting threshold.
     *
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public MaxTimedRegisteredListener(RegisteredListener original, boolean report, long reportThresholdNanos, EventDurationHandler handler)
    throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        super(original.getListener(), null, original.getPriority(), original.getPlugin(), original.isIgnoringCancelled());
        this.report = report;
        this.reportThresholdNanos = reportThresholdNanos;
        this.handler = handler;

        // Copy the executor field from the original TimedRegisteredListener.
        Class<RegisteredListener> rlClass = RegisteredListener.class;
        Field executorField = rlClass.getDeclaredField("executor");
        executorField.setAccessible(true);
        executor = (EventExecutor) executorField.get(original);
        executorField.set(this, executor);
    }

    @Override
    public void callEvent(Event event) throws EventException {
        if (event.isAsynchronous()) {
            super.callEvent(event);
            return;
        }
        count++;
        Class<? extends Event> newEventClass = event.getClass();
        if (this.eventClass == null) {
            this.eventClass = newEventClass;
        } else if (!this.eventClass.equals(newEventClass)) {
            multiple = true;
            this.eventClass = getCommonSuperclass(newEventClass, this.eventClass).asSubclass(Event.class);
        }
        long start = System.nanoTime();
        super.callEvent(event);
        long elapsed = System.nanoTime() - start;
        if (elapsed > maxNanos) {
            this.maxNanos = elapsed;
        }
        if (report && elapsed > reportThresholdNanos) {
            handler.reportDuration(this, elapsed);
        }
        totalNanos += elapsed;
    }

    protected static Class<?> getCommonSuperclass(Class<?> class1, Class<?> class2) {
        while (!class1.isAssignableFrom(class2)) {
            class1 = class1.getSuperclass();
        }
        return class1;
    }

    @Override
    public void reset() {
        count = 0;
        totalNanos = 0;
        maxNanos = 0;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public long getTotalTime() {
        return totalNanos;
    }

    @Override
    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    @Override
    public boolean hasMultiple() {
        return multiple;
    }

    // ------------------------------------------------------------------------
    // Additional methods not included in TimedRegisteredListener.
    /**
     * Return the EventExecutor.
     *
     * @return the EventExecutor.
     */
    public EventExecutor getEventExecutor() {
        return executor;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this plugin will report event handler durations exceeding
     * {@link #getReportThresholdNanos()}.
     *
     * @return true if this plugin will report event handler durations exceeding
     *         {@link #getReportThresholdNanos()}.
     */
    public boolean isReporting() {
        return report;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum event handler duration (in nanoseconds) since the last
     * reset().
     *
     * @return the maximum event handler duration (in nanoseconds) since the
     *         last reset().
     */
    public long getMaxDurationNanos() {
        return maxNanos;
    }

    // ------------------------------------------------------------------------
    /**
     * Event handlers that exceed this duration in nanoseconds will be reported
     * to {@link NerdLag#report(MaxTimedRegisteredListener, long)}.
     *
     * @return the reportable duration of an event handler in nanoseconds.
     */
    public long getReportThresholdNanos() {
        return reportThresholdNanos;
    }

    // ------------------------------------------------------------------------

    protected int count;
    protected long totalNanos;
    protected long maxNanos;
    protected Class<? extends Event> eventClass;
    protected boolean multiple = false;
    protected boolean report;
    protected long reportThresholdNanos;
    protected EventDurationHandler handler;
    protected EventExecutor executor;
} // class MaxTimedRegisteredListener