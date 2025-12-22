package me.croabeast.sir.module.channel;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.file.ConfigurableUnit;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.ChannelData;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

final class Command extends SIRCommand {

    private final Channels main;
    @Getter
    private final ConfigurableFile lang;

    Command(Channels main, ConfigurableFile lang) {
        super("chat-view");
        this.main = main;
        this.lang = lang;
    }

    private List<String> getKeys(SIRUser user) {
        return main.data.getLocals().values()
                .stream()
                .filter(c -> !c.hasPermission(user))
                .map(ConfigurableUnit::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            main.getApi().getLibrary().getLogger().log("&cYou can't toggle a local chat in console.");
            return true;
        }

        if (!isPermitted(sender)) return true;
        if (args.length == 0) return createSender(sender).send("help");

        if (args.length != 1)
            return isWrongArgument(sender, args[args.length - 1]);

        SIRUser user = main.getApi().getUserManager().getUser(sender);
        if (user == null) return createSender(sender).send("help");

        String key = null;
        for (String k : getKeys(user))
            if (k.matches("(?i)" + args[0])) {
                key = k;
                break;
            }

        if (key == null) return isWrongArgument(sender, args[0]);

        ChannelData data = user.getChannelData();
        data.toggle(key);

        return createSender(sender).addPlaceholder("{channel}", key)
                .send((data.isToggled(key)) + "");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return createBasicTabBuilder().addArguments(0, (s, a) -> getKeys(main.getApi().getUserManager().getUser(s)));
    }
}
