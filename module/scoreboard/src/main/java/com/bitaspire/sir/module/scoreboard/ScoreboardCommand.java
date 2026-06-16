package com.bitaspire.sir.module.scoreboard;

import com.bitaspire.sir.command.SIRCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

class ScoreboardCommand extends SIRCommand {

    protected final ScoreboardModule module;

    ScoreboardCommand(ScoreboardModule module, ConfigurableFile lang) {
        super("scoreboard", lang);
        this.module = module;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;
        if (args.length == 0) return Utils.create(this, sender).send("help");

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "help":
                return !isSubCommandPermitted(sender, "help", true) || Utils.create(this, sender).send("help");
            case "reload":
                return !isSubCommandPermitted(sender, "reload", true) || reload(sender);
            case "toggle":
                return !isSubCommandPermitted(sender, "toggle", true) || toggle(sender, args);
            case "refresh":
                return !isSubCommandPermitted(sender, "refresh", true) || refresh(sender, args);
            case "debug":
                return !isSubCommandPermitted(sender, "debug", true) || debug(sender, args);
            default:
                return executeExtra(sender, args);
        }
    }

    protected boolean executeExtra(CommandSender sender, String[] args) {
        return getArgumentCheck().test(sender, args[0]);
    }

    private boolean reload(CommandSender sender) {
        module.reloadFiles();
        return Utils.create(this, sender).send("reload");
    }

    private boolean toggle(CommandSender sender, String[] args) {
        Player target = target(sender, args.length > 1 ? args[1] : null);
        if (target == null) return true;

        boolean enabled = module.toggle(target);
        return Utils.create(this, sender)
                .addPlaceholder("{player}", target.getName())
                .send(enabled ? "toggle-on" : "toggle-off");
    }

    private boolean refresh(CommandSender sender, String[] args) {
        if (args.length < 2) {
            module.refreshAll();
            return Utils.create(this, sender).send("refresh-all");
        }

        Player target = target(sender, args[1]);
        if (target == null) return true;
        module.refresh(target);

        return Utils.create(this, sender)
                .addPlaceholder("{player}", target.getName())
                .send("refresh");
    }

    private boolean debug(CommandSender sender, String[] args) {
        Player target = target(sender, args.length > 1 ? args[1] : null);
        if (target == null) return true;

        ScoreboardProfile profile = module.resolveProfile(target);
        return Utils.create(this, sender)
                .addPlaceholder("{player}", target.getName())
                .addPlaceholder("{profile}", profile.getName())
                .addPlaceholder("{animated}", String.valueOf(profile.hasAnimations()))
                .addPlaceholder("{lines}", String.valueOf(profile.lines(target, module, ScoreboardRefreshContext.create(0)).size()))
                .send("debug");
    }

    protected Player target(CommandSender sender, String name) {
        if (name == null) {
            if (sender instanceof Player) return (Player) sender;
            Utils.create(this, sender).send("player-only");
            return null;
        }

        Player target = Bukkit.getPlayer(name);
        if (target == null)
            Utils.create(this, sender).addPlaceholder("{target}", name).send("not-player");

        return target;
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder()
                .addArguments(0, "help", "toggle", "reload", "refresh", "debug")
                .addArguments(1, (sender, args) -> args[0].matches("(?i)toggle|refresh|debug"), Utils.getOnlineNames());
    }
}
