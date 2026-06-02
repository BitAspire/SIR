package com.bitaspire.sir;

import com.bitaspire.sir.command.SIRCommand;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class CommandRegistry {

    private final CommandManagerImpl manager;
    private final Map<String, SIRCommand> commands = new LinkedHashMap<>();

    CommandRegistry(CommandManagerImpl manager) {
        this.manager = manager;
    }

    void register(SIRCommand command, boolean syncCommands) {
        if (command == null || StringUtils.isBlank(command.getName())) return;

        String name = command.getName();
        String key = CommandManagerImpl.key(name);
        if (!command.isEnabled()) {
            manager.log(LogLevel.INFO, "Command '" + name + "' is disabled, skipping registration.");
            return;
        }

        try {
            if (command.register(syncCommands)) {
                commands.put(key, command);
                return;
            }

            manager.startupFailures.add(name);
            manager.log(LogLevel.ERROR, "Failed to register command '" + key + "'");
        } catch (Exception e) {
            manager.startupFailures.add(name);
            manager.log(LogLevel.ERROR, "Failed to register command '" + key + "'");
            e.printStackTrace();
        }
    }

    void unload(String name, boolean syncCommands) {
        SIRCommand command = get(name);
        if (command == null) {
            manager.log(LogLevel.WARN, "Command '" + name + "' not found, skipping unload.");
            return;
        }

        String parentKey = CommandManagerImpl.key(name);
        for (SIRCommand loaded : new ArrayList<>(commands.values())) {
            if (!loaded.hasParent()) continue;

            SIRCommand parent = loaded.getParent();
            if (parent == null || !parentKey.equals(CommandManagerImpl.key(parent.getName()))) continue;

            try {
                loaded.unregister(syncCommands);
            } catch (Exception e) {
                e.printStackTrace();
            }

            commands.remove(CommandManagerImpl.key(loaded.getName()));
            manager.log(LogLevel.INFO,
                    "Disabled dependent command '" + loaded.getName() + "' (parent '" + name + "').");
        }

        try {
            command.unregister(syncCommands);
        } catch (Exception e) {
            e.printStackTrace();
        }

        commands.remove(parentKey);
    }

    SIRCommand get(String name) {
        String key = CommandManagerImpl.key(name);
        return key.isEmpty() ? null : commands.get(key);
    }

    boolean contains(String name) {
        return commands.containsKey(CommandManagerImpl.key(name));
    }

    Set<SIRCommand> getAll() {
        return new LinkedHashSet<>(commands.values());
    }

    Set<String> getNames() {
        return new LinkedHashSet<>(commands.keySet());
    }

    void clear() {
        commands.clear();
    }
}
