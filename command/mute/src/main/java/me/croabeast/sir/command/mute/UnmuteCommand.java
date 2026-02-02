package me.croabeast.sir.command.mute;

import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

final class UnmuteCommand extends Command {

    UnmuteCommand(MuteProvider main) {
        super(main, "unmute");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        if (args.length < 1)
            return Utils.create(this, sender).send("help.unmute");

        SIRUser target = main.getApi().getUserManager().fromClosest(args[0]);
        if (target == null) return checkPlayer(sender, args[0]);

        String reason = getLang().get(
                "lang.default.unmute-reason", "Time ended.");

        if (args.length > 1) {
            String temp = SIRApi.joinArray(1, args);
            if (temp != null) reason = temp;
        }

        MessageSender message = Utils.create(this, sender)
                .addPlaceholder("{reason}", reason)
                .addPlaceholder("{target}", target.getName())
                .addPlaceholder("{admin}", sender.getName());

        if (target.getMuteData().isMuted()) {
            target.getMuteData().unmute();
            return message.send("action.unmute");
        }

        return message.send("is-muted.unmute");
    }
}
