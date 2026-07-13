package com.bitaspire.sir.command;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@UtilityClass
class CommandMapCleaner {

    void purgeStaleEntries(SIRCommand command) {
        Map<String, Command> knownCommands = knownCommands();
        if (knownCommands == null) return;

        Set<String> labels = labels(command);
        if (labels.isEmpty()) return;

        Iterator<Map.Entry<String, Command>> iterator = knownCommands.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Command> entry = iterator.next();
            Command registered = entry.getValue();

            if (registered == command) {
                iterator.remove();
                continue;
            }

            if (!(registered instanceof SIRCommand)) continue;
            if (matchesLabel(entry.getKey(), labels)) iterator.remove();
        }
    }

    private Set<String> labels(SIRCommand command) {
        Set<String> labels = new HashSet<>();
        addLabel(labels, command.getName());

        List<String> aliases = command.getAliases();
        aliases.forEach(alias -> addLabel(labels, alias));

        return labels;
    }

    private void addLabel(Set<String> labels, String label) {
        if (StringUtils.isBlank(label)) return;
        labels.add(label.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesLabel(String entryKey, Set<String> labels) {
        String key = StringUtils.trimToEmpty(entryKey).toLowerCase(Locale.ROOT);
        if (labels.contains(key)) return true;

        int namespace = key.indexOf(':');
        return namespace >= 0 && labels.contains(key.substring(namespace + 1));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> knownCommands() {
        try {
            CommandMap commandMap = commandMap();
            if (!(commandMap instanceof SimpleCommandMap)) return null;

            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            Object value = field.get(commandMap);
            return value instanceof Map ? (Map<String, Command>) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private CommandMap commandMap() throws ReflectiveOperationException {
        Object server = Bukkit.getServer();
        Method method = server.getClass().getMethod("getCommandMap");
        method.setAccessible(true);
        return (CommandMap) method.invoke(server);
    }
}
