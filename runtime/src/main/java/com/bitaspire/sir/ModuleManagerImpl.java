package com.bitaspire.sir;

import com.bitaspire.sir.command.CommandProvider;
import com.bitaspire.sir.module.DiscordService;
import com.bitaspire.sir.module.JoinQuitService;
import com.bitaspire.sir.module.ModuleInformation;
import com.bitaspire.sir.module.ModuleManager;
import com.bitaspire.sir.module.SIRModule;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
final class ModuleManagerImpl extends ExtensionManager<ModuleInformation, SIRModule> implements ModuleManager {

    private final RuntimeDiagnostics startupDiagnostics;
    private final ExtensionMenu<ModuleInformation, SIRModule> menu;

    ModuleManagerImpl(SIRApi api, RuntimeDiagnostics startupDiagnostics) {
        super(api);
        this.startupDiagnostics = startupDiagnostics;
        ExtensionConfigEditor<SIRModule> configEditor = new ExtensionConfigEditor<>(api, "module", "Modules", this::getMenu, this::reloadExtension);
        menu = new ExtensionMenu<>(
                api,
                "Modules",
                "Module",
                Material.BOOKSHELF,
                SIRModule::getInformation,
                this::updateEnabled,
                configEditor
        );
    }

    @Override String extensionFolder() { return "modules"; }
    @Override String extensionType() { return "module"; }
    @Override String ymlFileName() { return "module.yml"; }
    @Override ModuleInformation parseInformation(YamlConfiguration config) { return new ModuleInformation(config); }
    @Override void onBundledJarExtracted(File jar) { load(jar, false); }
    @Override boolean defaultEnabledState() { return true; }

    @Override
    void log(LogLevel level, String... messages) {
        if (startupDiagnostics != null && startupDiagnostics.isCollecting()) {
            startupDiagnostics.module(level, messages);
            if (startupDiagnostics.notLogToConsole(level)) return;
        }
        super.log(level, messages);
    }

    @Override
    protected void onHardDependencyMissing(String extensionName, String missingDep) {
        if (startupDiagnostics != null)
            startupDiagnostics.moduleRequirement(extensionName, new String[]{missingDep});
    }

    @Override
    protected void onDeferredRecorded(String extensionName, String[] dependencies) {
        startupSkipped.add(extensionName);
        if (startupDiagnostics != null)
            startupDiagnostics.moduleRequirement(extensionName, dependencies);
    }

    @Override
    protected void onEnabledStateChanged(SIRModule module, boolean enabled) {
        if (!(module instanceof CommandProvider)) return;

        CommandProvider provider = (CommandProvider) module;
        if (enabled) api.getCommandManager().loadFromModule(module, true);
        else api.getCommandManager().unload(provider, true);
    }

    @Override
    boolean loadCandidate(Candidate<ModuleInformation> candidate, boolean syncCommands) {
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
                deferExtension(candidate, inferPluginFromClass(missingClass));
                classLoader.close();
                return false;
            }

            if (!SIRModule.class.isAssignableFrom(clazz)) {
                startupFailures.add(name);
                log(LogLevel.ERROR, "Main class '" + file.getMain() + "' does not extend SIRModule, skipping...");
                classLoader.close();
                return false;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);

            SIRModule module = (SIRModule) constructor.newInstance();
            if (module instanceof PluginDependant) {
                PluginDependant dependant = (PluginDependant) module;
                String[] dependencies = dependant.getDependencies();

                if (dependencies.length > 0) {
                    if (deferPluginDependants) {
                        deferExtension(candidate, dependencies);
                        classLoader.close();
                        return false;
                    }

                    if (!dependant.areDependenciesEnabled()) {
                        log(LogLevel.INFO, hookMessage(name, dependencies));
                        deferExtension(candidate, dependencies);
                        classLoader.close();
                        return false;
                    }
                }
            }

            ((SIRExtension<ModuleInformation>) module).init(api, classLoader, file);
            extensions.put(name, new LoadedExtension<>(module, classLoader));

            if (!module.isEnabled()) {
                log(LogLevel.INFO, "Module '" + name + "' is disabled, skipping registration.");
                return true;
            }

            if (module.register()) {
                classLoader.module = module;
                ((SIRExtension<?>) module).setRegistered(true);
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
            }

            startupFailures.add(name);
            unload(module.getName());
            return false;
        } catch (Exception e) {
            startupFailures.add(jarFile.getName());
            log(LogLevel.ERROR, "Failed to load module from " + jarFile.getName());
            e.printStackTrace();
            return false;
        }
    }

    @NotNull
    public List<SIRModule> getModules() {
        return getExtensions();
    }

    @NotNull
    public Set<String> getModuleNames() {
        return getExtensionNames();
    }

    public SIRModule getModule(@NotNull String name) {
        return getExtension(name);
    }

    @SuppressWarnings("unchecked")
    public <T> UserFormatter<T> getFormatter(@NotNull String name) {
        SIRModule module = getModule(name);
        return module instanceof UserFormatter ? (UserFormatter<T>) module : null;
    }

    public JoinQuitService getJoinQuitService() {
        SIRModule module = getModule("JoinQuit");
        return module instanceof JoinQuitService ? (JoinQuitService) module : null;
    }

    public DiscordService getDiscordService() {
        SIRModule module = getModule("Discord");
        return module instanceof DiscordService ? (DiscordService) module : null;
    }

    public void retryDeferredModules(String pluginName, boolean syncCommands) {
        retryDeferred(pluginName, syncCommands);
    }

    @NotNull
    public ChestBuilder getMenu() {
        return menu.build(getModules());
    }

    public void openConfigMenu(@NotNull InventoryClickEvent event) {
        menu.openConfig(event, getModules());
    }

    private static String[] inferPluginFromClass(String className) {
        if (className == null) return new String[]{"Unknown"};

        String normalized = className.replace('/', '.');
        if (normalized.startsWith("me.clip.placeholderapi")) return new String[]{"PlaceholderAPI"};
        if (normalized.startsWith("github.scarsz.discordsrv") || normalized.startsWith("com.discordsrv"))
            return new String[]{"DiscordSRV"};
        if (normalized.startsWith("net.luckperms")) return new String[]{"LuckPerms"};
        if (normalized.startsWith("me.realized.tokenmanager")) return new String[]{"TokenManager"};
        if (normalized.startsWith("com.earth2me.essentials")) return new String[]{"Essentials"};
        if (normalized.startsWith("net.milkbowl.vault")) return new String[]{"Vault"};
        return new String[]{"Unknown"};
    }
}
