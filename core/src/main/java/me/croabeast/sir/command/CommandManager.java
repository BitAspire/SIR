package me.croabeast.sir.command;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.command.Synchronizer;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.MenuToggleable;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.takion.character.SmallCaps;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class CommandManager {

    private boolean deferPluginDependants = false;

    private final SIRApi api;
    private final ModuleManager moduleManager;

    @Getter
    private final Synchronizer synchronizer;

    private final Map<String, SIRCommand> commands = new LinkedHashMap<>();
    private final Map<String, ProviderState> states = new LinkedHashMap<>();

    private final Map<String, LoadedProvider> providers = new LinkedHashMap<>();
    private final Map<String, DeferredProvider> deferredProviders = new LinkedHashMap<>();

    private final Set<SIRModule> processedModules = Collections.newSetFromMap(new IdentityHashMap<>());

    public CommandManager(SIRApi api) {
        this.api = api;
        moduleManager = api.getModuleManager();
        synchronizer = new Synchronizer() {

            private GlobalTask task = null;

            private void cancel0(boolean reassign) {
                if (task == null) return;

                task.cancel();
                if (reassign) task = null;
            }

            @Override
            public void sync() {
                if (!api.getPlugin().isEnabled()) {
                    cancel();
                    return;
                }

                GlobalScheduler scheduler = api.getScheduler();
                scheduler.runTask(() -> {
                    cancel0(false);

                    task = scheduler.runTaskLater(() -> {
                        task = null;
                        Synchronizer.syncCommands();
                    }, 1L);
                });
            }

            @Override
            public void cancel() {
                cancel0(true);
            }
        };
    }

    @RequiredArgsConstructor
    private static class LoadedProvider {
        final ProviderLoader loader;
        final CommandProvider provider;
        final ProviderInformation information;
    }

    private static class DeferredProvider {
        final File jarFile;
        final String[] dependencies;

        DeferredProvider(File jarFile, String[] dependencies) {
            this.jarFile = jarFile;
            this.dependencies = dependencies;
        }
    }

    private static class ProviderState {
        boolean enabled;
        final Map<String, Boolean> overrides = new LinkedHashMap<>();

        ProviderState(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private void log(LogLevel level, String... messages) {
        api.getLibrary().getLogger().log(level, messages);
    }

    private void registerCommand(SIRCommand command, boolean syncCommands) {
        if (command == null) return;

        String name = command.getName();
        if (name.isEmpty()) return;

        String key = name.toLowerCase(Locale.ENGLISH);
        if (!command.isEnabled()) {
            log(LogLevel.INFO, "Command '" + name + "' is disabled, skipping registration.");
            return;
        }

        try {
            if (command.register(syncCommands)) {
                commands.put(key, command);
                return;
            }

            log(LogLevel.ERROR, "Failed to register command '" + key + "'");
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to register command '" + key + "'");
            e.printStackTrace();
        }
    }

    private ProviderInformation readFile(InputStream stream, String source) {
        if (stream == null) {
            log(LogLevel.WARN, "commands.yml not found for " + source + ", skipping.");
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new ProviderInformation(YamlConfiguration.loadConfiguration(reader));
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to read commands.yml from " + source);
            e.printStackTrace();
            return null;
        }
    }

    private void registerProvider(CommandProvider provider, ProviderInformation file, boolean syncCommands) {
        if (provider == null || file == null) return;

        ProviderState state = ensureProviderState(file.getName());
        Set<SIRCommand> set = provider.getCommands();
        if (set.isEmpty()) {
            log(LogLevel.WARN, "No commands declared for provider '" + file.getName() + "'.");
            return;
        }

        List<SIRCommand> pending = new ArrayList<>();

        for (SIRCommand command : set) {
            if (command == null) continue;

            String commandKey = command.getName();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }

            if (StringUtils.isBlank(commandKey)) {
                log(LogLevel.WARN, "Command with empty name found in provider '" + file.getName() + "', skipping.");
                continue;
            }

            String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
            if (file.hasNoCommand(nameKey)) {
                log(LogLevel.WARN, "Command '" + commandKey + "' not declared in commands.yml, skipping.");
                continue;
            }

            ConfigurationSection section = file.getCommandSection(nameKey);
            if (section == null) {
                log(LogLevel.WARN, "Command '" + commandKey + "' section is missing in commands.yml, skipping.");
                continue;
            }

            if (section.getBoolean("depends.enabled") && StringUtils.isNotBlank(section.getString("depends.parent"))) {
                pending.add(command);
                continue;
            }

            boolean override = resolveOverrideState(state, nameKey, section);
            command.applyFile(new CommandFile(nameKey, section, null, override));
            registerCommand(command, syncCommands);
        }

        for (SIRCommand command : pending) {
            String commandKey = command.getName();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }

            if (StringUtils.isBlank(commandKey)) {
                log(LogLevel.WARN, "Command with empty name found in provider '" + file.getName() + "', skipping.");
                continue;
            }

            String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
            if (file.hasNoCommand(nameKey)) {
                log(LogLevel.WARN, "Command '" + commandKey + "' not declared in commands.yml, skipping.");
                continue;
            }

            ConfigurationSection section = file.getCommandSection(nameKey);
            if (section == null) {
                log(LogLevel.WARN, "Command '" + commandKey + "' section is missing in commands.yml, skipping.");
                continue;
            }

            String parentName = section.getString("depends.parent");
            SIRCommand parent = getCommand(parentName);
            if (parent == null) {
                log(LogLevel.WARN, "Command '" + commandKey + "' depends on '" + parentName + "', but it isn't loaded.");
                continue;
            }

            if (!parent.isEnabled()) {
                log(LogLevel.INFO, "Command '" + commandKey + "' depends on disabled command '" + parentName + "', skipping.");
                continue;
            }

            boolean override = resolveOverrideState(state, nameKey, section);
            command.applyFile(new CommandFile(nameKey, section, parent, override));
            registerCommand(command, syncCommands);
        }
    }

    public void loadFromModule(@NotNull SIRModule module, boolean syncCommands) {
        if (processedModules.contains(module)) return;

        InputStream stream = module.getClass().getClassLoader().getResourceAsStream("commands.yml");
        ProviderInformation file = readFile(stream, module.getName());
        if (file == null) {
            processedModules.add(module);
            return;
        }

        if (!(module instanceof CommandProvider)) {
            log(LogLevel.WARN, "Module '" + module.getName() + "' contains commands.yml but is not a CommandProvider.");
            processedModules.add(module);
            return;
        }

        CommandProvider provider = (CommandProvider) module;
        ProviderState state = ensureProviderState(file.getName());
        if (!state.enabled) {
            log(LogLevel.INFO, "Command provider '" + file.getName() + "' is disabled, skipping registration.");
            processedModules.add(module);
            return;
        }

        registerProvider(provider, file, syncCommands);
        processedModules.add(module);
    }

    private ProviderInformation readExternalCommandsFile(File jarFile) {
        JarEntry entry;

        try (JarFile jar = new JarFile(jarFile)) {
            entry = jar.getJarEntry("commands.yml");
            if (entry == null) {
                log(LogLevel.WARN, "commands.yml not found in " + jarFile.getName() + ", skipping.");
                return null;
            }

            try (InputStream stream = jar.getInputStream(entry);
                  InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new ProviderInformation(YamlConfiguration.loadConfiguration(reader));
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to read commands.yml from " + jarFile.getName());
            e.printStackTrace();
            return null;
        }
    }

    public void load(File jarFile, boolean syncCommands) {
        log(LogLevel.INFO, "Loading command provider from " + jarFile.getName() + "...");

        ProviderInformation file = readExternalCommandsFile(jarFile);
        if (file == null) return;

        if (providers.containsKey(file.getName())) {
            log(LogLevel.WARN, "Command provider '" + file.getName() + "' already loaded, skipping.");
            return;
        }

        try {
            URL url = jarFile.toURI().toURL();
            ProviderLoader loader = new ProviderLoader(api, url);

            Class<?> clazz = Class.forName(file.getMain(), true, loader);
            if (!CommandProvider.class.isAssignableFrom(clazz)) {
                log(LogLevel.ERROR, "Main class '" + file.getMain() + "' does not extend CommandProvider, skipping...");
                loader.close();
                return;
            }

            CommandProvider provider = (CommandProvider) clazz.getDeclaredConstructor().newInstance();
            if (provider instanceof StandaloneProvider)
                ((StandaloneProvider) provider).init(api, loader, file);

            if (provider instanceof PluginDependant) {
                PluginDependant dependant = (PluginDependant) provider;
                if (deferPluginDependants) {
                    deferProvider(file.getName(), jarFile, dependant.getDependencies());
                    loader.close();
                    return;
                }

                if (!dependant.areDependenciesEnabled()) {
                    deferProvider(file.getName(), jarFile, dependant.getDependencies());
                    loader.close();
                    return;
                }
            }

            boolean enabled = provider.isEnabled();
            providers.put(file.getName(), new LoadedProvider(loader, provider, file));

            if (!enabled) {
                if (provider instanceof StandaloneProvider) ((StandaloneProvider) provider).registered = false;
                log(LogLevel.INFO, "Command provider '" + file.getName() + "' is disabled, skipping registration.");
                return;
            }

            if (!provider.register()) {
                log(LogLevel.WARN, "Failed to register command provider '" + file.getName() + "'.");
                unload(provider, syncCommands);
                return;
            }

            if (provider instanceof StandaloneProvider)
                ((StandaloneProvider) provider).registered = true;

            registerProvider(provider, file, syncCommands);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to load command provider from " + jarFile.getName());
            e.printStackTrace();
        }
    }

    public void loadAll() {
        loadStates();
        loadBundledJars(api.getConfiguration().loadDefaultJars("commands"));

        moduleManager.getModules().forEach(m -> loadFromModule(m, false));

        File dir = new File(api.getPlugin().getDataFolder(), "commands");
        if (!dir.exists() && !dir.mkdirs()) {
            log(LogLevel.WARN, "Could not create commands directory: " + dir.getPath());
            return;
        }

        File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            log(LogLevel.INFO, "No command providers found in " + dir.getPath());
            return;
        }

        deferPluginDependants = true;
        for (File jar : jars) load(jar, false);

        deferPluginDependants = false;
        retryDeferredProviders(null, false);
        synchronizer.sync();
    }

    private void deferProvider(String providerName, File jarFile, String[] dependencies) {
        if (deferredProviders.containsKey(providerName) || providers.containsKey(providerName))
            return;

        deferredProviders.put(providerName, new DeferredProvider(jarFile, dependencies));
        log(LogLevel.INFO, "Command provider '" + providerName + "' deferred until its dependencies are ready.");
    }

    public void retryDeferredProviders(String pluginName, boolean syncCommands) {
        if (deferredProviders.isEmpty()) return;

        Iterator<Map.Entry<String, DeferredProvider>> iterator = deferredProviders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DeferredProvider> entry = iterator.next();
            DeferredProvider deferred = entry.getValue();
            if (pluginName != null && !matchesDependency(pluginName, deferred.dependencies)) continue;

            load(deferred.jarFile, syncCommands);
            if (providers.containsKey(entry.getKey())) {
                iterator.remove();
            }
        }
    }

    private boolean matchesDependency(String pluginName, String[] dependencies) {
        if (pluginName == null || dependencies == null) return true;
        for (String dependency : dependencies) {
            if (dependency.equalsIgnoreCase(pluginName)) return true;
        }
        return false;
    }

    @NotNull
    public Set<SIRCommand> getCommands() {
        return new LinkedHashSet<>(commands.values());
    }

    public SIRCommand getCommand(String name) {
        return name == null ? null : commands.get(name.toLowerCase(Locale.ENGLISH));
    }

    public SettingsService getSettingsService() {
        LoadedProvider loaded = getLoadedProvider("SettingsProvider");

        if (loaded == null)
            for (LoadedProvider entry : providers.values())
                if (entry.provider instanceof SettingsService) {
                    loaded = entry;
                    break;
                }
        if (loaded == null) return null;

        StandaloneProvider standalone = loaded.provider instanceof StandaloneProvider ?
                (StandaloneProvider) loaded.provider : null;
        return standalone instanceof SettingsService ? (SettingsService) standalone : null;
    }

    @NotNull
    public Collection<CommandProvider> getProviders() {
        return providers.values().stream().map(entry -> entry.provider).collect(Collectors.toList());
    }

    @NotNull
    public Set<String> getProviderNames() {
        return new LinkedHashSet<>(providers.keySet());
    }

    public ProviderInformation getInformation(String name) {
        LoadedProvider loaded = getLoadedProvider(name);
        return loaded == null ? null : loaded.information;
    }

    @NotNull
    public Set<String> getProviderCommands(String providerName) {
        ProviderInformation info = getInformation(providerName);
        if (info == null || info.getCommands().isEmpty()) return Collections.emptySet();
        return new LinkedHashSet<>(info.getCommands().keySet());
    }

    public boolean updateProviderEnabled(String providerName, boolean enabled, boolean syncCommands) {
        LoadedProvider loaded = getLoadedProvider(providerName);
        if (loaded == null) return false;

        ProviderInformation info = loaded.information;
        boolean current = isProviderEnabled(info.getName());
        if (current == enabled) return true;

        setProviderEnabled(info.getName(), enabled);
        if (enabled) {
            registerProvider(loaded.provider, info, syncCommands);
        } else {
            unload(loaded.provider, syncCommands);
        }
        return true;
    }

    @NotNull
    public ChestBuilder getMenu() {
        List<MenuToggleable.Button> buttons = providers.values().stream()
                .map(entry -> entry.provider)
                .filter(StandaloneProvider.class::isInstance)
                .map(StandaloneProvider.class::cast)
                .map(StandaloneProvider::getButton)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int itemsPerRow = 5;
        int rowsOfItems = (buttons.size() + itemsPerRow - 1) / itemsPerRow;
        int rows = Math.min(6, Math.max(3, rowsOfItems + 2));

        String title = "&8" + SmallCaps.toSmallCaps("Loaded SIR Commands:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        menu.addSingleItem(
                0, 1, 1,
                ItemCreator.of(Material.BARRIER)
                        .modifyLore("&8More commands will be added soon.")
                        .modifyName("&c&lCOMING SOON...")
                        .setActionToEmpty()
                        .create(api.getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        for (int index = 0; index < buttons.size(); index++) {
            int row = index / itemsPerRow;
            if (row >= 4) break;

            int column = index % itemsPerRow;
            int x = 3 + column;
            int y = 1 + row;

            MenuToggleable.Button button = buttons.get(index);
            button.setSlot(Slot.fromXY(x, y));
            menu.addPane(0, button);
        }

        return menu;
    }

    public void openOverrideMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        api.getLibrary().getLoadedSender().setTargets(event.getWhoClicked())
                .setLogger(!(event.getWhoClicked() instanceof Player))
                .send("<P> &cThis option is only available on &fSIR+&c.");
    }

    public boolean isEnabled(String name) {
        SIRCommand command = getCommand(name);
        return command != null && command.isEnabled();
    }

    public void unload(String name, boolean syncCommands) {
        SIRCommand command = getCommand(name);
        if (command == null) {
            log(LogLevel.WARN, "Command '" + name + "' not found, skipping unload.");
            return;
        }

        String parentKey = name.toLowerCase(Locale.ENGLISH);
        for (SIRCommand loaded : new ArrayList<>(commands.values())) {
            if (!loaded.hasParent()) continue;

            SIRCommand parent = loaded.getParent();
            if (parent == null || !parentKey.equals(parent.getName().toLowerCase(Locale.ENGLISH)))
                continue;

            try {
                loaded.unregister(syncCommands);
            } catch (Exception e) {
                e.printStackTrace();
            }

            commands.remove(loaded.getName().toLowerCase(Locale.ENGLISH));
            log(LogLevel.INFO, "Disabled dependent command '" + loaded.getName() + "' (parent '" + name + "').");
        }

        try {
            command.unregister(syncCommands);
        } catch (Exception e) {
            e.printStackTrace();
        }

        commands.remove(name.toLowerCase(Locale.ENGLISH));
    }

    public void unload(CommandProvider provider, boolean syncCommands) {
        if (provider == null) return;

        for (SIRCommand command : provider.getCommands()) {
            if (command == null) continue;

            String name = command.getName();
            if (StringUtils.isBlank(name) || getCommand(name) == null) continue;

            unload(name, syncCommands);
        }
    }

    public void unloadAll() {
        for (String name : new ArrayList<>(commands.keySet()))
            unload(name, false);

        for (LoadedProvider entry : providers.values()) {
            ProviderLoader loader = entry.loader;
            if (loader == null) continue;
            try {
                loader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        providers.clear();
        processedModules.clear();

        synchronizer.sync();
    }

    public void saveStates() {
        File dir = new File(api.getPlugin().getDataFolder(), "commands");
        if (!dir.exists() && !dir.mkdirs()) {
            log(LogLevel.WARN, "Could not create commands directory: " + dir.getPath());
            return;
        }

        File file = new File(dir, "states.yml");
        YamlConfiguration configuration = getConfiguration();

        try {
            configuration.save(file);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to save command states to " + file.getPath());
            e.printStackTrace();
        }
    }

    private YamlConfiguration getConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();

        for (Map.Entry<String, ProviderState> entry : states.entrySet()) {
            String main = entry.getKey();
            ProviderState state = entry.getValue();
            String base = "providers." + main;
            configuration.set(base + ".enabled", state.enabled);

            for (Map.Entry<String, Boolean> overrideEntry : state.overrides.entrySet()) {
                configuration.set(base + ".commands." + overrideEntry.getKey(), overrideEntry.getValue());
            }
        }
        return configuration;
    }

    public void setProviderEnabled(String main, boolean enabled) {
        ensureProviderState(main).enabled = enabled;
    }

    public boolean isProviderEnabled(String main) {
        return ensureProviderState(main).enabled;
    }

    private void loadStates() {
        states.clear();

        File file = new File(api.getPlugin().getDataFolder(), "commands" + File.separator + "states.yml");
        if (!file.exists()) return;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection providersSection = configuration.getConfigurationSection("providers");
        if (providersSection == null) return;

        for (String name : providersSection.getKeys(false)) {
            if (StringUtils.isBlank(name)) continue;

            ConfigurationSection providerSection = providersSection.getConfigurationSection(name);
            if (providerSection == null) continue;

            ProviderState state = ensureProviderState(name);
            state.enabled = providerSection.getBoolean("enabled", true);

            ConfigurationSection commandsSection = providerSection.getConfigurationSection("commands");
            if (commandsSection == null) continue;

            for (String commandKey : commandsSection.getKeys(false)) {
                if (StringUtils.isBlank(commandKey)) continue;
                state.overrides.put(commandKey.toLowerCase(Locale.ENGLISH),
                        commandsSection.getBoolean(commandKey));
            }
        }
    }

    private LoadedProvider getLoadedProvider(String name) {
        if (StringUtils.isBlank(name)) return null;

        for (Map.Entry<String, LoadedProvider> entry : providers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
        }

        return null;
    }

    private ProviderState ensureProviderState(String main) {
        return states.computeIfAbsent(main, key -> new ProviderState(true));
    }

    private boolean resolveOverrideState(ProviderState state, String commandKey, ConfigurationSection section) {
        if (state == null || StringUtils.isBlank(commandKey)) {
            return section.getBoolean("override-existing", false);
        }

        String key = commandKey.toLowerCase(Locale.ENGLISH);
        Boolean override = state.overrides.get(key);
        if (override == null) {
            override = section.getBoolean("override-existing", false);
            state.overrides.put(key, override);
        }
        return override;
    }

    private List<String> findBundledJars() {
        List<String> results = new ArrayList<>();
        try {
            URL location = api.getPlugin().getClass().getProtectionDomain().getCodeSource().getLocation();
            if (location == null) return results;

            File source = new File(location.toURI());
            if (source.isFile()) {
                try (JarFile jar = new JarFile(source)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory()) continue;

                        String name = entry.getName();
                        if (name.startsWith("commands" + "/") && name.endsWith(".jar")) {
                            results.add(name);
                        }
                    }
                }
            } else if (source.isDirectory()) {
                File resourceDir = new File(source, "commands");
                File[] jars = resourceDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jars != null) {
                    for (File jar : jars) {
                        results.add("commands" + "/" + jar.getName());
                    }
                }
            }
        } catch (Exception e) {
            log(LogLevel.WARN, "Failed to inspect bundled command jars.");
            e.printStackTrace();
        }

        return results;
    }

    private void loadBundledJars(boolean saveDefaults) {
        List<String> bundled = findBundledJars();
        if (bundled.isEmpty()) return;

        File outputDir = new File(api.getPlugin().getDataFolder(), "commands");
        if (saveDefaults && !outputDir.exists() && !outputDir.mkdirs()) {
            log(LogLevel.WARN, "Could not create default commands directory: " + outputDir.getPath());
            saveDefaults = false;
        }

        for (String resource : bundled) {
            String fileName = resource.substring(("commands" + "/").length());
            File target;
            try {
                if (saveDefaults) {
                    target = new File(outputDir, fileName);
                } else {
                    target = File.createTempFile("sir-commands-", ".jar", api.getPlugin().getDataFolder());
                    target.deleteOnExit();
                }
            } catch (Exception e) {
                log(LogLevel.ERROR, "Failed to create temp file for bundled command '" + fileName + "'.");
                e.printStackTrace();
                continue;
            }

            if (!target.exists() || !saveDefaults) {
                try (InputStream stream = api.getPlugin().getClass().getClassLoader().getResourceAsStream(resource)) {
                    if (stream == null) {
                        log(LogLevel.WARN, "Bundled command jar '" + fileName + "' could not be found.");
                        continue;
                    }
                    Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    log(LogLevel.ERROR, "Failed to copy bundled command jar '" + fileName + "'.");
                    e.printStackTrace();
                    continue;
                }
            }

            if (!saveDefaults) load(target, false);
        }
    }
}
