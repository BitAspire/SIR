package com.bitaspire.sir;

import lombok.NoArgsConstructor;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.takion.TakionLib;

import java.util.List;

/**
 * Simple elapsed-time utility for measuring and logging operation durations.
 *
 * <p> The placeholder {@code {time}} in log lines is replaced with the current elapsed
 * milliseconds at the moment {@code log} is called.
 *
 * <p> Create an instance with {@link #create()}.
 */
@NoArgsConstructor(staticName = "create")
public final class Timer {

    private long start = System.currentTimeMillis();

    /**
     * Resets the timer to the current time.
     *
     * @return this instance for chaining.
     */
    public Timer start() {
        start = System.currentTimeMillis();
        return this;
    }

    /**
     * Returns the number of milliseconds elapsed since the timer was last started.
     *
     * @return elapsed time in milliseconds.
     */
    public long current() {
        return System.currentTimeMillis() - start;
    }

    /**
     * Logs the given lines, replacing {@code {time}} with the current elapsed milliseconds.
     *
     * @param prefix if {@code true}, uses the plugin-prefixed logger; otherwise uses the plain server logger.
     * @param lines the lines to log.
     */
    public void log(boolean prefix, String... lines) {
        final TakionLib lib = SIRApi.instance().getLibrary();

        List<String> list = ArrayUtils.toList(lines);
        list.replaceAll(s -> s == null ? null : s.replaceAll("(?i)\\{time}", current() + ""));

        (prefix ? lib.getLogger() : lib.getServerLogger()).log(null, list);
    }

    @Override
    public String toString() {
        return "Timer{" + "start=" + start + '}';
    }

    /**
     * Logs the given lines using the plugin-prefixed logger.
     * Equivalent to {@link #log(boolean, String...) log(true, lines)}.
     *
     * @param lines the lines to log.
     */
    public void log(String... lines) {
        log(true, lines);
    }
}
