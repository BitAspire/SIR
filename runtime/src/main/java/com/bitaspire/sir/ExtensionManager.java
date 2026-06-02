package com.bitaspire.sir;

import me.croabeast.takion.logger.LogLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

abstract class ExtensionManager<I extends Information, E extends SIRExtension<I>> extends BaseManager<I> {

    final Map<String, LoadedExtension<E>>  extensions = new LinkedHashMap<>();
    final Map<String, DeferredExtension<I>> deferred  = new LinkedHashMap<>();
    final Map<String, Boolean>              states    = new LinkedHashMap<>();

    ExtensionManager(SIRApi api) {
        super(api);
    }

    // -- Inner types --

    static class LoadedExtension<E> {
        final E              extension;
        final URLClassLoader classLoader;

        LoadedExtension(E extension, URLClassLoader classLoader) {
            this.extension   = extension;
            this.classLoader = classLoader;
        }
    }

    static class DeferredExtension<I extends Information> {
        final Candidate<I> candidate;
        final String[]     dependencies;

        DeferredExtension(Candidate<I> candidate, String[] dependencies) {
            this.candidate    = candidate;
            this.dependencies = dependencies;
        }
    }

    // -- Abstract --

    abstract boolean defaultEnabledState();
    abstract boolean loadCandidate(Candidate<I> candidate, boolean syncCommands);

    // -- Hooks (no-op by default, override to add diagnostics etc.) --

    protected void onHardDependencyMissing(String extensionName, String missingDep) {}
    protected void onDeferredRecorded(String extensionName, String[] dependencies) {}
    protected void onEnabledStateChanged(E extension, boolean enabled) {}

    // -- Lookup --

