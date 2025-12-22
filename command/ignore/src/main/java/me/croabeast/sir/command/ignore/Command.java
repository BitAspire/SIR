package me.croabeast.sir.command.ignore;

import me.croabeast.command.TabBuilder;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.IgnoreData;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

final class Command extends SIRCommand {

    private final IgnoreProvider main;

    Command(IgnoreProvider main) throws IOException {
        super("ignore", new ExtensionFile(main, "lang", true));
        this.main = main;
    }

    private final String[] baseKeys = {"{target}", "{type}"};

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            main.getApi().getLibrary().getServerLogger().log(
                    "&cYou can not ignore players in the console.");
            return true;
        }

        if (!isPermitted(sender)) return true;
        if (args.length == 0) return createSender(sender).send("help");

        if (args.length > 2)
            return isWrongArgument(sender, args[args.length - 1]);

        SIRUser user = main.getApi().getUserManager().getUser(sender);
        assert user != null;

        boolean chat = args.length == 2 && args[1].matches("(?i)-chat");
        String channel = getLang()
                .get("lang.channels." + (chat ? "chat" : "msg"), "");

        IgnoreData data = user.getIgnoreData();
        if (args[0].matches("(?i)@a")) {
            if (data.isIgnoringAll(chat)) {
                data.unignoreAll(chat);
            } else {
                data.ignoreAll(chat);
            }

            String temp = (data.isIgnoringAll(chat) ? "success" : "remove") + ".all";
            return createSender(sender)
                    .addPlaceholders(baseKeys, null, channel).send(temp);
        }

        SIRUser target = main.getApi().getUserManager().fromClosest(args[0]);
        if (target == null) return checkPlayer(sender, args[0]);

        if (data.isIgnoring(target, chat)) {
            data.unignore(target, chat);
        } else {
            data.ignore(target, chat);
        }

        String path = (data.isIgnoring(target, chat) ? "success" : "remove") + ".player";
        return createSender(sender)
                .addPlaceholders(baseKeys, target.getName(), channel).send(path);
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return createBasicTabBuilder()
                .addArguments(0, getOnlineNames())
                .addArgument(0, "@a")
                .addArgument(1, "-chat");
    }
}
