package me.croabeast.sir.command.mute;

import me.croabeast.command.TabBuilder;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.MuteData;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TempCommand extends Command {

    TempCommand(MuteProvider main) {
        super(main, "tempmute");
    }

    int convertToSeconds(String string) {
        Pattern p = Pattern.compile("^(?i)(\\d+)([a-z])?$");

        Matcher matcher = p.matcher(string);
        if (!matcher.find()) return 1;

        final String before = matcher.group(2);
        char identifier =
                before == null ? 's' : before.toCharArray()[0];

        int number = Integer.parseInt(matcher.group(1));

        switch (identifier) {
            case 'm':
                number = number * 60;
                break;
            case 'h': case 'H':
                number = number * 3600;
                break;
            case 'd': case 'D':
                number = number * 3600 * 24;
                break;
            case 'w': case 'W':
                number = number * 3600 * 24 * 7;
                break;
            case 'M':
                number = number * 3600 * 24 * 30;
                break;
            case 'y': case 'Y':
                number = number * 3600 * 24 * 365;
                break;
            case 's': default:
                break;
        }

        return number;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        if (args.length < 2)
            return Utils.create(this, sender).send("help.temp");

        SIRUser target = main.getApi().getUserManager().fromClosest(args[0]);
        if (target == null) return checkPlayer(sender, args[0]);

        String reason = getLang().get(
                "lang.default.mute-reason", "Not following server rules.");

        if (args.length > 2) {
            String temp = SIRApi.joinArray(2, args);
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

        int time = convertToSeconds(args[1]);
        data.mute(time, sender.getName(), reason);

        return message.setTargets(Bukkit.getOnlinePlayers())
                .addPlaceholder("{time}", parseTime(time)).send("action.temp");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder().addArguments(0, Utils.getOnlineNames()).addArgument(1, "<time>").addArgument(2, "<reason>");
    }
}
