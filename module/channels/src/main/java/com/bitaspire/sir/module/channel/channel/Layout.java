package com.bitaspire.sir.module.channel.channel;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

@Getter
public final class Layout {

    private final YamlConfiguration root;
    private final ConfigurationSection defaults;
    private final ConfigurationSection channels;

    public Layout(@NotNull YamlConfiguration root) {
        this.root = root;

        ConfigurationSection defaults = root.getConfigurationSection("default-channel");
        if (defaults == null) defaults = root.createSection("default-channel");
        this.defaults = defaults;

        ConfigurationSection channels = root.getConfigurationSection("channels");
        if (channels == null) channels = root.createSection("channels");
        this.channels = channels;
    }
}
