package me.croabeast.sir.module;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.AnvilGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.croabeast.common.gui.ButtonBuilder;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.MenuToggleable;
import me.croabeast.sir.UserFormatter;
import me.croabeast.sir.command.CommandProvider;
import me.croabeast.takion.character.SmallCaps;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public final class ModuleManager {

    private final SIRApi api;

    private final Map<String, LoadedModule> modules = new LinkedHashMap<>();
    private final Map<String, DeferredModule> deferredModules = new LinkedHashMap<>();

    private final Map<String, Boolean> moduleStates = new LinkedHashMap<>();

    private final Map<UUID, BookEditSession> pendingBookEdits = new HashMap<>();
    private final Map<UUID, StringEditSession> pendingStringEdits = new HashMap<>();

    private boolean deferPluginDependants = false;

    public ModuleManager(SIRApi api) {
        this.api = api;
        new BookEditListener().register();
        new StringEditListener().register();
    }

    private static class ModuleCandidate {
        final File jarFile;
        final ModuleInformation file;

        ModuleCandidate(File jarFile, ModuleInformation file) {
            this.jarFile = jarFile;
            this.file = file;
        }
    }

    private static class LoadedModule {
        final SIRModule module;
        final URLClassLoader classLoader;

        LoadedModule(SIRModule module, URLClassLoader classLoader) {
            this.module = module;
            this.classLoader = classLoader;
        }
    }

    private static class DeferredModule {
        final ModuleCandidate candidate;
        final String[] dependencies;

        DeferredModule(ModuleCandidate candidate, String[] dependencies) {
            this.candidate = candidate;
            this.dependencies = dependencies;
        }
    }

    private static class BookEditSession {
        final SIRModule module;
        final File configFile;
        final String key;
        final String rootPath;

        BookEditSession(SIRModule module, File configFile, String key, String rootPath) {
            this.module = module;
            this.configFile = configFile;
            this.key = key;
            this.rootPath = rootPath;
        }
    }

    private static class StringEditSession {
        final SIRModule module;
        final File configFile;
        final String key;
        final String rootPath;

        StringEditSession(SIRModule module, File configFile, String key, String rootPath) {
            this.module = module;
            this.configFile = configFile;
            this.key = key;
            this.rootPath = rootPath;
        }
    }

    private final class BookEditListener extends me.croabeast.sir.Listener {
        @EventHandler
        void onEditBook(PlayerEditBookEvent event) {
            BookEditSession session = pendingBookEdits.remove(event.getPlayer().getUniqueId());
            if (session == null) return;

            BookMeta meta = event.getNewBookMeta();
            List<String> values = parseBookValues(meta.getPages());

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(session.configFile);
            configuration.set(session.key, values);
            saveConfiguration(configuration, session.configFile);

            api.getScheduler().runTask(
                    () -> showConfigMenu(session.module, event.getPlayer(), session.rootPath));
        }
    }

    private final class StringEditListener extends me.croabeast.sir.Listener {
        @EventHandler
        void onChatEdit(AsyncPlayerChatEvent event) {
            StringEditSession session = pendingStringEdits.remove(event.getPlayer().getUniqueId());
            if (session == null) return;

            event.setCancelled(true);
            String message = event.getMessage();

            api.getScheduler().runTask(() -> {
                Player player = event.getPlayer();
                if ("cancel".equalsIgnoreCase(message)) {
                    player.sendMessage(PrismaticAPI.colorize("&cEdit cancelled."));
                    showConfigMenu(session.module, player);
                    return;
                }

                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(session.configFile);
                configuration.set(session.key, message);
                saveConfiguration(configuration, session.configFile);
                showConfigMenu(session.module, player, session.rootPath);
            });
        }
    }

    @NotNull
    public List<SIRModule> getModules() {
        return modules.values().stream().map(l -> l.module).collect(Collectors.toList());
    }

    @NotNull
    public Set<String> getModuleNames() {
        Set<String> names = new LinkedHashSet<>(moduleStates.keySet());
        for (LoadedModule module : modules.values()) {
            names.add(module.module.getName());
        }
        return names;
    }

    public SIRModule getModule(@NotNull String name) {
        LoadedModule module = modules.get(name);
        return module == null || !isEnabled(name) ? null : module.module;
    }

    public <T> UserFormatter<T> getFormatter(@NotNull String name) {
        SIRModule module = getModule(name);
        return module instanceof UserFormatter ? (UserFormatter<T>) module : null;
    }

    public JoinQuitService getJoinQuitService() {
        SIRModule joinQuit = getModule("JoinQuit");
        return joinQuit == null ? null : (JoinQuitService) joinQuit;
    }

    public DiscordService getDiscordService() {
        SIRModule discord = getModule("Discord");
        return discord == null ? null : (DiscordService) discord;
    }

    void log(LogLevel level, String... messages) {
        api.getLibrary().getLogger().log(level, messages);
    }

    private ModuleCandidate createCandidate(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) {
                log(LogLevel.WARN, "module.yml not found in " + jarFile.getName() + ", skipping.");
                return null;
            }

            ModuleInformation file;
            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                file = new ModuleInformation(YamlConfiguration.loadConfiguration(reader));
            }

            return new ModuleCandidate(jarFile, file);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to read module.yml from " + jarFile.getName());
            e.printStackTrace();
            return null;
        }
    }

    private String hookMessage(String name, String[] plugins) {
        return "&7Module '" + name + "' can't be loaded since " +
                ((Function<String[], String>) strings -> {
                    final int length = strings.length;
                    if (length == 1)
                        return strings[0] + " is";

                    StringBuilder br = new StringBuilder();
                    for (int i = 0; i < length; i++) {
                        br.append(strings[i]);

                        if (i < length - 1)
                            br.append(i == length - 2 ?
                                    " or " : ", ");
                    }

                    return br.append(" are").toString();
                }).apply(plugins) + "n't installed in the server.";
    }

    private boolean loadCandidate(ModuleCandidate candidate, boolean syncCommands) {
        ModuleInformation file = candidate.file;
        String name = file.getName();
        File jarFile = candidate.jarFile;

        try {
            URL url = jarFile.toURI().toURL();

            ModuleLoader classLoader = new ModuleLoader(api, url);

            Class<?> clazz;
            try {
                clazz = Class.forName(file.getMain(), true, classLoader);
            } catch (NoClassDefFoundError e) {
                String missingClass = e.getMessage();
                log(LogLevel.INFO, "Module '" + name + "' deferred: missing class dependency '" + missingClass + "'.");
                String[] inferredDeps = inferPluginFromClass(missingClass);
                deferModule(candidate, inferredDeps);
                classLoader.close();
                return false;
            }

            if (!SIRModule.class.isAssignableFrom(clazz)) {
                log(LogLevel.ERROR, "Main class '" + file.getMain() + "' does not extend SIRModule, skipping...");
                classLoader.close();
                return false;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);

            SIRModule module = (SIRModule) constructor.newInstance();
            if (module instanceof PluginDependant) {
                PluginDependant dependant = (PluginDependant) module;
                String[] deps = dependant.getDependencies();

                if (deps.length > 0) {
                    if (deferPluginDependants) {
                        deferModule(candidate, deps);
                        classLoader.close();
                        return false;
                    }

                    if (!dependant.areDependenciesEnabled()) {
                        log(LogLevel.INFO, hookMessage(name, deps));
                        deferModule(candidate, deps);
                        classLoader.close();
                        return false;
                    }
                }
            }

            module.init(api, classLoader, file);
            modules.put(name, new LoadedModule(module, classLoader));

            if (!module.isEnabled()) {
                log(LogLevel.INFO, "Module '" + name + "' is disabled, skipping registration.");
                return true;
            }

            if (module.register()) {
                classLoader.module = module;
                module.setRegistered(true);
                log(LogLevel.INFO, "Module '" + name + "' loaded successfully.");

                if (module instanceof CommandProvider) {
                    try {
                        api.getCommandManager().loadFromModule(module, syncCommands);
                    } catch (Exception e) {
                        log(LogLevel.ERROR, "Failed to load commands for module '" + name + "'.");
                        e.printStackTrace();
                    }
                }
                return true;
            } else {
                unload(module.getName());
                return false;
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to load module from " + jarFile.getName());
            e.printStackTrace();
            return false;
        }
    }

    public boolean load(File jarFile, boolean syncCommands) {
        log(LogLevel.INFO, "Loading module from " + jarFile.getName() + "...");

        ModuleCandidate candidate = createCandidate(jarFile);
        if (candidate == null) return false;

        String key = candidate.file.getName();
        if (modules.containsKey(key)) {
            log(LogLevel.WARN, "Module with name '" + key + "' already loaded, skipping...");
            return false;
        }

        for (String dep : candidate.file.getDepend()) {
            if (!modules.containsKey(dep)) {
                log(LogLevel.WARN, "Module '" + candidate.file.getName()
                        + "' can't be loaded: missing hard dependency '" + dep + "'.");
                return false;
            }
        }

        for (String dep : candidate.file.getSoftDepend()) {
            if (!modules.containsKey(dep)) {
                log(LogLevel.INFO, "Soft dependency '" + dep + "' for module '"
                        + candidate.file.getName() + "' is not loaded.");
            }
        }

        return loadCandidate(candidate, syncCommands);
    }

    public void loadAll() {
        loadStates();
        loadBundledJars(api.getConfiguration().loadDefaultJars("modules"));

        File dir = new File(api.getPlugin().getDataFolder(), "modules");
        if (!dir.exists() && !dir.mkdirs())
            log(LogLevel.WARN, "Could not create modules directory: " + dir.getPath());

        File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            log(LogLevel.INFO, "No modules found in " + dir.getPath());
            return;
        }

        Map<String, ModuleCandidate> candidates = new LinkedHashMap<>();
        for (File jarFile : jars) {
            ModuleCandidate candidate = createCandidate(jarFile);
            if (candidate == null) continue;

            String key = candidate.file.getName();
            if (modules.containsKey(key)) {
                log(LogLevel.WARN, "Module with name '" + candidate.file.getName()
                        + "' already loaded, skipping '" + jarFile.getName() + "'.");
                continue;
            }

            if (candidates.containsKey(key)) {
                log(LogLevel.WARN, "Duplicate module name '" + candidate.file.getName()
                        + "' between '" + candidates.get(key).jarFile.getName()
                        + "' and '" + jarFile.getName() + "', skipping the latter.");
                continue;
            }

            candidates.put(key, candidate);
        }

        if (candidates.isEmpty()) {
            log(LogLevel.INFO, "No valid modules found in " + dir.getPath());
            return;
        }

        deferPluginDependants = true;

        Set<String> failed = new HashSet<>();
        boolean progress;
        try {
            do {
                progress = false;

                for (Map.Entry<String, ModuleCandidate> entry : candidates.entrySet()) {
                    String nameKey = entry.getKey();
                    if (modules.containsKey(nameKey) || failed.contains(nameKey)) continue;

                    ModuleInformation file = entry.getValue().file;

                    boolean wait = false;
                    boolean hardMissing = false;

                    for (String dep : file.getDepend()) {
                        if (modules.containsKey(dep)) continue;

                        if (candidates.containsKey(dep) && !failed.contains(dep)) {
                            wait = true;
                        } else {
                            log(LogLevel.WARN, "Module '" + file.getName()
                                    + "' can't be loaded: missing hard dependency '" + dep + "'.");
                            hardMissing = true;
                            break;
                        }
                    }

                    if (hardMissing) {
                        failed.add(nameKey);
                        continue;
                    }

                    if (wait) continue;

                    for (String dep : file.getSoftDepend()) {
                        if (modules.containsKey(dep)) continue;

                        if (candidates.containsKey(dep) && !failed.contains(dep)) {
                            wait = true;
                            break;
                        }
                    }
                    if (wait) continue;

                    if (!load(entry.getValue().jarFile, false)) failed.add(nameKey);
                    progress = true;
                }
            } while (progress);
        } finally {
            deferPluginDependants = false;
        }

        List<String> unresolved = candidates.keySet()
                .stream()
                .filter(name -> !modules.containsKey(name) && !failed.contains(name))
                .collect(Collectors.toList());

        if (!unresolved.isEmpty())
            log(LogLevel.ERROR, "Unresolved module dependency loop: " + String.join(", ", unresolved));

        retryDeferredModules(null, false);
    }

    private void deferModule(ModuleCandidate candidate, String[] dependencies) {
        String key = candidate.file.getName();
        if (deferredModules.containsKey(key) || modules.containsKey(key)) return;
        deferredModules.put(key, new DeferredModule(candidate, dependencies));
        log(LogLevel.INFO, "Module '" + key + "' deferred until its dependencies are ready.");
    }

    public void retryDeferredModules(String pluginName, boolean syncCommands) {
        if (deferredModules.isEmpty()) return;

        Iterator<Map.Entry<String, DeferredModule>> iterator = deferredModules.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DeferredModule> entry = iterator.next();

            DeferredModule deferred = entry.getValue();
            if (pluginName != null && !matchesDependency(pluginName, deferred.dependencies))
                continue;

            if (loadCandidate(deferred.candidate, syncCommands)) iterator.remove();
        }
    }

    private boolean matchesDependency(String pluginName, String[] dependencies) {
        if (pluginName == null || dependencies == null) return true;
        for (String dependency : dependencies) {
            if (dependency.equalsIgnoreCase(pluginName)) return true;
        }
        return false;
    }

    private String[] inferPluginFromClass(String className) {
        if (className == null) return new String[]{"Unknown"};

        // Normalize class name (replace / with .)
        String normalized = className.replace('/', '.');

        // Known plugin package mappings
        if (normalized.startsWith("me.clip.placeholderapi"))
            return new String[]{"PlaceholderAPI"};
        if (normalized.startsWith("github.scarsz.discordsrv") || normalized.startsWith("com.discordsrv"))
            return new String[]{"DiscordSRV"};
        if (normalized.startsWith("net.luckperms"))
            return new String[]{"LuckPerms"};
        if (normalized.startsWith("me.realized.tokenmanager"))
            return new String[]{"TokenManager"};
        if (normalized.startsWith("com.earth2me.essentials"))
            return new String[]{"Essentials"};
        if (normalized.startsWith("net.milkbowl.vault"))
            return new String[]{"Vault"};

        return new String[]{"Unknown"};
    }

    public void unload(String name) {
        LoadedModule loadedModule = modules.get(name);
        if (loadedModule == null) {
            log(LogLevel.WARN, "Module with name '" + name + "' not found, skipping unload.");
            return;
        }

        SIRModule module = loadedModule.module;
        try {
            if (module.unregister()) module.setRegistered(false);

            try {
                loadedModule.classLoader.close();
                String key = modules.keySet().stream()
                        .filter(entry -> entry.equalsIgnoreCase(name))
                        .findFirst()
                        .orElse(name);
                modules.remove(key);
                log(LogLevel.INFO, "Module '" + name + "' unloaded successfully.");
            } catch (Exception e) {
                log(LogLevel.ERROR, "Failed to close class loader for module '" + name + "'");
                e.printStackTrace();
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to unload module " + module.getName());
            e.printStackTrace();
        }
    }

    public void unloadAll() {
        for (String name : new ArrayList<>(modules.keySet())) unload(name);
    }

    @NotNull
    public ChestBuilder getMenu() {
        List<SIRModule> loadedModules = getModules();

        int itemsPerRow = 5;
        int rowsOfItems = (loadedModules.size() + itemsPerRow - 1) / itemsPerRow;
        int rows = Math.min(6, Math.max(3, rowsOfItems + 2));
        String title = "&8" + SmallCaps.toSmallCaps("Loaded SIR Modules:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        menu.addSingleItem(
                0, 1, 1,
                ItemCreator.of(Material.BARRIER)
                        .modifyLore("&8More modules will be added soon.")
                        .modifyName("&c&lCOMING SOON...")
                        .setActionToEmpty()
                        .create(api.getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        for (int index = 0; index < loadedModules.size(); index++) {
            int row = index / itemsPerRow;
            if (row >= 4) break;

            int column = index % itemsPerRow;
            int x = 3 + column;
            int y = 1 + row;

            SIRModule module = loadedModules.get(index);

            MenuToggleable.Button button = module.getButton();
            button.setEnabledItem(buildModuleItem(module, true));
            button.setDisabledItem(buildModuleItem(module, false));

            button.setSlot(Slot.fromXY(x, y));
            menu.addPane(0, button);
        }

        return menu;
    }

    public void saveStates() {
        File dir = new File(api.getPlugin().getDataFolder(), "modules");
        if (!dir.exists() && !dir.mkdirs()) {
            log(LogLevel.WARN, "Could not create modules directory: " + dir.getPath());
            return;
        }

        File file = new File(dir, "states.yml");
        YamlConfiguration configuration = new YamlConfiguration();

        for (Map.Entry<String, Boolean> entry : moduleStates.entrySet()) {
            configuration.set("modules." + entry.getKey() + ".enabled", entry.getValue());
        }

        try {
            configuration.save(file);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to save module states to " + file.getPath());
            e.printStackTrace();
        }
    }

    public void setModuleEnabled(String name, boolean enabled) {
        if (name != null) moduleStates.put(name, enabled);
    }

    public boolean isEnabled(String name) {
        return name != null && moduleStates.computeIfAbsent(name, key -> true);
    }

    public void updateModuleEnabled(String name, boolean enabled) {
        SIRModule module = getModule(name);
        if (module == null) return;

        if (module.isEnabled() != enabled)
            module.getButton().toggleAll();

        setModuleEnabled(module.getName(), enabled);
    }

    public void openConfigMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        api.getLibrary().getLoadedSender().setTargets(event.getWhoClicked())
                .setLogger(!(event.getWhoClicked() instanceof Player))
                .send("<P> &cThis option is only available on &fSIR+&c.");
    }

    private GuiItem buildModuleItem(SIRModule module, boolean enabled) {
        ModuleInformation info = module.getInformation();

        List<String> lore = new ArrayList<>();
        for (String line : info.getDescription()) {
            if (StringUtils.isBlank(line)) continue;
            lore.add("&7 " + SmallCaps.toSmallCaps(line));
        }

        lore.add("");
        lore.add("&f➤ &7Left-click: toggle module");

        Material material = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String title = SmallCaps.toSmallCaps(info.getTitle());
        String status = enabled ? " &a&l✔" : " &c&l❌";

        return ItemCreator.of(material)
                .modifyName("&7• &f" + title + ":" + status)
                .modifyLore(lore)
                .create(api.getPlugin());
    }

    private void showConfigMenu(@NotNull SIRModule module, @NotNull HumanEntity viewer) {
        showConfigMenu(module, viewer, null);
    }

    private void showConfigMenu(@NotNull SIRModule module, @NotNull HumanEntity viewer, @Nullable String rootPath) {
        File configFile = new File(module.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = rootPath == null ? configuration : configuration.getConfigurationSection(rootPath);
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        keys.removeIf(StringUtils::isBlank);
        keys.sort(String.CASE_INSENSITIVE_ORDER);

        int itemsPerPage = 28;
        int rows = getCenterMenuRows(keys.size());
        String title = "&8" + SmallCaps.toSmallCaps(module.getName() + " Config" + (rootPath == null ? ":" : " - " + rootPath + ":"));
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            String path = rootPath == null ? key : rootPath + "." + key;
            Object value = section.get(key);

            int page = index / itemsPerPage, indexOnPage = index % itemsPerPage;
            Slot slot = getCenterMenuSlot(indexOnPage);
            if (slot == null) continue;

            if (value instanceof Boolean) {
                boolean enabled = configuration.getBoolean(path);
                menu.addPane(page, ButtonBuilder
                        .of(api.getPlugin(), slot, enabled)
                        .setItem(
                                ItemCreator.of(Material.LIME_STAINED_GLASS_PANE)
                                        .modifyName("&7• &f" + SmallCaps.toSmallCaps(key) + ": &a&l✔")
                                        .modifyLore("&f➤ &7Toggle option", "&7Current: &aEnabled")
                                        .create(api.getPlugin()),
                                true
                        )
                        .setItem(
                                ItemCreator.of(Material.RED_STAINED_GLASS_PANE)
                                        .modifyName("&7• &f" + SmallCaps.toSmallCaps(key) + ": &c&l❌")
                                        .modifyLore("&f➤ &7Toggle option", "&7Current: &cDisabled")
                                        .create(api.getPlugin()),
                                false
                        )
                        .modify(button -> button.allowToggle(false))
                        .setAction(button -> click -> {
                            click.setCancelled(true);
                            boolean next = !button.isEnabled();
                            configuration.set(path, next);
                            saveConfiguration(configuration, configFile);
                            showConfigMenu(module, click.getWhoClicked(), rootPath);
                        })
                        .getValue());
            } else if (value instanceof List) {
                List<String> values = configuration.getStringList(path);
                menu.addSingleItem(
                        page, slot.getX(9), slot.getY(9),
                        ItemCreator.of(Material.WRITABLE_BOOK)
                                .modifyName("&7• &f" + SmallCaps.toSmallCaps(key) + ":")
                                .modifyLore(
                                        "&7Values: &f" + values.size(),
                                        "&f➤ &7Open book editor"
                                )
                                .setAction(click -> {
                                    click.setCancelled(true);
                                    openListEditor(module, configFile, path, values, click.getWhoClicked(), rootPath);
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            } else if (value instanceof ConfigurationSection) {
                ConfigurationSection child = section.getConfigurationSection(key);
                int childCount = child == null ? 0 : child.getKeys(false).size();
                menu.addSingleItem(
                        page, slot.getX(9), slot.getY(9),
                        ItemCreator.of(Material.BOOKSHELF)
                                .modifyName("&7• &f" + SmallCaps.toSmallCaps(key) + ":")
                                .modifyLore(
                                        "&7Keys: &f" + childCount,
                                        "&f➤ &7Open section"
                                )
                                .setAction(click -> {
                                    click.setCancelled(true);
                                    showConfigMenu(module, click.getWhoClicked(), path);
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            } else {
                String current = configuration.getString(path, "");
                menu.addSingleItem(
                        page, slot.getX(9), slot.getY(9),
                        ItemCreator.of(Material.PAPER)
                                .modifyName("&7• &f" + SmallCaps.toSmallCaps(key) + ":")
                                .modifyLore(
                                        "&7Current: &f" + (StringUtils.isBlank(current) ? "<empty>" : current),
                                        "&f➤ &7Edit value"
                                )
                                .setAction(click -> {
                                    click.setCancelled(true);
                                    openStringEditor(module, configFile, path, current, click.getWhoClicked(), rootPath);
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            }
        }

        int totalPages = Math.max(1, (keys.size() + itemsPerPage - 1) / itemsPerPage);
        int bottomRow = rows - 1;

        for (int page = 0; page < totalPages; page++) {
            if (page > 0) {
                int target = page - 1;
                menu.addSingleItem(
                        page, 1, bottomRow,
                        ItemCreator.of(Material.ARROW)
                                .modifyLore("&7Go to previous page.")
                                .modifyName("&e&lPrevious")
                                .setAction(click -> {
                                    click.setCancelled(true);
                                    menu.setDisplayedPage(target);
                                    menu.showGui(click.getWhoClicked());
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            }

            if (page < totalPages - 1) {
                int target = page + 1;
                menu.addSingleItem(
                        page, 7, bottomRow,
                        ItemCreator.of(Material.ARROW)
                                .modifyLore("&7Go to next page.")
                                .modifyName("&e&lNext")
                                .setAction(click -> {
                                    click.setCancelled(true);
                                    menu.setDisplayedPage(target);
                                    menu.showGui(click.getWhoClicked());
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            }

            if (rootPath != null) {
                String parentPath = rootPath.contains(".")
                        ? rootPath.substring(0, rootPath.lastIndexOf('.'))
                        : null;
                menu.addSingleItem(
                        page, 3, bottomRow,
                        ItemCreator.of(Material.ARROW)
                                .modifyLore("&7Return to the previous section.")
                                .modifyName("&a&lBack")
                                .setAction(e -> {
                                    e.setCancelled(true);
                                    showConfigMenu(module, e.getWhoClicked(), parentPath);
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );

                menu.addSingleItem(
                        page, 4, bottomRow,
                        ItemCreator.of(Material.ARROW)
                                .modifyLore("&7Return to the modules menu.")
                                .modifyName("&a&lBack to Modules")
                                .setAction(e -> {
                                    e.setCancelled(true);
                                    getMenu().showGui(e.getWhoClicked());
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            } else {
                menu.addSingleItem(
                        page, 3, bottomRow,
                        ItemCreator.of(Material.ARROW)
                                .modifyLore("&7Return to the modules menu.")
                                .modifyName("&a&lBack to Modules")
                                .setAction(e -> {
                                    e.setCancelled(true);
                                    getMenu().showGui(e.getWhoClicked());
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            }

            menu.addSingleItem(
                    page, 5, bottomRow,
                    ItemCreator.of(Material.BARRIER)
                            .modifyName("&c&lClose")
                            .modifyLore("&7Close this menu.")
                            .setAction(e -> {
                                e.setCancelled(true);
                                e.getWhoClicked().closeInventory();
                            })
                            .create(api.getPlugin()),
                    pane -> pane.setPriority(Pane.Priority.LOW)
            );
        }

        menu.showGui(viewer);
    }

    private static int getCenterMenuRows(int itemCount) {
        int itemsPerRow = 7;
        int rowsOfItems = (itemCount + itemsPerRow - 1) / itemsPerRow;
        return Math.max(1, Math.min(4, rowsOfItems)) + 2;
    }

    private static Slot getCenterMenuSlot(int index) {
        int itemsPerRow = 7;
        int row = index / itemsPerRow;
        if (row >= 4) return null;

        int column = index % itemsPerRow;
        int x = 1 + column;
        int y = 1 + row;
        return Slot.fromXY(x, y);
    }

    private void openStringEditor(SIRModule module, File configFile, String key, String current, HumanEntity viewer, String rootPath) {
        if (!(viewer instanceof Player)) return;

        Player player = (Player) viewer;
        try {
            AnvilGui anvilGui = new AnvilGui(PrismaticAPI.colorize("&8" + key), api.getPlugin());
            anvilGui.setCost((short) 0);

            InventoryComponent input = anvilGui.getFirstItemComponent();
            ItemStack base = new ItemStack(Material.PAPER);
            input.setItem(ItemCreator.of(base)
                    .modifyName("&f" + (StringUtils.isBlank(current) ? "<empty>" : current)).create(), 0, 0);

            InventoryComponent result = anvilGui.getResultComponent();
            GuiItem resultItem = ItemCreator.of(Material.LIME_STAINED_GLASS_PANE)
                    .modifyName("&a&lSave")
                    .modifyLore("&7Click to save the value.")
                    .setAction(click -> {
                        click.setCancelled(true);
                        String value = anvilGui.getRenameText();
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
                        configuration.set(key, value);
                        saveConfiguration(configuration, configFile);
                        showConfigMenu(module, click.getWhoClicked(), rootPath);
                    })
                    .create(api.getPlugin());
            result.setItem(resultItem, 0, 0);

            anvilGui.show(viewer);
        } catch (NoClassDefFoundError error) {
            startStringChatEditor(module, configFile, key, current, player, rootPath);
        }
    }

    private void startStringChatEditor(SIRModule module, File configFile, String key, String current, Player player, String rootPath) {
        pendingStringEdits.put(player.getUniqueId(), new StringEditSession(module, configFile, key, rootPath));
        player.closeInventory();
        player.sendMessage(PrismaticAPI.colorize("&7Type the new value for &f" + key + "&7."));
        player.sendMessage(PrismaticAPI.colorize("&7Current: &f" + (StringUtils.isBlank(current) ? "<empty>" : current)));
        player.sendMessage(PrismaticAPI.colorize("&7Type &c'cancel' &7to abort."));
    }

    private void openListEditor(SIRModule module, File configFile, String key, List<String> values, HumanEntity viewer, String rootPath) {
        if (!(viewer instanceof Player)) return;

        Player player = (Player) viewer;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        meta.setTitle(PrismaticAPI.colorize("&8" + key));
        meta.setAuthor(player.getName());
        meta.setPages(buildBookPages(values));
        book.setItemMeta(meta);

        pendingBookEdits.put(player.getUniqueId(), new BookEditSession(module, configFile, key, rootPath));
        player.openBook(book);
    }

    private List<String> buildBookPages(List<String> values) {
        List<String> pages = new ArrayList<>();
        StringBuilder current = new StringBuilder("# One value per line");
        int lineCount = 1;

        for (String value : values) {
            if (lineCount >= 12) {
                pages.add(current.toString());
                current = new StringBuilder();
                lineCount = 0;
            }
            if (lineCount > 0) {
                current.append("\n");
            }
            current.append(value == null ? "" : value);
            lineCount++;
        }

        pages.add(current.toString());
        return pages;
    }

    private List<String> parseBookValues(List<String> pages) {
        List<String> values = new ArrayList<>();
        for (String page : pages) {
            if (page == null) continue;
            for (String line : page.split("\n")) {
                if (StringUtils.isBlank(line)) continue;
                if (line.startsWith("#")) continue;
                values.add(line.trim());
            }
        }
        return values;
    }

    private void saveConfiguration(YamlConfiguration configuration, File configFile) {
        try {
            configuration.save(configFile);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to save config file: " + configFile.getPath());
            e.printStackTrace();
        }
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
                        if (name.startsWith("modules" + "/") && name.endsWith(".jar")) {
                            results.add(name);
                        }
                    }
                }
            } else if (source.isDirectory()) {
                File resourceDir = new File(source, "modules");
                File[] jars = resourceDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jars != null) {
                    for (File jar : jars) {
                        results.add("modules" + "/" + jar.getName());
                    }
                }
            }
        } catch (Exception e) {
            log(LogLevel.WARN, "Failed to inspect bundled module jars.");
            e.printStackTrace();
        }

        return results;
    }

    private void loadBundledJars(boolean saveDefaults) {
        List<String> bundled = findBundledJars();
        if (bundled.isEmpty()) return;

        File outputDir = new File(api.getPlugin().getDataFolder(), "modules");
        if (saveDefaults && !outputDir.exists() && !outputDir.mkdirs()) {
            log(LogLevel.WARN, "Could not create default modules directory: " + outputDir.getPath());
            saveDefaults = false;
        }

        for (String resource : bundled) {
            String fileName = resource.substring(("modules" + "/").length());
            File target;
            try {
                if (saveDefaults) {
                    target = new File(outputDir, fileName);
                } else {
                    target = File.createTempFile("sir-" + "modules" + "-", ".jar", api.getPlugin().getDataFolder());
                    target.deleteOnExit();
                }
            } catch (Exception e) {
                log(LogLevel.ERROR, "Failed to create temp file for bundled module '" + fileName + "'.");
                e.printStackTrace();
                continue;
            }

            if (!target.exists() || !saveDefaults) {
                try (InputStream stream = api.getPlugin().getClass().getClassLoader().getResourceAsStream(resource)) {
                    if (stream == null) {
                        log(LogLevel.WARN, "Bundled module jar '" + fileName + "' could not be found.");
                        continue;
                    }

                    Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    log(LogLevel.ERROR, "Failed to copy bundled module jar '" + fileName + "'.");
                    e.printStackTrace();
                    continue;
                }
            }

            if (!saveDefaults) load(target, false);
        }
    }

    private void loadStates() {
        moduleStates.clear();

        File file = new File(api.getPlugin().getDataFolder(), "modules" + File.separator + "states.yml");
        if (!file.exists()) return;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection("modules");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            if (name == null || name.trim().isEmpty()) continue;
            moduleStates.put(name, section.getBoolean(name + ".enabled", true));
        }
    }
}
