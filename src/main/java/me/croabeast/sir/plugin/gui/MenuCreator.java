package me.croabeast.sir.plugin.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.Getter;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.prismatic.PrismaticAPI;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class MenuCreator {

    private final PaginatedPane paginatedPane;
    @Getter
    private final ChestGui gui;

    private boolean isLoaded = false;

    private MenuCreator(int rows, String name) {
        gui = new ChestGui(rows, PrismaticAPI.colorize(name));
        paginatedPane = new PaginatedPane(0, 0, 9, rows);
    }

    private MenuCreator(MenuCreator creator) {
        this.paginatedPane = creator.paginatedPane;
        this.gui = creator.gui;

        this.isLoaded = creator.isLoaded;
    }

    public MenuCreator modifyGUI(Consumer<ChestGui> consumer) {
        Objects.requireNonNull(consumer).accept(gui);
        return this;
    }

    @SafeVarargs
    public final <P extends Pane> MenuCreator addPane(int index, P pane, Consumer<P>... consumers) {
        Objects.requireNonNull(pane);

        for (Consumer<P> c : ArrayUtils.toList(consumers))
            if (c != null) c.accept(pane);

        paginatedPane.addPane(index, pane);
        return this;
    }

    @SafeVarargs
    public final <P extends Pane> MenuCreator addPane(int index, PaneCreatable<P> creatable, Consumer<P>... consumers) {
        P pane = creatable.create();

        for (Consumer<P> c : ArrayUtils.toList(consumers))
            if (c != null) c.accept(pane);

        paginatedPane.addPane(index, pane);
        return this;
    }

    @SafeVarargs
    public final MenuCreator addSingleItem(int index, int x, int y, GuiItem item, Consumer<OutlinePane>... consumers) {
        OutlinePane pane = new OutlinePane(x, y, 1, 1);

        for (Consumer<OutlinePane> c : ArrayUtils.toList(consumers))
            if (c != null) c.accept(pane);

        pane.addItem(item);
        return addPane(index, pane);
    }

    @SafeVarargs
    public final MenuCreator addSingleItem(int index, int x, int y, ItemCreator item, Consumer<OutlinePane>... consumers) {
        return addSingleItem(index, x, y, item.create(), consumers);
    }

    @NotNull
    public List<Pane> getPanes(int index) {
        return new LinkedList<>(paginatedPane.getPanes(index));
    }

    public void setDisplayedPage(int rows, int index) {
        if (rows > 0) {
            paginatedPane.setHeight(rows);
            gui.setRows(rows);
        }

        paginatedPane.setPage(index);
        gui.update();
    }

    public void setDisplayedPage(int index) {
        setDisplayedPage(-1, index);
    }

    public void showGUI(HumanEntity entity) {
        if (!isLoaded) {
            gui.addPane(paginatedPane);
            isLoaded = true;
        }
        gui.show(entity);
    }

    @NotNull
    public static MenuCreator of(int rows, String name) {
        return new MenuCreator(rows, Objects.requireNonNull(name));
    }

    public static MenuCreator of(MenuCreator creator) {
        return new MenuCreator(Objects.requireNonNull(creator));
    }
}
