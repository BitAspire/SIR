package com.bitaspire.sir.module.channel;

import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.file.ConfigurableUnit;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.user.ChannelData;
import com.bitaspire.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

final class Command extends SIRCommand {

    private final Channels main;

    Command(Channels main, ConfigurableFile lang) {
        super("chat-view", lang);
        this.main = main;
    }

    private List<String> getKeys(SIRUser user) {
        return main.data.getLocals().values()
                .stream()
                .filter(c -> c.isLocalAccessible())
                .filter(c -> !c.getAccess().isDefault())
                .filter(c -> c.isDefaultPermission() || c.hasPermission(user))
                .filter(c -> StringUtils.isBlank(c.getGroup()) || c.isInGroup(user))
                .map(ConfigurableUnit::getName)
                .collect(Collectors.toList());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            main.getApi().getLibrary().getLogger().log("&cYou can't toggle a local chat in console.");
            return true;
        }

        if (!isPermitted(sender)) return true;
        if (args.length == 0) return Utils.create(this, sender).send("help");

        if (args.length != 1)
            return getArgumentCheck().test(sender, args[args.length - 1]);

        SIRUser user = main.getApi().getUserManager().getUser(sender);
        if (user == null) return Utils.create(this, sender).send("help");

        String key = null;
        for (String k : getKeys(user))
            if (k.equalsIgnoreCase(args[0])) {
                key = k;
                break;
            }

        if (key == null) return getArgumentCheck().test(sender, args[0]);

        ChannelData data = user.getChannelData();
        data.toggle(key);

        return Utils.create(this, sender).addPlaceholder("{channel}", key)
                .send((data.isToggled(key)) + "");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder().addArguments(0, (s, a) -> getKeys(main.getApi().getUserManager().getUser(s)));
    }
}
