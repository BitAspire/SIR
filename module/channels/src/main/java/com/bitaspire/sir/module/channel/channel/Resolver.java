package com.bitaspire.sir.module.channel.channel;

import lombok.experimental.UtilityClass;
import me.croabeast.file.Configurable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class Resolver {

    public void copyLeaves(@Nullable ConfigurationSection source, @NotNull ConfigurationSection target) {
        if (source == null) return;

        for (String key : source.getKeys(true)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection) continue;

            target.set(key, value);
        }
    }

    public void setIfAbsent(@NotNull ConfigurationSection section, @NotNull String path, Object value) {
        if (!section.isSet(path)) section.set(path, value);
    }

    @NotNull
    public List<String> strings(@Nullable ConfigurationSection section, @NotNull String path) {
        if (section == null) return new ArrayList<>();
        return Configurable.toStringList(section, path, new ArrayList<>());
    }

    public boolean hasExplicitAccess(@Nullable ConfigurationSection section) {
        if (section == null) return false;

        ConfigurationSection access = section.getConfigurationSection("access");
        if (access == null) return false;

        return !prefixes(access).isEmpty() || !strings(access, "commands").isEmpty();
    }

    @NotNull
    public List<String> prefixes(@Nullable ConfigurationSection access) {
        List<String> prefixes = strings(access, "prefixes");
        if (!prefixes.isEmpty()) {
            prefixes.removeIf(StringUtils::isBlank);
            prefixes.sort(Comparator.comparingInt(String::length).reversed());
            return prefixes;
        }

        String prefix = access == null ? null : StringUtils.trimToNull(access.getString("prefix"));
        if (prefix == null) return new ArrayList<>();

        List<String> list = new ArrayList<>();
        list.add(prefix);
        return list;
    }

    public void promotePrefixes(@Nullable ConfigurationSection access) {
        if (access == null) return;

        List<String> prefixes = prefixes(access);
        if (prefixes.isEmpty()) return;

        access.set("prefixes", prefixes);
        access.set("prefix", prefixes.get(0));
    }

    public void mirrorAudiencePermissions(@NotNull ConfigurationSection section, boolean implicit) {
        if (implicit && section.isSet("permission") && !section.isSet("recipient-permission"))
            section.set("recipient-permission", section.getString("permission"));

        if (implicit && section.isSet("group") && !section.isSet("recipient-group"))
            section.set("recipient-group", section.getString("group"));
    }

    public void promoteModernAliases(@NotNull ConfigurationSection section) {
        alias(section, "radius", "audience.radius");
        alias(section, "same-world", "audience.same-world");
        alias(section, "worlds", "audience.worlds");
        alias(section, "recipient-permission", "audience.permission");
        alias(section, "recipient-group", "audience.group");
        alias(section, "include-sender", "audience.include-sender");

        alias(section, "tag", "style.tag");
        alias(section, "prefix", "style.prefix");
        alias(section, "suffix", "style.suffix");
        alias(section, "color", "style.colors.default");
        alias(section, "color-options.normal", "style.colors.normal");
        alias(section, "color-options.special", "style.colors.special");
        alias(section, "color-options.rgb", "style.colors.rgb");
        alias(section, "color-options.mini-message", "style.colors.mini-message");
        alias(section, "hover", "style.hover");
        alias(section, "format", "style.format");
        alias(section, "click.action", "style.click.action");
        alias(section, "click.input", "style.click.value");
        alias(section, "click.input", "style.click.input");
    }

    private void alias(@NotNull ConfigurationSection section, @NotNull String targetPath, @NotNull String sourcePath) {
        if (!section.isSet(targetPath) && section.isSet(sourcePath))
            section.set(targetPath, section.get(sourcePath));
    }
}
