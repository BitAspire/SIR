package com.bitaspire.sir.module.custom;

import lombok.RequiredArgsConstructor;
import me.croabeast.file.Configurable;
import me.croabeast.inventory.builder.AnvilInputBuilder;
import me.croabeast.inventory.builder.ChestBuilder;
import me.croabeast.inventory.builder.ConfirmBuilder;
import me.croabeast.inventory.builder.ItemBuilder;
import me.croabeast.inventory.gui.GuiItem;
import me.croabeast.prismatic.PrismaticAPI;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
final class Menu {

    private static final List<String> SENDERS = Arrays.asList("ALL", "PLAYER", "CONSOLE");

    private final CustomCommands main;

    private Plugin plugin() {
        return main.getApi().getPlugin();
    }

    private ConfigurationSection texts() {
        return main.menuFile.getConfiguration();
    }

    private static String apply(String text, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2)
            text = text.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        return text;
    }

    private String text(String path, String... placeholders) {
        return apply(texts().getString(path, ""), placeholders);
    }

    private String title(String key, String... placeholders) {
        return PrismaticAPI.colorize(text("titles." + key, placeholders));
    }

    private String word(String key) {
        return texts().getString("words." + key, key);
    }

    private void send(HumanEntity viewer, String key, String name) {
        main.getApi().getSender().setTargets(viewer).setLogger(false)
                .addPlaceholder("{command}", name)
                .send(main.lang.toStringList("lang." + key));
    }

    private GuiItem item(Material material, String name, List<String> lore, Consumer<InventoryClickEvent> action) {
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(PrismaticAPI.colorize(line));

        return ItemBuilder.of(material)
                .setName(PrismaticAPI.colorize(name))
                .setLore(colored)
                .setAction(action)
                .build(plugin());
    }

    private GuiItem item(Material material, String path, Consumer<InventoryClickEvent> action, String... placeholders) {
        List<String> lore = Configurable.toStringList(texts(), path + ".lore");
        lore.replaceAll(line -> apply(line, placeholders));
        return item(material, text(path + ".name", placeholders), lore, action);
    }

    private GuiItem field(Material material, String template, String field, String value,
                          Consumer<InventoryClickEvent> action, String... extra) {
        List<String> placeholders = new ArrayList<>(Arrays.asList(extra));
        placeholders.addAll(Arrays.asList(
                "title", texts().getString("fields." + field + ".title", field),
                "info", texts().getString("fields." + field + ".info", ""),
                "value", value
        ));
        return item(material, "templates." + template, action, placeholders.toArray(new String[0]));
    }

    private GuiItem back(String key, Consumer<HumanEntity> back) {
        return item(Material.ARROW, "templates.back", event -> back.accept(event.getWhoClicked()),
                "info", text("back." + key));
    }

    private static boolean isDrop(InventoryClickEvent event) {
        return event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;
    }

    private void confirm(HumanEntity viewer, String target, Runnable action, Consumer<HumanEntity> after) {
        ConfirmBuilder.of(title("delete", "target", target), plugin())
                .setOnConfirm(player -> {
                    action.run();
                    after.accept(player);
                })
                .setOnCancel(after::accept)
                .show(viewer);
    }

    // Anvil for short values, the chat when the anvil isn't supported. The consumer gets null on cancel.
    private void input(HumanEntity viewer, String field, String current, Consumer<String> onInput) {
        String title = texts().getString("fields." + field + ".title", field);

        AnvilInputBuilder.of(PrismaticAPI.colorize("&8" + title), plugin())
                .setInput(current)
                .setOnConfirm((player, text) -> onInput.accept(text.trim()))
                .setOnUnsupported(player -> main.chat.request(player, title, current, onInput))
                .show(viewer);
    }

    private String preview(String value) {
        if (StringUtils.isBlank(value)) return word("none");
        return value.length() > 40 ? value.substring(0, 40) + "..." : value;
    }

    private static List<String> split(String text) {
        List<String> values = new ArrayList<>();
        for (String value : text.split(","))
            if (StringUtils.isNotBlank(value)) values.add(value.trim());
        return values;
    }

    private static Object number(String text) {
        if (!NumberUtils.isNumber(text)) throw new IllegalArgumentException();
        return NumberUtils.createNumber(text);
    }

    private static Object integer(String text) {
        int value = NumberUtils.toInt(text, -1);
        if (value < 1) throw new IllegalArgumentException();
        return value;
    }

    private static Object seconds(String text) {
        long value = NumberUtils.toLong(text, -1);
        if (value < 0) throw new IllegalArgumentException();
        return value;
    }

    private void paging(ChestBuilder menu) {
        menu.setPagingButtons(2, 5, 5,
                item(Material.ARROW, text("pages.previous"), Collections.emptyList(), e -> {}),
                item(Material.ARROW, text("pages.next"), Collections.emptyList(), e -> {}));
    }

    // Moves the key at index one place up, so the order of arguments and lines can be changed.
    private static List<String> moveUp(List<String> keys, int index) {
        List<String> moved = new ArrayList<>(keys);
        if (index > 0) Collections.swap(moved, index, index - 1);
        return moved;
    }

    void openList(HumanEntity viewer) {
        String prefix = main.config.get("permission-prefix", "sir.custom.");
        List<GuiItem> items = new ArrayList<>();

        for (String name : main.getNames()) {
            YamlConfiguration yml = main.read(name);
            boolean enabled = yml.getBoolean("enabled", true);

            String[] placeholders = {
                    "command", name,
                    "status", word(enabled ? "enabled" : "disabled"),
                    "aliases", preview(String.join(", ", Configurable.toStringList(yml, "aliases"))),
                    "permission", yml.getString("permission", prefix + name)
            };

            List<String> lore = Configurable.toStringList(texts(), "list.command.lore");
            lore.replaceAll(line -> apply(line, placeholders));
            if (enabled && !main.loaded.containsKey(name)) lore.add(Math.min(1, lore.size()), text("list.not-loaded"));

            items.add(item(enabled ? Material.COMMAND_BLOCK : Material.BARRIER, text("list.command.name", placeholders), lore, event -> {
                HumanEntity who = event.getWhoClicked();

                if (isDrop(event)) {
                    confirm(who, "/" + name, () -> main.delete(name), this::openList);
                    return;
                }

                if (event.isShiftClick()) {
                    main.toggle(name);
                    openList(who);
                    return;
                }

                if (event.isRightClick()) {
                    input(who, "duplicate", name + "-copy", copy -> {
                        if (copy != null && !main.duplicate(name, copy.toLowerCase(Locale.ENGLISH)))
                            send(who, "create-failed", copy);
                        openList(who);
                    });
                    return;
                }

                openEditor(who, name);
            }));
        }

        ChestBuilder menu = ChestBuilder.of(6, title("list"), plugin())
                .cancelClicks()
                .populate(0, 0, 9, 5, items);

        if (items.isEmpty()) menu.addItem(0, 4, 2, item(Material.PAPER, "list.empty", e -> {}));

        GuiItem create = item(Material.NAME_TAG, "list.create",
                event -> input(event.getWhoClicked(), "name", "", text -> {
                    if (text == null) {
                        openList(viewer);
                        return;
                    }

                    String name = text.toLowerCase(Locale.ENGLISH);
                    if (main.create(name)) {
                        openEditor(viewer, name);
                        return;
                    }

                    send(viewer, "create-failed", name);
                    openList(viewer);
                }));

        GuiItem reload = item(Material.CLOCK, "list.reload", event -> {
            main.load();
            openList(event.getWhoClicked());
        });

        menu.forEachPage(page -> menu.addItem(page, 0, 5, create).addItem(page, 8, 5, reload));
        paging(menu);
        menu.show(viewer);
    }

    private GuiItem flag(String name, ConfigurationSection yml, String path, String field, Consumer<HumanEntity> reopen) {
        return flag(name, yml, path, field, path.equals("enabled"), reopen);
    }

    private GuiItem flag(String name, ConfigurationSection yml, String path, String field, boolean def,
                         Consumer<HumanEntity> reopen) {
        boolean value = yml.getBoolean(path, def);

        return field(value ? Material.LIME_DYE : Material.GRAY_DYE, "flag", field, word(String.valueOf(value)),
                event -> {
                    main.set(name, path, !value);
                    reopen.accept(event.getWhoClicked());
                });
    }

    // Lists are edited as comma separated values. A blank value removes the key; an invalid one changes nothing.
    private GuiItem text(String name, ConfigurationSection yml, Material material, String path, String field,
                         boolean list, Function<String, Object> parser, Consumer<HumanEntity> reopen) {
        String current = list ?
                String.join(", ", Configurable.toStringList(yml, path)) :
                yml.getString(path, "");

        return field(material, "text", field, preview(current), event -> {
            HumanEntity who = event.getWhoClicked();
            input(who, field, current, text -> {
                if (text != null)
                    try {
                        main.set(name, path, StringUtils.isBlank(text) ? null : parser.apply(text));
                    } catch (IllegalArgumentException e) {
                        send(who, "invalid-value", text);
                    }
                reopen.accept(who);
            });
        });
    }

    void openEditor(HumanEntity viewer, String name) {
        YamlConfiguration yml = main.read(name);
        String sender = yml.getString("scope.sender", "ALL").toUpperCase(Locale.ENGLISH);
        String next = SENDERS.get((SENDERS.indexOf(sender) + 1) % SENDERS.size());

        Consumer<HumanEntity> reopen = who -> openEditor(who, name);

        ChestBuilder.of(3, title("editor", "command", name), plugin())
                .cancelClicks()
                .addItem(0, 0, 0, back("list", this::openList))
                .addItem(0, 1, 0, field(Material.NAME_TAG, "text", "name", name, event -> {
                    HumanEntity who = event.getWhoClicked();
                    input(who, "name", name, text -> {
                        String renamed = text == null ? null : text.toLowerCase(Locale.ENGLISH);
                        if (renamed == null || renamed.equals(name)) {
                            openEditor(who, name);
                            return;
                        }

                        if (main.rename(name, renamed)) {
                            openEditor(who, renamed);
                            return;
                        }

                        send(who, "create-failed", renamed);
                        openEditor(who, name);
                    });
                }))
                .addItem(0, 2, 0, flag(name, yml, "enabled", "enabled", reopen))
                .addItem(0, 3, 0, flag(name, yml, "override-existing", "override-existing", reopen))
                .addItem(0, 4, 0, flag(name, yml, "hidden", "hidden", reopen))
                .addItem(0, 5, 0, field(Material.PLAYER_HEAD, "cycle", "sender", sender, event -> {
                    main.set(name, "scope.sender", next);
                    reopen.accept(event.getWhoClicked());
                }))
                .addItem(0, 6, 0, text(name, yml, Material.CLOCK, "cooldown", "cooldown", false, Menu::seconds, reopen))
                .addItem(0, 7, 0, flag(name, yml, "toggleable", "toggleable", reopen))
                .addItem(0, 8, 0, field(Material.HOPPER, "open", "arguments", "",
                        event -> openNode(event.getWhoClicked(), name, "")))
                .addItem(0, 0, 1, flag(name, yml, "respect-mute", "respect-mute", reopen))
                .addItem(0, 1, 1, text(name, yml, Material.TRIPWIRE_HOOK, "permission", "permission", false, t -> t, reopen))
                .addItem(0, 2, 1, text(name, yml, Material.NAME_TAG, "aliases", "aliases", true, Menu::split, reopen))
                .addItem(0, 3, 1, text(name, yml, Material.BOOK, "description", "description", false, t -> t, reopen))
                .addItem(0, 4, 1, text(name, yml, Material.OAK_SIGN, "usage", "usage", false, t -> t, reopen))
                .addItem(0, 5, 1, text(name, yml, Material.PAPER, "permission-message", "permission-message", false, t -> t, reopen))
                .addItem(0, 6, 1, text(name, yml, Material.GRASS_BLOCK, "scope.worlds", "worlds", true, Menu::split, reopen))
                .addItem(0, 7, 1, linesButton(name, yml, "messages", "", "messages", reopen))
                .addItem(0, 8, 1, linesButton(name, yml, "commands", "", "commands", reopen))
                .addItem(0, 0, 2, text(name, yml, Material.REPEATER, "global-cooldown", "global-cooldown", false, Menu::seconds, reopen))
                .addItem(0, 1, 2, flag(name, yml, "argument-permissions", "argument-permissions", reopen))
                .addItem(0, 2, 2, text(name, yml, Material.PAPER, "cooldown-message", "cooldown-message", false, t -> t, reopen))
                .addItem(0, 3, 2, text(name, yml, Material.OAK_SIGN, "usage-message", "usage-message", false, t -> t, reopen))
                .addItem(0, 5, 2, linesButton(name, yml.getConfigurationSection("on-first-join"), "messages", "on-first-join", "on-first-join-messages", reopen))
                .addItem(0, 6, 2, linesButton(name, yml.getConfigurationSection("on-first-join"), "commands", "on-first-join", "on-first-join-commands", reopen))
                .addItem(0, 7, 2, linesButton(name, yml.getConfigurationSection("on-join"), "messages", "on-join", "on-join-messages", reopen))
                .addItem(0, 8, 2, linesButton(name, yml.getConfigurationSection("on-join"), "commands", "on-join", "on-join-commands", reopen))
                .show(viewer);
    }

    // Node paths look like "arguments.a.arguments.b"; the root node is "".
    private static String parentOf(String path) {
        int index = path.lastIndexOf(".arguments.");
        return index < 0 ? "" : path.substring(0, index);
    }

    private static String display(String name, String path) {
        StringBuilder builder = new StringBuilder("/" + name);
        String[] parts = path.split("\\.");
        for (int i = 1; i < parts.length; i += 2) builder.append(' ').append(parts[i]);
        return builder.toString();
    }

    private static Material material(String type) {
        switch (type) {
            case "LITERAL": return Material.NAME_TAG;
            case "INTEGER": case "NUMBER": return Material.GOLD_NUGGET;
            case "BOOLEAN": return Material.LEVER;
            case "PLAYER": return Material.PLAYER_HEAD;
            case "CHOICE": return Material.CHEST;
            case "WORLD": return Material.GRASS_BLOCK;
            case "OFFLINE_PLAYER": return Material.SKELETON_SKULL;
            case "DURATION": return Material.CLOCK;
            case "MATERIAL": return Material.DIAMOND;
            case "ENTITY": return Material.SPAWNER;
            default: return Material.PAPER;
        }
    }

    private static String type(ConfigurationSection section) {
        return section.getString("type", "LITERAL").toUpperCase(Locale.ENGLISH);
    }

    // 'section' is the one holding the lines, null when it doesn't exist yet.
    private GuiItem linesButton(String name, @Nullable ConfigurationSection section, String key, String node,
                                String field, Consumer<HumanEntity> back) {
        String path = node.isEmpty() ? key : node + "." + key;
        String count = String.valueOf(section == null ? 0 : Configurable.toStringList(section, key).size());

        Material material = key.equals("messages") ? Material.WRITABLE_BOOK : key.equals("requires") ? Material.IRON_BARS : Material.COMMAND_BLOCK;
        return field(material, "lines", field, "",
                event -> openLines(event.getWhoClicked(), name, path, back), "count", count);
    }

    void openNode(HumanEntity viewer, String name, String path) {
        YamlConfiguration yml = main.read(name);
        ConfigurationSection section = path.isEmpty() ? yml : yml.getConfigurationSection(path);
        if (section == null) {
            openEditor(viewer, name);
            return;
        }

        boolean root = path.isEmpty();
        String type = root ? "LITERAL" : type(section);
        String pre = root ? "" : path + ".";
        Consumer<HumanEntity> reopen = who -> openNode(who, name, path);

        ConfigurationSection arguments = section.getConfigurationSection("arguments");
        List<String> keys = arguments == null ? new ArrayList<>() : new ArrayList<>(arguments.getKeys(false));
        List<GuiItem> children = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            int index = i;
            String key = keys.get(i);

            ConfigurationSection child = arguments.getConfigurationSection(key);
            if (child == null) continue;

            String childType = type(child);
            String childPath = pre + "arguments." + key;
            ConfigurationSection grand = child.getConfigurationSection("arguments");

            children.add(item(material(childType), "node.argument", event -> {
                HumanEntity who = event.getWhoClicked();

                if (isDrop(event)) {
                    confirm(who, key, () -> main.set(name, childPath, null), reopen);
                    return;
                }

                if (event.isShiftClick()) {
                    List<String> moved = moveUp(keys, index);
                    main.rebuild(name, pre + "arguments", moved, moved);
                    reopen.accept(who);
                    return;
                }

                openNode(who, name, childPath);
            },
                    "argument", key,
                    "type", childType,
                    "permission", preview(child.getString("permission", "")),
                    "arguments", String.valueOf(grand == null ? 0 : grand.getKeys(false).size()),
                    "messages", String.valueOf(Configurable.toStringList(child, "messages").size()),
                    "commands", String.valueOf(Configurable.toStringList(child, "commands").size())));
        }

        List<GuiItem> settings = new ArrayList<>();
        settings.add(back(root ? "editor" : "node", who -> {
            if (root) openEditor(who, name);
            else openNode(who, name, parentOf(path));
        }));

        if (!root) {
            List<Node.Type> types = Arrays.asList(Node.Type.values());
            Node.Type current = types.stream().filter(t -> t.name().equals(type)).findFirst().orElse(Node.Type.LITERAL);
            Node.Type next = types.get((types.indexOf(current) + 1) % types.size());

            String parent = path.substring(0, path.lastIndexOf('.'));
            String key = path.substring(path.lastIndexOf('.') + 1);

            settings.add(field(Material.NAME_TAG, "text", "name", key, event -> {
                HumanEntity who = event.getWhoClicked();
                input(who, "name", key, text -> {
                    ConfigurationSection siblings = main.read(name).getConfigurationSection(parent);
                    if (text == null || text.equals(key) || siblings == null) {
                        reopen.accept(who);
                        return;
                    }

                    if (!text.matches("[A-Za-z0-9_@-]+") || siblings.contains(text)) {
                        send(who, "argument-failed", text);
                        reopen.accept(who);
                        return;
                    }

                    List<String> from = new ArrayList<>(siblings.getKeys(false)), to = new ArrayList<>(from);
                    to.set(to.indexOf(key), text);
                    main.rebuild(name, parent, from, to);
                    openNode(who, name, parent + "." + text);
                });
            }));
            settings.add(field(material(type), "cycle", "type", type, event -> {
                main.set(name, pre + "type", next.name());
                reopen.accept(event.getWhoClicked());
            }));
            settings.add(text(name, yml, Material.TRIPWIRE_HOOK, pre + "permission", "argument-permission",
                    false, t -> t, reopen));

            switch (current) {
                case LITERAL:
                    settings.add(text(name, yml, Material.NAME_TAG, pre + "aliases", "argument-aliases",
                            true, Menu::split, reopen));
                    break;
                case INTEGER:
                case NUMBER:
                case DURATION:
                    settings.add(text(name, yml, Material.IRON_NUGGET, pre + "min", "min", false, Menu::number, reopen));
                    settings.add(text(name, yml, Material.GOLD_NUGGET, pre + "max", "max", false, Menu::number, reopen));
                    break;
                case CHOICE:
                    settings.add(text(name, yml, Material.CHEST, pre + "choices", "choices", true, Menu::split, reopen));
                    break;
                case STRING:
                    settings.add(flag(name, yml, pre + "greedy", "greedy", reopen));
                    break;
                default:
                    break;
            }

            if (current != Node.Type.LITERAL)
                settings.add(text(name, yml, Material.BOOKSHELF, pre + "suggestions", "suggestions",
                        true, Menu::split, reopen));
        }

        settings.add(linesButton(name, section, "messages", path, "messages", reopen));
        settings.add(linesButton(name, section, "commands", path, "commands", reopen));

        ChestBuilder menu = ChestBuilder.of(6, title("node", "path", display(name, path)), plugin())
                .cancelClicks()
                .populate(0, 1, 9, 4, children);

        if (children.isEmpty()) menu.addItem(0, 4, 2, item(Material.PAPER, "node.empty", e -> {}));

        GuiItem add = item(Material.NAME_TAG, "node.add", event -> {
            HumanEntity who = event.getWhoClicked();
            input(who, "name", "", text -> {
                if (text != null && text.matches("[A-Za-z0-9_@-]+") && !keys.contains(text)) {
                    main.set(name, pre + "arguments." + text + ".type", "LITERAL");
                    openNode(who, name, pre + "arguments." + text);
                    return;
                }

                if (text != null) send(who, "argument-failed", text);
                reopen.accept(who);
            });
        });

        GuiItem gates = field(Material.IRON_BARS, "lines", "conditions", "",
                event -> openGates(event.getWhoClicked(), name, path),
                "count", String.valueOf(Configurable.toStringList(section, "requires").size()));
        ConfigurationSection values = section.getConfigurationSection("random-values");
        GuiItem random = field(Material.ENDER_PEARL, "lines", "random-values", "",
                event -> openValues(event.getWhoClicked(), name, path, true),
                "count", String.valueOf(values == null ? 0 : values.getKeys(false).size()));
        ConfigurationSection pool = section.getConfigurationSection("random.outcomes");
        GuiItem outcomes = field(Material.ENDER_EYE, "lines", "outcomes", "",
                event -> openOutcomes(event.getWhoClicked(), name, path),
                "count", String.valueOf(pool == null ? 0 : pool.getKeys(false).size()));

        menu.forEachPage(page -> {
            for (int i = 0; i < settings.size(); i++) menu.addItem(page, i, 0, settings.get(i));
            menu.addItem(page, 0, 5, add).addItem(page, 1, 5, gates).addItem(page, 7, 5, random).addItem(page, 8, 5, outcomes);
        });
        paging(menu);
        menu.show(viewer);
    }

    void openLines(HumanEntity viewer, String name, String path, Consumer<HumanEntity> back) {
        List<String> lines = Configurable.toStringList(main.read(name), path);
        boolean messages = path.endsWith("messages"), entries = path.contains("random-values"),
                conditions = path.endsWith("requires");
        Consumer<HumanEntity> reopen = who -> openLines(who, name, path, back);

        List<GuiItem> items = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int index = i;
            String line = lines.get(i);

            List<String> lore = new ArrayList<>();
            for (int start = 0; start < line.length(); start += 40)
                lore.add("&f" + line.substring(start, Math.min(line.length(), start + 40)));

            List<String> actions = Configurable.toStringList(texts(), "lines.line.lore");
            lore.addAll(actions);

            items.add(item(Material.PAPER, text("lines.line.name", "number", String.valueOf(i + 1)), lore, event -> {
                HumanEntity who = event.getWhoClicked();
                List<String> edited = new ArrayList<>(lines);

                if (isDrop(event)) {
                    edited.remove(index);
                    main.set(name, path, edited.isEmpty() ? null : edited);
                    reopen.accept(who);
                    return;
                }

                if (event.isShiftClick()) {
                    main.set(name, path, moveUp(lines, index));
                    reopen.accept(who);
                    return;
                }

                main.chat.request(who, texts().getString("fields.line.title", "Line") + " " + (index + 1), line, text -> {
                    if (StringUtils.isNotBlank(text)) {
                        edited.set(index, text);
                        main.set(name, path, edited);
                    }
                    reopen.accept(who);
                });
            }));
        }

        ChestBuilder menu = ChestBuilder.of(6, title(conditions ? "requires" : entries ? "values" : messages ? "messages" : "commands"), plugin())
                .cancelClicks()
                .populate(0, 0, 9, 5, items);

        if (items.isEmpty()) menu.addItem(0, 4, 2, item(Material.PAPER, "lines.empty", e -> {}));

        GuiItem add = item(Material.WRITABLE_BOOK, conditions ? "lines.add-condition" : entries ? "lines.add-entry" : messages ? "lines.add-message" : "lines.add-command", event -> {
            HumanEntity who = event.getWhoClicked();
            main.chat.request(who, texts().getString("fields.line.title", "Line"), "", text -> {
                if (StringUtils.isNotBlank(text)) {
                    List<String> edited = new ArrayList<>(lines);
                    edited.add(text);
                    main.set(name, path, edited);
                }
                reopen.accept(who);
            });
        });

        GuiItem backItem = back("previous", back);
        menu.forEachPage(page -> menu.addItem(page, 0, 5, backItem).addItem(page, 8, 5, add));
        paging(menu);
        menu.show(viewer);
    }

    // What must happen before an argument is used: its cost, conditions and confirmation.
    void openGates(HumanEntity viewer, String name, String node) {
        YamlConfiguration yml = main.read(name);
        ConfigurationSection section = node.isEmpty() ? yml : yml.getConfigurationSection(node);
        if (section == null) {
            openNode(viewer, name, "");
            return;
        }

        String pre = node.isEmpty() ? "" : node + ".";
        Consumer<HumanEntity> reopen = who -> openGates(who, name, node);

        ChestBuilder.of(1, title("conditions", "path", display(name, node)), plugin())
                .cancelClicks()
                .addItem(0, 0, 0, back("node", who -> openNode(who, name, node)))
                .addItem(0, 2, 0, text(name, yml, Material.GOLD_INGOT, pre + "cost", "cost", false, Menu::number, reopen))
                .addItem(0, 3, 0, linesButton(name, section, "requires", node, "requires", reopen))
                .addItem(0, 4, 0, text(name, yml, Material.PAPER, pre + "deny-message", "deny-message", false, t -> t, reopen))
                .addItem(0, 6, 0, text(name, yml, Material.CLOCK, pre + "confirm", "confirm", false, Menu::seconds, reopen))
                .addItem(0, 7, 0, text(name, yml, Material.PAPER, pre + "confirm-message", "confirm-message", false, t -> t, reopen))
                .show(viewer);
    }

    // "Diamond 20%, Coal 80%": the entries of a random value with their final chance.
    private String chances(List<String> raw) {
        List<String> shown = new ArrayList<>(), parts = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        RandomValue.pool(raw, shown, new ArrayList<>(), weights, "", new ArrayList<>());

        for (int i = 0; i < shown.size(); i++)
            parts.add((shown.get(i).isEmpty() ? word("none") : shown.get(i)) + " "
                    + BigDecimal.valueOf(weights.get(i)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%");
        return String.join(", ", parts);
    }

    // A random value rolled several times: its entries, times, separator and unique.
    void openDraw(HumanEntity viewer, String name, String path, Consumer<HumanEntity> back) {
        YamlConfiguration yml = main.read(name);
        ConfigurationSection section = yml.getConfigurationSection(path);
        if (section == null) {
            back.accept(viewer);
            return;
        }

        Consumer<HumanEntity> reopen = who -> openDraw(who, name, path, back);
        String count = String.valueOf(section.getStringList("entries").size());

        ChestBuilder.of(1, title("draw", "key", path.substring(path.lastIndexOf('.') + 1)), plugin())
                .cancelClicks()
                .addItem(0, 0, 0, back("previous", back))
                .addItem(0, 2, 0, text(name, yml, Material.HOPPER, path + ".times", "times", false, t -> t, reopen))
                .addItem(0, 3, 0, text(name, yml, Material.STRING, path + ".separator", "separator", false, t -> t, reopen))
                .addItem(0, 4, 0, flag(name, yml, path + ".unique", "unique-draw", false, reopen))
                .addItem(0, 8, 0, field(Material.BOOK, "lines", "entries", "",
                        event -> openLines(event.getWhoClicked(), name, path + ".entries", reopen), "count", count))
                .show(viewer);
    }

    // 'random-values' when random, else the fixed 'values'; a button switches between both.
    void openValues(HumanEntity viewer, String name, String node, boolean random) {
        String key0 = random ? "random-values" : "values";
        String path = node.isEmpty() ? key0 : node + "." + key0;
        ConfigurationSection section = main.read(name).getConfigurationSection(path);
        Consumer<HumanEntity> reopen = who -> openValues(who, name, node, random);

        List<GuiItem> items = new ArrayList<>();
        if (section != null)
            for (String key : section.getKeys(false)) {
                String valuePath = path + "." + key;
                boolean list = random && section.isList(key), draw = random && section.isConfigurationSection(key);

                // Lists and draws show the final chance of each entry.
                String spec;
                if (list) spec = chances(section.getStringList(key));
                else if (draw) spec = "x" + section.getString(key + ".times", "1") + ": " + chances(section.getStringList(key + ".entries"));
                else spec = section.getString(key, "");

                String template = !random ? "values.fixed" : list ? "values.list" : draw ? "values.draw" : "values.value";
                items.add(item(list || draw ? Material.BOOK : Material.PAPER, template, event -> {
                    HumanEntity who = event.getWhoClicked();
                    if (isDrop(event)) {
                        main.set(name, valuePath, null);
                        reopen.accept(who);
                        return;
                    }

                    if (draw) {
                        openDraw(who, name, valuePath, reopen);
                        return;
                    }

                    if (list) {
                        // Shift-click turns the list into a draw, to roll it more than once.
                        if (event.isShiftClick()) {
                            main.set(name, valuePath + ".entries", section.getStringList(key));
                            main.set(name, valuePath + ".times", 1);
                            openDraw(who, name, valuePath, reopen);
                            return;
                        }

                        openLines(who, name, valuePath, reopen);
                        return;
                    }

                    // Shift-click turns "a|b|c" into a list of a, b and c.
                    if (random && event.isShiftClick()) {
                        List<String> entries = new ArrayList<>();
                        for (String entry : spec.split("\\|")) if (StringUtils.isNotBlank(entry)) entries.add(entry.trim());
                        main.set(name, valuePath, entries);
                        openLines(who, name, valuePath, reopen);
                        return;
                    }

                    input(who, random ? "value-spec" : "value-text", spec, text -> {
                        if (text != null && (!random || StringUtils.isNotBlank(text))) main.set(name, valuePath, text);
                        reopen.accept(who);
                    });
                }, "key", key, "spec", preview(spec)));
            }

        ChestBuilder menu = ChestBuilder.of(6, title(random ? "values" : "fixed-values"), plugin())
                .cancelClicks()
                .populate(0, 0, 9, 5, items);

        if (items.isEmpty()) menu.addItem(0, 4, 2, item(Material.PAPER, "values.empty", e -> {}));

        GuiItem add = item(Material.NAME_TAG, "values.add", event -> {
            HumanEntity who = event.getWhoClicked();
            input(who, "value-key", "", key -> {
                if (key == null || !key.matches("[A-Za-z0-9_-]+")) {
                    if (key != null) send(who, "argument-failed", key);
                    reopen.accept(who);
                    return;
                }

                input(who, random ? "value-spec" : "value-text", random ? "1-10" : "", spec -> {
                    if (spec != null && (!random || StringUtils.isNotBlank(spec))) main.set(name, path + "." + key, spec);
                    reopen.accept(who);
                });
            });
        });

        GuiItem backItem = back("node", who -> openNode(who, name, node));
        GuiItem other = item(Material.COMPARATOR, random ? "values.to-fixed" : "values.to-random",
                event -> openValues(event.getWhoClicked(), name, node, !random));
        menu.forEachPage(page -> menu.addItem(page, 0, 5, backItem).addItem(page, 1, 5, other).addItem(page, 8, 5, add));
        paging(menu);
        menu.show(viewer);
    }

    void openOutcomes(HumanEntity viewer, String name, String node) {
        String pre = node.isEmpty() ? "random." : node + ".random.";
        YamlConfiguration yml = main.read(name);
        ConfigurationSection pool = yml.getConfigurationSection(pre + "outcomes");
        Consumer<HumanEntity> reopen = who -> openOutcomes(who, name, node);

        List<String> keys = pool == null ? new ArrayList<>() : new ArrayList<>(pool.getKeys(false));
        List<GuiItem> items = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            int index = i;
            String key = keys.get(i);

            ConfigurationSection outcome = pool.getConfigurationSection(key);
            if (outcome == null) continue;

            items.add(item(Material.ENDER_EYE, "outcomes.outcome", event -> {
                HumanEntity who = event.getWhoClicked();

                if (isDrop(event)) {
                    confirm(who, key, () -> main.set(name, pre + "outcomes." + key, null), reopen);
                    return;
                }

                if (event.isShiftClick()) {
                    List<String> moved = moveUp(keys, index);
                    main.rebuild(name, pre + "outcomes", moved, moved);
                    reopen.accept(who);
                    return;
                }

                openOutcome(who, name, node, key);
            },
                    "outcome", key,
                    "weight", outcome.getString("weight", "1"),
                    "permission", preview(outcome.getString("permission", "")),
                    "messages", String.valueOf(Configurable.toStringList(outcome, "messages").size()),
                    "commands", String.valueOf(Configurable.toStringList(outcome, "commands").size())));
        }

        ChestBuilder menu = ChestBuilder.of(6, title("outcomes"), plugin())
                .cancelClicks()
                .populate(0, 1, 9, 4, items);

        if (items.isEmpty()) menu.addItem(0, 4, 2, item(Material.PAPER, "outcomes.empty", e -> {}));

        GuiItem back = back("node", who -> openNode(who, name, node));
        GuiItem picks = text(name, yml, Material.HOPPER, pre + "picks", "picks", false, Menu::integer, reopen);
        GuiItem unique = flag(name, yml, pre + "unique", "unique", true, reopen);

        GuiItem add = item(Material.NAME_TAG, "outcomes.add", event -> {
            HumanEntity who = event.getWhoClicked();
            input(who, "name", "", text -> {
                if (text != null && text.matches("[A-Za-z0-9_-]+") && !keys.contains(text)) {
                    main.set(name, pre + "outcomes." + text + ".weight", 1);
                    openOutcome(who, name, node, text);
                    return;
                }

                if (text != null) send(who, "argument-failed", text);
                reopen.accept(who);
            });
        });

        menu.forEachPage(page -> menu.addItem(page, 0, 0, back).addItem(page, 1, 0, picks)
                .addItem(page, 2, 0, unique).addItem(page, 0, 5, add));
        paging(menu);
        menu.show(viewer);
    }

    void openOutcome(HumanEntity viewer, String name, String node, String key) {
        String path = (node.isEmpty() ? "" : node + ".") + "random.outcomes." + key;
        YamlConfiguration yml = main.read(name);
        ConfigurationSection section = yml.getConfigurationSection(path);
        if (section == null) {
            openOutcomes(viewer, name, node);
            return;
        }

        Consumer<HumanEntity> reopen = who -> openOutcome(who, name, node, key);

        ChestBuilder.of(1, title("outcome", "outcome", key), plugin())
                .cancelClicks()
                .addItem(0, 0, 0, back("outcomes", who -> openOutcomes(who, name, node)))
                .addItem(0, 2, 0, text(name, yml, Material.ANVIL, path + ".weight", "weight", false, Menu::number, reopen))
                .addItem(0, 3, 0, text(name, yml, Material.TRIPWIRE_HOOK, path + ".permission", "outcome-permission",
                        false, t -> t, reopen))
                .addItem(0, 7, 0, linesButton(name, section, "messages", path, "messages", reopen))
                .addItem(0, 8, 0, linesButton(name, section, "commands", path, "commands", reopen))
                .show(viewer);
    }
}
