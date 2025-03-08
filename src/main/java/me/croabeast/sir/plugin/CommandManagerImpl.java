package me.croabeast.sir.plugin;

import lombok.Getter;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.sir.plugin.manager.CommandManager;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.gui.MenuCreator;
import me.croabeast.takion.logger.TakionLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;

final class CommandManagerImpl implements CommandManager {

    private final Map<String, SIRCommand> commands = new LinkedHashMap<>();
    private final Set<Class<?>> classes = new HashSet<>();

    private final SIRPlugin plugin;

    @Getter
    private boolean loaded = false;

    CommandManagerImpl(SIRPlugin plugin) {
        this.plugin = plugin;
        final ClassLoader classLoader = plugin.classLoader();

        SIRPlugin.getJarEntries()
                .filter(s -> !s.contains("$") &&
                        s.startsWith("me/croabeast/sir/plugin/command") &&
                        s.endsWith(".class"))
                .apply(s -> s.replace('/', '.').replace(".class", ""))
                .forEach(s -> {
                    final Class<?> c;
                    try {
                        c = Class.forName(s, true, classLoader);
                    } catch (Exception e) {
                        TakionLogger.getLogger().log(s + " can not be loaded.");
                        return;
                    }

                    if (Modifier.isFinal(c.getModifiers())) classes.add(c);
                });
    }

    @Override
    public void load() {
        if (loaded) return;

        for (final Class<?> clazz : classes) {
            final Object init = Reflector.of(clazz).create();

            if (SIRCommand.class.isAssignableFrom(clazz)) {
                SIRCommand command = (SIRCommand) init;

                if (!commands.containsValue(command))
                    commands.put(command.getName(), command);
                continue;
            }

            if (!Commandable.class.isAssignableFrom(clazz))
                continue;

            @SuppressWarnings("unchecked")
            Commandable<SIRCommand> c = (Commandable<SIRCommand>) init;

            final Set<SIRCommand> set = c.getCommands();
            set.forEach(command -> {
                if (!commands.containsValue(command))
                    commands.putIfAbsent(command.getName(), command);
            });
        }

        Set<SIRCommand> set = new HashSet<>();
        plugin.getModuleManager()
                .getCommands().values().forEach(set::addAll);

        set.forEach(command -> {
            if (!commands.containsValue(command))
                commands.putIfAbsent(command.getName(), command);
        });
        loaded = true;
    }

    @Override
    public void unload() {
        if (!loaded) return;

        unregister();
        commands.clear();

        loaded = false;
    }

    @Override
    public boolean register() {
        commands.values().forEach(SIRCommand::register);
        return true;
    }

    @Override
    public boolean unregister() {
        commands.values().forEach(SIRCommand::unregister);
        return true;
    }

    @Nullable
    public SIRCommand fromName(String name) {
        return commands.get(name);
    }

    @NotNull
    public Set<SIRCommand> getValues() {
        return new HashSet<>(commands.values());
    }

    @NotNull
    public MenuCreator getMenu() {
        return null;
    }
}
