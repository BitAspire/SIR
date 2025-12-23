package me.croabeast.sir;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.sir.command.CommandManager;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.vault.ChatAdapter;
import me.croabeast.vault.EconomyAdapter;
import org.bukkit.command.PluginCommand;
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
        Api.api = this;

        scheduler = GlobalScheduler.getScheduler(this);
        chat = ChatAdapter.create();
        economy = EconomyAdapter.create();

        configuration = new ConfigImpl(this);
        (library = new Library(this)).reload();

        commandLang = new ConfigurableFile(this, "commands", "lang");
        commandLang.saveDefaults();

        moduleManager = new ModuleManager(this);

        userManager = new UserManagerImpl(this);
        userManager.register();

        commandManager = new CommandManager(this);

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
    }

    @Override
    public void onDisable() {
        userManager.shutdown();

        commandManager.saveStates();
        moduleManager.saveStates();

        commandManager.unloadAll();
        moduleManager.unloadAll();
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
        library.reload();

        commandManager.unloadAll();
        moduleManager.unloadAll();

        moduleManager.loadAll();
        commandManager.loadAll();
    }
}
