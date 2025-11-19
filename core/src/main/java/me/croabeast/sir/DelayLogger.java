package me.croabeast.sir;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.takion.TakionLib;

import java.util.ArrayList;
import java.util.List;

/**
 * A logger that collects log lines and sends them later.
 * This is useful for delaying logging until the plugin is fully loaded.
 *
 * <p> Example usage:
 * <pre> {@code
 * DelayLogger logger = new DelayLogger(takionLib);
 * logger.add("This is a log line", true);
 * // Add more lines as needed
 * logger.sendLines(); // Sends all collected log lines
 * }</pre>
 * @see TakionLib
 */
public final class DelayLogger {

    private final TakionLib lib = SIRApi.instance().getLibrary();
    private final List<LoggerLine> list = new ArrayList<>();

    /**
     * Adds a line to the logger.
     * The line will be logged later when {@link #sendLines()} is called.
     *
     * @param logger the DelayLogger to add lines from
     * @return this DelayLogger instance for method chaining
     */
    public DelayLogger add(DelayLogger logger) {
        list.addAll(logger.list);
        return this;
    }

    /**
     * Adds a line to the logger.
     * The line will be logged later when {@link #sendLines()} is called.
     *
     * @param line the line to log
     * @param usePrefix if true, the line will be logged with the plugin's prefix,
     *                  otherwise it will be logged without the prefix
     * @return this DelayLogger instance for method chaining
     */
    public DelayLogger add(String line, boolean usePrefix) {
        list.add(new LoggerLine(line, usePrefix));
        return this;
    }

    /**
     * Adds multiple lines to the logger.
     * Each line will be logged later when {@link #sendLines()} is called.
     *
     * @param usePrefix if true, each line will be logged with the plugin's prefix,
     * @param lines the lines to log
     * @return this DelayLogger instance for method chaining
     */
    public DelayLogger add(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> add(s, usePrefix));
        return this;
    }

    /**
     * Sends all collected log lines.
     *
     * <p> Each line will be logged using the appropriate logger based on whether
     * it uses the plugin's prefix or not.
     *
     * <p> If the list is empty, this method does nothing.
     */
    public void sendLines() {
        if (!list.isEmpty())
            list.forEach(l -> (l.use ? lib.getLogger() : lib.getServerLogger()).log(l.line));
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class LoggerLine {
        final String line;
        final boolean use;
    }
}
