package me.croabeast.sir.plugin.user;

public interface MuteData {

    boolean isMuted();

    void mute(long time, String reason, String by);

    void mute(long time);

    void unmute();

    String getReason();

    String getMuteBy();

    long getRemaining();
}
