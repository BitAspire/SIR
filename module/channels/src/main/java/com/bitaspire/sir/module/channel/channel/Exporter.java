package com.bitaspire.sir.module.channel.channel;

import com.bitaspire.sir.ExtensionFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class Exporter {

    @NotNull
    YamlConfiguration export(@NotNull Layout layout) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 2);

        writeSection(layout.getDefaults(), config.createSection("default-channel"), true);

        ConfigurationSection channels = config.createSection("channels");
        for (String key : layout.getChannels().getKeys(false)) {
            ConfigurationSection source = layout.getChannels().getConfigurationSection(key);
            if (source == null) continue;

            ConfigurationSection target = channels.createSection(key);
            writeSection(source, target, false);

            ConfigurationSection local = source.getConfigurationSection("local");
            if (local == null) continue;

            String localKey = resolveLocalKey(channels, key, local.getString("name"));
            ConfigurationSection localTarget = channels.createSection(localKey);
            localTarget.set("inherits", key);
            writeSection(local, localTarget, false);
        }

        return config;
    }

    void persist(@NotNull ExtensionFile file, @NotNull Layout layout) {
        YamlConfiguration config = export(layout);
        ConfigurationSection target = file.getConfiguration();

        for (String key : new ArrayList<>(target.getKeys(false)))
            target.set(key, null);

        Resolver.copyLeaves(config, target);
        file.save();
    }

    private void writeSection(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target, boolean defaults) {
        setIfPresent(source, target, "enabled");
        setIfPresent(source, target, "description");
        setIfPresent(source, target, "priority");
        setIfPresent(source, target, "permission");
        setIfPresent(source, target, "group");
        setIfPresent(source, target, "inherits");

        writeAccess(source, target, defaults);
        writeAudience(source, target);
        writeStyle(source, target);
        writeLogging(source, target);
        writeGlobalOverride(source, target);
    }

    private void writeAccess(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target, boolean defaults) {
        ConfigurationSection access = target.createSection("access");
        access.set("default", source.getBoolean("access.default", defaults));
        access.set("strip-prefix", source.getBoolean("access.strip-prefix", true));

        List<String> prefixes = Resolver.prefixes(source.getConfigurationSection("access"));
        if (!prefixes.isEmpty())
            access.set("prefixes", prefixes);

        List<String> commands = Resolver.strings(source.getConfigurationSection("access"), "commands");
        if (!commands.isEmpty())
            access.set("commands", commands);
    }

    private void writeAudience(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target) {
        ConfigurationSection audience = target.createSection("audience");
        audience.set("radius", Math.max(0, source.getInt("radius", 0)));
        audience.set("same-world", source.getBoolean("same-world", false));
        audience.set("include-sender", source.getBoolean("include-sender", true));

        List<String> worlds = source.getStringList("worlds");
        if (!worlds.isEmpty())
            audience.set("worlds", worlds);

        setIfPresent(source, audience, "permission", "recipient-permission");
        setIfPresent(source, audience, "group", "recipient-group");
    }

    private void writeStyle(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target) {
        ConfigurationSection style = target.createSection("style");

        setIfPresent(source, style, "tag");
        setIfPresent(source, style, "prefix");
        setIfPresent(source, style, "suffix");
        setIfPresent(source, style, "format");

        List<String> hover = source.getStringList("hover");
        if (!hover.isEmpty())
            style.set("hover", hover);

        ConfigurationSection colors = style.createSection("colors");
        if (source.isSet("color"))
            colors.set("default", source.getString("color"));

        colors.set("normal", source.getBoolean("color-options.normal", false));
        colors.set("special", source.getBoolean("color-options.special", false));
        colors.set("rgb", source.getBoolean("color-options.rgb", false));

        writeClick(source, style);
    }

    private void writeClick(@NotNull ConfigurationSection source, @NotNull ConfigurationSection style) {
        String action = null;
        String value = null;

        if (source.isSet("click.action")) {
            action = source.getString("click.action");
            value = source.getString("click.input");
        }
        else {
            Object click = source.get("click-action");
            if (click == null) click = source.get("click");

            if (click instanceof String) {
                String[] split = ((String) click).replace("\"", "").split(":", 2);
                action = split.length > 0 ? split[0] : null;
                value = split.length > 1 ? split[1] : null;
            }
            else if (click instanceof ConfigurationSection) {
                ConfigurationSection section = (ConfigurationSection) click;
                action = section.getString("action");
                if (action == null) action = section.getString("type");

                value = section.getString("input");
                if (value == null) value = section.getString("value");
                if (value == null) value = section.getString("url");
            }
        }

        if (isBlank(action) && isBlank(value)) return;

        ConfigurationSection click = style.createSection("click");
        if (!isBlank(action)) click.set("action", action);
        if (!isBlank(value)) click.set("value", value);
    }

    private void writeLogging(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target) {
        ConfigurationSection logging = target.createSection("logging");
        logging.set("enabled", source.getBoolean("logging.enabled", false));
        setIfPresent(source, logging, "format", "logging.format");
    }

    private void writeGlobalOverride(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target) {
        if (!source.isSet("global")) return;

        boolean global = source.getBoolean("global");
        boolean derived = source.getInt("radius", 0) <= 0;

        if (global != derived)
            target.set("global", global);
    }

    private void setIfPresent(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target, @NotNull String path) {
        setIfPresent(source, target, path, path);
    }

    private void setIfPresent(
            @NotNull ConfigurationSection source,
            @NotNull ConfigurationSection target,
            @NotNull String targetPath,
            @NotNull String sourcePath
    ) {
        if (source.isSet(sourcePath))
            target.set(targetPath, source.get(sourcePath));
    }

    @NotNull
    private String resolveLocalKey(@NotNull ConfigurationSection channels, @NotNull String parentKey, @Nullable String preferred) {
        String base = isBlank(preferred) ? parentKey + "-local" : preferred.trim();
        String candidate = base;
        int index = 2;

        while (channels.isSet(candidate)) {
            candidate = base + "-" + index;
            index++;
        }

        return candidate;
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }
}
