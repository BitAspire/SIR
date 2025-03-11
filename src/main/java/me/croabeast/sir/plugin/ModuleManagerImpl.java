package me.croabeast.sir.plugin;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.Registrable;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.sir.plugin.module.ModuleCommand;
import me.croabeast.sir.plugin.gui.MenuCreator;
import me.croabeast.sir.plugin.manager.ModuleManager;
import me.croabeast.sir.plugin.module.*;
import me.croabeast.sir.plugin.aspect.AspectButton;
import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.gui.ButtonCreator;
import me.croabeast.sir.plugin.gui.ItemCreator;
import me.croabeast.takion.character.SmallCaps;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("unchecked")
final class ModuleManagerImpl implements ModuleManager {

    private final List<Class<SIRModule>> classes = new ArrayList<>();
    private final ModuleMap modules = new ModuleMap();

    @Getter
    private final MenuCreator menu;
    @Getter
    private boolean loaded = false;

    @RequiredArgsConstructor
    @SuppressWarnings("all")
    static final class Type<T> {

        static final Type<Actionable> ACTIONABLE = new Type<>(Actionable.class);
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

        private final Map<AspectKey, ModuleHolder> map = new LinkedHashMap<>();

        void addModule(SIRModule module) {
            ModuleHolder holder = new ModuleHolder(module);
            if (map.containsValue(holder)) return;

            map.putIfAbsent(module.getAspectKey(), holder);
            menu.addPane(0, module.getButton());
        }

        @NotNull
        Set<SIRModule> getModules() {
            return CollectionBuilder.of(map.values()).map(m -> m.module).toSet();
        }

        @NotNull
        <T> Set<T> getModulesAsType(Type<T> type) {
            return CollectionBuilder.of(map.values()).map(m -> m.asType(type)).filter(Objects::nonNull).toSet();
        }

        @Nullable
        <T> T getAsType(AspectKey key, Type<T> type) {
            return map.get(key).asType(type);
        }
    }

    ModuleManagerImpl(SIRPlugin plugin) {
        final ClassLoader classLoader = plugin.classLoader();

        SIRPlugin.getJarEntries()
                .filter(s -> !s.contains("$") &&
                        s.startsWith("me/croabeast/sir/plugin/module") &&
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

        menu = MenuCreator
                .of(5, "&8" + SmallCaps.toSmallCaps("Loaded SIR Modules:"))
                .addSingleItem(
                        0, 1, 1, ItemCreator.of(Material.BARREL)
                                .modifyLore(
                                        "&7Opens a new menu with all the available",
                                        "&7options from each module.",
                                        "&eComing soon in SIR+. &8" +
                                                SmallCaps.toSmallCaps("[Paid Version]")
                                )
                                .modifyName("&f&lModules Options:").setActionToEmpty(),
                        b -> b.setPriority(Pane.Priority.LOW)
                )
                .addSingleItem(
                        0, 6, 3, ItemCreator.of(Material.BARRIER)
                                .modifyLore("&8More modules will be added soon.")
                                .modifyName("&c&lCOMING SOON...").setActionToEmpty(),
                        b -> b.setPriority(Pane.Priority.LOW)
                );
    }

    @Override
    public void load() {
        if (loaded) return;

        for (Class<SIRModule> clazz : classes) {
            try {
                modules.addModule(Reflector.of(clazz).create());
            } catch (Exception ignored) {}
        }

        final String message = "able all available modules.";
        menu.addPane(0, ButtonCreator
                .of(1, 2, false)
                .setItem(
                        ItemCreator.of(Material.LIME_DYE)
                                .modifyLore("&f➤ &7En" + message)
                                .modifyName("&a&lENABLE ALL:"),
                        true
                )
                .setItem(ItemCreator.of(Material.RED_DYE)
                                .modifyLore("&f➤ &7Dis" + message)
                                .modifyName("&c&lDISABLE ALL:"),
                        false
                )
                .modifyPane(b -> b.setPriority(Pane.Priority.LOW))
                .setAction(b -> event -> {
                    for (AspectButton button : CollectionBuilder
                            .of(modules.getModules())
                            .map(SIRModule::getButton).toList())
                    {
                        if (b.isEnabled() != button.isEnabled())
                            continue;

                        button.toggle();
                        button.getAction().accept(event);
                    }
                }));

        modules.getModulesAsType(Type.LOADABLE).forEach(loadable -> {
            loadable.load();

            if (!loadable.isPluginEnabled()) {
                AspectButton button = ((SIRModule) loadable).getButton();
                button.allowToggle(false);
                if (button.isEnabled()) button.toggle();
            }
        });
        loaded = true;
    }

    @Override
    public void unload() {
        if (!loaded) return;

        unregister();
        modules.getModulesAsType(Type.LOADABLE).forEach(HookLoadable::unload);

        modules.map.clear();
        loaded = false;
    }

    @Override
    public boolean register() {
        modules.getModules().forEach(Registrable::register);
        return true;
    }

    @Override
    public boolean unregister() {
        modules.getModules().forEach(Registrable::unregister);
        return true;
    }

    @NotNull
    public Set<SIRModule> getValues() {
        return modules.getModules();
    }

    @Override
    public SIRModule fromName(String name) {
        for (SIRModule module : modules.getModules())
            if (Objects.equals(name, module.getName()))
                return module;

        return null;
    }

    @Nullable
    public <S extends SIRModule> S getModule(AspectKey key) {
        ModuleHolder holder = modules.map.get(key);
        return holder != null ? (S) holder.module : null;
    }

    @Nullable
    public <T> PlayerFormatter<T> getFormatter(AspectKey key) {
        try {
            return (PlayerFormatter<T>) modules.getAsType(key, Type.FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public Actionable getActionable(AspectKey key) {
        return modules.getAsType(key, Type.ACTIONABLE);
    }

    @NotNull
    public Set<ModuleCommand> getCommands(AspectKey key) {
        Commandable<ModuleCommand> c = modules.getAsType(key, Type.COMMANDABLE);
        return c != null ? c.getCommands() : new HashSet<>();
    }

    @NotNull
    public Map<SIRModule, Set<ModuleCommand>> getCommands() {
        Map<SIRModule, Set<ModuleCommand>> commands = new HashMap<>();

        for (Commandable<ModuleCommand> c :
                modules.getModulesAsType(Type.COMMANDABLE))
            commands.put((SIRModule) c, c.getCommands());

        return commands;
    }
}
