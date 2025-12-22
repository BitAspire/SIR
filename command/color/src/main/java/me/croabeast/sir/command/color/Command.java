package me.croabeast.sir.command.color;

import me.croabeast.command.BaseCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.ColorData;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

final class Command extends SIRCommand {

    Command(ColorProvider main) throws IOException {
        super("chat-color", new ExtensionFile(main, "lang", true));
        editSubCommand("help", (s, a) -> createSender(s).send("help"));

        for (ChatColor color : ChatColor.values()) {
            final String name = color.asBungee().getName();

            switch (color) {
                case MAGIC:
                case STRIKETHROUGH:
                case UNDERLINE:
                case BOLD:
                case ITALIC:
                    editSubCommand(name, (s, a) -> {
                        if (!(s instanceof Player))
                            return createSender(s).send("need-player");

                        SIRUser user = main.getApi().getUserManager().getUser(s);
                        if (user == null)
                            return checkPlayer(s, s.getName());

                        ColorData data = user.getColorData();
                        Set<String> formats = data.getFormats();

                        String to = color.toString();
                        boolean added = !formats.remove(to) && formats.add(to);

                        String path = added ? "add" : "remove";
                        return createSender(s)
                                .addPlaceholder("{format}", to + name)
                                .send("format." + path);
                    });
                    continue;

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
                    editSubCommand(name, (s, a) -> {
                        if (!(s instanceof Player))
                            return createSender(s).send("need-player");

                        SIRUser user = main.getApi().getUserManager().getUser(s);
                        if (user == null)
                            return checkPlayer(s, s.getName());

                        user.getColorData().setColorStart(color.toString());
                        return createSender(s)
                                .addPlaceholder("{color}", color + name)
                                .send("color");
                    });
                    continue;

                case RESET:
                    editSubCommand("reset", (s, a) -> {
                        if (!(s instanceof Player))
                            return createSender(s).send("need-player");

                        SIRUser user = main.getApi().getUserManager().getUser(s);
                        if (user == null)
                            return checkPlayer(s, s.getName());

                        user.getColorData().removeAnyFormats();
                        return createSender(s).send("reset");
                    });
            }
        }

        editSubCommand("gradient", (s, a) -> {
            if (!(s instanceof Player))
                return createSender(s).send("need-player");

            if (a.length != 2)
                return createSender(s).send("gradient.usage");

            SIRUser user = main.getApi().getUserManager().getUser(s);
            if (user == null)
                return checkPlayer(s, s.getName());

            ColorData data = user.getColorData();
            data.setColorStart(a[0]);
            data.setColorEnd(a[1]);

            return createSender(s)
                    .addPlaceholder("{gradient}", a[0])
                    .addPlaceholder("{end}", a[1])
                    .send("gradient.success");
        });

        editSubCommand("rainbow", (s, a) -> {
            if (!(s instanceof Player))
                return createSender(s).send("need-player");

            SIRUser user = main.getApi().getUserManager().getUser(s);
            if (user == null)
                return checkPlayer(s, s.getName());

            user.getColorData().setColorStart("<R:1>");
            user.getColorData().setColorEnd("</R>");

            return createSender(s)
                    .addPlaceholder("{rainbow}", "<R:1>")
                    .addPlaceholder("{end}", "</R>")
                    .send("rainbow");
        });
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return createSender(sender).send("help");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        TabBuilder builder = createBasicTabBuilder();

        for (BaseCommand sub : getSubCommands()) {
            Deque<String> list = new LinkedList<>(sub.getAliases());
            list.addFirst(sub.getName());

            builder.addArguments(0, (s, a) -> sub.isPermitted(s), list);
        }

        return builder
                .addArguments(1, (s, a) -> a[0].matches("(?i)gradient"), "<#FFFFFF>", "<#FFFFFFF:#FFFFFF>")
                .addArgument(2, (s, a) -> a[0].matches("(?i)gradient"), "</#FFFFFF>");
    }
}
