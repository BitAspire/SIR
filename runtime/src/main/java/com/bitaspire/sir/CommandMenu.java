package com.bitaspire.sir;

import com.bitaspire.sir.command.StandaloneProvider;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.croabeast.common.gui.ChestBuilder;
import me.croabeast.takion.character.SmallCaps;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class CommandMenu {

    private final SIRApi api;

    CommandMenu(SIRApi api) {
        this.api = api;
    }

    @NotNull
    ChestBuilder build(List<StandaloneProvider> providers) {
        int rows = MenuVisuals.mainRowsFor(providers.size());
        int totalPages = MenuVisuals.pageCount(providers.size(), MenuVisuals.MAIN_ITEMS_PER_PAGE);
        String title = "&8" + SmallCaps.toSmallCaps("Loaded SIR Commands:");
        ChestBuilder menu = ChestBuilder.of(api.getPlugin(), rows, title);

        MenuVisuals.addFrame(
                menu,
                api.getPlugin(),
                rows,
                totalPages,
                Material.COMMAND_BLOCK,
                "SIR Commands",
                "&7Loaded providers: &f" + providers.size(),
                "&7Left-click a provider to toggle it.",
                "&7Right-click opens override options in SIR+."
        );
        MenuVisuals.addPageControls(menu, api.getPlugin(), rows, totalPages);

        if (providers.isEmpty()) {
            menu.addSingleItem(
                    0, 4, 1,
                    MenuVisuals.emptyItem(api.getPlugin(), "No Commands",
                            "No command provider jars are loaded right now."),
                    pane -> pane.setPriority(Pane.Priority.LOW)
            );
            return menu;
        }

        for (int index = 0; index < providers.size(); index++) {
            int page = index / MenuVisuals.MAIN_ITEMS_PER_PAGE;
            Slot slot = MenuVisuals.mainSlot(index % MenuVisuals.MAIN_ITEMS_PER_PAGE);
            if (slot == null) continue;

            StandaloneProvider provider = providers.get(index);
            MenuToggleable.Button button = provider.getButton();
            if (button == null) continue;

            button.setEnabledItem(MenuVisuals.toggleItem(
                    api.getPlugin(),
                    provider.getInformation(),
                    true,
                    "Command Provider",
                    "toggle provider",
                    "open overrides (SIR+)"
            ));
            button.setDisabledItem(MenuVisuals.toggleItem(
                    api.getPlugin(),
                    provider.getInformation(),
                    false,
                    "Command Provider",
                    "toggle provider",
                    "open overrides (SIR+)"
            ));
            menu.addPane(page, slot, button);
        }

        return menu;
    }
}
