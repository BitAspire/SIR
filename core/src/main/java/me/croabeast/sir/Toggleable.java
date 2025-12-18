package me.croabeast.sir;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Registrable;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.takion.character.SmallCaps;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Toggleable {

    @NotNull
    Information getInformation();

    @NotNull
    Button getButton();

    class Button extends ToggleButton {

        private final Information information;
        private final Registrable registrable;

        public Button(Information information, Registrable registrable, boolean enabled) {
            super(information.getSlot(), 1, 1, Priority.HIGHEST, enabled);
            this.information = information;
            this.registrable = registrable;
        }

        public Button(Information information, boolean enabled) {
            this(information, null, enabled);
        }

        @NotNull
        public Consumer<InventoryClickEvent> getAction() {
            return Objects.requireNonNull(onClick);
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
            return ItemCreator.of(
                            enabled ?
                                    Material.LIME_STAINED_GLASS_PANE :
                                    Material.RED_STAINED_GLASS_PANE
                    )
                    .modifyName("&7• &f" +
                            SmallCaps.toSmallCaps(title != null ?
                                    title :
                                    information.getTitle()) +
                            ':' + (enabled ? " &a&l✔" : " &c&l❌")
                    )
                    .modifyMeta(m -> m.setLore(CollectionBuilder
                            .of(information.getDescription())
                            .apply(SmallCaps::toSmallCaps)
                            .apply(s -> "&7 " + s)
                            .apply(PrismaticAPI::colorize)
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
