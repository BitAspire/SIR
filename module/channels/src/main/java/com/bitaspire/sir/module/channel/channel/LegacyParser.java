package com.bitaspire.sir.module.channel.channel;

import com.bitaspire.sir.file.ExtensionFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LegacyParser {

    private final ExtensionFile file;

    LegacyParser(@NotNull ExtensionFile file) {
        this.file = file;
    }

    Layout parse() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", 1);

        ConfigurationSection sourceDefault = file.getSection("default-channel");
        if (sourceDefault == null) sourceDefault = file.getSection("default");

        normalizeDefault(sourceDefault, config.createSection("default-channel"));

        ConfigurationSection channels = config.createSection("channels");
        ConfigurationSection sourceChannels = file.getSection("channels");

        if (sourceChannels != null)
            for (String key : sourceChannels.getKeys(false)) {
                ConfigurationSection source = sourceChannels.getConfigurationSection(key);
                if (source == null) continue;

                ConfigurationSection target = channels.createSection(key);
                normalizeEntry(source, target, false);

                ConfigurationSection sourceLocal = source.getConfigurationSection("local");
                if (sourceLocal == null) continue;

                ConfigurationSection local = target.createSection("local");
                normalizeEntry(sourceLocal, local, true);
                local.set("global", false);
                local.set("access.default", false);
            }

        return new Layout(config);
    }

    private void normalizeDefault(@Nullable ConfigurationSection source, @NotNull ConfigurationSection target) {
        Resolver.copyLeaves(source, target);
        Resolver.promoteModernAliases(target);

        Resolver.setIfAbsent(target, "enabled", true);
        Resolver.setIfAbsent(target, "radius", 0);
        Resolver.setIfAbsent(target, "global", target.getInt("radius", 0) <= 0);
        Resolver.setIfAbsent(target, "same-world", false);
        Resolver.setIfAbsent(target, "include-sender", true);
        Resolver.setIfAbsent(target, "access.default", true);
        Resolver.setIfAbsent(target, "access.strip-prefix", true);
        Resolver.setIfAbsent(target, "logging.enabled", false);

        Resolver.promotePrefixes(target.getConfigurationSection("access"));
        Resolver.mirrorAudiencePermissions(target);
    }

    private void normalizeEntry(@NotNull ConfigurationSection source, @NotNull ConfigurationSection target, boolean local) {
        Resolver.copyLeaves(source, target);
        Resolver.promoteModernAliases(target);

        Resolver.setIfAbsent(target, "enabled", true);
        Resolver.setIfAbsent(target, "same-world", false);
        Resolver.setIfAbsent(target, "include-sender", true);
        Resolver.setIfAbsent(target, "logging.enabled", false);
        Resolver.setIfAbsent(target, "access.strip-prefix", true);

        Resolver.promotePrefixes(target.getConfigurationSection("access"));
        Resolver.mirrorAudiencePermissions(target);

        if (!local)
            target.set("access.default",
                    !Resolver.hasExplicitAccess(source) && source.getBoolean("global", true)
            );
    }
}
