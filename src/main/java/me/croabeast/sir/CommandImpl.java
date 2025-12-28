package me.croabeast.sir;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.Getter;
import me.croabeast.common.Registrable;
import me.croabeast.common.reflect.Reflector;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.sir.aspect.AspectButton;
import me.croabeast.sir.manager.CommandManager;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.takion.character.SmallCaps;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;

final class CommandImpl implements CommandManager {

    private final Map<String, SIRCommand> commands = new LinkedHashMap<>();
    private final Set<Class<?>> classes = new HashSet<>();

    @Getter
    private final ChestBuilder menu;
    private final SIRPlugin plugin;

    @Getter
    private boolean loaded = false;

    CommandImpl(SIRPlugin plugin) {
        this.plugin = plugin;
        final ClassLoader classLoader = plugin.classLoader();

        SIRPlugin.getJarEntries()
                .filter(s -> !s.contains("$") &&
                        s.startsWith("me/croabeast/sir/command") &&
                        s.endsWith(".class"))
                .apply(s -> s.replace('/', '.').replace(".class", ""))
                .forEach(s -> {
                    final Class<?> c;
                    try {
                        c = Class.forName(s, true, classLoader);
                    } catch (Exception e) {
                        plugin.getLibrary().getServerLogger().log(s + " can not be loaded.");
                        return;
                    }

                    if (Modifier.isFinal(c.getModifiers())) classes.add(c);
                });

        menu = ChestBuilder
                .of(plugin, 4, "&8" + SmallCaps.toSmallCaps("Loaded SIR Commands:"))
                .addSingleItem(
                        0, 1, 1,
                        ItemCreator.of(Material.BARREL)
                                .modifyLore(
                                        "&7Opens a new menu with all the available",
                                        "&7options from each command.",
                                        "&eComing soon in SIR+. &8" +
                                                SmallCaps.toSmallCaps("[Paid Version]")
                                )
                                .modifyName("&f&lCommands Options:")
                                .setActionToEmpty().create(plugin),
                        b -> b.setPriority(Pane.Priority.LOW)
                )
                .addSingleItem(
                        0, 7, 2,
                        ItemCreator.of(Material.BARRIER)
                                .modifyLore("&8More commands will be added soon.")
                                .modifyName("&c&lCOMING SOON...")
                                .setActionToEmpty().create(plugin),
                        b -> b.setPriority(Pane.Priority.LOW)
                );
    }

    final Set<Registrable> registrar = new HashSet<>();

    @Override
    public void load() {
        if (loaded) return;

        for (final Class<?> clazz : classes) {
            final Object init;
            try {
                init = Reflector.of(clazz).create();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            if (SIRCommand.class.isAssignableFrom(clazz)) {
                SIRCommand command = (SIRCommand) init;
                commands.put(command.getName(), command);
                continue;
            }

            if (!Commandable.class.isAssignableFrom(clazz))
                continue;

            Commandable commandable = (Commandable) init;
            if (init instanceof Registrable)
                registrar.add((Registrable) init);

            final Set<SIRCommand> set = commandable.getCommands();
            set.forEach(c -> commands.put(c.getName(), c));
        }

        plugin.getModuleManager()
                .getCommands()
                .forEach(c -> commands.put(c.getName(), c));

        Set<AspectButton> buttons = new HashSet<>();
        commands.values().forEach(c -> buttons.add(c.getButton()));

        buttons.forEach(b -> menu.addPane(0, b));
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
        commands.values().forEach(c -> c.register(false));
        registrar.forEach(Registrable::register);
        SIRCommand.scheduleSync();
        return true;
    }

    @Override
    public boolean unregister() {
        commands.values().forEach(c -> c.unregister(false));
        registrar.forEach(Registrable::unregister);
        SIRCommand.scheduleSync();
        return true;
    }

    @Nullable
    public SIRCommand fromName(String name) {
        return StringUtils.isBlank(name) ? null : commands.get(name);
    }

    @NotNull
    public Set<SIRCommand> getValues() {
        return new HashSet<>(commands.values());
    }
}
