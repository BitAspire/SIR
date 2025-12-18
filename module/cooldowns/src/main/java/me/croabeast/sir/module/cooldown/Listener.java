package me.croabeast.sir.module.cooldown;

import lombok.RequiredArgsConstructor;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
final class Listener extends me.croabeast.sir.Listener {

    private final Cooldowns main;

    private final Map<UUID, Integer> spamMap = new HashMap<>();
    private final Map<UUID, Long> timers = new HashMap<>();

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !main.isEnabled()) return;

        SIRUser user = main.getApi()
                .getUserManager()
                .getUser(event.getPlayer());
        if (user == null) return;

        CooldownUnit unit = main.data.getUnit(user);
        if (unit == null) return;

        long timer = timers.getOrDefault(user.getUuid(), 0L);

        int time = unit.getTime();
        if (time <= 0 || timer <= 0) return;

        long rest = System.currentTimeMillis() - timer;
        if (rest >= time * 1000L) return;

        event.setCancelled(true);
        int result = Math.round(rest / 1000F);

        if (unit.isSpamEnabled()) {
            int count = spamMap.getOrDefault(user.getUuid(), 0);

            if (rest >= unit.getSpamTimeLimit() * 1000L &&
                    count >= unit.getSpamCount())
            {
                SIRApi.executeCommands(user, unit.getSpamCommands());
                spamMap.put(user.getUuid(), ++count);
            }
        }

        if (user.isOnline())
            main.getApi().getLibrary().getLoadedSender()
                    .addPlaceholder("{time}", time - result)
                    .setLogger(false)
                    .setTargets(user.getPlayer())
                    .send(unit.getMessages());

        timers.put(user.getUuid(), System.currentTimeMillis());
    }
}
