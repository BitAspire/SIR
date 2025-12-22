package me.croabeast.sir.command.message;

import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

final class Values {

    private final SIRApi api;
    private final ConfigurableFile lang;
    private final boolean sender;

    Values(MessageProvider main, boolean sender) {
        this.api = main.getApi();
        this.lang = main.getLang();
        this.sender = sender;
    }

    String getPath() {
        return "lang.for-" + (sender ? "sender" : "receiver") + '.';
    }

    void playSound(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;

        SIRUser user = api.getUserManager().getUser(player);
        if (user != null) user.playSound(lang.get(getPath() + "sound", ""));
    }

    List<String> getOutput() {
        return lang.toStringList(getPath() + "message");
    }
}
