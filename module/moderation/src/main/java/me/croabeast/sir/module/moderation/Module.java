package me.croabeast.sir.module.moderation;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.CustomListener;
import me.croabeast.common.Registrable;
import me.croabeast.file.Configurable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.SIRApi;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

abstract class Module implements Registrable {

    final String moduleName, bypass;
    final Map<UUID, Integer> violations = new HashMap<>();

    private final Moderation main;
    final ConfigurableFile file;

    private int replaceIndex = 0;

    private final CustomListener listener = new CustomListener() {
        @Getter
        private final Status status = new Status();

        @EventHandler(priority = EventPriority.LOWEST)
        private void onChatViolation(AsyncPlayerChatEvent event) {
            if (!file.get("enabled", true)) return;
            if (processCancellation(event)) event.setCancelled(true);
        }
    };

    @SneakyThrows
    Module(Moderation main, String name) {
        this.main = main;
        this.moduleName = name;
        this.bypass = (this.file = new ExtensionFile(main, name, true)).get("bypass", "sir.moderation.bypass." + name);
    }

    private final Random random = new Random();

    String getReplacement(List<String> replacements, String word) {
        if (replacements.isEmpty()) return word;

        final int size = replacements.size();
        if (replaceIndex >= size) replaceIndex = 0;

        String type = file.get("replace-options.type", "CHARACTER");

        boolean isCharacter = type.matches("(?i)character");
        boolean order = file.get("replace-options.order", true);

        int index = order ? replaceIndex++ : random.nextInt(size);

        if (isCharacter) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                sb.append(replacements.get(index));
            }
            return sb.toString();
        }

        return replacements.get(index);
    }

    abstract boolean processCancellation(AsyncPlayerChatEvent event);

    boolean validateAndExecuteActions(Player player, String message, int max) {
        MessageSender sender = main.getApi().getLibrary().getLoadedSender()
                .addPlaceholder("{player}", player.getName())
                .addPlaceholder("{message}", message)
                .addPlaceholder("{type}", main.config.getName(moduleName))
                .setLogger(true);

        String loggerResults = main.config.getViolationLogFormat();
        if (main.config.isStaffNotified())
            sender.copy().setTargets(
                            CollectionBuilder.of(Bukkit.getOnlinePlayers())
                                    .filter(p -> main.getApi().getUserManager()
                                            .hasPermission(p, main.config.getNotifyPermission()))
                                    .toSet()
                    )
                    .send(loggerResults);

        if (main.config.isViolationLogging())
            sender.copy().setTargets((Player) null).send(loggerResults);

        main.getApi().getLibrary().getLoadedSender()
                .setTargets(player)
                .send(file.toStringList("warnings"));

        ConfigurationSection actions = file.getSection("actions");
        if (actions == null) return false;

        UUID uuid = player.getUniqueId();

        int count = violations.getOrDefault(uuid, 0) + 1;
        violations.put(uuid, count);

        if (count >= max) {
            violations.put(uuid, 0);
            main.getApi().getLibrary().getLoadedSender()
                    .setTargets(player)
                    .send(Configurable.toStringList(actions, "messages"));

            SIRApi.executeCommands(
                    main.getApi().getUserManager().getUser(player),
                    Configurable.toStringList(actions, "commands")
            );
            return true;
        }

        return false;
    }

    @Override
    public boolean isRegistered() {
        return listener.isRegistered();
    }

    @Override
    public boolean register() {
        return listener.register(main.getApi().getPlugin());
    }

    @Override
    public boolean unregister() {
        return listener.unregister();
    }
}
