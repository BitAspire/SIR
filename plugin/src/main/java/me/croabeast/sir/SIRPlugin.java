package me.croabeast.sir;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.common.MetricsLoader;
import me.croabeast.common.util.Exceptions;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.sir.command.CommandManager;
import me.croabeast.sir.command.CommandProvider;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.vault.ChatAdapter;
import me.croabeast.vault.EconomyAdapter;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Getter
public final class SIRPlugin extends JavaPlugin implements SIRApi {

    private GlobalScheduler scheduler;
    private ChatAdapter<?> chat;
    private EconomyAdapter<?> economy;
    private Library library;
    private Config configuration;

    private ModuleManager moduleManager;
    private UserManagerImpl userManager;
    private CommandManager commandManager;

    private ConfigurableFile commandLang;

    @SneakyThrows
    public void onEnable() {
        Timer timer = Timer.create();
        Api.api = this;

        scheduler = GlobalScheduler.getScheduler(this);
        chat = ChatAdapter.create();
        economy = EconomyAdapter.create();

        configuration = new ConfigImpl(this);
        moduleManager = new ModuleManager(this);
        commandManager = new CommandManager(this);

        (new Listener() {
            @EventHandler
            private void onPluginEnable(PluginEnableEvent event) {
                String name = event.getPlugin().getName();
                moduleManager.retryDeferredModules(name, false);
                commandManager.retryDeferredProviders(name, false);
                commandManager.getSynchronizer().sync();
            }
        }).register();

        library = new Library(this);

        userManager = new UserManagerImpl(this);
        userManager.register();

        commandLang = new ConfigurableFile(this, "commands", "lang");
        commandLang.saveDefaults();

        try {
            MigrationService service = new MigrationService(this);
            MigrationService.Result result = service.migrateSir();
            if (!result.isOk()) return;

            getLogger().info("SIR legacy migration completed from " + result.getPath() + ".");
            getLogger().info("Migrated " + result.getUsers() + " users, "
                    + result.getConfigs() + " configs, "
                    + result.getModuleStates() + " module states, "
                    + result.getCommandStates() + " command states.");

            if (result.getBackupPath() != null)
                getLogger().info("Legacy data backup stored at " + result.getBackupPath() + ".");
        } catch (Exception exception) {
            getLogger().warning("SIR legacy migration failed: " + exception.getMessage());
        }

        PluginCommand command = getCommand("sir");
        if (command != null)
            try {
                MainCommand main = new MainCommand(this);
                command.setExecutor(main);
                command.setTabCompleter(main);
            } catch (Exception ignored) {}

        library.getGameRuleManager().load();

        moduleManager.loadAll();
        commandManager.loadAll();

        moduleManager.saveStates();
        commandManager.saveStates();

        library.reload();

        DelayLogger logger = new DelayLogger();
        logger.add(true,
                "===================================",
                "&0 * &6____ &0* &6___ &0* &6____",
                "&0* &6(___&0 * * &6|&0* * &6|___)",
                "&0* &6____) . _|_ . | &0* &6\\ . &f" + getDescription().getVersion(),
                "      &f&oDeveloped by " + getDescription().getAuthors().get(0),
                "===================================",
                "&e[Server]",
                "- Fork & Version: " + ServerInfoUtils.SERVER_FORK,
                "- Java Version: " + ServerInfoUtils.JAVA_VERSION
        );

        logger.add(true, "&e[Modules]");
        int totalModules = moduleManager.getModules().size();
        int enabledModules = (int) moduleManager.getModules().stream()
                .filter(SIRModule::isEnabled)
                .count();
        int registeredModules = (int) moduleManager.getModules().stream()
                .filter(SIRModule::isRegistered)
                .count();
        logger.add(true,
                "- Loaded: " + totalModules + "/" + totalModules,
                "- Enabled: " + enabledModules + "/" + totalModules,
                "- Registered: " + registeredModules + "/" + totalModules
        );
        if (totalModules == 0) {
            logger.add("- No modules found. Skipping...", true);
        } else {
            moduleManager.getModules().forEach(module -> logger.add(true,
                    "- " + module.getName() + ": " +
                            (module.isEnabled() ? "&aenabled" : "&cdisabled") +
                            " &7/ " +
                            (module.isRegistered() ? "&aregistered" : "&cunregistered")
            ));
        }

        logger.add(true, "&e[Commands]");
        int moduleProviders = (int) moduleManager.getModules().stream()
                .filter(module -> module instanceof CommandProvider).count();

        int standaloneProviders = commandManager.getProviderNames().size();
        int totalProviders = moduleProviders + standaloneProviders;

        int totalCommands = commandManager.getCommands().size();
        int enabledCommands = (int) commandManager.getCommands().stream()
                .filter(SIRCommand::isEnabled).count();

        logger.add(true, "&e[Command Providers]");
        logger.add(true,
                "- Module Providers: " + moduleProviders,
                "- Standalone Providers: " + standaloneProviders
        );
        logger.add(true, "&e[Providers: Modules]");
        if (moduleProviders == 0) {
            logger.add("- No module providers found. Skipping...", true);
        } else {
            moduleManager.getModules().stream()
                    .filter(module -> module instanceof CommandProvider)
                    .forEach(module -> {
                        CommandProvider provider = (CommandProvider) module;
                        logger.add(true,
                                "- " + module.getName() + ": " + provider.getCommands().size() + " commands"
                        );
                    });
        }

        logger.add(true, "&e[Providers: Standalone]");
        if (standaloneProviders == 0) {
            logger.add("- No standalone providers found. Skipping...", true);
        } else {
            commandManager.getProviderNames().forEach(name -> logger.add(true,
                    "- " + name + ": " + commandManager.getProviderCommands(name).size() + " commands"
            ));
        }

        logger.add(true,
                "- Total Providers: " + totalProviders,
                "- Total Commands: " + totalCommands
        );

        logger.add(true, "&e[Command Status]");
        logger.add(true,
                "- Registered: " + totalCommands,
                "- Enabled: " + enabledCommands
        );
        if (totalCommands == 0) {
            logger.add("- No commands registered. Skipping...", true);
        } else {
            commandManager.getCommands().forEach(c -> logger.add(true,
                    "- " + c.getName() + ": " +
                            (c.isEnabled() ? "&aenabled" : "&cdisabled")
            ));
        }

        logger.add(true,
                "&e[Status]",
                "SIR initialized completely.",
                "Loading time: " + timer.current() + " ms.",
                "==================================="
        ).sendLines();

        System.setProperty("bstats.relocatecheck", "false");
        MetricsLoader.initialize(this, 25264)
                .addSimplePie("hasPAPI", Exceptions.isPluginEnabled("PlaceholderAPI"))
                .addSimplePie("hasInteractive", Exceptions.isPluginEnabled("InteractiveChat"))
                .addDrillDownPie(
                        "chatPlugins", "Chat Plugins",
                        chat.getPlugin() != null ? chat.getPlugin().getName() : "None/Other"
                )
                .addDrillDownPie(
                        "economyPlugins", "Economy Plugins",
                        economy.getPlugin() != null ? economy.getPlugin().getName() : "None/Other"
                );
    }

