package me.croabeast.sir.plugin.misc;

public final class Timer {

    private long startTime = 0;

    private Timer(boolean initialize) {
        if (initialize) startTime = System.currentTimeMillis();
    }

    public void setStart() {
        startTime = System.currentTimeMillis();
    }

    public long result() {
        return System.currentTimeMillis() - startTime;
    }

    public static Timer create(boolean initialize) {
        return new Timer(initialize);
    }
}
