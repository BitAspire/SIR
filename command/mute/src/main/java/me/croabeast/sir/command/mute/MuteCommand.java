package me.croabeast.sir.command.mute;

import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.MuteData;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

final class MuteCommand extends Command {

    MuteCommand(MuteProvider main) {
        super(main, "mute");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        if (args.length == 0)
            return Utils.create(this, sender).send("help.perm");

        SIRUser target = main.getApi().getUserManager().fromClosest(args[0]);
        if (target == null) return checkPlayer(sender, args[0]);

        String reason = getLang().get(
                "lang.default.mute-reason", "Not following server rules.");

        if (args.length > 1) {
            String temp = SIRApi.joinArray(1, args);
            if (temp != null) reason = temp;
        }

        MessageSender message = Utils.create(this, sender)
                .addPlaceholder("{reason}", reason)
                .addPlaceholder("{target}", target.getName())
                .addPlaceholder("{admin}", sender.getName());

        MuteData data = target.getMuteData();
        if (data.isMuted()) {
            long remaining = data.getRemaining();
            String path = remaining < 1 ? "perm" : "temp";

            if (remaining > 0)
                message.addPlaceholder("{time}", parseTime(remaining));

            return message.send("is-muted." + path);
        }

        data.mute(-1, sender.getName(), reason);
        return message.setTargets(Bukkit.getOnlinePlayers()).send("action.perm");
    }
}
