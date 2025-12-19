package me.croabeast.sir.command;

import lombok.RequiredArgsConstructor;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class CommandManager {

    private final SIRApi api;
    private final ModuleManager moduleManager;

    private final Map<String, SIRCommand> commands = new LinkedHashMap<>();
    private final Map<String, LoadedProvider> providers = new LinkedHashMap<>();
    private final Set<SIRModule> processedModules = Collections.newSetFromMap(new IdentityHashMap<>());

    public CommandManager(SIRApi api) {
        this.api = api;
        moduleManager = api.getModuleManager();
    }

    @RequiredArgsConstructor
    private static class LoadedProvider {
        final ProviderLoader loader;
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

    private ProviderFile readFile(InputStream stream, String source) {
        if (stream == null) {
            log(LogLevel.WARN, "commands.yml not found for " + source + ", skipping.");
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new ProviderFile(YamlConfiguration.loadConfiguration(reader));
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to read commands.yml from " + source);
            e.printStackTrace();
            return null;
        }
    }

    private void registerProvider(CommandProvider provider, ProviderFile file) {
        if (provider == null || file == null) return;

        Set<SIRCommand> set = provider.getCommands();
        if (set.isEmpty()) {
            log(LogLevel.WARN, "No commands declared for provider '" + file.getMain() + "'.");
            return;
        }

        for (SIRCommand command : set) {
            if (command == null) continue;

            String commandKey = command.getCommandKey();
            if (StringUtils.isBlank(commandKey)) {
                commandKey = command.getName();
            }

            if (StringUtils.isBlank(commandKey)) {
                log(LogLevel.WARN, "Command with empty name found in provider '" + file.getMain() + "', skipping.");
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

            boolean depends = section.getBoolean("depends.enabled");
            String parentName = section.getString("depends.parent");
            SIRCommand parent = null;

            if (depends && StringUtils.isNotBlank(parentName)) {
                parent = getCommand(parentName);
                if (parent == null) {
                    log(LogLevel.WARN, "Command '" + commandKey + "' depends on '" + parentName + "', but it isn't loaded.");
                    continue;
                }

                if (!parent.isEnabled()) {
                    log(LogLevel.INFO, "Command '" + commandKey + "' depends on disabled command '" + parentName + "', skipping.");
                    continue;
                }
            }

            command.applyFile(new CommandFile(nameKey, section, parent));
            registerCommand(command);
        }
    }

    public void loadFromModule(@NotNull SIRModule module) {
        if (processedModules.contains(module)) return;

        InputStream stream = module.getClass().getClassLoader().getResourceAsStream("commands.yml");
        ProviderFile file = readFile(stream, module.getName());
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
        registerProvider(provider, file);
        processedModules.add(module);
    }

    private ProviderFile readExternalCommandsFile(File jarFile) {
        JarEntry entry;

        try (JarFile jar = new JarFile(jarFile)) {
            entry = jar.getJarEntry("commands.yml");
            if (entry == null) {
                log(LogLevel.WARN, "commands.yml not found in " + jarFile.getName() + ", skipping.");
                return null;
            }

            try (InputStream stream = jar.getInputStream(entry);
                  InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new ProviderFile(YamlConfiguration.loadConfiguration(reader));
            }
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to read commands.yml from " + jarFile.getName());
            e.printStackTrace();
            return null;
        }
    }

    public void load(File jarFile) {
        log(LogLevel.INFO, "Loading command provider from " + jarFile.getName() + "...");

        ProviderFile file = readExternalCommandsFile(jarFile);
        if (file == null) return;

        if (providers.containsKey(file.getMain())) {
            log(LogLevel.WARN, "Command provider '" + file.getMain() + "' already loaded, skipping.");
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
            providers.put(file.getMain(), new LoadedProvider(loader));
            registerProvider(provider, file);
        } catch (Exception e) {
            log(LogLevel.ERROR, "Failed to load command provider from " + jarFile.getName());
            e.printStackTrace();
        }
    }

    public void loadAll() {
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
    public ChestBuilder getMenu() {
        throw new IllegalStateException();
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
                loaded.disable();
                loaded.unregister(true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            commands.remove(loaded.getName().toLowerCase(Locale.ENGLISH));
            log(LogLevel.INFO, "Disabled dependent command '" + loaded.getName() + "' (parent '" + name + "').");
        }

        try {
            command.disable();
            command.unregister(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        commands.remove(name.toLowerCase(Locale.ENGLISH));
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
}
