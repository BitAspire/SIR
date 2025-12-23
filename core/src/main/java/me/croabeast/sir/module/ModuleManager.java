package me.croabeast.sir.module;

import lombok.RequiredArgsConstructor;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.command.CommandProvider;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
@RequiredArgsConstructor
public final class ModuleManager {

    private final SIRApi api;
    private final Map<String, LoadedModule> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> moduleStates = new LinkedHashMap<>();

    @RequiredArgsConstructor
    private static class ModuleCandidate {
        final File jarFile;
        final ModuleInformation file;
    }

    @RequiredArgsConstructor
    private static class LoadedModule {
        final SIRModule module;
        final URLClassLoader classLoader;
    }

    @NotNull
    public List<SIRModule> getModules() {
        return modules.values().stream().map(l -> l.module).collect(Collectors.toList());
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
        throw new UnsupportedOperationException();
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

    public void openConfigMenu(@NotNull SIRModule module, @NotNull org.bukkit.event.inventory.InventoryClickEvent event) {
        File configFile = new File(module.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;
        // Placeholder for config menu opening logic.
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
