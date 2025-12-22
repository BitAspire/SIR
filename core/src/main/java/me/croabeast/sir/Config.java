package me.croabeast.sir;

public interface Config {

    boolean isUpdaterOnStart();

    boolean isUpdaterToOp();

    boolean isColoredConsole();

    boolean isShowPrefix();

    boolean isOverrideOp();

    boolean isCheckMute();

    boolean isDefaultBukkitMethods();

    boolean loadDefaultJars(String type);

    String getPrefixKey();

    String getPrefix();

    String getCenterPrefix();

    String getLineSeparator();
}
