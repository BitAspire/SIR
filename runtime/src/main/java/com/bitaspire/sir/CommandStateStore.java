package com.bitaspire.sir;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

final class CommandStateStore {

    static final class ProviderState {
        boolean enabled;
        final Map<String, Boolean> overrides = new LinkedHashMap<>();

        ProviderState(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private final Map<String, ProviderState> states = new LinkedHashMap<>();

    ProviderState ensure(String providerName) {
        return states.computeIfAbsent(providerName, ignored -> new ProviderState(true));
    }

    boolean isEnabled(String providerName) {
        return ensure(providerName).enabled;
    }

    void setEnabled(String providerName, boolean enabled) {
        ensure(providerName).enabled = enabled;
    }

    boolean resolveOverride(ProviderState state, String commandKey, ConfigurationSection section) {
        if (state == null || StringUtils.isBlank(commandKey)) {
            return section.getBoolean("override-existing", false);
        }

        String key = CommandManagerImpl.key(commandKey);
        Boolean override = state.overrides.get(key);
        if (override == null) {
            override = section.getBoolean("override-existing", false);
            state.overrides.put(key, override);
        }
        return override;
    }

    void load(File file) {
        states.clear();
        if (!file.exists()) return;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection providersSection = configuration.getConfigurationSection("providers");
        if (providersSection == null) return;

        for (String name : providersSection.getKeys(false)) {
            if (StringUtils.isBlank(name)) continue;

            ConfigurationSection providerSection = providersSection.getConfigurationSection(name);
            if (providerSection == null) continue;

            ProviderState state = ensure(name);
            state.enabled = providerSection.getBoolean("enabled", true);

            ConfigurationSection commandsSection = providerSection.getConfigurationSection("commands");
            if (commandsSection == null) continue;

            for (String commandKey : commandsSection.getKeys(false)) {
                if (StringUtils.isBlank(commandKey)) continue;
                state.overrides.put(CommandManagerImpl.key(commandKey), commandsSection.getBoolean(commandKey));
            }
        }
    }

    YamlConfiguration toConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<String, ProviderState> entry : states.entrySet()) {
            String base = "providers." + entry.getKey();
            ProviderState state = entry.getValue();
            configuration.set(base + ".enabled", state.enabled);

            for (Map.Entry<String, Boolean> override : state.overrides.entrySet())
                configuration.set(base + ".commands." + override.getKey(), override.getValue());
        }
        return configuration;
    }
}
