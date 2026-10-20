package com.bitaspire.sir.module.custom;

import lombok.Getter;
import me.croabeast.file.Configurable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
final class Node {

    enum Type { LITERAL, STRING, INTEGER, NUMBER, BOOLEAN, PLAYER, CHOICE, WORLD, OFFLINE_PLAYER, DURATION, MATERIAL, ENTITY }

    @Getter
    static final class Outcome {

        private final String name;
        private final double weight;
        private final String permission;
        private final List<String> messages, commands;

        private Outcome(String name, ConfigurationSection section) {
            this.name = name;
            weight = section.getDouble("weight", 1);
            permission = section.getString("permission", "");
            messages = Configurable.toStringList(section, "messages");
            commands = Configurable.toStringList(section, "commands");
        }
    }

    private final String name;
    private final Type type;
    // Not final: 'argument-permissions' fills the empty ones after the tree is read.
    private String permission;
    // Conditions that must all hold to use this argument, checked like [if].
    private final List<String> requires;
    private final String denyMessage, confirmMessage;
    // Seconds to use the command again in to confirm it; 0 asks for no confirmation.
    private final long confirm;
    private final List<String> aliases, choices, suggestions, messages, commands;
    private final double min, max;
    // Money taken from players who use this argument as the last one; -1 when it has no cost.
    private final double cost;
    private final boolean greedy, exact;
    private final List<Node> children = new ArrayList<>();

    // Fixed values of 'values', like a placeholder written once and used in many lines.
    private final Map<String, String> values = new LinkedHashMap<>();
    // Keys of the values written with [once], solved a single time when the command is used.
    private final Set<String> once = new HashSet<>();

    // Rolled once per use and shared by every line, unlike the inline {random:...}.
    private final Map<String, RandomValue> randomValues = new LinkedHashMap<>();
    private final int picks;
    private final boolean unique;
    private final List<Outcome> outcomes = new ArrayList<>();

    Node(String name, Type type, ConfigurationSection section, List<String> warnings) {
        this.name = name;
        this.type = type;

        permission = section.getString("permission", "");
        requires = Configurable.toStringList(section, "requires");
        denyMessage = section.getString("deny-message", "");
        confirm = Math.max(0, section.getLong("confirm"));
        confirmMessage = section.getString("confirm-message", "");
        aliases = Configurable.toStringList(section, "aliases");
        choices = Configurable.toStringList(section, "choices");
        suggestions = Configurable.toStringList(section, "suggestions");
        messages = Configurable.toStringList(section, "messages");
        commands = Configurable.toStringList(section, "commands");

        min = section.getDouble("min", -Double.MAX_VALUE);
        max = section.getDouble("max", Double.MAX_VALUE);
        cost = section.isSet("cost") ? section.getDouble("cost") : -1;
        greedy = section.getBoolean("greedy");
        exact = section.getBoolean("exact", true);

        ConfigurationSection fixed = section.getConfigurationSection("values");
        if (fixed != null)
            for (String key : fixed.getKeys(false)) {
                String value = fixed.getString(key, "");
                if (value.regionMatches(true, 0, "[once]", 0, 6)) {
                    once.add(key);
                    value = value.substring(6);
                }
                this.values.put(key, value);
            }

        ConfigurationSection values = section.getConfigurationSection("random-values");
        if (values != null)
            for (String key : values.getKeys(false)) {
                RandomValue value = RandomValue.of(key, values, "random value '" + key + "' of argument '" + name + "'", warnings);
                if (value != null) randomValues.put(key, value);
            }

        picks = section.getInt("random.picks", 1);
        unique = section.getBoolean("random.unique", true);

        ConfigurationSection pool = section.getConfigurationSection("random.outcomes");
        if (pool != null)
            for (String key : pool.getKeys(false)) {
                ConfigurationSection outcome = pool.getConfigurationSection(key);
                if (outcome == null) continue;

                Outcome parsed = new Outcome(key, outcome);
                if (parsed.weight <= 0)
                    warnings.add("outcome '" + key + "' of argument '" + name + "' has no weight, so it is never picked");
                outcomes.add(parsed);
            }

        if (section.isConfigurationSection("random") && outcomes.isEmpty())
            warnings.add("argument '" + name + "' has a random section without outcomes");

        if (type == Type.CHOICE && choices.isEmpty())
            warnings.add("argument '" + name + "' is a CHOICE without choices");
        if (min > max)
            warnings.add("argument '" + name + "' has a min greater than its max");

        ConfigurationSection arguments = section.getConfigurationSection("arguments");
        if (arguments == null) return;

        if (greedy) {
            warnings.add("argument '" + name + "' is greedy, so its arguments are ignored");
            return;
        }

        for (String key : arguments.getKeys(false)) {
            ConfigurationSection child = arguments.getConfigurationSection(key);
            if (child == null) continue;

            String raw = child.getString("type", "LITERAL").toUpperCase(Locale.ENGLISH);
            try {
                children.add(new Node(key, Type.valueOf(raw), child, warnings));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type '" + raw + "' in argument '" + key + "'");
            }
        }
    }

