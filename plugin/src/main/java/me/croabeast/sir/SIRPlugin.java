package me.croabeast.sir;

import lombok.Getter;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.sir.command.CommandManager;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.vault.ChatAdapter;
import me.croabeast.vault.EconomyAdapter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Getter
public final class SIRPlugin extends JavaPlugin implements SIRApi {

    private GlobalScheduler scheduler;
    private ChatAdapter<?> chat;
    private EconomyAdapter<?> economy;
    private TakionLib library;

    private ModuleManager moduleManager;
    private UserManager userManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        Api.api = this;

        scheduler = GlobalScheduler.getScheduler(this);
        chat = ChatAdapter.create();
        economy = EconomyAdapter.create();

        library = new TakionLib(this);
        moduleManager = new ModuleManager(this);

        moduleManager.loadAll();
    }

    @Override
    public void onDisable() {
        // ok
    }

    @NotNull
    public Plugin getPlugin() {
        return this;
    }
}
