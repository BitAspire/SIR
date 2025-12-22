package me.croabeast.sir;

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

    private boolean moduleJars = true,
            addonJars = true,
            commandJars = true;

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

            prefixKey = file.get("values.lang-prefix-key", prefixKey);
            prefix = file.get("values.lang-prefix", prefix);
            centerPrefix = file.get("values.center-prefix", centerPrefix);
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
}