    // Puts the 'values' and 'random-values' of the reached nodes. The deepest node wins, and typed values
    // win over every 'values'; a value can use the typed ones. The [once] values are solved last, after the
    // random values, so every line sees the same result even when what they read changes during the use.
    static void fill(List<Node> reached, Map<String, String> values, Random random, int maxRepeat,
                     BiFunction<String, String, String> external) {
        List<String> once = new ArrayList<>();
        for (int i = reached.size() - 1; i >= 0; i--) {
            Node node = reached.get(i);
            for (Map.Entry<String, String> entry : node.values.entrySet()) {
                if (values.containsKey(entry.getKey())) continue;

                values.put(entry.getKey(), Lines.apply(entry.getValue(), values));
                if (node.once.contains(entry.getKey())) once.add(entry.getKey());
            }
        }

        for (Node node : reached) node.randomValues.values().forEach(value -> value.roll(values, random, maxRepeat));

        for (String key : once)
            values.put(key, Lines.evaluate(Lines.apply(values.get(key), values), random, external));
    }

    // 'argument-permissions': the LITERAL arguments right after the command without a permission get
    // '<command permission>.<name>', like the sub-commands of SIR. Deeper ones are left alone, so whoever
    // can use '/print chat' can use '/print chat <target> centered' too.
    void derivePermissions(String parent) {
        for (Node child : children)
            if (child.type == Type.LITERAL && StringUtils.isBlank(child.permission))
                child.permission = parent + "." + child.name.toLowerCase(Locale.ENGLISH);
    }

    boolean hasActions() {
        return !messages.isEmpty() || !commands.isEmpty() || !outcomes.isEmpty();
    }

    // Weighted picks among the outcomes the sender can use; with unique, an outcome is picked once at most.
    List<Outcome> pick(Predicate<String> permitted, Random random) {
        List<Outcome> pool = outcomes.stream()
                .filter(o -> o.weight > 0 && (StringUtils.isBlank(o.permission) || permitted.test(o.permission)))
                .collect(Collectors.toList());

        List<Outcome> picked = new ArrayList<>();
        for (int i = 0; i < picks && !pool.isEmpty(); i++) {
            double total = pool.stream().mapToDouble(o -> o.weight).sum(), roll = random.nextDouble() * total;

            Outcome chosen = pool.get(pool.size() - 1);
            for (Outcome outcome : pool)
                if ((roll -= outcome.weight) < 0) {
                    chosen = outcome;
                    break;
                }

            picked.add(chosen);
            if (unique) pool.remove(chosen);
        }
        return picked;
    }

    @Nullable
    private Player findPlayer(CommandSender sender, String arg) {
        Player player = exact ? Bukkit.getPlayerExact(arg) : Bukkit.getPlayer(arg);
        return player != null && isVisible(sender, player) ? player : null;
    }

    @Nullable
    private static World findWorld(String arg) {
        for (World world : Bukkit.getWorlds())
            if (world.getName().equalsIgnoreCase(arg)) return world;
        return null;
    }

