package me.croabeast.sir.command.settings;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.common.gui.ButtonBuilder;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.*;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public final class SettingsProvider extends StandaloneProvider implements SettingsService {

    private static final int ITEMS_PER_ROW = 5;

    private final Set<SIRCommand> commands = new HashSet<>();
    private ConfigurableFile lang, settings;

    @Getter(AccessLevel.NONE)
    private Expansion expansion;

    @SneakyThrows
    public boolean register() {
        lang = new ExtensionFile(this, "lang", true);
        settings = new ConfigurableFile(this, "settings");
        settings.save();

        commands.add(new Command(this));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            (expansion = new Expansion(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        if (expansion != null)
            expansion.unregister();
        return true;
    }

    @Override
    public boolean isToggled(@NotNull SIRUser user, @NotNull ChatToggleable toggleable) {
        return isEnabled(user, resolveCategory(toggleable), toggleable.getKey());
    }

    @Override
    public void setToggle(@NotNull SIRUser user, @NotNull ChatToggleable toggleable, boolean enabled) {
        setEnabled(user, resolveCategory(toggleable), toggleable.getKey(), enabled);
    }

    boolean isEnabled(@NotNull SIRUser user, @NotNull Category category, @Nullable String key) {
        String normalized = normalizeKey(key);
        return StringUtils.isBlank(normalized) || settings.get(pathFor(user, category, normalized), true);
    }

    void setEnabled(@NotNull SIRUser user, @NotNull Category category, @Nullable String key, boolean enabled) {
        String normalized = normalizeKey(key);
        if (StringUtils.isBlank(normalized)) return;

        settings.set(pathFor(user, category, normalized), enabled);
        settings.save();
    }

    void openMainMenu(@NotNull Player player) {
        getMainMenu().showGui(player);
    }

    void openCategoryMenu(@NotNull Player player, @NotNull Category category) {
        getCategoryMenu(player, category).showGui(player);
    }

    @NotNull
    List<ChatToggleable> getToggleableList(@NotNull Category category) {
        List<ChatToggleable> list = new ArrayList<>();
        switch (category) {
            case MODULES:
                list = getApi().getModuleManager().getModules().stream()
                        .filter(ChatToggleable.class::isInstance)
                        .filter(SIRModule::isEnabled)
                        .map(ChatToggleable.class::cast)
                        .collect(Collectors.toList());
                break;
            case COMMANDS:
                list = getApi().getCommandManager().getProviders().stream()
                        .filter(ChatToggleable.class::isInstance)
                        .map(ChatToggleable.class::cast)
                        .filter(toggleable -> getApi().getCommandManager()
                                .isProviderEnabled(toggleable.getKey()))
                        .collect(Collectors.toList());
                break;
            default:
                break;
        }

        list.sort(Comparator.comparing(toggleable -> normalizeKey(toggleable.getKey())));
        return list;
    }

    @Nullable
    ChatToggleable findToggleable(@NotNull Category category, @Nullable String name) {
        String normalized = normalizeKey(name);
        if (StringUtils.isBlank(normalized)) return null;

        for (ChatToggleable toggleable : getToggleableList(category)) {
            if (normalizeKey(toggleable.getKey()).equals(normalized))
                return toggleable;
        }

        return null;
    }

    @NotNull
    private ChestBuilder getMainMenu() {
        String title = menuString("main.title", "&8Chat Settings:");
        ChestBuilder menu = ChestBuilder.of(getApi().getPlugin(), 3, title);

        List<ChatToggleable> modules = getToggleableList(Category.MODULES);
        List<ChatToggleable> commands = getToggleableList(Category.COMMANDS);

        menu.addSingleItem(0, 3, 1, buildCategoryItem(Category.MODULES, modules), pane -> pane.setPriority(Pane.Priority.LOW));
        menu.addSingleItem(0, 5, 1, buildCategoryItem(Category.COMMANDS, commands), pane -> pane.setPriority(Pane.Priority.LOW));

        menu.addSingleItem(
                0, 7, 1,
                ItemCreator.of(Material.BARRIER)
                        .modifyName(menuString("main.close.name", "&cClose"))
                        .modifyLore(menuList("main.close.lore", Collections.singletonList("&7Close this menu.")))
                        .setAction(event -> {
                            event.setCancelled(true);
                            event.getWhoClicked().closeInventory();
                        })
                        .create(getApi().getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        return menu;
    }

    @NotNull
    private ChestBuilder getCategoryMenu(@NotNull Player player, @NotNull Category category) {
        List<ChatToggleable> list = getToggleableList(category);
        int rowsOfItems = (list.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int rows = Math.min(6, Math.max(3, rowsOfItems + 2));

        String title = applyPlaceholders(
                menuString("category.title", "&8Chat Settings - {category}:"),
                Collections.singletonMap("category", category.getLabel())
        );
        ChestBuilder menu = ChestBuilder.of(getApi().getPlugin(), rows, title);

        SIRUser viewer = getApi().getUserManager().getUser(player);
        if (viewer == null) return menu;

        for (int index = 0; index < list.size(); index++) {
            int row = index / ITEMS_PER_ROW;
            if (row >= rows - 2) break;

            int column = index % ITEMS_PER_ROW;
            int x = 3 + column;
            int y = 1 + row;

            ChatToggleable toggleable = list.get(index);
            Slot slot = Slot.fromXY(x, y);
            menu.addPane(0, ButtonBuilder
                    .of(getApi().getPlugin(), slot, isToggled(viewer, toggleable))
                    .setItem(buildToggleItem(toggleable, true), true)
                    .setItem(buildToggleItem(toggleable, false), false)
                    .modify(button -> button.allowToggle(false))
                    .setAction(button -> event -> {
                        event.setCancelled(true);
                        setToggle(viewer, toggleable, !button.isEnabled());
                        openCategoryMenu((Player) event.getWhoClicked(), category);
                    })
                    .getValue());
        }

        menu.addSingleItem(
                0, 1, rows,
                ItemCreator.of(Material.ARROW)
                        .modifyName(menuString("category.back.name", "&eBack"))
                        .modifyLore(menuList("category.back.lore", Collections.singletonList("&7Return to the main menu.")))
                        .setAction(event -> {
                            event.setCancelled(true);
                            openMainMenu((Player) event.getWhoClicked());
                        })
                        .create(getApi().getPlugin()),
                pane -> pane.setPriority(Pane.Priority.LOW)
        );

        return menu;
    }

    @NotNull
    private GuiItem buildCategoryItem(@NotNull Category category, @NotNull List<ChatToggleable> list) {
        boolean hasEntries = !list.isEmpty();
        Material material;
        if (!hasEntries) {
            material = Material.BARRIER;
        } else if (category == Category.MODULES) {
            material = Material.BOOKSHELF;
        } else {
            material = Material.PAPER;
        }

        List<String> lore = new ArrayList<>();

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("category", category.getLabel());
        placeholders.put("count", String.valueOf(list.size()));

        if (hasEntries) {
            lore.addAll(applyPlaceholders(
                    menuList("main." + category.getKey() + ".lore",
                            Arrays.asList("&7Chat settings for {category}.",
                                    "&7Available: &f{count}",
                                    "&f➤ &7Open this menu")
                    ),
                    placeholders
            ));
        } else {
            lore.addAll(applyPlaceholders(
                    menuList("main." + category.getKey() + ".empty-lore",
                            Arrays.asList("&7Chat settings for {category}.", "&cNo entries available to toggle.")
                    ),
                    placeholders
            ));
        }

        ItemCreator creator = ItemCreator.of(material)
                .modifyName(applyPlaceholders(
                        menuString("main." + category.getKey() + (hasEntries ? ".name" : ".empty-name"),
                                "&7• &f{category}:"),
                        placeholders
                ))
                .modifyLore(lore);

        if (hasEntries) {
            creator.setAction(event -> {
                event.setCancelled(true);
                openCategoryMenu((Player) event.getWhoClicked(), category);
            });
        } else {
            creator.setActionToEmpty();
        }

        return creator.create(getApi().getPlugin());
    }

    @NotNull
    private GuiItem buildToggleItem(@NotNull ChatToggleable toggleable, boolean enabled) {
        List<String> lore = new ArrayList<>(), description = buildToggleDescription(toggleable);

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("title", buildToggleTitle(toggleable));
        placeholders.put("state", enabled ? menuString("toggle.state.enabled", "&aEnabled")
                : menuString("toggle.state.disabled", "&cDisabled"));
        placeholders.put("status", enabled ? menuString("toggle.status.enabled", "&a&l✔")
                : menuString("toggle.status.disabled", "&c&l❌"));

        List<String> baseLore = menuList("toggle.lore", Arrays.asList("{description}", "", "&f➤ &7Left-click: toggle", "&7Current: {state}"));
        for (String line : expandDescription(baseLore, description)) {
            if (StringUtils.isBlank(line)) continue;
            lore.add(applyPlaceholders(line, placeholders));
        }

        Material material = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        return ItemCreator.of(material)
                .modifyName(applyPlaceholders(
                        menuString("toggle.name", "&7• &f{title}: {status}"),
                        placeholders
                ))
                .modifyLore(lore)
                .create(getApi().getPlugin());
    }

    @NotNull
    private String buildToggleTitle(@NotNull ChatToggleable toggleable) {
        if (toggleable instanceof SIRModule)
            return ((SIRModule) toggleable).getInformation().getTitle();
        if (toggleable instanceof CommandProvider) {
            ProviderInformation info = getApi().getCommandManager().getInformation(toggleable.getKey());
            if (info != null && StringUtils.isNotBlank(info.getTitle())) return info.getTitle();
        }
        return toggleable.getKey();
    }

    @NotNull
    private List<String> buildToggleDescription(@NotNull ChatToggleable toggleable) {
        if (toggleable instanceof SIRModule)
            return Arrays.asList(((SIRModule) toggleable).getInformation().getDescription());
        if (toggleable instanceof CommandProvider) {
            ProviderInformation info = getApi().getCommandManager().getInformation(toggleable.getKey());
            if (info != null && info.getDescription().length > 0) return Arrays.asList(info.getDescription());
        }

        return Collections.singletonList(menuString("toggle.description.empty", "No description available."));
    }

    @NotNull
    private String normalizeKey(@Nullable String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ENGLISH);
    }

    @NotNull
    private String menuString(@NotNull String path, @NotNull String fallback) {
        return lang.get("lang.menu." + path, fallback);
    }

    @NotNull
    private List<String> menuList(@NotNull String path, @NotNull List<String> fallback) {
        List<String> result = lang.toStringList("lang.menu." + path);
        return result.isEmpty() ? fallback : result;
    }

    @NotNull
    private String applyPlaceholders(@NotNull String input, @NotNull Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet())
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        return result;
    }

    @NotNull
    private List<String> applyPlaceholders(@NotNull List<String> input, @NotNull Map<String, String> placeholders) {
        List<String> result = new ArrayList<>();
        for (String line : input) result.add(applyPlaceholders(line, placeholders));
        return result;
    }

    @NotNull
    private List<String> expandDescription(@NotNull List<String> base, @NotNull List<String> description) {
        List<String> result = new ArrayList<>();
        for (String line : base) {
            if (line.contains("{description}")) {
                if (description.isEmpty()) continue;
                for (String desc : description) {
                    if (StringUtils.isBlank(desc)) continue;
                    result.add(line.replace("{description}", desc));
                }
                continue;
            }
            result.add(line);
        }
        return result;
    }

    @NotNull
    private String pathFor(@NotNull SIRUser user, @NotNull Category category, @NotNull String key) {
        return "users." + user.getUuid() + "." + category.getKey() + "." + key;
    }

    @NotNull
    private Category resolveCategory(@NotNull ChatToggleable toggleable) {
        return toggleable instanceof SIRModule ? Category.MODULES : Category.COMMANDS;
    }

    enum Category {
        MODULES("modules", "Modules"),
        COMMANDS("commands", "Commands");

        private final String key;
        private final String label;

        Category(String key, String label) {
            this.key = key;
            this.label = label;
        }

        @NotNull
        public String getKey() {
            return key;
        }

        @NotNull
        public String getLabel() {
            return label;
        }

        @Nullable
        static Category fromInput(@Nullable String value) {
            if (StringUtils.isBlank(value)) return null;
            if (value.matches("(?i)modules?")) return MODULES;
            if (value.matches("(?i)commands?")) return COMMANDS;
            return null;
        }
    }
}
