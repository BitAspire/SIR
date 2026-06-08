package com.bitaspire.sir.module.motd;

import lombok.RequiredArgsConstructor;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Random;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
final class Listener extends com.bitaspire.sir.Listener {

    private final MOTD main;

    private final Random random = new Random();
    private int motdIndex = 0, iconIndex = 0;

    UnaryOperator<String> getOperator(InetAddress inetAddress) {
        Player[] player = new Player[1];
        for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {
            Player temp = off.getPlayer();
            if (temp == null) continue;

            InetSocketAddress address = temp.getAddress();
            if (address != null && address.getAddress() == inetAddress)
                player[0] = temp;
        }

        TakionLib lib = main.getApi().getLibrary();
        return text -> {
            String[] lines = text.split("\\r?\\n", -1);
            for (int i = 0; i < lines.length; i++)
                lines[i] = centerLine(lib, player[0], lines[i]);
            return String.join("\n", lines);
        };
    }

    private String centerLine(TakionLib lib, Player player, String line) {
        String text = line == null ? "" : line;

        String p = main.getApi().getConfiguration().getCenterPrefix();
        if (p == null || p.isEmpty() || !text.startsWith(p))
            return lib.colorize(player, text);

        text = text.substring(p.length()).trim();
        if (text.isEmpty()) return "";

        text = lib.replace(player, text);
        return PrismaticAPI.colorize(player, lib.getCharacterManager().align(130, p + text));
    }

    @EventHandler
    private void onPing(ServerListPingEvent event) {
        if (!main.isEnabled()) return;

        int motdCount = main.data.getList().size() - 1;
        if (motdIndex > motdCount) motdIndex = 0;

        String[] array = main.data.getList().get(motdIndex);
        String temp = array[1] != null ?
                String.join("\n", array) : array[0];

        event.setMotd(getOperator(event.getAddress()).apply(temp));

        motdIndex = main.config.isRandomMotd() ?
                random.nextInt(motdCount + 1) :
                (motdIndex < motdCount ? (motdIndex + 1) : 0);

        switch (main.config.getMaxPlayersType().toUpperCase(Locale.ENGLISH)) {
            case "CUSTOM":
                event.setMaxPlayers(main.config.getMaxPlayersCount());
                break;

            case "MAXIMUM":
                event.setMaxPlayers(Bukkit.getOnlinePlayers().size() + 1);
                break;

            default: break;
        }

        int iconCount = main.loader.getIcons().size() - 1;
        if (iconIndex > iconCount) iconIndex = 0;

        try {
            switch (main.config.getServerIconUsage().toUpperCase(Locale.ENGLISH)) {
                case "SINGLE":
                    event.setServerIcon(main.loader.getDefaultIcon());
                    break;

                case "LIST":
                    event.setServerIcon(main.loader.getIcons().get(iconIndex));
                    iconIndex = iconIndex < iconCount ? (iconIndex + 1) : 0;
                    break;

                case "RANDOM":
                    event.setServerIcon(main.loader.getIcons().get(iconIndex));
                    iconIndex = new Random().nextInt(iconCount + 1);
                    break;

                default: break;
            }
        } catch (Exception e) {
            main.getApi().getLibrary().getLogger().log(
                    LogLevel.WARN,
                    "Error when trying to set the server icon. Your server doesn't support this feature.",
                    "Avoid this error upgrading your server version and/or jar!"
            );
            e.printStackTrace();
        }
    }
}
