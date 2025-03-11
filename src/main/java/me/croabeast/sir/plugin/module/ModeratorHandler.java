package me.croabeast.sir.plugin.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.Registrable;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.manager.SIRUserManager;
import me.croabeast.sir.plugin.LangUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ModeratorHandler extends ListenerModule {

    final Set<Registrable> modules = new HashSet<>();

    ConfigurableFile getFile() {
        return FileData.Module.Chat.MODERATION.getFile();
    }

    interface Result<T> {

        boolean isEnabled();

        T get();
    }

    class MainOptions {

        private final ConfigurableFile file = getFile();

        private final Result<String> logOptions;
        private final Result<String> notifyOptions;

        MainOptions() {
            logOptions = new Result<String>() {

                private final String path = "options.log-violations.";

                @Override
                public boolean isEnabled() {
                    return file.get(path + "enabled", true);
                }

                @Override
                public String get() {
                    return file.get(path + "format", "");
                }
            };
            notifyOptions = new Result<String>() {

                private final String path = "options.notify-staff.";

                @Override
                public boolean isEnabled() {
                    return file.get(path + "enabled", true);
                }

                @Override
                public String get() {
                    return file.get(path + "permission", "sir.moderation.staff.notify");
                }
            };
        }

        String getName(String path) {
            return file.get("options.lang-names." + path, path);
        }
    }

    ModeratorHandler() {
        super(Key.MODERATION);
        modules.add(new CustomListener() {

            private final String path = "modules.format.";

            @Getter @Setter
            private boolean registered = false;

            private final Result<String> base = new Result<String>() {
                @Override
                public boolean isEnabled() {
                    return getFile().get(path + "enabled", false);
                }

                @Override
                public String get() {
                    return getFile().get(path + "bypass", "sir.moderation.bypass.format");
                }
            };

            @EventHandler(priority = EventPriority.LOWEST)
            private void onChatEvent(AsyncPlayerChatEvent event) {
                if (!base.isEnabled() ||
                        SIRUserManager.hasPerm(event.getPlayer(), base.get()))
                    return;

                String message = event.getMessage();

                if (getFile().get(path + "capitalize", false) &&
                        !message.isEmpty())
                    message = Character.toUpperCase(message.charAt(0)) +
                            message.substring(1);

                final String charPath = path + "characters.";

                String prefix = getFile().get(charPath + "prefix", "");
                String suffix = getFile().get(charPath + "suffix", "");

                prefix = StringUtils.isBlank(prefix) ? "" : prefix;
                suffix = StringUtils.isBlank(suffix) ? "" : suffix;

                event.setMessage(prefix + message + suffix);
            }

            @Override
            public boolean register() {
                return register(plugin);
            }
        });

        modules.add(new BaseModule("swearing", "banned-words") {

            @Override
            boolean hasViolation(String message) {
                return false;
            }

            @Override
            String applyReplacements(String message) {
                return "";
            }
        });
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Replacer {

        private final Random random = new Random();
        private int index = 0;

        private final String main, inputs;

        List<RegexLine> getInputs() {
            return CollectionBuilder.of(getFile().toStringList(main + inputs)).map(RegexLine::new).toList();
        }

        boolean isCharacter() {
            final String s = getFile().get(main + "replace-options.type", "");
            return !s.matches("(?i)character|word") || s.matches("(?i)character");
        }

        boolean isOrder() {
            return getFile().get(main + "replace-options.order", false);
        }

        List<String> getReplacements() {
            return getFile().toStringList(main + "replace-options.replacements");
        }
    }

    static class RegexLine {

        private boolean regex = false;
        private final String line;

        private RegexLine(String line) {
            Pattern pattern = Pattern.compile("(?i)^ *\\[regex] *");

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                line = line.replace(matcher.group(), "");
                this.regex = true;
            }

            this.line = line;
        }

        boolean find(String input) {
            return regex ? Pattern.compile(line).matcher(input).find() : input.contains(line);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Actions {

        private final String main;

        List<String> getMessages() {
            return getFile().toStringList(main + "actions.messages");
        }

        List<String> getCommands() {
            return getFile().toStringList(main + "actions.commands");
        }

        int getMax() {
            return getFile().get(main + "actions.maximum-violations", 3);
        }
    }

    abstract class BaseModule implements Registrable {

        private final String[] keys = {"{player}", "{type}", "{message}"};

        private final CustomListener listener = new CustomListener() {
            @Getter @Setter
            private boolean registered = false;

            @EventHandler(priority = EventPriority.LOWEST)
            private void onChatViolation(AsyncPlayerChatEvent event) {
                handleViolation(event);
            }
        };

        private final String name, mainPath, inputPath;
        private final Map<Player, Integer> violations = new HashMap<>();

        private int index = 0;
        private final Random random = new Random();

        BaseModule(String name, String inputPath) {
            this.name = name;
            mainPath = "modules." + name + ".";
            this.inputPath = inputPath;
        }

        boolean isEnabled() {
            return getFile().get(mainPath + "enabled", false);
        }

        boolean isBlocking() {
            final String s = getFile().get(mainPath + "control", "");
            return !s.matches("(?i)block|replace") || s.matches("(?i)block");
        }

        List<String> getWarnings() {
            return getFile().toStringList(mainPath + "warnings");
        }

        Actions getActions() {
            return new Actions(this.mainPath);
        }

        private String getNextReplacement(Replacer replacer) {
            List<String> replacements = replacer.getReplacements();
            if (replacements.isEmpty()) return "";

            if (replacer.isOrder()) {
                String replacement = replacements.get(index);
                index = (index + 1) % replacements.size();
                return replacement;
            }

            return replacements.get(random.nextInt(replacements.size()));
        }

        abstract boolean hasViolation(String message);

        String applyReplacements(String message) {
            Replacer replacer = new Replacer(mainPath, inputPath);

            return message;
        }

        void verifyBlockAndReplace(AsyncPlayerChatEvent event) {
            if (isBlocking()) {
                event.setCancelled(true);
                return;
            }

            if (inputPath != null)
                event.setMessage(applyReplacements(event.getMessage()));
        }

        private void handleViolation(AsyncPlayerChatEvent event) {
            if (!isEnabled() || !hasViolation(event.getMessage())) return;

            Player player = event.getPlayer();

            plugin.getLibrary().getLoadedSender()
                    .addPlaceholder("{player}", player)
                    .setTargets(player)
                    .send(getWarnings());

            int count = violations.getOrDefault(player, 0) + 1;
            violations.put(player, count);

            verifyBlockAndReplace(event);

            final String message = event.getMessage();
            MainOptions options = new MainOptions();

            Object[] objects = {player, options.getName(name), message};

            UnaryOperator<String> operator = s ->
                    ReplaceUtils.replaceEach(keys, objects, s);

            Result<String> logs = options.logOptions;

            String format = operator.apply(logs.get());
            if (logs.isEnabled())
                plugin.getLibrary().getServerLogger().log(format);

            Result<String> staff = options.notifyOptions;

            if (staff.isEnabled()) {
                Set<? extends Player> set = CollectionBuilder
                        .of(Bukkit.getOnlinePlayers())
                        .filter(p -> SIRUserManager.hasPerm(p, staff.get())).toSet();

                plugin.getLibrary().getLoadedSender().setTargets(set).send(format);
            }

            Actions actions = getActions();
            if (count < actions.getMax()) return;

            LangUtils.executeCommands(player, actions.getCommands());
            plugin.getLibrary().getLoadedSender()
                    .addPlaceholder("{player}", player)
                    .setTargets(player)
                    .send(actions.getMessages());
        }

        @Override
        public boolean isRegistered() {
            return listener.isRegistered();
        }

        @Override
        public boolean register() {
            return listener.register(plugin);
        }

        @Override
        public boolean unregister() {
            return listener.unregister();
        }
    }
}
