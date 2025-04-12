package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.CustomListener;
import me.croabeast.common.Registrable;
import me.croabeast.common.util.TextUtils;
import me.croabeast.file.Configurable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.sir.plugin.manager.UserManager;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ModeratorHandler extends ListenerModule {

    @Getter
    private final ConfigurableFile file = FileData.Module.Chat.MODERATION.getFile();

    private final MainOptions options = new MainOptions();
    private final Map<String, Registrable> modules = new HashMap<>();

    private final Random random = new Random();

    ModeratorHandler() {
        super(Key.MODERATION);

        modules.put("format", new CustomListener() {

            private final String path = "modules.format.";
            private final Options options = new Options(path, "bypass", "bypass.format");

            @Getter
            private final Status status = new Status();

            @EventHandler(priority = EventPriority.LOWEST)
            private void onChatEvent(AsyncPlayerChatEvent event) {
                if (!options.isEnabled() ||
                        UserManager.hasPerm(event.getPlayer(), options.getResult()))
                    return;

                String message = event.getMessage();

                if (file.get(path + "capitalize", false) && !message.isEmpty())
                    message = Character.toUpperCase(message.charAt(0)) + message.substring(1);

                final String charPath = path + "characters.";

                String prefix = file.get(charPath + "prefix", "");
                String suffix = file.get(charPath + "suffix", "");

                event.setMessage(prefix + message + suffix);
            }

            @Override
            public boolean register() {
                return register(plugin);
            }
        });

        modules.put("swearing", new Module("swearing") {

            @Override
            boolean processCancellation(AsyncPlayerChatEvent event) {
                String message = event.getMessage();
                Player player = event.getPlayer();

                List<RegexLine> lines = CollectionBuilder
                        .of(file.toStringList(path + "banned-words"))
                        .map(RegexLine::new).toList();

                boolean foundAny = false;
                boolean block = file.get(path + "control", "BLOCK").matches("(?i)block");

                for (RegexLine line : lines) {
                    Matcher matcher = line.matcher(message);
                    if (block && matcher.find()) {
                        foundAny = true;
                        break;
                    }

                    while (matcher.find()) {
                        List<String> list = file.toStringList(
                                path + "replace-options.replacements");

                        String group = matcher.group();
                        String replace = getReplacement(list, group);

                        if (!foundAny) foundAny = true;
                        event.setMessage(
                                message = message.replace(group, replace));
                    }
                }

                return foundAny && validateAndExecuteActions(
                        player, message,
                        file.get(path + "actions.maximum-violations", 3)
                );
            }
        });

        modules.put("caps", new Module("caps") {

            private int longestConsecutiveUppercase(String msg) {
                int maxConsecutive = 0;
                int currentCount = 0;

                for (final char ch : msg.toCharArray()) {
                    if (Character.isUpperCase(ch)) {
                        currentCount++;
                        if (currentCount > maxConsecutive)
                            maxConsecutive = currentCount;

                        continue;
                    }
                    currentCount = 0;
                }

                return maxConsecutive;
            }

            @Override
            boolean processCancellation(AsyncPlayerChatEvent event) {
                String message = event.getMessage();
                Player player = event.getPlayer();

                int capsCount = longestConsecutiveUppercase(message);

                int max = file.get(path + "maximum-caps", 10);
                if (capsCount <= max) return false;

                validateAndExecuteActions(player, message, max);
                if (file.get(path + "control", "BLOCK").matches("(?i)block"))
                    return true;

                event.setMessage(message.toLowerCase(Locale.ENGLISH));
                return false;
            }
        });

        modules.put("links", new Module("links") {
            @Override
            boolean processCancellation(AsyncPlayerChatEvent event) {
                String message = event.getMessage();
                Player player = event.getPlayer();

                List<String> links = file.toStringList(path + "allowed-links");
                boolean foundAny = false;

                Matcher matcher = TextUtils.URL_PATTERN.matcher(message);
                List<String> restrictedLinks = new ArrayList<>();

                while (matcher.find()) {
                    String match = matcher.group();
                    boolean allowed = false;

                    for (String link : links)
                        if (match.matches("(?i)" + Pattern.quote(link))) {
                            allowed = true;
                            break;
                        }

                    if (allowed) continue;

                    restrictedLinks.add(match);
                    foundAny = true;
                }

                if (foundAny) {
                    validateAndExecuteActions(
                            player, message,
                            file.get(path + "actions.maximum-violations", 3)
                    );

                    if (file.get(path + "control", "BLOCK").matches("(?i)block"))
                        return true;

                    List<String> list = file.toStringList(
                            path + "replace-options.replacements");

                    for (String link : restrictedLinks) {
                        String replace = getReplacement(list, link);
                        message = message.replace(link, replace);
                    }

                    event.setMessage(message);
                }

                return false;
            }
        });
    }

    private final static class RegexLine {

        private final Pattern pattern;

        private RegexLine(String line) {
            Pattern regex = Pattern.compile("(?i)\\[regex] *");
            Matcher matcher = regex.matcher(line);

            pattern = Pattern.compile(matcher.find() ?
                    line.replace(matcher.group(), "") : Pattern.quote(line));
        }

        Matcher matcher(String string) {
            return pattern.matcher(string);
        }
    }

    private class Options {

        private final String path, resultPath, defValue;

        private Options(String path, String resultPath, String perm) {
            this.path = path;
            this.resultPath = resultPath;
            this.defValue = "sir.moderation." + perm;
        }

        private Options(String path, String resultPath) {
            this.path = path;
            this.resultPath = resultPath;
            this.defValue = "";
        }

        boolean isEnabled() {
            return file.get(path + "enabled", true);
        }

        String getResult() {
            return file.get(path + resultPath, defValue);
        }
    }

    private final class MainOptions {

        final Options notifier = new Options("options.notify-staff.", "permission", "staff.notify");
        final Options logger = new Options("options.log-violations.", "format");

        String getName(String path) {
            return file.get("options.lang-names." + path, path);
        }
    }

    private abstract class Module implements Registrable {

        final String moduleName, path, bypass;
        final Map<UUID, Integer> violations = new HashMap<>();

        private int replaceIndex = 0;

        private final CustomListener listener = new CustomListener() {
            @Getter
            private final Status status = new Status();

            @EventHandler(priority = EventPriority.LOWEST)
            private void onChatViolation(AsyncPlayerChatEvent event) {
                if (!file.get(path + "enabled", true)) return;
                if (processCancellation(event)) event.setCancelled(true);
            }
        };

        Module(String name) {
            this.path = "modules." + (this.moduleName = name) + '.';
            this.bypass = file.get(path + "bypass", "sir.moderation.bypass." + name);
        }

        String getReplacement(List<String> replacements, String word) {
            if (replacements.isEmpty()) return word;

            final int size = replacements.size();
            if (replaceIndex >= size) replaceIndex = 0;

            String type = file.get(path + "replace-options.type", "CHARACTER");

            boolean isCharacter = type.matches("(?i)character");
            boolean order = file.get(path + "replace-options.order", true);

            int index = order ? replaceIndex++ : random.nextInt(size);

            if (isCharacter) {
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < word.length(); i++) {
                    sb.append(replacements.get(index));
                }
                return sb.toString();
            }

            return replacements.get(index);
        }

        abstract boolean processCancellation(AsyncPlayerChatEvent event);

        boolean validateAndExecuteActions(Player player, String message, int max) {
            MessageSender sender = plugin.getLibrary().getLoadedSender()
                    .addPlaceholder("{player}", player.getName())
                    .addPlaceholder("{message}", message)
                    .addPlaceholder("{type}", options.getName(moduleName))
                    .setLogger(true);

            String loggerResults = options.logger.getResult();

            if (options.notifier.isEnabled())
                sender.copy().setTargets(
                                CollectionBuilder.of(Bukkit.getOnlinePlayers())
                                        .filter(p -> UserManager.hasPerm(
                                                p,
                                                options.notifier.getResult())
                                        ).toSet()
                        )
                        .send(loggerResults);

            if (options.logger.isEnabled())
                sender.copy().setTargets((Player) null)
                        .send(loggerResults);

            plugin.getLibrary().getLoadedSender()
                    .setTargets(player)
                    .send(file.toStringList(path + "warnings"));

            ConfigurationSection actions = file.getSection(path + "actions");
            if (actions == null) return false;

            UUID uuid = player.getUniqueId();

            int count = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, count);

            if (count >= max) {
                violations.put(uuid, 0);
                plugin.getLibrary().getLoadedSender()
                        .setTargets(player)
                        .send(Configurable.toStringList(actions, "messages"));

                LangUtils.executeCommands(
                        player,
                        Configurable.toStringList(actions, "commands")
                );
                return true;
            }

            return false;
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

    @Override
    public boolean register() {
        modules.values().forEach(Registrable::register);
        return true;
    }

    @Override
    public boolean unregister() {
        modules.values().forEach(Registrable::unregister);
        return true;
    }
}
