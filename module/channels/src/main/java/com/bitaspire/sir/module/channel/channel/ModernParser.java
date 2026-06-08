package com.bitaspire.sir.module.channel.channel;

import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.module.channel.Channels;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ModernParser {

    private final Channels main;
    private final ConfigurationSection defaults;
    private final Map<String, ConfigurationSection> channels = new HashMap<>();
    private final Map<String, YamlConfiguration> cache = new HashMap<>();

    ModernParser(@NotNull Channels main, @NotNull ExtensionFile file) {
        this.main = main;

        ConfigurationSection defaults = file.getSection("default-channel");
        if (defaults == null) defaults = file.getSection("default");
        this.defaults = defaults;

        ConfigurationSection channels = file.getSection("channels");
        if (channels != null)
            for (String key : channels.getKeys(false)) {
                ConfigurationSection section = channels.getConfigurationSection(key);
                if (section != null) this.channels.put(key, section);
            }
    }

    Layout parse() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 2);

        ConfigurationSection targetDefault = config.createSection("default-channel");
        Resolver.copyLeaves(resolveDefault(), targetDefault);

        ConfigurationSection targetChannels = config.createSection("channels");
        for (String key : channels.keySet()) {
            ConfigurationSection section = targetChannels.createSection(key);
            Resolver.copyLeaves(resolve(key, new HashSet<>()), section);
        }

        return new Layout(config);
    }

    @NotNull
    private ConfigurationSection resolveDefault() {
        YamlConfiguration cached = cache.get("__default__");
        if (cached != null) return cached.getConfigurationSection("root");

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection target = config.createSection("root");

        Resolver.setIfAbsent(target, "enabled", true);
        Resolver.setIfAbsent(target, "radius", 0);
        Resolver.setIfAbsent(target, "same-world", false);
        Resolver.setIfAbsent(target, "include-sender", true);
        Resolver.setIfAbsent(target, "access.default", true);
        Resolver.setIfAbsent(target, "access.strip-prefix", true);
        Resolver.setIfAbsent(target, "logging.enabled", false);

        apply(defaults, target);
        finalize(target, true);

        cache.put("__default__", config);
        return target;
    }

    @NotNull
    private ConfigurationSection resolve(@NotNull String key, @NotNull Set<String> resolving) {
        YamlConfiguration cached = cache.get(key);
        if (cached != null) return cached.getConfigurationSection("root");

        ConfigurationSection source = channels.get(key);
        if (source == null) return resolveDefault();

        if (!resolving.add(key)) {
            warn("Circular inheritance detected while resolving '" + key + "'. Falling back to default-channel.");
            return resolveDefault();
        }

        ConfigurationSection parent;
        String inherit = StringUtils.trimToNull(source.getString("inherits"));

        if (inherit == null) {
            parent = resolveDefault();
        } else {
            ConfigurationSection inherited = channels.get(inherit);
            if (inherited == null) {
                warn("Channel '" + key + "' inherits unknown channel '" + inherit + "'. Falling back to default-channel.");
                parent = resolveDefault();
            } else {
                parent = resolve(inherit, resolving);
            }
        }

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection target = config.createSection("root");

        Resolver.copyLeaves(parent, target);
        // Fallback selection and global/local routing must be explicit per channel, not inherited from the base channel.
        target.set("access.default", null);
        target.set("global", null);
        apply(source, target);
        normalizeImplicitGlobalChannel(key, source, target);
        finalize(target, false);

        cache.put(key, config);
        resolving.remove(key);
        return target;
    }

    private void apply(@Nullable ConfigurationSection source, @NotNull ConfigurationSection target) {
        if (source == null) return;

        copy(source, target, "enabled");
        copy(source, target, "description");
        copy(source, target, "priority");
        copy(source, target, "global");
        copy(source, target, "permission");
        copy(source, target, "group");

        copy(source, target, "access.default", "access.default");
        copy(source, target, "access.strip-prefix", "access.strip-prefix");
        copy(source, target, "access.prefixes", "access.prefixes");
        copy(source, target, "access.commands", "access.commands");

        copy(source, target, "audience.radius", "radius");
        copy(source, target, "audience.same-world", "same-world");
        copy(source, target, "audience.worlds", "worlds");
        copy(source, target, "audience.permission", "recipient-permission");
        copy(source, target, "audience.group", "recipient-group");
        copy(source, target, "audience.include-sender", "include-sender");

        copy(source, target, "style.tag", "tag");
        copy(source, target, "style.prefix", "prefix");
        copy(source, target, "style.suffix", "suffix");
        copy(source, target, "style.colors.default", "color");
        copy(source, target, "style.colors.normal", "color-options.normal");
        copy(source, target, "style.colors.special", "color-options.special");
        copy(source, target, "style.colors.rgb", "color-options.rgb");
        copy(source, target, "style.hover", "hover");
        copy(source, target, "style.format", "format");
        copy(source, target, "style.click.action", "click.action");
        copy(source, target, "style.click.value", "click.input");

        copy(source, target, "logging.enabled", "logging.enabled");
        copy(source, target, "logging.format", "logging.format");
    }

    private void normalizeImplicitGlobalChannel(
            @NotNull String key,
            @NotNull ConfigurationSection source,
            @NotNull ConfigurationSection target
    ) {
        if (!key.matches("(?i)global")) return;
        if (source.isSet("global") || source.isSet("audience.radius") || source.isSet("radius")) return;

        target.set("radius", 0);
    }

    private void copy(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target, @NotNull String path) {
        copy(source, target, path, path);
    }

    private void copy(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target,
                      @NotNull String sourcePath, @NotNull String targetPath) {
        if (!source.isSet(sourcePath)) return;
        target.set(targetPath, source.get(sourcePath));
    }

    private void finalize(@NotNull ConfigurationSection target, boolean defaults) {
        Resolver.setIfAbsent(target, "enabled", true);
        Resolver.setIfAbsent(target, "radius", 0);
        Resolver.setIfAbsent(target, "same-world", false);
        Resolver.setIfAbsent(target, "include-sender", true);
        Resolver.setIfAbsent(target, "access.strip-prefix", true);
        Resolver.setIfAbsent(target, "logging.enabled", false);
        Resolver.setIfAbsent(target, "access.default", defaults);

        int radius = Math.max(0, target.getInt("radius", 0));
        target.set("radius", radius);

        if (!target.isSet("global"))
            target.set("global", radius <= 0);
        else
            target.set("global", target.getBoolean("global"));

        Resolver.promotePrefixes(target.getConfigurationSection("access"));
    }

    private void warn(String message) {
        main.getLogger().log(LogLevel.WARN, message);
    }
}
