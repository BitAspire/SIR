package me.croabeast.sir.module;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.Getter;
import me.croabeast.common.Registrable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.SIRExtension;
import me.croabeast.sir.Commandable;
import me.croabeast.sir.aspect.AspectButton;
import me.croabeast.sir.aspect.AspectKey;
import me.croabeast.sir.aspect.SIRAspect;
import me.croabeast.sir.SIRPlugin;
import me.croabeast.sir.FileData;
import me.croabeast.sir.command.SIRCommand;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

@Getter
public abstract class SIRModule implements SIRAspect, SIRExtension, Registrable {

    protected final SIRPlugin plugin;
    private final AspectKey key;

    private final ConfigurableFile file;
    private final AspectButton button;

    SIRModule(Key key) {
        this.plugin = SIRPlugin.getInstance();

        this.key = key;
        key.init = this;

        file = FileData.Module.getMain();

        String path = "modules." + key.getFullName();
        boolean e = file.get(path, false);

        button = new AspectButton(this, this.key, e);
        button.setDefaultItems();

        button.setOnClick(b -> event -> {
            HookLoadable loadable = !(SIRModule.this instanceof HookLoadable) ? null : (HookLoadable) SIRModule.this;

            if (loadable != null && !loadable.isPluginEnabled()) {
                String[] plugins = loadable.getSupportedPlugins();

                plugin.getLibrary().getLoadedSender().
                        setLogger(false).
                        setTargets(event.getView().getPlayer()).
                        send("<P> &7Module can't be enabled since " +
                                ((Function<String[], String>) strings -> {
                                    final int length = strings.length;
                                    if (length == 1)
                                        return strings[0] + " is";

                                    StringBuilder br = new StringBuilder();
                                    for (int i = 0; i < length; i++) {
                                        br.append(strings[i]);

                                        if (i < length - 1)
                                            br.append(i == length - 2 ?
                                                    " or " : ", ");
                                    }

                                    return br.append(" are").toString();
                                }).apply(plugins) +
                                "n't installed in the server."
                        );

                if (isRegistered()) {
                    unregister();
                    file.set(path, false);
                    file.save();
                }
                return;
            }

            if (SIRModule.this instanceof Commandable) {
                Set<SIRCommand> set = ((Commandable) SIRModule.this).getCommands();
                set.forEach(c -> c.getButton().toggleAll());
            }

            file.set(path, b.isEnabled());
            file.save();
            String.valueOf(b.isEnabled() ? register() : unregister());

            String s = "Module '" + getName() + "' active: " + b.isEnabled();
            plugin.getLibrary().getLogger().log(s);
        });
    }

    @NotNull
    public final String getName() {
        return key.getName();
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public final boolean isRegistered() {
        return isEnabled();
    }

    @Override
    public final boolean isEnabled() {
        return button.isEnabled();
    }

    @Override
    public String toString() {
        return "SIRModule{key=" + key + '}';
    }

    enum Type {
        CHAT, HOOK
    }

    @Getter
    public enum Key implements AspectKey {
        ADVANCEMENTS(3, 1,
                "Handles if custom advancement messages",
                "and/or rewards should be enabled.",
                "Each advancement can be in a different",
                "category."
        ),
        ANNOUNCEMENTS(4, 1,
                "Handles if custom scheduled and",
                "automated messages will be displayed",
                "in a defined time frame in ticks."
        ),
        JOIN_QUIT(5, 1,
                "Handles if custom join and quit",
                "messages are enabled.",
                "Works with multiple messages types",
                "like chat, title, action bar, boss",
                "bar, json, etc."
        ),
        MOTD(6, 1,
                "Handles the motd of the server,",
                "having motds at order/random."
        ),
        CHANNELS(Type.CHAT, 7, 1,
                "Handles how the chat will display",
                "and if will be a local channel as",
                "well with the global channel.",
                "Local channels only can be accessed",
                "using a prefix and/or command."
        ),
        COOLDOWNS(Type.CHAT, 3, 2,
                "Handles if a cooldown will be",
                "applied in each chat message to",
                "avoid spamming.",
                "Commands can be executed if the",
                "player keeps spamming messages",
                "for a custom time depending on",
                "its permission."
        ),
        EMOJIS(Type.CHAT, 4, 2,
                "Handles if custom emojis should be",
                "added in chat and/or other SIR",
                "features and files."
        ),
        MENTIONS(Type.CHAT, 5, 2,
                "Handles if players can be mentioned",
                "or tagged in the chat, similar how",
                "Discord use their mentions."
        ),
        TAGS(Type.CHAT, 6, 2,
                "Handles if SIR can create custom",
                "tags for chat.",
                "Tags can be parsed in any plugin",
                "or message by PlaceholderAPI."
        ),
        MODERATION(Type.CHAT, 7, 2,
                "Handles if moderation should be",
                "applied on the chat to avoid insults,",
                "caps, spamming, etc."
        ),
        DISCORD(Type.HOOK, 3, 3,
                "Handles if DiscordSRV will work",
                "with SIR to display join-quit",
                "messages, chat messages, and more."
        ),
        LOGIN(Type.HOOK, 4, 3,
                "Handles if any login plugin can",
                "verify the player's login status",
                "before joining the server."
        ),
        VANISH(Type.HOOK, 5, 3,
                "Handles if any vanish plugin can",
                "modify the player's vanish status",
                "to handle join and quit messages."
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
