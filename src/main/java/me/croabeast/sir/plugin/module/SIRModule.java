package me.croabeast.sir.plugin.module;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.Getter;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.plugin.aspect.AspectButton;
import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.aspect.SIRAspect;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileData;
import me.croabeast.sir.plugin.gui.ItemCreator;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
public abstract class SIRModule implements SIRAspect, SIRExtension {

    private final AspectKey aspectKey;
    protected final SIRPlugin plugin;

    private final ConfigurableFile mainFile;
    private final AspectButton button;

    SIRModule(Key key) {
        key.init = this;

        this.aspectKey = key;
        this.mainFile = FileData.Module.getMain();

        String path = "modules." + key.getFullName();
        boolean e = mainFile.get(path, false);

        button = new AspectButton(this, e);

        loadButton(true, Material.LIME_STAINED_GLASS_PANE);
        loadButton(false, Material.RED_STAINED_GLASS_PANE);

        button.setOnClick(b -> event -> {
            mainFile.set(path, b.isEnabled());
            mainFile.save();

            if (b.isEnabled()) {
                register();
                return;
            }

            unregister();
        });

        this.plugin = SIRPlugin.getInstance();
    }

    void loadButton(boolean enabled, Material material) {
        GuiItem item = ItemCreator.of(material)
                .modifyName(getAspectKey().getTitle())
                .modifyMeta(m -> {
                    final List<String> list = new LinkedList<>();
                    list.add(" &8Enabled: " +
                            (enabled ? " &a&l✔" : " &c&l❌"));

                    list.add("");
                    list.addAll(Arrays.asList(aspectKey.getDescription()));
                    list.add("");

                    list.replaceAll(s ->
                            PrismaticAPI.colorize("&7 " + s));

                    m.setLore(list);
                }).create();

        if (enabled) {
            button.setEnabledItem(item);
            return;
        }

        button.setDisabledItem(item);
    }

    @NotNull
    public final String getName() {
        return aspectKey.getName();
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isRegistered() {
        return isEnabled();
    }

    @Override
    public final boolean isEnabled() {
        return button.isEnabled();
    }

    @Override
    public String toString() {
        return "SIRModule{key=" + aspectKey + '}';
    }

    enum Type {
        CHAT, HOOK
    }

    @Getter
    public enum Key implements AspectKey {
        ADVANCEMENTS(3, 1,
                "Handles if custom advancement messages and/or",
                "rewards should be enabled.",
                "Each advancement can be in a different category."
        ),
        ANNOUNCEMENTS(4, 1,
                "Handles if custom scheduled and automated messages",
                "will be displayed in a defined time frame in ticks."
        ),
        JOIN_QUIT(5, 1,
                "Handles if custom join and quit messages are",
                "enabled. Works with multiple messages types",
                "like chat, title, action bar, boss bar, json, etc."
        ),
        MOTD(6, 1,
                "Handles the motd of the server, having multiple",
                "motds in order or random."
        ),
        CHANNELS(Type.CHAT, 7, 1,
                "Handles how the chat will display and if will be a",
                "local channel as well with the global channel.",
                "Local channels only can be accessed using a prefix",
                "and/or command."
        ),
        COOLDOWNS(Type.CHAT, 3, 2,
                "Handles if a cooldown will be applied in each chat",
                "message to avoid spamming. Commands can be executed",
                "if the player keeps spamming messages for a custom",
                "time depending on its permission."
        ),
        EMOJIS(Type.CHAT, 4, 2,
                "Handles if custom emojis should be added in chat",
                "and/or other SIR features and files."
        ),
        MENTIONS(Type.CHAT, 5, 2,
                "Handles if players can be mentioned or tagged in",
                "the chat, similar how Discord use their mentions."
        ),
        TAGS(Type.CHAT, 6, 2,
                "Handles if SIR can create custom tags for chat.",
                "Tags can be parsed in any plugin or message managed",
                "by PlaceholderAPI."
        ),
        MODERATION(Type.CHAT, 7, 2,
                "Handles if moderation should be applied on the chat",
                "to avoid insults, caps, spamming, etc."
        ),
        DISCORD(Type.HOOK, 3, 3,
                "Handles if DiscordSRV will work with SIR to display",
                "join-quit messages, chat messages, and more."
        ),
        LOGIN(Type.HOOK, 4, 3,
                "Handles if DiscordSRV will work with SIR to display",
                "join-quit messages, chat messages, and more."
        ),
        VANISH(Type.HOOK, 5, 3,
                "Handles if DiscordSRV will work with SIR to display",
                "join-quit messages, chat messages, and more."
        );

        private final String name;
        private final String[] description;
        private final UUID uuid;
        private final Slot menuSlot;

        private SIRModule init;
        private final Type type;

        Key(Type type, int x, int y, String... description) {
            this.type = type;
            name = name()
                    .toLowerCase(Locale.ENGLISH)
                    .replace('_', '-');

            uuid = AspectKey.super.getUuid();
            menuSlot = Slot.fromXY(x, y);

            this.description = description;
        }

        Key(int x, int y, String... description) {
            this(null, x, y, description);
        }

        @Override
        public boolean isEnabled() {
            if (init == null)
                throw new IllegalStateException("Module is not initialized!");

            return init.isEnabled();
        }

        @NotNull
        public String getFullName() {
            return type == null ? name : (type.name().toLowerCase(Locale.ENGLISH) + '.' + name);
        }

        @Override
        public String toString() {
            return "Key{name='" + getFullName() + '\'' + ", uuid=" + uuid + '}';
        }
    }
}
