package me.croabeast.sir.command.print;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
class TargetLoader {

    @NotNull
    Set<Player> getTargets(CommandSender sender, String input) {
        if (StringUtils.isBlank(input)) return new HashSet<>();

        if (input.matches("@[Aa]"))
            return new HashSet<>(Bukkit.getOnlinePlayers());

        Player player = Bukkit.getPlayer(input);
        if (player == sender || player != null) return Sets.newHashSet(player);

        SIRApi api = SIRApi.instance();

        String[] array = input.split(":", 2);
        switch (array[0].toLowerCase(Locale.ENGLISH)) {
            case "world":
                World world = Bukkit.getWorld(array[1]);
                return world != null ?
                        new HashSet<>(world.getPlayers()) : new HashSet<>();

            case "perm":
            case "permission":
                return Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(p -> api.getUserManager().hasPermission(p, array[1]))
                        .collect(Collectors.toSet());

            case "group":
                return Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(p -> api.getChat().isPrimaryGroup(p, array[1]))
                        .collect(Collectors.toSet());

            default:
                return new HashSet<>();
        }
    }

    void sendConfirmation(SIRCommand command, CommandSender sender, String input) {
        Set<Player> targets = getTargets(sender, input);
        MessageSender displayer = SIRCommand.Utils.create(command, sender);

        if (targets.isEmpty()) {
            displayer.addPlaceholder("{target}", input).send("reminder.empty");
            return;
        }

        if (targets.size() == 1) {
            String target = Lists.newArrayList(targets).get(0).getName();

            if ((!(sender instanceof Player) || !targets.contains(sender))) {
                displayer.addPlaceholder("{target}", target).send("reminder.success");
            }
            return;
        }

        displayer.addPlaceholder("{target}", input).send("reminder.success");
    }
}
