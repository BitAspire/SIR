package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.MenuToggleable;
import com.bitaspire.sir.command.SIRCommand;
import me.croabeast.command.TabBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

final class AdminCommand extends SIRCommand {

    private final CustomCommands main;

    AdminCommand(CustomCommands main) {
        super("customcommands", main.lang);
        this.main = main;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player) || !MenuToggleable.supportsButtons())
                return Utils.create(this, sender).send("help");

            main.menu.openList((Player) sender);
            return true;
        }

        String name = args.length > 1 ? args[1].toLowerCase(Locale.ENGLISH) : null;

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "reload":
                return Utils.create(this, sender).addPlaceholder("{count}", main.load()).send("reloaded");

            case "list":
                List<String> enabled = new ArrayList<>(), disabled = new ArrayList<>();
                for (String command : main.getNames())
                    (main.loaded.containsKey(command) ? enabled : disabled).add(command);

                return Utils.create(this, sender)
                        .addPlaceholder("{enabled}", String.join(", ", enabled))
                        .addPlaceholder("{disabled}", String.join(", ", disabled))
                        .send("list");

            case "create":
                if (name == null) return Utils.create(this, sender).send("help");

                return Utils.create(this, sender)
                        .addPlaceholder("{command}", name)
                        .send(main.create(name) ? "created" : "create-failed");

            case "toggle":
            case "delete":
                if (name == null) return Utils.create(this, sender).send("help");
                if (!main.getNames().contains(name))
                    return Utils.create(this, sender).addPlaceholder("{command}", name).send("unknown");

                String key = args[0].equalsIgnoreCase("delete") ?
                        (main.delete(name) ? "deleted" : "delete-failed") :
                        (main.toggle(name) ? "toggled-on" : "toggled-off");

                return Utils.create(this, sender).addPlaceholder("{command}", name).send(key);

            case "rename":
            case "duplicate":
                if (name == null || args.length < 3) return Utils.create(this, sender).send("help");
                if (!main.getNames().contains(name))
                    return Utils.create(this, sender).addPlaceholder("{command}", name).send("unknown");

                String target = args[2].toLowerCase(Locale.ENGLISH);
                boolean done = args[0].equalsIgnoreCase("rename") ?
                        main.rename(name, target) :
                        main.duplicate(name, target);

                return Utils.create(this, sender)
                        .addPlaceholder("{command}", done ? name : target)
                        .addPlaceholder("{target}", target)
                        .send(!done ? "create-failed" : args[0].equalsIgnoreCase("rename") ? "renamed" : "duplicated");

            // data <list|get|set|delete> [key] [value...]
            // player-data <player> <list|get|set|delete> [key] [value...]
            case "data":
            case "player-data":
                boolean global = args[0].equalsIgnoreCase("data");
                int at = global ? 1 : 2;
                if (args.length <= at) return Utils.create(this, sender).send("help");

                UUID uuid = null;
                if (!global) {
                    OfflinePlayer owner = findPlayer(args[1]);
                    if (owner == null)
                        return Utils.create(this, sender).addPlaceholder("{player}", args[1]).send("unknown-player");
                    uuid = owner.getUniqueId();
                }

                String action = args[at].toLowerCase(Locale.ENGLISH);
                if (action.equals("list"))
                    return Utils.create(this, sender)
                            .addPlaceholder("{keys}", String.join(", ", main.data.keys(uuid)))
                            .send("data-list");

                if (args.length <= at + 1) return Utils.create(this, sender).send("help");
                String dataKey = args[at + 1];

                switch (action) {
                    case "get":
                        String value = main.data.get(uuid, dataKey);
                        return Utils.create(this, sender).addPlaceholder("{key}", dataKey).addPlaceholder("{value}", value)
                                .send(value.isEmpty() ? "data-empty" : "data-value");

                    case "set":
                        if (args.length <= at + 2) return Utils.create(this, sender).send("help");
                        String text = String.join(" ", Arrays.copyOfRange(args, at + 2, args.length));
                        main.data.set(uuid, dataKey, text);
                        return Utils.create(this, sender).addPlaceholder("{key}", dataKey).addPlaceholder("{value}", text).send("data-set");

                    case "delete":
                        main.data.set(uuid, dataKey, "");
                        return Utils.create(this, sender).addPlaceholder("{key}", dataKey).send("data-deleted");

                    default:
                        return getArgumentCheck().test(sender, args[at]);
                }

            default:
                return getArgumentCheck().test(sender, args[0]);
        }
    }

    // Online first; offline players are only looked up among those who played here.
    @Nullable
    private static OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers())
            if (name.equalsIgnoreCase(offline.getName())) return offline;
        return null;
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder()
                .addArguments(0, "reload", "list", "create", "toggle", "rename", "duplicate", "delete", "data", "player-data")
                .addArguments(1, (s, a) -> a[0].matches("(?i)toggle|rename|duplicate|delete"), (s, a) -> main.getNames())
                .addArguments(1, (s, a) -> a[0].equalsIgnoreCase("data"), "list", "get", "set", "delete")
                .addArguments(1, (s, a) -> a[0].equalsIgnoreCase("player-data"),
                        (s, a) -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()))
                .addArguments(2, (s, a) -> a[0].equalsIgnoreCase("player-data"), "list", "get", "set", "delete")
                .addArguments(2, (s, a) -> a[0].equalsIgnoreCase("data") && a[1].matches("(?i)get|set|delete"),
                        (s, a) -> main.data.keys(null));
    }
}
