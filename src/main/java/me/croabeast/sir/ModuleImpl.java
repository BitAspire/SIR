package me.croabeast.sir;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Registrable;
import me.croabeast.common.reflect.Reflector;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.sir.manager.ModuleManager;
import me.croabeast.sir.module.*;
import me.croabeast.sir.aspect.AspectButton;
import me.croabeast.sir.aspect.AspectKey;
import me.croabeast.common.gui.ButtonBuilder;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.takion.character.SmallCaps;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("unchecked")
final class ModuleImpl implements ModuleManager {

    private final List<Class<SIRModule>> classes = new ArrayList<>();
    private final ModuleMap moduleMap = new ModuleMap();

    private final SIRPlugin plugin;

    @Getter
    private final ChestBuilder menu;
    @Getter
    private boolean loaded = false;

    @RequiredArgsConstructor
    static final class Type<T> {

        static final Type<Actionable> ACTIONABLE = new Type<>(Actionable.class);
        @SuppressWarnings("all")
        static final Type<PlayerFormatter> FORMATTER = new Type<>(PlayerFormatter.class);
        static final Type<HookLoadable> LOADABLE = new Type<>(HookLoadable.class);
        static final Type<Commandable> COMMANDABLE = new Type<>(Commandable.class);

        private final Class<T> clazz;
    }

    static class ModuleHolder {

        private final Set<Type<?>> types = new HashSet<>();
        private final SIRModule module;

        ModuleHolder(SIRModule module) {
            this.module = module;

            if (module instanceof PlayerFormatter) types.add(Type.FORMATTER);
            if (module instanceof Actionable) types.add(Type.ACTIONABLE);
            if (module instanceof HookLoadable) types.add(Type.LOADABLE);
            if (module instanceof Commandable) types.add(Type.COMMANDABLE);
        }

        @Nullable
        <T> T asType(Type<T> type) {
            return types.contains(type) ? type.clazz.cast(module) : null;
        }
    }

    class ModuleMap {

        private final Map<AspectKey, ModuleHolder> data = new LinkedHashMap<>();

        void addModule(SIRModule module) {
            ModuleHolder holder = new ModuleHolder(module);
            if (data.containsValue(holder)) return;

            data.putIfAbsent(module.getKey(), holder);
            menu.addPane(0, module.getButton());
        }

        @NotNull
        Set<SIRModule> getModules() {
            return CollectionBuilder.of(data.values()).map(m -> m.module).toSet();
        }

        @NotNull
        <T> Set<T> asType(Type<T> type) {
            return CollectionBuilder.of(data.values()).map(m -> m.asType(type)).filter(Objects::nonNull).toSet();
        }

        @Nullable
        <T> T asType(AspectKey key, Type<T> type) {
            return data.get(key).asType(type);
        }
    }

    ModuleImpl(SIRPlugin plugin) {
        final ClassLoader classLoader = (this.plugin = plugin).classLoader();

        SIRPlugin.getJarEntries()
                .filter(s -> !s.contains("$") &&
                        s.startsWith("me/croabeast/sir/module") &&
                        s.endsWith(".class"))
                .apply(s -> s.replace('/', '.').replace(".class", ""))
                .forEach(s -> {
                    final Class<?> c;
                    try {
                        c = Class.forName(s, true, classLoader);
                    } catch (Exception e) {
                        return;
                    }

                    if (SIRModule.class.isAssignableFrom(c) &&
                            !Modifier.isAbstract(c.getModifiers()))
                        classes.add((Class<SIRModule>) c);
                });

        menu = ChestBuilder
                .of(plugin, 5, "&8" + SmallCaps.toSmallCaps("Loaded SIR Modules:"))
                .addSingleItem(
                        0, 1, 1,
                        ItemCreator.of(Material.BARREL)
                                .modifyLore(
                                        "&7Opens a new menu with all the available",
                                        "&7options from each module.",
                                        "&eComing soon in SIR+. &8" +
                                                SmallCaps.toSmallCaps("[Paid Version]")
                                )
                                .modifyName("&f&lModules Options:")
                                .setActionToEmpty().create(plugin),
                        b -> b.setPriority(Pane.Priority.LOW)
                )
                .addSingleItem(
                        0, 6, 3,
                        ItemCreator.of(Material.BARRIER)
                                .modifyLore("&8More modules will be added soon.")
                                .modifyName("&c&lCOMING SOON...")
                                .setActionToEmpty().create(plugin),
                        b -> b.setPriority(Pane.Priority.LOW)
                );
    }

