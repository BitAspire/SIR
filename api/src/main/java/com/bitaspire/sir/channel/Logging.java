package com.bitaspire.sir.channel;

import org.jetbrains.annotations.NotNull;

public interface Logging {

    boolean isEnabled();

    @NotNull
    String getFormat();
}
