package me.croabeast.sir.plugin.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.prismatic.PrismaticAPI;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ItemCreator {

    private Consumer<InventoryClickEvent> consumer;
    private final ItemStack item;

    private ItemCreator(ItemStack stack) {
        item = Objects.requireNonNull(stack);
    }

    public ItemCreator modifyItem(Consumer<ItemStack> consumer) {
        Objects.requireNonNull(consumer).accept(item);
        return this;
    }

    public ItemCreator setAction(Consumer<InventoryClickEvent> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
        return this;
    }

    public ItemCreator setActionToEmpty() {
        return setAction(e -> e.setCancelled(true));
    }

    public ItemCreator modifyMeta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Objects.requireNonNull(consumer).accept(meta);
            item.setItemMeta(meta);
        }

        return this;
    }

    public ItemCreator modifyName(String name) {
        return modifyMeta(m -> m.setDisplayName(PrismaticAPI.colorize(name)));
    }

    public ItemCreator modifyLore(List<String> lore) {
        lore.replaceAll(PrismaticAPI::colorize);
        return modifyMeta(m -> m.setLore(lore));
    }

    public ItemCreator modifyLore(String... lore) {
        return modifyLore(ArrayUtils.toList(lore));
    }

    public GuiItem create() {
        GuiItem item = new GuiItem(this.item);

        if (consumer != null)
            item.setAction(consumer);

        return item;
    }

    public static ItemCreator of(ItemStack stack) {
        return new ItemCreator(stack);
    }

    public static ItemCreator of(Material material) {
        return of(new ItemStack(Objects.requireNonNull(material)));
    }
}
