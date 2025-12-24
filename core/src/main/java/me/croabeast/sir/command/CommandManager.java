package me.croabeast.sir.command;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.RequiredArgsConstructor;
import me.croabeast.common.gui.ButtonBuilder;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.SlotCalculator;
import me.croabeast.sir.Toggleable;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.takion.character.SmallCaps;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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

    private final SIRApi api;
    private final ModuleManager moduleManager;

    private final Map<String, SIRCommand> commands = new LinkedHashMap<>();
    private final Map<String, LoadedProvider> providers = new LinkedHashMap<>();
    private final Map<String, ProviderState> states = new LinkedHashMap<>();
    private final Set<SIRModule> processedModules = Collections.newSetFromMap(new IdentityHashMap<>());

    private int slotCount = 0;

    public CommandManager(SIRApi api) {
        this.api = api;
        moduleManager = api.getModuleManager();
    }

    @RequiredArgsConstructor
    private static class LoadedProvider {
        final ProviderLoader loader;
        final CommandProvider provider;
        final ProviderInformation information;
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

    private void registerCommand(SIRCommand command) {
        if (command == null) return;

        String name = command.getName();
        if (name.isEmpty()) return;

        String key = name.toLowerCase(Locale.ENGLISH);
        if (!command.isEnabled()) {
            log(LogLevel.INFO, "Command '" + name + "' is disabled, skipping registration.");
            return;
        }

        try {
            if (command.register(true)) {
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

    private void registerProvider(CommandProvider provider, ProviderInformation file) {
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

            String commandKey = command.getCommandKey();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }

            if (StringUtils.isBlank(commandKey)) {
                log(LogLevel.WARN, "Command with empty name found in provider '" + file.getName() + "', skipping.");
                continue;
            }

            String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
            if (!file.hasCommand(nameKey)) {
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
            registerCommand(command);
        }

        for (SIRCommand command : pending) {
            String commandKey = command.getCommandKey();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }

            if (StringUtils.isBlank(commandKey)) {
                log(LogLevel.WARN, "Command with empty name found in provider '" + file.getName() + "', skipping.");
                continue;
            }

            String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
            if (!file.hasCommand(nameKey)) {
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
            registerCommand(command);
        }
    }

    public void loadFromModule(@NotNull SIRModule module) {
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

        registerProvider(provider, file);
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

    public void load(File jarFile) {
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

            file.setSlot(SlotCalculator.EXTENSION_LAYOUT.toSlot(slotCount++));

            CommandProvider provider = (CommandProvider) clazz.getDeclaredConstructor().newInstance();
            if (provider instanceof StandaloneProvider)
                ((StandaloneProvider) provider).init(api, loader, file);

            boolean enabled = provider.isEnabled();
            providers.put(file.getName(), new LoadedProvider(loader, provider, file));

            if (!enabled) {
                if (provider instanceof StandaloneProvider) ((StandaloneProvider) provider).registered = false;
                log(LogLevel.INFO, "Command provider '" + file.getName() + "' is disabled, skipping registration.");
                return;
            }

            if (!provider.register()) {
                log(LogLevel.WARN, "Failed to register command provider '" + file.getName() + "'.");
                unload(provider);
                return;
            }

            if (provider instanceof StandaloneProvider)
                ((StandaloneProvider) provider).registered = true;

            registerProvider(provider, file);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to load command provider from " + jarFile.getName());
            e.printStackTrace();
        }
    }

    public void loadAll() {
        loadStates();
        loadBundledJars(api.getConfiguration().loadDefaultJars("commands"));

        moduleManager.getModules().forEach(this::loadFromModule);

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

        for (File jar : jars) load(jar);
    }

    @NotNull
    public Set<SIRCommand> getCommands() {
        return new LinkedHashSet<>(commands.values());
    }

    public SIRCommand getCommand(String name) {
        if (name == null) return null;
        return commands.get(name.toLowerCase(Locale.ENGLISH));
    }

    public <C extends SIRCommand> C getCommand(Class<C> clazz) {
        try {
            if (!SIRCommand.class.isAssignableFrom(clazz)) return null;
        } catch (Exception e) {
            return null;
        }

        for (SIRCommand command : commands.values())
            if (clazz.isInstance(command)) return clazz.cast(command);

        return null;
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

    public boolean updateProviderEnabled(String providerName, boolean enabled) {
        LoadedProvider loaded = getLoadedProvider(providerName);
        if (loaded == null) return false;

        ProviderInformation info = loaded.information;
        boolean current = isProviderEnabled(info.getName());
        if (current == enabled) return true;

        setProviderEnabled(info.getName(), enabled);
        if (enabled) {
            registerProvider(loaded.provider, info);
        } else {
            unload(loaded.provider);
        }
        return true;
    }

    public Boolean getCommandOverride(String providerName, String commandKey) {
        LoadedProvider loaded = getLoadedProvider(providerName);
        if (loaded == null || StringUtils.isBlank(commandKey)) return null;

        String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
        ProviderInformation info = loaded.information;
        ConfigurationSection section = info.getCommandSection(nameKey);
        if (section == null) return null;

        ProviderState state = ensureProviderState(info.getName());
        return resolveOverrideState(state, nameKey, section);
    }

    public boolean updateCommandOverride(String providerName, String commandKey, boolean override) {
        LoadedProvider loaded = getLoadedProvider(providerName);
        if (loaded == null || StringUtils.isBlank(commandKey)) return false;

        String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
        ProviderInformation info = loaded.information;
        ConfigurationSection section = info.getCommandSection(nameKey);
        if (section == null) return false;

        ProviderState state = ensureProviderState(info.getName());
        state.overrides.put(nameKey, override);

        SIRCommand command = getProviderCommand(loaded.provider, nameKey);
        if (command == null) return true;

        SIRCommand parent = null;
        boolean depends = section.getBoolean("depends.enabled");
        String parentName = section.getString("depends.parent");
        if (depends && StringUtils.isNotBlank(parentName))
            parent = getCommand(parentName);

        command.applyFile(new CommandFile(nameKey, section, parent, override));
        if (!commands.containsKey(nameKey)) return true;

        try {
            command.unregister(true);
            command.register(true);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to re-register command '" + commandKey + "' after override change.");
            e.printStackTrace();
        }

        return true;
    }

    @NotNull
    public ChestBuilder getMenu() {
        List<Toggleable.Button> buttons = providers.values().stream()
                .map(entry -> entry.provider)
                .filter(StandaloneProvider.class::isInstance)
                .map(StandaloneProvider.class::cast)
                .map(StandaloneProvider::getButton)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int rows = SlotCalculator.EXTENSION_LAYOUT.getTotalRows(buttons.size()) / 9;
        String title = "&8" + SmallCaps.toSmallCaps("Loaded SIR Commands:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        menu.addSingleItem(
                0, 7, 2,
                ItemCreator.of(Material.BARRIER)
                        .modifyLore("&8More commands will be added soon.")
                        .modifyName("&c&lCOMING SOON...")
                        .setActionToEmpty()
                        .create(api.getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        for (Toggleable.Button button : buttons) {
            menu.addPane(0, button);
        }

        return menu;
    }

    public void openOverrideMenu(@NotNull StandaloneProvider provider, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!provider.isEnabled()) return;

        ProviderInformation info = provider.getInformation();

        Map<String, ConfigurationSection> commandSections = info.getCommands();
        if (commandSections.isEmpty()) return;

        Map<String, SIRCommand> commandMap = new LinkedHashMap<>();
        for (SIRCommand command : provider.getCommands()) {
            if (command == null) continue;

            String commandKey = StringUtils.isBlank(command.getCommandKey()) ? command.getName() : command.getCommandKey();
            if (StringUtils.isBlank(commandKey)) continue;

            commandMap.put(commandKey.toLowerCase(Locale.ENGLISH), command);
        }

        if (commandSections.size() == 1) {
            Map.Entry<String, ConfigurationSection> entry = commandSections.entrySet().iterator().next();

            String commandKey = entry.getKey();
            SIRCommand command = commandMap.get(commandKey.toLowerCase(Locale.ENGLISH));
            toggleOverride(info, commandKey, entry.getValue(), command);
            return;
        }

        showOverrideMenu(provider, info, commandSections, commandMap, event.getWhoClicked());
    }

    private void showOverrideMenu(StandaloneProvider provider,
                                  ProviderInformation info,
                                  Map<String, ConfigurationSection> commandSections,
                                  Map<String, SIRCommand> commandMap,
                                  org.bukkit.entity.HumanEntity viewer)
    {
        int itemsPerPage = 28;
        List<String> commandKeys = new ArrayList<>(commandSections.keySet());
        commandKeys.sort(String.CASE_INSENSITIVE_ORDER);

        int rows = SlotCalculator.CENTER_LAYOUT.getTotalRows(commandKeys.size()) / 9;
        String title = "&8" + SmallCaps.toSmallCaps(provider.getName() + " Overrides:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        for (int index = 0; index < commandKeys.size(); index++) {
            String commandKey = commandKeys.get(index);

            ConfigurationSection section = commandSections.get(commandKey);
            if (section == null) continue;

            int page = index / itemsPerPage, indexOnPage = index % itemsPerPage;
            Slot slot = SlotCalculator.CENTER_LAYOUT.toSlot(indexOnPage);
            if (slot == null) continue;

            ProviderState state = ensureProviderState(info.getMain());
            boolean override = resolveOverrideState(state, commandKey, section);

            String titleName = "/" + commandKey;
            String actionLine = "&f➤ &7Toggle override-existing";

            menu.addPane(page, ButtonBuilder
                    .of(api.getPlugin(), slot, override)
                    .setItem(
                            ItemCreator.of(Material.LIME_STAINED_GLASS_PANE)
                                    .modifyName("&7• &f" + SmallCaps.toSmallCaps(titleName) + ": &a&l✔")
                                    .modifyLore(actionLine, "&7Current: &aEnabled")
                                    .create(api.getPlugin()),
                            true
                    )
                    .setItem(
                            ItemCreator.of(Material.RED_STAINED_GLASS_PANE)
                                    .modifyName("&7• &f" + SmallCaps.toSmallCaps(titleName) + ": &c&l❌")
                                    .modifyLore(actionLine, "&7Current: &cDisabled")
                                    .create(api.getPlugin()),
                            false
                    )
                    .modify(button -> button.allowToggle(false))
                    .setAction(button -> click -> {
                        click.setCancelled(true);
                        toggleOverride(info, commandKey, section, commandMap.get(commandKey.toLowerCase(Locale.ENGLISH)));
                        showOverrideMenu(provider, info, commandSections, commandMap, click.getWhoClicked());
                    })
                    .getValue());
        }

        int bottomRow = rows - 1;
        menu.addSingleItem(
                0, 7, bottomRow,
                ItemCreator.of(Material.BARRIER).modifyName("&c&lClose")
                        .modifyLore("&7Close this menu.")
                        .setAction(e -> {
                            e.setCancelled(true);
                            e.getWhoClicked().closeInventory();
                        })
                        .create(api.getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        menu.showGui(viewer);
    }

    private void toggleOverride(ProviderInformation info,
                                String commandKey,
                                ConfigurationSection section,
                                SIRCommand command)
    {
        if (info == null || section == null) return;

        ProviderState state = ensureProviderState(info.getMain());
        String normalized = commandKey.toLowerCase(Locale.ENGLISH);

        boolean current = resolveOverrideState(state, normalized, section);
        boolean next = !current;
        state.overrides.put(normalized, next);

        if (command == null) command = getCommand(normalized);
        if (command == null) return;

        SIRCommand parent = null;

        boolean depends = section.getBoolean("depends.enabled");
        String parentName = section.getString("depends.parent");
        if (depends && StringUtils.isNotBlank(parentName)) parent = getCommand(parentName);

        command.applyFile(new CommandFile(normalized, section, parent, next));
        if (!commands.containsKey(normalized)) return;

        try {
            command.unregister(true);
            command.register(true);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to re-register command '" + normalized + "' after override change.");
            e.printStackTrace();
        }
    }

    public boolean isEnabled(String name) {
        SIRCommand command = getCommand(name);
        return command != null && command.isEnabled();
    }

    public void unload(String name) {
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
                loaded.unregister(true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            commands.remove(loaded.getName().toLowerCase(Locale.ENGLISH));
            log(LogLevel.INFO, "Disabled dependent command '" + loaded.getName() + "' (parent '" + name + "').");
        }

        try {
            command.unregister(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        commands.remove(name.toLowerCase(Locale.ENGLISH));
    }

    public void unload(CommandProvider provider) {
        if (provider == null) return;

        for (SIRCommand command : provider.getCommands()) {
            if (command == null) continue;

            String name = StringUtils.isBlank(command.getName()) ? command.getCommandKey() : command.getName();
            if (StringUtils.isBlank(name)) continue;

            unload(name);
        }
    }

    public void unloadAll() {
        for (String name : new ArrayList<>(commands.keySet()))
            unload(name);

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

    public void toggleOverrides(CommandProvider provider) {
        if (provider == null) return;

        LoadedProvider loaded = providers.values().stream()
                .filter(entry -> entry.provider == provider)
                .findFirst()
                .orElse(null);

        if (loaded == null) return;

        ProviderInformation info = loaded.information;
        ProviderState state = ensureProviderState(info.getName());

        for (SIRCommand command : provider.getCommands()) {
            if (command == null) continue;

            String commandKey = command.getCommandKey();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }
            if (StringUtils.isBlank(commandKey)) continue;

            String nameKey = commandKey.toLowerCase(Locale.ENGLISH);
            ConfigurationSection section = info.getCommandSection(nameKey);
            if (section == null) continue;

            boolean current = resolveOverrideState(state, nameKey, section);
            boolean next = !current;
            state.overrides.put(nameKey, next);

            SIRCommand parent = null;
            boolean depends = section.getBoolean("depends.enabled");
            String parentName = section.getString("depends.parent");
            if (depends && StringUtils.isNotBlank(parentName)) {
                parent = getCommand(parentName);
            }

            command.applyFile(new CommandFile(nameKey, section, parent, next));
            if (!commands.containsKey(nameKey)) continue;
            try {
                command.unregister(true);
                command.register(true);
            } catch (Exception e) {
                log(LogLevel.ERROR, "Failed to re-register command '" + commandKey + "' after override change.");
                e.printStackTrace();
            }
        }
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

    private SIRCommand getProviderCommand(CommandProvider provider, String nameKey) {
        if (provider == null || StringUtils.isBlank(nameKey)) return null;

        for (SIRCommand command : provider.getCommands()) {
            if (command == null) continue;

            String commandKey = command.getCommandKey();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }
            if (StringUtils.isBlank(commandKey)) continue;

            if (nameKey.equalsIgnoreCase(commandKey)) return command;
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

            if (!saveDefaults) load(target);
        }
    }
}
