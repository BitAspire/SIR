package me.croabeast.sir;

import me.croabeast.common.util.ArrayUtils;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.sir.command.CommandManager;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.placeholder.PlaceholderManager;
import me.croabeast.vault.ChatAdapter;
import me.croabeast.vault.EconomyAdapter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
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
    Config getConfiguration();

    @NotNull
    ModuleManager getModuleManager();

    @NotNull
    UserManager getUserManager();

    @NotNull
    CommandManager getCommandManager();

    @NotNull
    TakionLib getLibrary();

    void reload();

    @NotNull
    static SIRApi instance() {
        return Objects.requireNonNull(Api.api, "SIR's API isn't initialized yet");
    }

    static String joinArray(int index, String... array) {
        if (ArrayUtils.isArrayEmpty(array) || index >= array.length)
            return null;

        StringBuilder b = new StringBuilder();

        for (int i = index; i < array.length; i++) {
            b.append(array[i]);
            if (i != array.length - 1) b.append(" ");
        }

        return b.toString();
    }

    static void executeCommands(SIRUser user, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        commands.removeIf(StringUtils::isBlank);
        commands.replaceAll(String::trim);

        Player[] player = {null};
        if (user != null && user.isOnline()) {
            PlaceholderManager manager = instance().getLibrary().getPlaceholderManager();
            commands.replaceAll(s -> manager.replace(player[0] = user.getPlayer(), s));
        }

        instance().getScheduler().runTask(new Runnable() {
            private String format(String prefix, String command) {
                return (!command
                        .toLowerCase(Locale.ENGLISH)
                        .startsWith(prefix) ?
                        command :
                        command.substring(prefix.length()))
                        .trim();
            }

            @Override
            public void run() {
                for (String command : commands) {
                    final String temp = command.toLowerCase(Locale.ENGLISH);

                    if (temp.startsWith("[player]") && player[0] != null) {
                        Bukkit.dispatchCommand(player[0], format("[player]", command));
                        continue;
                    }

                    if (temp.startsWith("[op]") && player[0] != null) {
                        boolean op = player[0].isOp();
                        try {
                            if (!op) player[0].setOp(true);
                            Bukkit.dispatchCommand(player[0], format("[op]", command));
                        } finally {
                            if (!op) player[0].setOp(false);
                        }
                        continue;
                    }

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format("[console]", command));
                }
            }
        });
    }
}
