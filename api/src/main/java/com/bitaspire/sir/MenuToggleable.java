package com.bitaspire.sir;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Registrable;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.takion.character.SmallCaps;
import me.croabeast.vnc.VNC;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MenuToggleable {

    @NotNull
    Information getInformation();

    @Nullable
    Button getButton();

    static boolean supportsButtons() {
        return !VNC.isBefore("1.14") && hasClass("org.bukkit.persistence.PersistentDataType");
    }

    static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @NotNull
    static String formatMenuText(@Nullable String text) {
        return PrismaticAPI.colorize(text == null ? "" : text);
    }

    @NotNull
    static String smallCapsMenuText(@Nullable String text) {
        if (text == null) return "";
        return hasColorTags(text) ? text : SmallCaps.toSmallCaps(text);
    }

    static boolean hasColorTags(@NotNull String text) {
        int open = text.indexOf('<');
        return open >= 0 && text.indexOf('>', open) > open;
    }

    class Button extends ToggleButton {

        private final Information information;
        private final Registrable registrable;

        public Button(Information information, Registrable registrable, boolean enabled) {
            super(1, 1, Priority.HIGHEST, enabled, SIRApi.instance().getPlugin());
            this.information = information;
            this.registrable = registrable;
        }

        public Button(Information information, boolean enabled) {
            this(information, null, enabled);
        }

        @NotNull
        @SuppressWarnings("unchecked")
        public Consumer<InventoryClickEvent> getAction() {
            return (Consumer<InventoryClickEvent>) Objects.requireNonNull(onClick);
        }

        public void setOnClick(Function<Button, Consumer<InventoryClickEvent>> function) {
            setOnClick(Objects.requireNonNull(function).apply(this));
        }

        public void toggleRegistering() {
            if (registrable != null)
                String.valueOf(isEnabled() ? registrable.register() : registrable.unregister());
        }

        public void toggleAll() {
            toggle();
            toggleRegistering();
        }

        private GuiItem defaultItem(String title, boolean enabled) {
            String resolvedTitle = smallCapsMenuText(title != null ? title : information.getTitle());
            String status = enabled ? " &a&l✔" : " &c&l❌";

            return ItemCreator.of(
                            enabled ?
                                    Material.LIME_STAINED_GLASS_PANE :
                                    Material.RED_STAINED_GLASS_PANE
                    )
                    .modifyName(formatMenuText("&7• &f" + resolvedTitle + ':' + status))
                    .modifyMeta(m -> m.setLore(CollectionBuilder
                            .of(information.getDescription())
                            .apply(MenuToggleable::smallCapsMenuText)
                            .apply(s -> "&7 " + s)
                            .apply(MenuToggleable::formatMenuText)
                            .toList()
                    ))
                    .create();
        }

        public void setDefaultItems(String title) {
            setEnabledItem(defaultItem(title, true));
            setDisabledItem(defaultItem(title, false));
        }

        public void setDefaultItems() {
            setDefaultItems(null);
        }
    }
}
