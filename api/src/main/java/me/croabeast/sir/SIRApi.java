package me.croabeast.sir;

import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.sir.command.CommandManager;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.vault.ChatAdapter;
import me.croabeast.vault.EconomyAdapter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface SIRApi {

    @NotNull
    GlobalScheduler getScheduler();

    @NotNull
    ChatAdapter<?> getChat();

    @NotNull
    EconomyAdapter<?> getEconomy();

    @NotNull
    Plugin getPlugin();

    @NotNull
    ModuleManager getModuleManager();

    @NotNull
    UserManager getUserManager();

    @NotNull
    CommandManager getCommandManager();

    @NotNull
    TakionLib getLibrary();

    @NotNull
    static SIRApi instance() {
        return Objects.requireNonNull(Api.api, "SIR's API isn't initialized yet");
    }
}
