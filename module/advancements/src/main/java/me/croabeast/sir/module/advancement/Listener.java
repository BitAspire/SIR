package me.croabeast.sir.module.advancement;

import me.croabeast.sir.SIRApi;
import me.croabeast.sir.module.DiscordService;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

final class Listener extends me.croabeast.sir.Listener {

    private final Advancements main;
    private final Config config;
    private final Messages messages;

    Listener(Advancements main) {
        config = (this.main = main).config;
        messages = main.messages;
    }

    @EventHandler
    private void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!main.isEnabled()) return;

        Player player = event.getPlayer();
        SIRApi api = main.getApi();

        SIRUser user = api.getUserManager().getUser(player);
        if (user.isVanished() ||
                config.isProhibited(Config.Type.WORLD, player.getWorld().getName()))
            return;

        String gameMode = player.getGameMode().toString();
        if (config.isProhibited(Config.Type.GAME_MODE, gameMode)) return;

        Advancement advancement = event.getAdvancement();

        String key = advancement.getKey().toString();
        if (!main.data.information.containsKey(advancement) ||
                config.isProhibited(Config.Type.ADVANCEMENT, key)) return;

        List<String> criteria = new ArrayList<>(advancement.getCriteria());
        if (criteria.isEmpty()) return;

        AdvancementProgress progress = player.getAdvancementProgress(advancement);

        Date date = progress.getDateAwarded(criteria.get(criteria.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - (5 * 1000L))
            return;

        messages.send(advancement, player);

        if (!api.getModuleManager().isEnabled("Discord")) return;

        DiscordService discord = api.getModuleManager().getDiscordService();
        if (discord == null) return;

        UnaryOperator<String> replacer = messages.getReplacer(advancement);
        discord.sendMessage("advancements", player, replacer);
    }
}