    @Override
    public void load() {
        if (loaded) return;

        for (Class<SIRModule> clazz : classes) {
            try {
                moduleMap.addModule(Reflector.of(clazz).create());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final String message = "able all available modules.";
        menu.addPane(0, ButtonBuilder
                .of(plugin, 1, 2, false)
                .setItem(
                        ItemCreator.of(Material.LIME_DYE)
                                .modifyName("&a&l" +
                                       SmallCaps.toSmallCaps("ENABLE ALL") +
                                        ":")
                                .modifyLore("&f➤ &7En" + message).create(plugin),
                        true
                )
                .setItem(
                        ItemCreator.of(Material.RED_DYE)
                                .modifyName("&c&l" +
                                        SmallCaps.toSmallCaps("DISABLE ALL") +
                                        ":")
                                .modifyLore("&f➤ &7Dis" + message).create(plugin),
                        false
                )
                .modify(b -> b.setPriority(Pane.Priority.LOW))
                .setAction(b -> event -> {
                    for (SIRModule module : moduleMap.getModules())
                    {
                        if (module instanceof HookLoadable)
                            continue;

                        AspectButton button = module.getButton();
                        if (b.isEnabled() != button.isEnabled())
                            continue;

                        button.toggleAll();
                        button.getAction().accept(event);
                    }
                }).getValue());

        moduleMap.asType(Type.LOADABLE).forEach(loadable -> {
            loadable.load();
            if (loadable.isPluginEnabled()) return;

            SIRModule module = (SIRModule) loadable;
            AspectButton button = module.getButton();
            button.allowToggle(false);

            if (!button.isEnabled()) return;

            button.toggle();
            // Save the disabled state to file
            String path = "modules." + ((SIRModule.Key) module.getKey()).getFullName();
            module.getFile().set(path, false);
            module.getFile().save();
        });
        loaded = true;
    }

    @Override
    public void unload() {
        if (!loaded) return;

        unregister();
        moduleMap.asType(Type.LOADABLE).forEach(HookLoadable::unload);

        moduleMap.data.clear();
        loaded = false;
    }

    @Override
    public boolean register() {
        moduleMap.getModules().forEach(Registrable::register);
        return true;
    }

    @Override
    public boolean unregister() {
        moduleMap.getModules().forEach(Registrable::unregister);
        return true;
    }

    @NotNull
    public Set<SIRModule> getValues() {
        return moduleMap.getModules();
    }

    @Override
    public SIRModule fromName(String name) {
        for (SIRModule module : moduleMap.getModules())
            if (Objects.equals(name, module.getName()))
                return module;

        return null;
    }

    @Nullable
    public <S extends SIRModule> S getModule(AspectKey key) {
        ModuleHolder holder = moduleMap.data.get(key);
        return holder != null ? (S) holder.module : null;
    }

    @Nullable
    public <T> PlayerFormatter<T> getFormatter(AspectKey key) {
        try {
            return (PlayerFormatter<T>) moduleMap.asType(key, Type.FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public Actionable getActionable(AspectKey key) {
        return moduleMap.asType(key, Type.ACTIONABLE);
    }

    @NotNull
    public Set<SIRCommand> getCommands(AspectKey key) {
        Commandable c = moduleMap.asType(key, Type.COMMANDABLE);
        return c != null ? c.getCommands() : new HashSet<>();
    }

    @NotNull
    public Set<SIRCommand> getCommands() {
        Set<SIRCommand> commands = new HashSet<>();

        for (Commandable c : moduleMap.asType(Type.COMMANDABLE))
            commands.addAll(c.getCommands());

        return commands;
    }
}
