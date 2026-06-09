package com.bitaspire.sir.file;

/**
 * Represents the main SIR plugin configuration.
 *
 * <p> Provides access to general plugin settings such as updater behaviour,
 * console output, permission handling, and chat formatting values.
 */
public interface Config {

    /**
     * Returns whether the updater should run on server start.
     *
     * @return {@code true} if the updater is enabled on startup.
     */
    boolean isUpdaterOnStart();

    /**
     * Returns whether update notifications are sent to online operators.
     *
     * @return {@code true} if operators receive update messages.
     */
    boolean isUpdaterToOp();

    /**
     * Returns whether console output uses colour codes.
     *
     * @return {@code true} if the console is coloured.
     */
    boolean isColoredConsole();

    /**
     * Returns whether the plugin prefix is prepended to log messages.
     *
     * @return {@code true} if the prefix is shown.
     */
    boolean isShowPrefix();

    /**
     * Returns whether operators bypass SIR permission checks.
     *
     * @return {@code true} if OP status grants all permissions.
     */
    boolean isOverrideOp();

    /**
     * Returns whether SIR checks the mute state before allowing chat.
     *
     * @return {@code true} if mute checks are active.
     */
    boolean isCheckMute();

    /**
     * Returns whether SIR falls back to default Bukkit chat/command methods.
     *
     * @return {@code true} if default Bukkit methods are used.
     */
    boolean isDefaultBukkitMethods();

    /**
     * Returns whether default jars of the specified type should be loaded automatically.
     *
     * @param type the jar type identifier (e.g., {@code "modules"}, {@code "commands"}).
     * @return {@code true} if default jars of that type are loaded.
     */
    boolean loadDefaultJars(String type);

    /**
     * Returns whether jars are always re-downloaded even if already present.
     *
     * @return {@code true} if jars are always updated.
     */
    boolean isAlwaysUpdateJars();

    /**
     * Returns the configuration key used to locate the plugin prefix in the language file.
     *
     * @return the prefix key.
     */
    String getPrefixKey();

    /**
     * Returns the resolved plugin prefix string (with colour codes applied).
     *
     * @return the plugin prefix.
     */
    String getPrefix();

    /**
     * Returns the prefix used for centered chat messages.
     *
     * @return the center prefix.
     */
    String getCenterPrefix();

    /**
     * Returns the pixel width used when centering in-game chat messages.
     *
     * @return the chat center width.
     */
    default int getChatCenterWidth() {
        return 154;
    }

    /**
     * Returns the string used as a visual line separator in chat output.
     *
     * @return the line separator.
     */
    String getLineSeparator();
}
