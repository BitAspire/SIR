package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.SIRApi;
import com.bitaspire.sir.command.CommandFile;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.user.SIRUser;
import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.Configurable;
import me.croabeast.takion.format.PlainFormat;
import me.croabeast.takion.logger.LogLevel;
import me.croabeast.takion.message.MessageSender;
import me.croabeast.vault.economy.EconomyProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class CustomCommand extends SIRCommand {

    // What the server sends as its F3 brand: its own name, except Spigot, which calls itself
    // CraftBukkit but sends "Spigot".
    private static final String SERVER_BRAND = serverBrand();

    private final CustomCommands main;
    @Getter
    private final Node root;
    @Getter
    private final boolean hidden;
    private final boolean toggleable, respectMute;

    private final String scope;
    private final List<String> worlds;
    private final long cooldown, globalCooldown;
    private final int maxRepeat;
    private final List<String> joinMessages, joinCommands, firstJoinMessages, firstJoinCommands;
    private final String cooldownMessage, usageMessage;

    CustomCommand(CustomCommands main, String name, ConfigurationSection section, List<String> warnings) {
        super(name, main.lang);
        this.main = main;

        root = new Node(name, Node.Type.LITERAL, section, warnings);
        hidden = section.getBoolean("hidden");
        toggleable = section.getBoolean("toggleable");
        respectMute = section.getBoolean("respect-mute");

        scope = section.getString("scope.sender", "ALL").toUpperCase(Locale.ENGLISH);
        if (!scope.matches("PLAYER|CONSOLE|ALL"))
            throw new IllegalArgumentException("Invalid scope.sender '" + scope + "'");

        worlds = Configurable.toStringList(section, "scope.worlds");
        cooldown = section.getLong("cooldown", main.config.getConfiguration().getLong("cooldown")) * 1000L;
        globalCooldown = section.getLong("global-cooldown") * 1000L;
        maxRepeat = main.config.getConfiguration().getInt("max-repeat", 1000);
        joinMessages = Configurable.toStringList(section, "on-join.messages");
        joinCommands = Configurable.toStringList(section, "on-join.commands");
        firstJoinMessages = Configurable.toStringList(section, "on-first-join.messages");
        firstJoinCommands = Configurable.toStringList(section, "on-first-join.commands");
        cooldownMessage = section.getString("cooldown-message", "");
        usageMessage = section.getString("usage-message", "");

        if (!section.isSet("permission"))
            section.set("permission", main.config.get("permission-prefix", "sir.custom.") + name);
        if (StringUtils.isBlank(section.getString("usage")))
            section.set("usage", ("/" + name + " " + root.syntax()).trim());

        if (section.getBoolean("argument-permissions")) root.derivePermissions(section.getString("permission", ""));
        applyFile(new CommandFile(name, section, null, section.getBoolean("override-existing")));
    }

    private boolean isNodePermitted(CommandSender sender, Node node, boolean log) {
        String permission = node.getPermission();
        if (StringUtils.isBlank(permission) || api.getUserManager().hasPermission(sender, permission))
            return true;

        if (log) Utils.create(this, sender)
                .addPlaceholder("{perm}", permission)
                .addPlaceholder("{permission}", permission)
                .send("no-permission", "<P> &cYou do not have permission: &f{perm}&c.");
        return false;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        boolean player = sender instanceof Player;

        if (scope.equals("PLAYER") && !player)
            return Utils.create(this, sender).send("only-player", "<P> &cOnly players can use this command.");
        if (scope.equals("CONSOLE") && player)
            return Utils.create(this, sender).send("only-console", "<P> &cOnly the console can use this command.");
        if (player && !worlds.isEmpty() && !worlds.contains(((Player) sender).getWorld().getName()))
            return Utils.create(this, sender).send("wrong-world", "<P> &cYou can't use this command in this world.");

        SIRUser user = api.getUserManager().getUser(sender);
        if (respectMute && user != null && user.getMuteData().isMuted())
            return Utils.create(this, sender).send("muted", "<P> &cYou can't use this command while muted.");

        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", sender.getName());
        values.put("args", String.join(" ", args));
        values.put("server-brand", SERVER_BRAND);
        putSender(values, sender);

        Node node = root;
        List<Node> reached = new ArrayList<>(Collections.singletonList(root));
        StringBuilder path = new StringBuilder("/" + getName());

        for (int i = 0; i < args.length; i++) {
            Node next = node.find(sender, args[i]);
            if (next == null) return getArgumentCheck().test(sender, args[i]);
            if (!isNodePermitted(sender, next, true)) return true;

            reached.add(node = next);
            if (node.isGreedy()) {
                values.put(node.getName(), SIRApi.joinArray(i, args));
                break;
            }

            if (node.getType() != Node.Type.LITERAL) values.put(node.getName(), node.value(sender, args[i]));
            path.append(' ').append(args[i]);
        }

        if (!node.hasActions()) {
            String usage = node == root ? getUsage() : (path + " " + node.syntax()).trim();
            return reply(sender, Lines.apply(usageMessage, values), "usage", "<P> &7Usage: &f{usage}", "{usage}", usage);
        }

        // The deepest argument reached with a cost sets it.
        double cost = -1;
        for (Node reachedNode : reached)
            if (reachedNode.getCost() >= 0) cost = reachedNode.getCost();

        String shown = BigDecimal.valueOf(Math.max(0, cost)).stripTrailingZeros().toPlainString();
        values.put("cost", shown);

        // Values first, so 'requires' and every message below can use them.
        Random random = ThreadLocalRandom.current();
        Node.fill(reached, values, random, maxRepeat, (name, spec) -> external(name, spec, sender));

        for (Node reachedNode : reached)
            for (String condition : reachedNode.getRequires())
                if (!Boolean.TRUE.equals(Lines.test(evaluate(condition, values, sender, random))))
                    return reply(sender, Lines.apply(reachedNode.getDenyMessage(), values),
                            "denied", "<P> &cYou can't use this command right now.");

        // The console never waits nor pays. '.bypass' skips both cooldowns, '.free' the cost.
        boolean bypass = !player || api.getUserManager().hasPermission(sender, getPermission() + ".bypass");
        UUID uuid = player ? ((Player) sender).getUniqueId() : null;

        if (!bypass) {
            long left = Math.max(
                    cooldown > 0 ? main.cooldowns.getRemaining(getName(), uuid) : 0,
                    globalCooldown > 0 ? main.cooldowns.getRemaining(getName(), CooldownStore.GLOBAL) : 0);

            long seconds = (left + 999) / 1000;
            if (left > 0) return reply(sender, Lines.apply(cooldownMessage, values),
                    "cooldown", "<P> &cWait &f{time}s &cbefore using this command again.",
                    "{time}", seconds, "{time-left}", Lines.duration(seconds));
        }

        EconomyProvider economy = api.getEconomy();
        boolean pays = player && cost > 0 && economy != null && economy.isEnabled()
                && !api.getUserManager().hasPermission(sender, getPermission() + ".free");

        if (pays && !economy.hasAmount((Player) sender, cost))
            return reply(sender, null, "not-enough-money", "<P> &cYou need &f{cost} &cto use this command.", "{cost}", shown);

        // The deepest argument with 'confirm' asks to use the same command again within that many seconds.
        Node confirming = null;
        for (Node reachedNode : reached)
            if (reachedNode.getConfirm() > 0) confirming = reachedNode;

        if (confirming != null) {
            String key = sender.getName() + " /" + getName() + " " + String.join(" ", args).toLowerCase(Locale.ENGLISH);
            Long until = main.confirms.remove(key);

            if (until == null || until < System.currentTimeMillis()) {
                long now = System.currentTimeMillis();
                main.confirms.values().removeIf(time -> time < now);
                main.confirms.put(key, System.currentTimeMillis() + confirming.getConfirm() * 1000L);
                return reply(sender, Lines.apply(confirming.getConfirmMessage(), values),
                        "confirm", "<P> &eUse the command again within &f{time}s &eto confirm.", "{time}", confirming.getConfirm());
            }
        }

        if (!bypass) {
            if (cooldown > 0) main.cooldowns.start(getName(), uuid, cooldown);
            if (globalCooldown > 0) main.cooldowns.start(getName(), CooldownStore.GLOBAL, globalCooldown);
        }
        if (pays) economy.withdraw((Player) sender, cost);

        List<Node.Outcome> picked = node.pick(perm -> api.getUserManager().hasPermission(sender, perm), random);
        values.put("outcomes", picked.stream().map(Node.Outcome::getName).collect(Collectors.joining(", ")));

        run(sender, node.getMessages(), node.getCommands(), values, random);
        for (Node.Outcome outcome : picked) {
            Map<String, String> outcomeValues = new LinkedHashMap<>(values);
            outcomeValues.put("outcome", outcome.getName());
            run(sender, outcome.getMessages(), outcome.getCommands(), outcomeValues, random);
        }
        return true;
    }

    // Sends 'override' when the command file sets one, else the lang key. Placeholders come in pairs.
    private boolean reply(CommandSender sender, @Nullable String override, String key, String def, Object... placeholders) {
        MessageSender reply = Utils.create(this, sender);
        for (int i = 0; i + 1 < placeholders.length; i += 2)
            reply.addPlaceholder(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));

        if (StringUtils.isBlank(override)) return reply.send(key, def);
        reply.send(Collections.singletonList(override));
        return true;
    }

    // Placeholders of who uses the command, or joins; the console has none of them.
    private static void putSender(Map<String, String> values, CommandSender sender) {
        if (!(sender instanceof Player)) return;

        Player player = (Player) sender;
        Location location = player.getLocation();

        values.put("display-name", player.getDisplayName());
        values.put("uuid", player.getUniqueId().toString());
        values.put("world", player.getWorld().getName());
        values.put("x", String.valueOf(location.getBlockX()));
        values.put("y", String.valueOf(location.getBlockY()));
        values.put("z", String.valueOf(location.getBlockZ()));
        values.put("health", BigDecimal.valueOf(player.getHealth()).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        values.put("food", String.valueOf(player.getFoodLevel()));
        values.put("level", String.valueOf(player.getLevel()));
        values.put("gamemode", player.getGameMode().name().toLowerCase(Locale.ENGLISH));
    }

    // Keywords win over argument names: an argument called 'all' can't be a [to:] target.
    private Collection<? extends CommandSender> getTargets(CommandSender sender, @Nullable String to, Map<String, String> values) {
        if (to == null) return Collections.singletonList(sender);

        String key = Lines.apply(to, values).trim(), lower = key.toLowerCase(Locale.ENGLISH);
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        switch (lower) {
            case "sender":
                return Collections.singletonList(sender);
            case "all":
            case "@a":
                return online;
            case "others":
                return online.stream().filter(p -> p != sender).collect(Collectors.toList());
            case "world":
                return sender instanceof Player ? ((Player) sender).getWorld().getPlayers() : online;
            default:
                break;
        }

        if (lower.startsWith("world:")) {
            World world = Bukkit.getWorld(key.substring(6));
            return world == null ? Collections.emptyList() : world.getPlayers();
        }

        if (lower.startsWith("radius:")) {
            if (!(sender instanceof Player)) return online;
            Location center = ((Player) sender).getLocation();

            try {
                double radius = Double.parseDouble(key.substring(7).trim());
                return center.getWorld().getPlayers().stream()
                        .filter(p -> p.getLocation().distanceSquared(center) <= radius * radius)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
        }

        if (lower.startsWith("perm:")) {
            String permission = key.substring(5);
            return online.stream()
                    .filter(p -> api.getUserManager().hasPermission(p, permission))
                    .collect(Collectors.toList());
        }

        if (lower.startsWith("group:")) {
            String group = key.substring(6);
            return online.stream()
                    .filter(p -> api.getChat().getPermissionProvider().isPrimaryGroup(p, group))
                    .collect(Collectors.toList());
        }

        Player target = Bukkit.getPlayerExact(values.getOrDefault(key, key));
        return target == null ? Collections.emptyList() : Collections.singletonList(target);
    }

    private static String serverBrand() {
        String name = Bukkit.getName();
        if (!name.equals("CraftBukkit")) return name;

        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return "Spigot";
        } catch (ClassNotFoundException e) {
            return name;
        }
    }

    // Player#getPing is 1.16.5+; older servers keep it in the 'ping' field of the handle.
    @Nullable
    private static String ping(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) return null;

        try {
            return String.valueOf(player.getPing());
        } catch (NoSuchMethodError e) {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                return String.valueOf(handle.getClass().getField("ping").get(handle));
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }
    }

    private static Map<String, String> withReceiver(Map<String, String> values, CommandSender receiver) {
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.put("to", receiver.getName());
        return copy;
    }

    private boolean isToggled(CommandSender receiver) {
        SIRUser user = api.getUserManager().getUser(receiver);
        return user == null || main.isToggled(user);
    }

    private void message(CommandSender receiver, String text) {
        // Takion drops blank lines, so empty ones are sent directly.
        if (StringUtils.isBlank(text)) {
            receiver.sendMessage("");
            return;
        }

        if (receiver instanceof Player) text = PlainFormat.PLACEHOLDER_API.accept((Player) receiver, text);
        Utils.create(receiver).send(Collections.singletonList(text));
    }

    // The functions of Lines.evaluate that need the server. {saved-player:...} reads the receiver's value.
    private String external(String name, String spec, CommandSender receiver) {
        switch (name) {
            case "ping":
                return ping(spec);
            case "saved":
                return main.data.get(null, spec);
            default:
                return receiver instanceof Player ? main.data.get(((Player) receiver).getUniqueId(), spec) : "";
        }
    }

    private String evaluate(String text, Map<String, String> values, CommandSender receiver, Random random) {
        return Lines.evaluate(Lines.apply(text, values), random, (name, spec) -> external(name, spec, receiver));
    }

    // [if:...] is checked for each receiver, so it can read their own saved values.
    private boolean passes(Lines.Line line, Map<String, String> values, CommandSender receiver, Random random) {
        return line.getCondition() == null ||
                Boolean.TRUE.equals(Lines.test(evaluate(line.getCondition(), values, receiver, random)));
    }

    // "name", "name:volume" or "name:volume:pitch"; the name is a Sound or a resource key like minecraft:block.bell.use.
    private static void playSound(Player player, String spec) {
        String[] parts = spec.split(":");
        int end = parts.length;
        float volume = 1, pitch = 1;

        if (end > 2 && NumberUtils.isNumber(parts[end - 1]) && NumberUtils.isNumber(parts[end - 2])) {
            volume = Float.parseFloat(parts[end - 2]);
            pitch = Float.parseFloat(parts[end - 1]);
            end -= 2;
        } else if (end > 1 && NumberUtils.isNumber(parts[end - 1])) {
            volume = Float.parseFloat(parts[--end]);
        }

        String name = String.join(":", Arrays.copyOf(parts, end));
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name.toUpperCase(Locale.ENGLISH)), volume, pitch);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), name.toLowerCase(Locale.ENGLISH), volume, pitch);
        }
    }

    // Runs 'on-first-join' the first time a player joins, then 'on-join', without permission, scope or
    // cooldown checks.
    void join(Player player, boolean first) {
        boolean firstLines = first && !(firstJoinMessages.isEmpty() && firstJoinCommands.isEmpty());
        if (!firstLines && joinMessages.isEmpty() && joinCommands.isEmpty()) return;

        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", player.getName());
        values.put("args", "");
        values.put("server-brand", SERVER_BRAND);
        putSender(values, player);

        Random random = ThreadLocalRandom.current();
        Node.fill(Collections.singletonList(root), values, random, maxRepeat, (name, spec) -> external(name, spec, player));

        if (firstLines) run(player, firstJoinMessages, firstJoinCommands, values, random);
        run(player, joinMessages, joinCommands, values, random);
    }

    private void run(CommandSender sender, List<String> messages, List<String> commands,
                     Map<String, String> values, Random random) {
        for (String raw : messages) schedule(sender, Lines.parse(raw, maxRepeat), values, random, true);
        for (String raw : commands) schedule(sender, Lines.parse(raw, maxRepeat), values, random, false);
    }

    // A [delay:...] only holds back its own line; later lines don't wait for it.
    private void schedule(CommandSender sender, Lines.Line line, Map<String, String> values, Random random, boolean message) {
        if (line.getDelay() <= 0) {
            use(sender, line, values, random, message);
            return;
        }

        long ticks = Math.max(1, Math.round(line.getDelay() * 20));
        api.getScheduler().runTaskLater(() -> use(sender, line, values, ThreadLocalRandom.current(), message), ticks);
    }

    // [for:<key>] uses the line once for each roll of that random value, with {<key>} and {<key>.value}
    // being the ones of that roll. Without rolls, the line isn't used.
    private void use(CommandSender sender, Lines.Line line, Map<String, String> values, Random random, boolean message) {
        if (line.getEach() == null) {
            useOnce(sender, line, values, random, message);
            return;
        }

        String key = Lines.apply(line.getEach(), values);
        int count = NumberUtils.toInt(values.get(key + ".count"), 0);

        for (int i = 1; i <= count; i++) {
            Map<String, String> roll = new LinkedHashMap<>(values);
            roll.put(key, values.getOrDefault(key + "." + i, ""));
            roll.put(key + ".value", values.getOrDefault(key + "." + i + ".value", ""));
            useOnce(sender, line, roll, random, message);
        }
    }

    private void useOnce(CommandSender sender, Lines.Line line, Map<String, String> values, Random random, boolean message) {
        // [log] writes the line to the console once, for the sender.
        if (line.isLog()) {
            Map<String, String> own = withReceiver(values, sender);
            if (passes(line, own, sender, random))
                main.getLogger().log(LogLevel.INFO, evaluate(line.getText(), own, sender, random));
            return;
        }

        // [save:...] stores the text instead of sending or running it. The key takes values too.
        if (line.getSave() != null) {
            String key = Lines.apply(line.getSave(), values);
            Collection<? extends CommandSender> owners = line.isPerPlayer() ?
                    getTargets(sender, line.getTo(), values) : Collections.singletonList(sender);

            for (CommandSender owner : owners) {
                Map<String, String> own = withReceiver(values, owner);
                if (!passes(line, own, owner, random)) continue;

                String text = evaluate(line.getText(), own, owner, random);
                if (!line.isPerPlayer()) main.data.set(null, key, text);
                else if (owner instanceof Player) main.data.set(((Player) owner).getUniqueId(), key, text);
            }
            return;
        }

        for (CommandSender target : getTargets(sender, line.getTo(), values)) {
            if (message && toggleable && target != sender && !isToggled(target)) continue;

            Map<String, String> own = withReceiver(values, target);
            if (!passes(line, own, target, random)) continue;

            if (line.getSound() != null && target instanceof Player)
                playSound((Player) target, evaluate(line.getSound(), own, target, random));

            // A line with a sound and no text only plays the sound.
            if (line.getSound() != null && StringUtils.isBlank(line.getText())) continue;

            if (message) {
                for (int i = 0; i < line.getRepeat(); i++) message(target, evaluate(line.getText(), own, target, random));
                continue;
            }

            List<String> queued = new ArrayList<>();
            for (int i = 0; i < line.getRepeat(); i++) queued.add(evaluate(line.getText(), own, target, random));
            SIRApi.executeCommands(api.getUserManager().getUser(target), queued);
        }
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return null;
    }

    @NotNull
    @Override
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] args) {
        return () -> {
            List<String> list = new ArrayList<>();
            if (!testPermissionSilent(sender)) return list;

            Node node = root;
            for (int i = 0; i < args.length - 1; i++) {
                if (node.isGreedy()) return list;

                node = node.find(sender, args[i]);
                if (node == null || !isNodePermitted(sender, node, false)) return list;
            }
            if (node.isGreedy()) return list;

            String last = args[args.length - 1];
            for (Node child : node.getChildren()) {
                if (!isNodePermitted(sender, child, false)) continue;

                for (String suggestion : child.suggest(sender))
                    if (suggestion.regionMatches(true, 0, last, 0, last.length()))
                        list.add(suggestion);
            }
            return list;
        };
    }
}
