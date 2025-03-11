package me.croabeast.sir.plugin.misc;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.takion.TakionLib;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class DelayLogger {

    private final List<LoggerLine> list = new ArrayList<>();
    private final TakionLib lib;

    public DelayLogger() {
        this(SIRPlugin.getLib());
    }

    public DelayLogger add(DelayLogger logger) {
        list.addAll(logger.list);
        return this;
    }

    public DelayLogger add(String line, boolean usePrefix) {
        list.add(new LoggerLine(line, usePrefix));
        return this;
    }

    public DelayLogger add(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> add(s, usePrefix));
        return this;
    }

    public void sendLines() {
        if (list.isEmpty()) return;

        list.forEach(l -> (l.use ? lib.getLogger() : lib.getServerLogger()).log(l.line));
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class LoggerLine {
        final String line;
        final boolean use;
    }
}
