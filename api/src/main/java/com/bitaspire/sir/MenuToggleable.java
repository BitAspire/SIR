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

/**
 * Marks a SIR extension as having a toggle button in the in-game management GUI.
 *
 * <p> Implementors provide an {@link Information} descriptor and an optional {@link Button}
 * that lets administrators enable or disable the component at runtime.
 */
public interface MenuToggleable {

    /**
     * Returns the metadata descriptor used to populate the GUI button.
     *
     * @return the information object.
     */
    @NotNull
    Information getInformation();

    /**
     * Returns the toggle button for this component, or {@code null} if buttons are not supported
     * or not yet initialised.
     *
     * @return the button, or {@code null}.
     */
    @Nullable
    Button getButton();

    /**
     * Returns whether the current server version supports GUI toggle buttons.
     *
     * <p> Requires Bukkit 1.14+ and the {@code PersistentDataType} API.
     *
     * @return {@code true} if buttons are supported.
     */
    static boolean supportsButtons() {
        if (VNC.isBefore("1.14")) return false;

        try {
            Class.forName("org.bukkit.persistence.PersistentDataType");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Applies colour codes to the given text for display in the GUI.
     *
     * @param text the raw text, or {@code null} to return an empty string.
     * @return the colourised text.
     */
    @NotNull
    static String formatMenuText(@Nullable String text) {
        return PrismaticAPI.colorize(text == null ? "" : text);
    }

    /**
     * Converts the given text to small caps for GUI display, unless it already contains
     * a MiniMessage tag ({@code <...>}).
     *
     * @param text the raw text, or {@code null} to return an empty string.
     * @return the small-caps text, or the original text if a tag is detected.
     */
    @NotNull
    static String smallCapsMenuText(@Nullable String text) {
        if (text == null) return "";

        int open = text.indexOf('<');
        if (open >= 0 && text.indexOf('>', open) > open)
            return text;

        return SmallCaps.toSmallCaps(text);
    }

    /**
     * A GUI toggle button that controls the enabled state of a {@link MenuToggleable} component.
     *
     * <p> Clicking the button in the management GUI toggles the component on or off and,
     * when a {@link Registrable} is associated, also registers or unregisters it.
     */
    class Button extends ToggleButton {

        private final Information information;
        private final Registrable registrable;

        /**
         * Creates a button with an associated registrable for automatic register/unregister on toggle.
         *
         * @param information the metadata used to populate the button items.
         * @param registrable the registrable to call on state change, or {@code null} to skip.
         * @param enabled the initial enabled state.
         */
        public Button(Information information, Registrable registrable, boolean enabled) {
            super(1, 1, Priority.HIGHEST, enabled, SIRApi.instance().getPlugin());
            this.information = information;
            this.registrable = registrable;
        }

        /**
         * Creates a standalone button with no associated registrable.
         *
         * @param information the metadata used to populate the button items.
         * @param enabled the initial enabled state.
         */
        public Button(Information information, boolean enabled) {
            this(information, null, enabled);
        }

        /**
         * Returns the current click action attached to this button.
         *
         * @return the click consumer.
         * @throws NullPointerException if no click action has been set.
         */
        @NotNull
        @SuppressWarnings("unchecked")
        public Consumer<InventoryClickEvent> getAction() {
            return (Consumer<InventoryClickEvent>) Objects.requireNonNull(onClick);
        }

        /**
         * Sets the click action using a factory that receives this button instance.
         *
         * @param function a function that takes this button and returns an event consumer.
         */
        public void setOnClick(Function<Button, Consumer<InventoryClickEvent>> function) {
            setOnClick(Objects.requireNonNull(function).apply(this));
        }

        /**
         * Registers or unregisters the associated {@link Registrable} based on the current enabled state.
         *
         * <p> If no registrable is set, returns {@code true} immediately.
         *
         * @return {@code true} if the operation succeeded.
         */
        public boolean toggleRegistering() {
            if (registrable == null) return true;

            boolean successful = isEnabled() ? registrable.register() : registrable.unregister();
            if (successful && registrable instanceof SIRExtension)
                ((SIRExtension<?>) registrable).setRegistered(isEnabled());
            return successful;
        }

        /**
         * Toggles the button state and applies the corresponding register/unregister operation.
         *
         * <p> If the register/unregister call fails, the toggle is reverted.
         *
         * @return {@code true} if the toggle and registration change both succeeded.
         */
        public boolean toggleAll() {
            toggle();
            if (toggleRegistering()) return true;

            toggle();
            return false;
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

        /**
         * Sets the enabled and disabled GUI items using a custom display title.
         *
         * @param title the display title to use, or {@code null} to fall back to {@link Information#getTitle()}.
         */
        public void setDefaultItems(String title) {
            setEnabledItem(defaultItem(title, true));
            setDisabledItem(defaultItem(title, false));
        }

        /**
         * Sets the enabled and disabled GUI items using the title from the associated {@link Information}.
         */
        public void setDefaultItems() {
            setDefaultItems(null);
        }
    }
}
