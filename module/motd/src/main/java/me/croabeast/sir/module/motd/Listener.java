package me.croabeast.sir.module.motd;

import lombok.RequiredArgsConstructor;
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
final class Listener extends me.croabeast.sir.Listener {

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
        return s -> lib.getCharacterManager()
                .align(134, lib.colorize(player[0], s));
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
