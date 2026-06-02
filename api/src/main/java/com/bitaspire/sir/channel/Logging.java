package com.bitaspire.sir.channel;

import org.jetbrains.annotations.NotNull;

/**
 * Controls console logging behaviour for messages sent through a {@link ChatChannel}.
 */
public interface Logging {

    /**
     * Returns whether logging is enabled for this channel.
     *
     * @return {@code true} if messages should be logged to the console.
     */
    boolean isEnabled();

    /**
     * Returns the format string used when logging messages to the console.
     *
     * @return the log format.
     */
    @NotNull
    String getFormat();
}