    private String loadedKey(String name) {
        if (name == null) return null;
        return extensions.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private LoadedExtension<E> getLoadedExtension(String name) {
        String key = loadedKey(name);
        return key == null ? null : extensions.get(key);
    }

    private boolean containsExtension(String name) {
        return getLoadedExtension(name) != null;
    }

    private String candidateKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    // -- Load --

    public boolean load(File jarFile, boolean syncCommands) {
        log(LogLevel.INFO, "Loading " + extensionType() + " from " + jarFile.getName() + "...");

        Candidate<I> candidate = createCandidate(jarFile);
        if (candidate == null) return false;

        String name = candidate.file.getName();
        if (containsExtension(name)) {
            startupSkipped.add(name);
            log(LogLevel.WARN, extensionType() + " with name '" + name + "' already loaded, skipping...");
            return false;
        }

        for (String dep : candidate.file.getDepend()) {
            if (!containsExtension(dep)) {
                startupSkipped.add(name);
                onHardDependencyMissing(candidate.file.getName(), dep);
                log(LogLevel.WARN, extensionType() + " '" + candidate.file.getName()
                        + "' can't be loaded: missing hard dependency '" + dep + "'.");
                return false;
            }
        }

        for (String dep : candidate.file.getSoftDepend()) {
            if (!containsExtension(dep))
                log(LogLevel.INFO, "Soft dependency '" + dep + "' for " + extensionType()
                        + " '" + candidate.file.getName() + "' is not loaded.");
        }

        return loadCandidate(candidate, syncCommands);
    }

    public void loadAll() {
        clearStartupStats();
        loadStates();
        loadBundledJars(api.getConfiguration().loadDefaultJars(extensionFolder()));

        File dir = new File(api.getPlugin().getDataFolder(), extensionFolder());
        if (!dir.exists() && !dir.mkdirs())
            log(LogLevel.WARN, "Could not create " + extensionFolder() + " directory: " + dir.getPath());

        File[] jars = dir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            log(LogLevel.INFO, "No " + extensionFolder() + " found in " + dir.getPath());
            return;
        }

        Map<String, Candidate<I>> candidates = new LinkedHashMap<>();
        for (File jarFile : jars) {
            Candidate<I> c = createCandidate(jarFile);
            if (c == null) continue;

            String key = candidateKey(c.file.getName());
            if (containsExtension(c.file.getName())) {
                log(LogLevel.WARN, extensionType() + " with name '" + c.file.getName()
                        + "' already loaded, skipping '" + jarFile.getName() + "'.");
                continue;
            }

            if (candidates.containsKey(key)) {
                log(LogLevel.WARN, "Duplicate " + extensionType() + " name '" + c.file.getName()
                        + "' between '" + candidates.get(key).jarFile.getName()
                        + "' and '" + jarFile.getName() + "', skipping the latter.");
                continue;
            }

            candidates.put(key, c);
        }

        if (candidates.isEmpty()) {
            log(LogLevel.INFO, "No valid " + extensionFolder() + " found in " + dir.getPath());
            return;
        }

        deferPluginDependants = true;

        Set<String> failed = new HashSet<>();
        boolean progress;
        try {
            do {
                progress = false;

                for (Map.Entry<String, Candidate<I>> entry : candidates.entrySet()) {
                    String nameKey = entry.getKey();
                    if (containsExtension(nameKey) || failed.contains(nameKey)) continue;

                    I file = entry.getValue().file;
                    boolean wait = false, hardMissing = false;

                    for (String dep : file.getDepend()) {
                        if (containsExtension(dep)) continue;

                        String dependencyKey = candidateKey(dep);
                        if (candidates.containsKey(dependencyKey) && !failed.contains(dependencyKey)) {
                            wait = true;
                        } else {
                            startupSkipped.add(file.getName());
                            onHardDependencyMissing(file.getName(), dep);
                            log(LogLevel.WARN, extensionType() + " '" + file.getName()
                                    + "' can't be loaded: missing hard dependency '" + dep + "'.");
                            hardMissing = true;
                            break;
                        }
                    }

                    if (hardMissing) { failed.add(nameKey); continue; }
                    if (wait) continue;

                    for (String dep : file.getSoftDepend()) {
                        if (containsExtension(dep)) continue;
                        String dependencyKey = candidateKey(dep);
                        if (candidates.containsKey(dependencyKey) && !failed.contains(dependencyKey)) {
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

        // Soft dependencies influence ordering, but must never prevent loading.
        do {
            progress = false;
            for (Map.Entry<String, Candidate<I>> entry : candidates.entrySet()) {
                String nameKey = entry.getKey();
                if (containsExtension(nameKey) || failed.contains(nameKey)) continue;

                boolean hardDependenciesReady = true;
                for (String dep : entry.getValue().file.getDepend()) {
                    if (containsExtension(dep)) continue;
                    hardDependenciesReady = false;
                    break;
                }
                if (!hardDependenciesReady) continue;

                if (!load(entry.getValue().jarFile, false)) failed.add(nameKey);
                progress = true;
            }
        } while (progress);

        List<String> unresolved = candidates.entrySet().stream()
                .filter(entry -> !containsExtension(entry.getKey()) && !failed.contains(entry.getKey()))
                .map(entry -> entry.getValue().file.getName())
                .collect(Collectors.toList());

        if (!unresolved.isEmpty()) {
            startupFailures.addAll(unresolved);
            log(LogLevel.ERROR, "Unresolved " + extensionType() + " dependency loop: "
                    + String.join(", ", unresolved));
        }

        retryDeferred(null, false);
    }

    // -- Deferred --

    void deferExtension(Candidate<I> candidate, String[] dependencies) {
        String key = candidateKey(candidate.file.getName());
        if (deferred.containsKey(key) || containsExtension(candidate.file.getName())) return;
        deferred.put(key, new DeferredExtension<>(candidate, dependencies));
        onDeferredRecorded(candidate.file.getName(), dependencies);
        log(LogLevel.INFO, extensionType() + " '" + candidate.file.getName() + "' deferred until its dependencies are ready.");
    }

    public void retryDeferred(String pluginName, boolean syncCommands) {
        if (deferred.isEmpty()) return;

        Iterator<Map.Entry<String, DeferredExtension<I>>> it = deferred.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DeferredExtension<I>> entry = it.next();
            DeferredExtension<I> d = entry.getValue();
            if (pluginName != null && !matchesDependency(pluginName, d.dependencies)) continue;
            if (loadCandidate(d.candidate, syncCommands)) it.remove();
        }
    }

    boolean matchesDependency(String pluginName, String[] dependencies) {
        if (pluginName == null || dependencies == null) return true;
        for (String dep : dependencies)
            if (dep != null && dep.equalsIgnoreCase(pluginName)) return true;
        return false;
    }

    // -- Unload --

    public void unload(String name) {
        String key = loadedKey(name);
        LoadedExtension<E> loaded = key == null ? null : extensions.get(key);
        if (loaded == null) {
            log(LogLevel.WARN, extensionType() + " with name '" + name + "' not found, skipping unload.");
            return;
        }

        E ext = loaded.extension;
        try {
            if (ext.unregister()) {
                ext.setRegistered(false);
                onEnabledStateChanged(ext, false);
            }

            try {
                loaded.classLoader.close();
                extensions.remove(key);
                log(LogLevel.INFO, extensionType() + " '" + name + "' unloaded successfully.");
            } catch (Exception e) {
                log(LogLevel.ERROR, "Failed to close class loader for " + extensionType() + " '" + name + "'");
                e.printStackTrace();
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to unload " + extensionType() + " " + ext.getName());
            e.printStackTrace();
        }
    }

    public void unloadAll() {
        for (String name : new ArrayList<>(extensions.keySet())) unload(name);
    }

    // -- States --

    public void setEnabled(String name, boolean enabled) {
        if (name != null) states.put(name, enabled);
    }

    public boolean isEnabled(String name) {
        return name != null && states.computeIfAbsent(name, k -> defaultEnabledState());
    }

    public void saveStates() {
        String folder = extensionFolder();
        File dir = new File(api.getPlugin().getDataFolder(), folder);
        if (!dir.exists() && !dir.mkdirs()) {
            log(LogLevel.WARN, "Could not create " + folder + " directory: " + dir.getPath());
            return;
        }

        File file = new File(dir, "states.yml");
        YamlConfiguration configuration = new YamlConfiguration();

        for (Map.Entry<String, Boolean> entry : states.entrySet())
            configuration.set(folder + "." + entry.getKey() + ".enabled", entry.getValue());

        try {
            configuration.save(file);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to save " + extensionType() + " states to " + file.getPath());
            e.printStackTrace();
        }
    }

    private void loadStates() {
        states.clear();

        File file = new File(api.getPlugin().getDataFolder(), extensionFolder() + File.separator + "states.yml");
        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection(extensionFolder());
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            if (name == null || name.trim().isEmpty()) continue;
            states.put(name, section.getBoolean(name + ".enabled", defaultEnabledState()));
        }
    }

    // -- Enable/disable --

    public void updateEnabled(E ext, boolean enabled) {
        if (ext == null) return;

        boolean changed = ext.isEnabled() != enabled;
        boolean successful = true;

        if (changed) {
            MenuToggleable.Button button = ext.getButton();
            if (button != null) {
                successful = button.toggleAll();
            } else {
                successful = enabled ? ext.register() : ext.unregister();
                if (successful) {
                    ext.setEnabledState(enabled);
                    ext.setRegistered(enabled);
                }
            }
        }

        if (!successful) {
            log(LogLevel.WARN, "Failed to " + (enabled ? "enable " : "disable ")
                    + extensionType() + " '" + ext.getName() + "'.");
            return;
        }

        setEnabled(ext.getName(), enabled);
        if (changed) onEnabledStateChanged(ext, enabled);
    }

    public void updateEnabled(String name, boolean enabled) {
        LoadedExtension<E> loaded = getLoadedExtension(name);
        updateEnabled(loaded == null ? null : loaded.extension, enabled);
    }

    protected void reloadExtension(E extension) {
        if (extension == null || !extension.isEnabled()) return;

        try {
            if (!extension.unregister()) {
                log(LogLevel.WARN, "Failed to unregister " + extensionType() + " '"
                        + extension.getName() + "' before config reload.");
                return;
            }

            extension.setRegistered(false);
            if (extension.register()) {
                extension.setRegistered(true);
            } else {
                log(LogLevel.WARN, "Failed to register " + extensionType() + " '"
                        + extension.getName() + "' after config reload.");
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to reload " + extensionType() + " '"
                    + extension.getName() + "' after config change: " + e.getMessage());
        }
    }

    List<E> getExtensions() {
        return extensions.values().stream().map(l -> l.extension).collect(Collectors.toList());
    }

    Set<String> getExtensionNames() {
        Set<String> names = new LinkedHashSet<>(states.keySet());
        for (LoadedExtension<E> loaded : extensions.values())
            names.add(loaded.extension.getName());
        return names;
    }

    E getExtension(String name) {
        LoadedExtension<E> loaded = getLoadedExtension(name);
        return loaded == null || !isEnabled(loaded.extension.getName()) ? null : loaded.extension;
    }
}