    @Override
    public void onDisable() {
        Timer timer = Timer.create();

        DelayLogger logger = new DelayLogger();
        logger.add(true,
                "===================================",
                "&0 * &6____ &0* &6___ &0* &6____",
                "&0* &6(___&0 * * &6|&0* * &6|___)",
                "&0* &6____) . _|_ . | &0* &6\\ . &f" + getDescription().getVersion(),
                "      &f&oDeveloped by " + getDescription().getAuthors().get(0),
                "==================================="
        );

        userManager.shutdown();

        commandManager.saveStates();
        moduleManager.saveStates();

        commandManager.unloadAll();
        moduleManager.unloadAll();

        logger.add(true,
                "SIR disabled completely in " + timer.current() + " ms.",
                "==================================="
        ).sendLines();

        HandlerList.unregisterAll(this);

        Api.api = null;
    }

    @NotNull
    public TakionLib getLibrary() {
        return library;
    }

    @NotNull
    public Plugin getPlugin() {
        return this;
    }

    @NotNull
    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public void reload() {
        commandLang.reload();

        configuration = new ConfigImpl(this);

        commandManager.saveStates();
        moduleManager.saveStates();

        commandManager.unloadAll();
        moduleManager.unloadAll();

        moduleManager.loadAll();
        commandManager.loadAll();

        library.reload();
    }
}