package com.bitaspire.sir.command;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.file.Configurable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed configuration data for a single {@link SIRCommand}.
 *
 * <p> Loaded from a provider's {@code commands.yml} section and applied to the command
 * at registration time via {@link SIRCommand#applyFile(CommandFile)}.
 */
@Getter
public final class CommandFile {

    /** The command name (no leading slash). */
    private final String name;

    /** The Bukkit permission node required to execute the command. */
    private final String permission;

    /** Whether this command overrides an existing Bukkit/plugin command of the same name. */
    private final boolean override;

    /** Command aliases declared in the configuration. */
    private final List<String> aliases;

    /** Mapping of sub-command names to their individual permission nodes. */
    private final Map<String, String> subCommands;

    /** Short description shown in the command help. */
    private final String description;

    /** Usage hint shown on incorrect invocation. */
    private final String usage;

    /** Message sent when a player lacks permission. */
    private final String permissionMessage;

    /** Raw flag indicating whether a parent dependency is declared. */
    @Getter(AccessLevel.NONE)
    final boolean hasParent;

    /** The name of the parent command this command depends on, if any. */
    final String parentName;

    @ApiStatus.Internal
    public CommandFile(String name, ConfigurationSection section, SIRCommand parent, boolean override) {
        Objects.requireNonNull(section, "Configuration section cannot be null");

        this.name = Objects.requireNonNull(name, "Command name cannot be null");
        permission = section.getString("permission", "sir." + this.name);
        Objects.requireNonNull(permission, "Command permission cannot be null");

        aliases = Configurable.toStringList(section, "aliases");

        Map<String, String> subCommands = new LinkedHashMap<>();
        ConfigurationSection sub = section.getConfigurationSection("sub-commands");
        if (sub != null)
            sub.getKeys(false).forEach(key -> {
                String temp = sub.getString(key);
                if (StringUtils.isBlank(temp)) temp = permission + "." + key;
                subCommands.put(key, temp);
            });

        this.subCommands = new LinkedHashMap<>(subCommands);

        description = section.getString("description", "");
        usage = section.getString("usage", "/" + this.name);
        permissionMessage = section.getString("permission-message", "You do not have permission to execute this command.");

        this.hasParent = section.getBoolean("depends.enabled");
        parentName = section.getString("depends.parent");

        SIRCommand resolved = hasParent() && parent != null &&
                !parent.getName().equalsIgnoreCase(parentName) ? parent : null;

        boolean resolvedOverride = override;
        if (hasParent() && resolved != null) resolvedOverride = resolved.isOverriding();
        this.override = resolvedOverride;
    }

    /**
     * Returns whether this command has a valid parent dependency declared.
     *
     * @return {@code true} if a non-blank parent name is set and the dependency flag is enabled.
     */
    public boolean hasParent() {
        return hasParent && StringUtils.isNotBlank(parentName);
    }
}
