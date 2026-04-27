package com.bitaspire.sir.module.channel.channel;

import com.bitaspire.sir.ExtensionFile;
import com.bitaspire.sir.module.channel.Channels;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class Loader {

    private static final String[] LEGACY_PATHS = {
            "global",
            "radius",
            "same-world",
            "worlds",
            "include-sender",
            "recipient-permission",
            "recipient-group",
            "tag",
            "prefix",
            "suffix",
            "color",
            "hover",
            "format",
            "click",
            "click-action",
            "click.action",
            "click.input",
            "log-format",
            "color-options",
            "access.prefix",
            "local"
    };

    private final Channels main;

    public Loader(@NotNull Channels main) {
        this.main = main;
    }

    @NotNull
    public Layout load() throws Exception {
        ExtensionFile file = new ExtensionFile(main, "channels", true);
        boolean legacyShape = hasLegacyShape(file);

        if (legacyShape) {
            info("Legacy or hybrid channels format detected. Rewriting channels.yml to channels v2.");

            Layout layout = new LegacyParser(file).parse();
            new Exporter().persist(file, layout);
            return new ModernParser(main, file).parse();
        }

        boolean changed = normalizeModernFile(file);
        Layout layout = new ModernParser(main, file).parse();

        if (changed)
            info("Channels file normalized to the canonical channels v2 format.");

        return layout;
    }

    private boolean isModern(@NotNull ExtensionFile file) {
        Object raw = file.get("version", (Object) null);
        if (raw == null) return false;

        if (raw instanceof Number)
            return ((Number) raw).intValue() >= 2;

        try {
            return Integer.parseInt(String.valueOf(raw).trim()) >= 2;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasLegacyShape(@NotNull ExtensionFile file) {
        return hasLegacySection(file.getSection("default-channel")) ||
                hasLegacySection(file.getSection("default")) ||
                hasLegacyChannels(file.getSection("channels"));
    }

    private boolean hasLegacyChannels(ConfigurationSection channels) {
        if (channels == null) return false;

        for (String key : channels.getKeys(false)) {
            ConfigurationSection section = channels.getConfigurationSection(key);
            if (hasLegacySection(section)) return true;
        }

        return false;
    }

    private boolean hasLegacySection(ConfigurationSection section) {
        if (section == null) return false;

        for (String path : LEGACY_PATHS)
            if (section.isSet(path))
                return true;

        return false;
    }

    private boolean normalizeModernFile(@NotNull ExtensionFile file) {
        boolean changed = false;
        ConfigurationSection root = file.getConfiguration();

        if (!isModern(file)) {
            root.set("version", 2);
            changed = true;
        }

        ConfigurationSection defaults = file.getSection("default");
        if (defaults != null) {
            ConfigurationSection target = root.getConfigurationSection("default-channel");
            if (target == null)
                target = root.createSection("default-channel");

            copyMissingLeaves(defaults, target);

            root.set("default", null);
            changed = true;
        }

        if (changed) file.save();
        return changed;
    }

    private void info(@NotNull String message) {
        main.getLogger().log(LogLevel.INFO, message);
    }

    private void copyMissingLeaves(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target) {
        for (String key : source.getKeys(true)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection || target.isSet(key)) continue;

            target.set(key, value);
        }
    }
}
