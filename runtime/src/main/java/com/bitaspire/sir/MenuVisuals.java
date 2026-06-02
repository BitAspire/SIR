package com.bitaspire.sir;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.experimental.UtilityClass;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.common.gui.ItemCreator;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@UtilityClass
class MenuVisuals {

    static final int MAIN_ITEMS_PER_ROW = 7;
    static final int MAIN_ITEMS_PER_PAGE = MAIN_ITEMS_PER_ROW * 4;
    static final int CENTER_ITEMS_PER_PAGE = 7 * 4;

    // Symbols
    private static final String POINTER = "➼";  // ➼  for module description lines
    private static final String SEP     = "〢";  // 〢 for config item details
    private static final String INFO    = "◆";  // ◆
    private static final String CHECK   = "✔";  // ✔
    private static final String CROSS   = "✘";  // ✘
    private static final String ACTION  = "»";  // »
    private static final String BACK    = "←";  // ←

    // Type badge icons
    private static final String MODULE_ICON  = "✦";  // ✦
    private static final String COMMAND_ICON = "⌘";  // ⌘
    private static final String ADDON_ICON   = "✚";  // ✚

    // Type badge colors (hex, via PrismaticAPI)
    private static final String MODULE_COLOR  = "&#7ECEFF";
    private static final String COMMAND_COLOR = "&#FFD166";
    private static final String ADDON_COLOR   = "&#C4A0FF";

    // Per-name color palette (picked by name hash)
    private static final String[] NAME_PALETTE = {
            "&#5BF5B0",  // mint
            "&#5BB8F5",  // sky blue
            "&#F5A35B",  // amber
            "&#D45BF5",  // purple
            "&#F5E55B",  // yellow
            "&#5BF5D4",  // teal
            "&#F55B9E",  // pink
            "&#A0F55B",  // lime
            "&#5B7BF5",  // periwinkle
            "&#F5705B",  // salmon
    };

    static int pageCount(int itemCount, int itemsPerPage) {
        return Math.max(1, (Math.max(0, itemCount) + itemsPerPage - 1) / itemsPerPage);
    }

    static int mainRowsFor(int itemCount) {
        int visibleItems = Math.max(1, Math.min(MAIN_ITEMS_PER_PAGE, itemCount));
        int rowsOfItems = (visibleItems + MAIN_ITEMS_PER_ROW - 1) / MAIN_ITEMS_PER_ROW;
        return Math.max(3, Math.min(6, rowsOfItems + 2));
    }

    @Nullable
    static Slot mainSlot(int index) {
        int row = index / MAIN_ITEMS_PER_ROW;
        return row >= 4 ? null : Slot.fromXY(1 + (index % MAIN_ITEMS_PER_ROW), 1 + row);
    }

    static void addFrame(@NotNull ChestBuilder menu,
                         @NotNull Plugin plugin,
                         int rows,
                         int pages,
                         @NotNull Material summaryMaterial,
                         @NotNull String title,
                         @NotNull String... summaryLore) {
        for (int page = 0; page < pages; page++) {
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < 9; x++) {
                    menu.addSingleItem(
                            page, x, y,
                            filler(plugin),
                            pane -> pane.setPriority(Pane.Priority.LOWEST)
                    );
                }
            }

