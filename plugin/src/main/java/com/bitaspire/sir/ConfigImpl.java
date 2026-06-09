package com.bitaspire.sir;

import com.bitaspire.sir.file.Config;
import lombok.Getter;
import me.croabeast.file.ConfigurableFile;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;

@Getter
final class ConfigImpl implements Config {

    private boolean updaterOnStart = true,
            updaterToOp = true,
            coloredConsole = true,
            showPrefix = true,
            overrideOp = false,
            checkMute = true,
            defaultBukkitMethods = false;

    private String prefixKey = "<P>",
            prefix = " &e&lSIR &8>&7",
            centerPrefix = "<C>",
            lineSeparator = "<n>";
    private int chatCenterWidth = 154;

    private boolean moduleJars = true,
            addonJars = true,
            commandJars = true,
            alwaysUpdateJars = true;

    private String startupLogConsole = "summary";
    private boolean startupLogDetails = true,
            startupLogLatestFolder = true;
    private int startupLogMaxSessions = 25;

    ConfigImpl(SIRPlugin main) {
        try {
            ConfigurableFile file = new ConfigurableFile(main, "config");
            file.saveDefaults();

            updaterOnStart = file.get("updater.on-start", true);
            updaterToOp = file.get("updater.send-op", true);

            coloredConsole = file.get("options.colored-console", true);
            showPrefix = file.get("options.show-prefix", true);
            overrideOp = file.get("options.override-op", false);
            checkMute = file.get("options.check-mute", true);
            defaultBukkitMethods = file.get("options.default-bukkit-plugin-methods", false);

            moduleJars = file.get("options.load-default-jars.modules", true);
            addonJars = file.get("options.load-default-jars.addons", true);
            commandJars = file.get("options.load-default-jars.commands", true);
            alwaysUpdateJars = file.get("options.load-default-jars.always-update", true);

            startupLogConsole = file.get("options.startup-logs.console", startupLogConsole);
            startupLogDetails = file.get("options.startup-logs.save-details", true);
            startupLogLatestFolder = file.get("options.startup-logs.latest-folder", true);
            startupLogMaxSessions = file.getConfiguration().getInt("options.startup-logs.max-sessions", 25);

            prefixKey = file.get("values.lang-prefix-key", prefixKey);
            prefix = file.get("values.lang-prefix", prefix);
            centerPrefix = file.get("values.center-prefix", centerPrefix);
            chatCenterWidth = file.getConfiguration().getInt("values.center-width.chat", chatCenterWidth);
            lineSeparator = file.get("values.line-separator", lineSeparator);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean loadDefaultJars(String type) {
        if (StringUtils.isBlank(type))
            return false;

        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "modules": return moduleJars;
            case "addons": return addonJars;
            case "commands": return commandJars;
            default: return true;
        }
    }

    @Override
    public boolean isAlwaysUpdateJars() {
        return alwaysUpdateJars;
    }
}
