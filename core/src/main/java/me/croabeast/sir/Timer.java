package me.croabeast.sir;

import lombok.NoArgsConstructor;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.takion.TakionLib;

import java.util.List;

@NoArgsConstructor(staticName = "create")
public final class Timer {

    private long start = System.currentTimeMillis();

    public Timer start() {
        start = System.currentTimeMillis();
        return this;
    }

    public long current() {
        return System.currentTimeMillis() - start;
    }

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

    public void log(String... lines) {
        log(true, lines);
    }
}