            menu.addSingleItem(
                    page, 4, 0,
                    item(plugin, summaryMaterial, headerName(title), summaryLore, event -> event.setCancelled(true)),
                    pane -> pane.setPriority(Pane.Priority.LOW)
            );
            menu.addSingleItem(
                    page, 8, 0,
                    close(plugin),
                    pane -> pane.setPriority(Pane.Priority.LOW)
            );
        }
    }

    static void addPageControls(@NotNull ChestBuilder menu,
                                @NotNull Plugin plugin,
                                int rows,
                                int pages) {
        if (pages <= 1) return;

        int bottomRow = rows - 1;
        for (int page = 0; page < pages; page++) {
            int displayPage = page + 1;
            menu.addSingleItem(
                    page, 4, bottomRow,
                    item(plugin, Material.PAPER, "&e&l" + small("Page") + " &f" + displayPage + "/" + pages,
                            new String[]{"&8" + POINTER + " &7Use navigation buttons to browse."}, event -> event.setCancelled(true)),
                    pane -> pane.setPriority(Pane.Priority.LOW)
            );

            if (page > 0) {
                int target = page - 1;
                menu.addSingleItem(
                        page, 2, bottomRow,
                        navigation(plugin, "&e&l" + BACK + " " + small("Previous"), "&7Go to page &f" + (target + 1) + "&7.", event -> {
                            event.setCancelled(true);
                            menu.setDisplayedPage(target);
                            menu.showGui(event.getWhoClicked());
                        }),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            }

            if (page < pages - 1) {
                int target = page + 1;
                menu.addSingleItem(
                        page, 6, bottomRow,
                        navigation(plugin, "&e&l" + small("Next") + " →", "&7Go to page &f" + (target + 1) + "&7.", event -> {
                            event.setCancelled(true);
                            menu.setDisplayedPage(target);
                            menu.showGui(event.getWhoClicked());
                        }),
                        pane -> pane.setPriority(Pane.Priority.LOW)
                );
            }
        }
    }

    @NotNull
    static GuiItem emptyItem(@NotNull Plugin plugin, @NotNull String title, @NotNull String detail) {
        return item(plugin, Material.BARRIER, "&c&l" + CROSS + " " + small(title),
                new String[]{"&8" + POINTER + " &7" + detail}, event -> event.setCancelled(true));
    }

    @NotNull
    static GuiItem toggleItem(@NotNull Plugin plugin,
                              @NotNull Information information,
                              boolean enabled,
                              @NotNull String type,
                              @NotNull String primaryAction,
                              @Nullable String secondaryAction) {
        List<String> lore = new ArrayList<>();

        for (String line : information.getDescription()) {
            if (StringUtils.isBlank(line)) continue;
            lore.add("&8" + POINTER + " &7" + small(line));
        }

        if (!lore.isEmpty()) lore.add("");
        lore.add("&8" + INFO + " &7" + small("Status") + "&8: " + status(enabled));
        lore.add("&8" + ACTION + " &e" + small("Left-click") + " &8- &7" + small(primaryAction));
        if (StringUtils.isNotBlank(secondaryAction)) {
            lore.add("&8" + ACTION + " &e" + small("Right-click") + " &8- &7" + small(secondaryAction));
        }

        return item(
                plugin,
                enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                toggledName(information.getTitle(), type),
                lore.toArray(new String[0]),
                event -> event.setCancelled(true)
        );
    }

    @NotNull
    static GuiItem booleanItem(@NotNull Plugin plugin, @NotNull String key, boolean enabled) {
        return stateItem(plugin, key, enabled);
    }

    @NotNull
    static GuiItem stateItem(@NotNull Plugin plugin,
                             @NotNull String key,
                             boolean enabled) {
        return item(
                plugin,
                enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                typedName(key, "Boolean"),
                new String[]{
                        "&8" + SEP + " &7Toggle this option on or off.",
                        "&8" + INFO + " &7" + small("Current") + "&8: " + status(enabled),
                        "",
                        "&8" + ACTION + " &e" + small("Left-click") + " &8- &7" + small("toggle option")
                },
                event -> event.setCancelled(true)
        );
    }

    @NotNull
    static GuiItem configItem(@NotNull Plugin plugin,
                              @NotNull Material material,
                              @NotNull String key,
                              @NotNull String type,
                              @NotNull String description,
                              @NotNull List<String> details,
                              @NotNull String action,
                              @NotNull Consumer<InventoryClickEvent> handler) {
        List<String> lore = new ArrayList<>();
        lore.add("&8" + SEP + " &7" + description);
        for (String detail : details) {
            if (StringUtils.isBlank(detail)) continue;
            lore.add("&8" + SEP + " " + detail);
        }
        lore.add("");
        lore.add("&8" + ACTION + " &e" + small("Left-click") + " &8- &7" + small(action));
        return item(plugin, material, typedName(key, type), lore.toArray(new String[0]), handler);
    }

    @NotNull
    static GuiItem navigation(@NotNull Plugin plugin,
                              @NotNull String name,
                              @NotNull String lore,
                              @NotNull Consumer<InventoryClickEvent> handler) {
        return item(plugin, Material.YELLOW_STAINED_GLASS_PANE, name, new String[]{"&8" + POINTER + " " + lore}, handler);
    }

    @NotNull
    static GuiItem navigation(@NotNull Plugin plugin,
                              @NotNull Material material,
                              @NotNull String name,
                              @NotNull String lore,
                              @NotNull Consumer<InventoryClickEvent> handler) {
        return item(plugin, material, name, new String[]{"&8" + POINTER + " " + lore}, handler);
    }

    @NotNull
    static GuiItem back(@NotNull Plugin plugin,
                        @NotNull String target,
                        @NotNull String lore,
                        @NotNull Consumer<InventoryClickEvent> handler) {
        return item(plugin, Material.YELLOW_STAINED_GLASS_PANE,
                "&e&l" + BACK + " " + small("Back") + " &8| &7" + small(target),
                new String[]{"&8" + POINTER + " " + lore},
                handler);
    }

    @NotNull
    static GuiItem close(@NotNull Plugin plugin) {
        return item(plugin, Material.RED_STAINED_GLASS_PANE,
                "&c&l" + CROSS + " " + small("Close"),
                new String[]{"&8" + POINTER + " &7Close this menu."}, event -> {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
        });
    }

    @NotNull
    static String preview(@Nullable String value) {
        boolean blank = StringUtils.isBlank(value);
        String normalized = blank ? "" : value.replace('\n', ' ').replace('\r', ' ');
        return blank ? "<empty>" : normalized.length() <= 40 ? normalized : normalized.substring(0, 37) + "...";
    }

    // --- Private helpers ---

    @NotNull
    private static GuiItem filler(@NotNull Plugin plugin) {
        return item(plugin, Material.GRAY_STAINED_GLASS_PANE, " ", new String[0], event -> event.setCancelled(true));
    }

    @NotNull
    private static GuiItem item(@NotNull Plugin plugin,
                                @NotNull Material material,
                                @NotNull String name,
                                @NotNull String[] lore,
                                @NotNull Consumer<InventoryClickEvent> handler) {
        List<String> formattedLore = new ArrayList<>();
        for (String line : lore) {
            formattedLore.add(format(line));
        }

        return ItemCreator.of(material)
                .modifyName(format(name))
                .modifyLore(formattedLore)
                .setAction(handler)
                .create(plugin);
    }

    @NotNull
    private static String small(@Nullable String text) {
        return MenuToggleable.smallCapsMenuText(text);
    }

    @NotNull
    private static String status(boolean enabled) {
        return (enabled ? "&a" + CHECK + " " : "&c" + CROSS + " ")
                + small(enabled ? "Enabled" : "Disabled");
    }

    /** For config-menu items (section/value/boolean): white name | gray type. */
    @NotNull
    private static String typedName(@Nullable String title, @NotNull String type) {
        return "&f" + small(title) + " &8| &7" + small(type);
    }

    /**
     * For module/command toggle buttons:
     * - title gets a deterministic color from the name palette
     * - type gets an icon + badge color (Module vs Command Provider)
     */
    @NotNull
    private static String toggledName(@Nullable String title, @NotNull String type) {
        String raw = stripAllFormatting(title);
        String nameColor = nameColor(raw);
        String badge = typeBadge(type);
        return nameColor + small(raw) + " &8| " + badge;
    }

    @NotNull
    private static String nameColor(@Nullable String name) {
        if (name == null || name.isEmpty()) return "&f";
        int index = Math.abs(name.hashCode()) % NAME_PALETTE.length;
        return NAME_PALETTE[index];
    }

    @NotNull
    private static String stripAllFormatting(@Nullable String text) {
        if (text == null) return "";
        return text
                .replaceAll("(?i)&[0-9a-fk-or]", "")  // &7, &a, etc.
                .replaceAll("<[^>]*>", "");             // <r:1>, </r>, <#RRGGBB>, etc.
    }

    @NotNull
    private static String typeBadge(@NotNull String type) {
        String lower = type.toLowerCase();
        if (lower.contains("command")) return COMMAND_COLOR + COMMAND_ICON + " " + small(type);
        if (lower.contains("addon"))   return ADDON_COLOR   + ADDON_ICON   + " " + small(type);
        return MODULE_COLOR + MODULE_ICON + " " + small(type);
    }

    @NotNull
    private static String headerName(@NotNull String title) {
        return "&f&l" + small(title);
    }

    @NotNull
    private static String format(@Nullable String text) {
        return MenuToggleable.formatMenuText(text);
    }
}