    // Online first; offline players are only looked up among those who played here, so no web request is made.
    @Nullable
    private String findOffline(CommandSender sender, String arg) {
        Player online = findPlayer(sender, arg);
        if (online != null) return online.getName();

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers())
            if (arg.equalsIgnoreCase(offline.getName())) return offline.getName();
        return null;
    }

    // Legacy materials only exist for old plugins on 1.13+.
    @Nullable
    private static Material findMaterial(String arg) {
        Material material = Material.matchMaterial(arg);
        return material == null || material.name().startsWith("LEGACY_") ? null : material;
    }

    @Nullable
    private static EntityType findEntity(String arg) {
        String name = arg.toLowerCase(Locale.ENGLISH).replace("minecraft:", "");
        for (EntityType type : EntityType.values())
            if (type.name().equalsIgnoreCase(name)) return type;
        return null;
    }

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    // "90", "90s", "1h30m" or "2d" to seconds; -1 when it isn't a duration.
    static long seconds(String arg) {
        if (arg.matches("\\d+")) return Long.parseLong(arg);
        if (!arg.matches("(?i)(\\d+[smhdw])+")) return -1;

        long total = 0;
        Matcher matcher = DURATION_PART.matcher(arg);
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            switch (Character.toLowerCase(matcher.group(2).charAt(0))) {
                case 'w': total += amount * 604800; break;
                case 'd': total += amount * 86400; break;
                case 'h': total += amount * 3600; break;
                case 'm': total += amount * 60; break;
                default: total += amount;
            }
        }
        return total;
    }

    // The value exposed as {<name>}: the full name for PLAYER, OFFLINE_PLAYER and WORLD, so a partial or
    // differently cased name still gives the real one, and the seconds for DURATION.
    String value(CommandSender sender, String arg) {
        switch (type) {
            case PLAYER:
                Player player = findPlayer(sender, arg);
                return player != null ? player.getName() : arg;
            case OFFLINE_PLAYER:
                String offline = findOffline(sender, arg);
                return offline != null ? offline : arg;
            case WORLD:
                World world = findWorld(arg);
                return world != null ? world.getName() : arg;
            case DURATION:
                return String.valueOf(seconds(arg));
            case MATERIAL:
                Material material = findMaterial(arg);
                return material != null ? material.name().toLowerCase(Locale.ENGLISH) : arg;
            case ENTITY:
                EntityType entity = findEntity(arg);
                return entity != null ? entity.name().toLowerCase(Locale.ENGLISH) : arg;
            default:
                return arg;
        }
    }

    private static boolean isVisible(CommandSender sender, Player player) {
        return !(sender instanceof Player) || ((Player) sender).canSee(player);
    }

    boolean matches(CommandSender sender, String arg) {
        switch (type) {
            case LITERAL:
                return name.equalsIgnoreCase(arg) || aliases.stream().anyMatch(arg::equalsIgnoreCase);

            case INTEGER:
                try {
                    int value = Integer.parseInt(arg);
                    return value >= min && value <= max;
                } catch (NumberFormatException e) {
                    return false;
                }

            case NUMBER:
                try {
                    double value = Double.parseDouble(arg);
                    return value >= min && value <= max;
                } catch (NumberFormatException e) {
                    return false;
                }

            case BOOLEAN:
                return arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("false");

            case PLAYER:
                return findPlayer(sender, arg) != null;

            case CHOICE:
                return choices.stream().anyMatch(arg::equalsIgnoreCase);

            case WORLD:
                return findWorld(arg) != null;

            case OFFLINE_PLAYER:
                return findOffline(sender, arg) != null;

            case DURATION:
                long seconds = seconds(arg);
                return seconds >= 0 && seconds >= min && seconds <= max;

            case MATERIAL:
                return findMaterial(arg) != null;

            case ENTITY:
                return findEntity(arg) != null;

            default:
                return true;
        }
    }

    @Nullable
    Node find(CommandSender sender, String arg) {
        for (Node child : children)
            if (child.type == Type.LITERAL && child.matches(sender, arg)) return child;

        for (Node child : children)
            if (child.type != Type.LITERAL && child.matches(sender, arg)) return child;

        return null;
    }

    private static List<String> players(CommandSender sender) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> isVisible(sender, player))
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private static List<String> worlds() {
        return Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
    }

    // '@players' and '@worlds' inside 'suggestions' expand to the online players and the worlds.
    List<String> suggest(CommandSender sender) {
        if (!suggestions.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (String suggestion : suggestions)
                if (suggestion.equalsIgnoreCase("@players")) list.addAll(players(sender));
                else if (suggestion.equalsIgnoreCase("@worlds")) list.addAll(worlds());
                else list.add(suggestion);
            return list;
        }

        switch (type) {
            case LITERAL:
                return Collections.singletonList(name);
            case BOOLEAN:
                return Arrays.asList("true", "false");
            case PLAYER:
            case OFFLINE_PLAYER:
                return players(sender);
            case CHOICE:
                return choices;
            case WORLD:
                return worlds();
            case DURATION:
                return Arrays.asList("30s", "5m", "1h", "1d");
            case MATERIAL:
                return Arrays.stream(Material.values())
                        .filter(material -> !material.name().startsWith("LEGACY_"))
                        .map(material -> material.name().toLowerCase(Locale.ENGLISH))
                        .collect(Collectors.toList());
            case ENTITY:
                return Arrays.stream(EntityType.values())
                        .filter(EntityType::isSpawnable)
                        .map(type -> type.name().toLowerCase(Locale.ENGLISH))
                        .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    String syntax() {
        if (children.isEmpty()) return "";

        String joined = children.stream()
                .map(c -> c.type == Type.LITERAL ? c.name : "<" + c.name + ">")
                .collect(Collectors.joining("|"));

        return hasActions() ? "[" + joined + "]" : "<" + joined + ">";
    }
}
