package me.croabeast.sir.command.color;

import me.croabeast.command.TabBuilder;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.ColorData;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

final class Command extends SIRCommand {

    private final ColorProvider main;

    Command(ColorProvider main) throws IOException {
        super("chat-color", new ExtensionFile(main, "lang", true));
        this.main = main;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;
        if (args.length == 0) return Utils.create(this, sender).send("help");

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "help":
                if (!isSubCommandPermitted(sender, "help", true)) return true;
                return Utils.create(this, sender).send("help");

            case "gradient": {
                if (!isSubCommandPermitted(sender, "gradient", true)) return true;

                if (!(sender instanceof Player)) return Utils.create(this, sender).send("need-player");
                if (rest.length != 2) return Utils.create(this, sender).send("gradient.usage");

                SIRUser user = main.getApi().getUserManager().getUser(sender);
                if (user == null) return checkPlayer(sender, sender.getName());

                ColorData data = user.getColorData();
                data.setColorStart(rest[0]);
                data.setColorEnd(rest[1]);

                return Utils.create(this, sender)
                        .addPlaceholder("{gradient}", rest[0])
                        .addPlaceholder("{end}", rest[1])
                        .send("gradient.success");
            }

            case "rainbow": {
                if (!isSubCommandPermitted(sender, "rainbow", true)) return true;
                if (!(sender instanceof Player)) return Utils.create(this, sender).send("need-player");

                SIRUser user = main.getApi().getUserManager().getUser(sender);
                if (user == null) return checkPlayer(sender, sender.getName());

                user.getColorData().setColorStart("<R:1>");
                user.getColorData().setColorEnd("</R>");

                return Utils.create(this, sender)
                        .addPlaceholder("{rainbow}", "<R:1>")
                        .addPlaceholder("{end}", "</R>")
                        .send("rainbow");
            }
        }

        ChatColor resolved = null;
        for (ChatColor color : ChatColor.values()) {
            String name = color.asBungee().getName();
            if (name.equalsIgnoreCase(subCommand)) {
                resolved = color;
                break;
            }
        }

        if (resolved == null) return getArgumentCheck().test(sender, args[0]);
        String colorName = resolved.asBungee().getName();
        if (!isSubCommandPermitted(sender, colorName, true)) return true;

        if (!(sender instanceof Player))
            return Utils.create(this, sender).send("need-player");

        SIRUser user = main.getApi().getUserManager().getUser(sender);
        if (user == null)
            return checkPlayer(sender, sender.getName());

        switch (resolved) {
            case MAGIC:
            case STRIKETHROUGH:
            case UNDERLINE:
            case BOLD:
            case ITALIC:
                ColorData data = user.getColorData();
                Set<String> formats = data.getFormats();

                String to = resolved.toString();
                boolean added = !formats.remove(to) && formats.add(to);

                String path = added ? "add" : "remove";
                return Utils.create(this, sender)
                        .addPlaceholder("{format}", to + colorName)
                        .send("format." + path);

            case RED:
            case GREEN:
            case BLUE:
            case YELLOW:
            case AQUA:
            case DARK_RED:
            case DARK_GREEN:
            case DARK_BLUE:
            case DARK_AQUA:
            case DARK_GRAY:
            case GRAY:
            case BLACK:
            case WHITE:
            case GOLD:
            case DARK_PURPLE:
            case LIGHT_PURPLE:
                user.getColorData().setColorStart(resolved.toString());
                return Utils.create(this, sender)
                        .addPlaceholder("{color}", resolved + colorName)
                        .send("color");

            case RESET:
                user.getColorData().removeAnyFormats();
                return Utils.create(this, sender).send("reset");

            default: break;
        }

        return getArgumentCheck().test(sender, args[0]);
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        LinkedHashSet<String> subCommands = new LinkedHashSet<>();
        subCommands.add("help");
        subCommands.add("gradient");
        subCommands.add("rainbow");

        for (ChatColor color : ChatColor.values())
            subCommands.add(color.asBungee().getName());

        TabBuilder builder = Utils.newBuilder();
        for (String name : subCommands)
            builder.addArguments(0, (s, a) -> isSubCommandPermitted(s, name, false), name);

        return builder
                .addArguments(1, (s, a) -> a[0].matches("(?i)gradient"), "<#FFFFFF>", "<#FFFFFFF:#FFFFFF>")
                .addArgument(2, (s, a) -> a[0].matches("(?i)gradient"), "</#FFFFFF>");
    }
}
