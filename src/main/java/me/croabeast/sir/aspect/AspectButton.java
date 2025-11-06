package me.croabeast.sir.aspect;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Registrable;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.common.gui.ItemCreator;
import me.croabeast.sir.SIRPlugin;
import me.croabeast.takion.character.SmallCaps;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public final class AspectButton extends ToggleButton {

    private final AspectKey key;
    private final Registrable registrable;

    public AspectButton(Registrable registrable, AspectKey key, boolean enabled) {
        super(key.getMenuSlot(), 1, 1, Priority.HIGHEST, enabled, SIRPlugin.getInstance());

        this.key = key;
        this.registrable = registrable;
    }

    public AspectButton(AspectKey key, boolean enabled) {
        this(null, key, enabled);
    }

    @NotNull
    public Consumer<InventoryClickEvent> getAction() {
        return Objects.requireNonNull(onClick);
    }

    public void setOnClick(Function<AspectButton, Consumer<InventoryClickEvent>> function) {
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
                                key.getTitle()) +
                        ':' + (enabled ? " &a&l✔" : " &c&l❌")
                )
                .modifyMeta(m -> m.setLore(CollectionBuilder
                        .of(key.getDescription())
                        .apply(SmallCaps::toSmallCaps)
                        .apply(s -> "&7 " + s)
                        .apply(PrismaticAPI::colorize)
                        .toList()
                ))
                .create(SIRPlugin.getInstance());
    }

    public void setDefaultItems(String title) {
        setEnabledItem(defaultItem(title, true));
        setDisabledItem(defaultItem(title, false));
    }

    public void setDefaultItems() {
        setDefaultItems(null);
    }
}
