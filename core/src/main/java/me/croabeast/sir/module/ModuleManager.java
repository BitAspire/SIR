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
import me.croabeast.sir.SlotCalculator;
import me.croabeast.sir.Toggleable;
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
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

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
    private final Map<String, Boolean> moduleStates = new LinkedHashMap<>();
    private final Map<UUID, BookEditSession> pendingBookEdits = new HashMap<>();

    public ModuleManager(SIRApi api) {
        this.api = api;
        api.getPlugin().getServer().getPluginManager().registerEvents(new BookEditListener(), api.getPlugin());
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

    private static class BookEditSession {
        final SIRModule module;
        final File configFile;
        final String key;

        BookEditSession(SIRModule module, File configFile, String key) {
            this.module = module;
            this.configFile = configFile;
            this.key = key;
        }
    }

    private final class BookEditListener implements Listener {
        @EventHandler
        void onEditBook(PlayerEditBookEvent event) {
            BookEditSession session = pendingBookEdits.remove(event.getPlayer().getUniqueId());
            if (session == null) return;

            BookMeta meta = event.getNewBookMeta();
            List<String> values = parseBookValues(meta.getPages());

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(session.configFile);
            configuration.set(session.key, values);
            saveConfiguration(configuration, session.configFile);

            api.getPlugin().getServer().getScheduler().runTask(api.getPlugin(),
                    () -> showConfigMenu(session.module, event.getPlayer()));
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

    public <M extends SIRModule> M getModule(@NotNull String name) {
        LoadedModule module = modules.get(name);
        try {
            return module == null ? null : (M) module.module;
        } catch (Exception e) {
            return null;
        }
    }

    public <M extends SIRModule> M getModule(Class<M> clazz) {
        try {
            if (!SIRModule.class.isAssignableFrom(clazz))
                return null;
        } catch (Exception e) {
            return null;
        }

        ClassLoader loader = clazz.getClassLoader();
        if (!(loader instanceof ModuleLoader)) return null;

        SIRModule module = ((ModuleLoader) loader).module;
        try {
            return module == null ? null : (M) module;
        } catch (Exception e) {
            return null;
        }
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

    private void loadCandidate(ModuleCandidate candidate) {
        ModuleInformation file = candidate.file;
        String name = file.getName();
        File jarFile = candidate.jarFile;

        try {
            URL url = jarFile.toURI().toURL();

            ModuleLoader classLoader = new ModuleLoader(api, url);

            Class<?> clazz = Class.forName(file.getMain(), true, classLoader);
            if (!SIRModule.class.isAssignableFrom(clazz)) {
                log(LogLevel.ERROR, "Main class '" + file.getMain() + "' does not extend SIRModule, skipping...");
                classLoader.close();
                return;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);

            SIRModule module = (SIRModule) constructor.newInstance();
            if (module instanceof PluginDependant && !((PluginDependant) module).isPluginEnabled()) {
                log(LogLevel.INFO, hookMessage(name, ((PluginDependant) module).getDependencies()));
                classLoader.close();
                return;
            }

            module.init(api, classLoader, file);
            modules.put(name, new LoadedModule(module, classLoader));

            if (!module.isEnabled()) {
                log(LogLevel.INFO, "Module '" + name + "' is disabled, skipping registration.");
                return;
            }

            if (module.register()) {
                classLoader.module = module;
                module.setRegistered(true);
                log(LogLevel.INFO, "Module '" + name + "' loaded successfully.");

                if (module instanceof CommandProvider) {
                    try {
                        api.getCommandManager().loadFromModule(module);
                    } catch (Exception e) {
                        log(LogLevel.ERROR, "Failed to load commands for module '" + name + "'.");
                        e.printStackTrace();
                    }
                }
            } else {
                unload(module.getName());
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to load module from " + jarFile.getName());
            e.printStackTrace();
        }
    }

    public void load(File jarFile) {
        log(LogLevel.INFO, "Loading module from " + jarFile.getName() + "...");

        ModuleCandidate candidate = createCandidate(jarFile);
        if (candidate == null) return;

        String key = candidate.file.getName();
        if (modules.containsKey(key)) {
            log(LogLevel.WARN, "Module with name '" + key + "' already loaded, skipping...");
            return;
        }

        for (String dep : candidate.file.getDepend()) {
            if (!modules.containsKey(dep)) {
                log(LogLevel.WARN, "Module '" + candidate.file.getName()
                        + "' can't be loaded: missing hard dependency '" + dep + "'.");
                return;
            }
        }

        for (String dep : candidate.file.getSoftDepend()) {
            if (!modules.containsKey(dep)) {
                log(LogLevel.INFO, "Soft dependency '" + dep + "' for module '"
                        + candidate.file.getName() + "' is not loaded.");
            }
        }

        loadCandidate(candidate);
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

        Set<String> failed = new HashSet<>();
        boolean progress;

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

                load(entry.getValue().jarFile);
                progress = true;
            }
        } while (progress);

        List<String> unresolved = candidates.keySet()
                .stream()
                .filter(name -> !modules.containsKey(name) && !failed.contains(name))
                .collect(Collectors.toList());

        if (!unresolved.isEmpty())
            log(LogLevel.ERROR, "Unresolved module dependency loop: " + String.join(", ", unresolved));
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
                modules.remove(name.toLowerCase());
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

        int rows = SlotCalculator.EXTENSION_LAYOUT.getTotalRows(loadedModules.size()) / 9;
        String title = "&8" + SmallCaps.toSmallCaps("Loaded SIR Modules:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        int infoRow = Math.min(rows - 1, 3);
        menu.addSingleItem(
                0, 6, infoRow,
                ItemCreator.of(Material.BARRIER)
                        .modifyLore("&8More modules will be added soon.")
                        .modifyName("&c&lCOMING SOON...")
                        .setActionToEmpty()
                        .create(api.getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        for (SIRModule module : loadedModules) {
            Toggleable.Button button = module.getButton();
            boolean hasConfig = hasConfigFile(module);
            button.setEnabledItem(buildModuleItem(module, true, hasConfig));
            button.setDisabledItem(buildModuleItem(module, false, hasConfig));
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

    public void openConfigMenu(@NotNull SIRModule module, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        showConfigMenu(module, event.getWhoClicked());
    }

    private boolean hasConfigFile(SIRModule module) {
        return new File(module.getDataFolder(), "config.yml").exists();
    }

    private GuiItem buildModuleItem(SIRModule module, boolean enabled, boolean hasConfig) {
        ModuleInformation info = module.getInformation();

        List<String> lore = new ArrayList<>();
        for (String line : info.getDescription()) {
            if (StringUtils.isBlank(line)) continue;
            lore.add("&7 " + SmallCaps.toSmallCaps(line));
        }

        lore.add("");
        lore.add("&f➤ &7Left-click: toggle module");
        if (hasConfig)
            lore.add("&f➤ &7Right-click: open config");

        Material material = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String title = SmallCaps.toSmallCaps(info.getTitle());
        String status = enabled ? " &a&l✔" : " &c&l❌";

        return ItemCreator.of(material)
                .modifyName("&7• &f" + title + ":" + status)
                .modifyLore(lore)
                .create(api.getPlugin());
    }

    private void showConfigMenu(@NotNull SIRModule module, @NotNull HumanEntity viewer) {
        File configFile = new File(module.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        List<String> keys = new ArrayList<>(configuration.getKeys(false));
        keys.removeIf(StringUtils::isBlank);
        keys.sort(String.CASE_INSENSITIVE_ORDER);

        int itemsPerPage = 28;
        int rows = SlotCalculator.CENTER_LAYOUT.getTotalRows(keys.size()) / 9;
        String title = "&8" + SmallCaps.toSmallCaps(module.getName() + " Config:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            Object value = configuration.get(key);

            int page = index / itemsPerPage, indexOnPage = index % itemsPerPage;
            Slot slot = SlotCalculator.CENTER_LAYOUT.toSlot(indexOnPage);
            if (slot == null) continue;

            if (value instanceof Boolean) {
                boolean enabled = (Boolean) value;
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
                            configuration.set(key, next);
                            saveConfiguration(configuration, configFile);
                            showConfigMenu(module, click.getWhoClicked());
                        })
                        .getValue());
            } else if (value instanceof List) {
                List<String> values = configuration.getStringList(key);
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
                                    openListEditor(module, configFile, key, values, click.getWhoClicked());
                                })
                                .create(api.getPlugin()),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            } else {
                String current = configuration.getString(key, "");
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
                                    openStringEditor(module, configFile, key, current, click.getWhoClicked());
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

    private void openStringEditor(SIRModule module, File configFile, String key, String current, HumanEntity viewer) {
        if (!(viewer instanceof Player)) return;

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
                    showConfigMenu(module, click.getWhoClicked());
                })
                .create(api.getPlugin());
        result.setItem(resultItem, 0, 0);

        anvilGui.show(viewer);
    }

    private void openListEditor(SIRModule module, File configFile, String key, List<String> values, HumanEntity viewer) {
        if (!(viewer instanceof Player)) return;

        Player player = (Player) viewer;
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);

        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        meta.setTitle(PrismaticAPI.colorize("&8" + key));
        meta.setAuthor(player.getName());
        meta.setPages(buildBookPages(values));
        book.setItemMeta(meta);

        pendingBookEdits.put(player.getUniqueId(), new BookEditSession(module, configFile, key));
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

            load(target);
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
