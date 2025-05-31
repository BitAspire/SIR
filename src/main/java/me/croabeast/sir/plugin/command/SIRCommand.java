package me.croabeast.sir.plugin.command;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.command.*;
import me.croabeast.file.Configurable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.common.util.Exceptions;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.aspect.AspectButton;
import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.aspect.SIRAspect;
import me.croabeast.sir.plugin.manager.UserManager;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@Accessors(makeFinal = true)
public abstract class SIRCommand extends BukkitCommand {

    protected final SIRPlugin plugin;
    private Options options;

    protected final ConfigurableFile file;

    @Getter
    private final String name;
    @Getter
    private final AspectKey aspectKey;

    final boolean modifiable;
    @Setter @Getter
    protected AspectButton button;

    @Accessors(makeFinal = false)
    @Nullable @Getter
    private SIRAspect parent = null;

    @Getter(AccessLevel.PRIVATE)
    private class Options {

        private final String permission;

        private final boolean override;
        private final boolean enabled;

        private final List<String> subCommands;
        private final List<String> aliases;

        @Nullable
        private final String parent;

        Options(String name) throws IllegalStateException {
            ConfigurationSection s = file.getSection("commands." + name);
            Objects.requireNonNull(s);

            permission = s.getString("permissions.main", "");
            Exceptions.validate(permission, StringUtils::isNotBlank);

            parent = s.getString("parent");

            if (StringUtils.isNotBlank(parent)) {
                final String path = "commands." + parent + ".";

                override = file.get(path + "override-existing", true);
                enabled = file.get(path + "enabled", true);
            } else {
                override = s.getBoolean("override-existing");
                enabled = s.getBoolean("enabled", true);
            }

            subCommands = Configurable.toStringList(s, "permissions.subcommands");
            aliases = Configurable.toStringList(s, "aliases");
        }
    }

    static Player fromSender(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    private SIRCommand(AspectKey key, String name, boolean modifiable) {
        super(SIRPlugin.getInstance(), name);

        this.file = FileData.Command.getMain();
        this.name = name;

        this.plugin = SIRPlugin.getInstance();
        this.modifiable = modifiable;

        this.aspectKey = key;
        this.options = new Options(this.name);

        if (aspectKey instanceof Key)
            ((Key) aspectKey).init = this;

        options.getSubCommands()
                .forEach(s -> registerSubCommand(new SubCommand(this, s) {
                    @Override
                    public boolean isPermitted(CommandSender s, boolean log) {
                        final String wild = getWildcardPermission();
                        return UserManager.hasPermission(s, wild) ||
                                UserManager.hasPermission(s, getPermission());
                    }
                }));
        setExecutingError((sender, e) -> {
            e.printStackTrace();

            return plugin.getLibrary().getLoadedSender()
                    .setTargets(fromSender(sender))
                    .send("<P> &7Error executing the command " +
                            getName() + ": &c" +
                            e.getLocalizedMessage());
        });
        setCompletingError((sender, e) -> {
            e.printStackTrace();

            return plugin.getLibrary().getLoadedSender()
                    .setTargets(fromSender(sender))
                    .send("<P> &7Error completing the command " +
                            getName() + ": &c" +
                            e.getLocalizedMessage());
        });
        setWrongArgumentAction((sender, arg) -> {
            plugin.getLibrary()
                    .getLoadedSender().setTargets(fromSender(sender))
                    .addPlaceholder("{arg}", arg)
                    .send(file.toStringList("lang.wrong-arg"));
            return true;
        });

        button = new AspectButton(this, key, options.isEnabled());

        button.setDefaultItems();
        button.allowToggle(modifiable);

        button.setOnClick(b -> e -> {
            if (parent != null) {
                final String pName = parent.getName();
                plugin.getLibrary().getLoadedSender()
                        .setLogger(false)
                        .setTargets(e.getView().getPlayer())
                        .send(
                                "<P> The module &a" + pName + "&7 should be toggled instead of this command.");

                e.setCancelled(true);
                return;
            }

            if (!modifiable) {
                plugin.getLibrary().getLoadedSender()
                        .setLogger(false)
                        .setTargets(e.getView().getPlayer())
                        .send(
                                "<P> This command was marked as not modifiable. It can't be disabled.");

                e.setCancelled(true);
                return;
            }

            file.set("commands." + name + ".enabled", b.isEnabled());
            file.save();

            loadOptionsFromFile();

            plugin.getLibrary().getLogger()
                    .log("Command '/" + name + "' registered: " + b.isEnabled());
            b.toggleRegistering();
        });
    }

    private void loadParent(SIRAspect parent) {
        this.parent = parent;
        button.allowToggle(false);

        if (parent.isEnabled() != button.isEnabled())
            button.toggleAll();
    }

    protected SIRCommand(@NotNull SIRAspect parent, String name) {
        this(parent.getKey(), name, true);
        loadParent(parent);
    }

    protected SIRCommand(@NotNull SIRAspect parent, Key key) {
        this(key, key.getName(), true);
        loadParent(parent);
    }

    protected SIRCommand(@NotNull Key key, boolean modifiable) {
        this(key, key.getName(), modifiable);
    }

    private void loadOptionsFromFile() {
        if (modifiable) options = new Options(name);
    }

    @Override
    public final boolean isRegistered() {
        return super.isRegistered();
    }

    @Override
    public final boolean isEnabled() {
        return button.isEnabled();
    }

    @Override
    public final boolean isOverriding() {
        return !modifiable || options.isOverride();
    }

    @NotNull
    protected abstract ConfigurableFile getLang();

    @Override
    public final boolean setLabel(@NotNull String name) {
        return false;
    }

    @Override
    public final boolean setName(@NotNull String name) {
        return false;
    }

    @NotNull
    public final Command setDescription(@NotNull String description) {
        return this;
    }

    @NotNull
    public final Command setUsage(@NotNull String usage) {
        return this;
    }

    @NotNull
    public Command setPermissionMessage(@Nullable String message) {
        return this;
    }

    @NotNull
    public final String getPermission() {
        return options.getPermission();
    }

    public final void setPermission(String permission) {
        loadOptionsFromFile();
    }

    @NotNull
    public final List<String> getAliases() {
        return options.getAliases();
    }

    @NotNull
    public final SIRCommand setAliases(@NotNull List<String> aliases) {
        loadOptionsFromFile();
        return this;
    }

    protected abstract boolean execute(CommandSender sender, String[] args);

    @NotNull
    public final CommandPredicate getPredicate() {
        return this::execute;
    }

    public abstract TabBuilder getCompletionBuilder();

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        TabBuilder builder = getCompletionBuilder();
        return () -> builder == null ? new ArrayList<>() : builder.build(sender, arguments);
    }

    private class CommandDisplayer extends MessageSender {

        private CommandDisplayer(MessageSender sender) {
            super(sender);
        }

        private CommandDisplayer() {
            super(plugin.getLibrary().getLoadedSender());
        }

        @NotNull
        public MessageSender copy() {
            return new CommandDisplayer(this);
        }

        @Override
        public boolean send(String... strings) {
            if (strings.length != 1)
                throw new NullPointerException("Needs only a single path");

            return super.send(getLang().toStringList("lang." + strings[0]));
        }
    }

    public final boolean testPermissionSilent(@NotNull CommandSender target) {
        return UserManager.hasPermission(target, getPermission()) ||
                UserManager.hasPermission(target, getWildcardPermission());
    }

    public final boolean isPermitted(CommandSender sender, boolean log) {
        if (testPermissionSilent(sender))
            return true;

        Player player = sender instanceof Player ? (Player) sender : null;
        if (log)
            plugin.getLibrary().getLoadedSender().setTargets(player)
                    .addPlaceholder("{perm}", getPermission())
                    .send(file.toStringList("lang.no-permission"));

        return false;
    }

    protected final MessageSender createSender(CommandSender sender) {
        return new CommandDisplayer().setLogger(fromSender(sender) == null).setTargets(sender);
    }

    protected final boolean editSubCommand(String name, CommandPredicate predicate) {
        if (StringUtils.isBlank(name) || predicate == null)
            return false;

        final BaseCommand subCommand = getSubCommand(name);
        if (subCommand == null) return false;

        ((SubCommand) subCommand).setPredicate(predicate);
        return true;
    }

    @Override
    public final boolean register(boolean sync) {
        loadOptionsFromFile();
        return super.register(sync);
    }

    @Override
    public final boolean unregister(boolean sync) {
        loadOptionsFromFile();
        return super.unregister(sync);
    }

    @Override
    public final boolean register() {
        return register(true);
    }

    @Override
    public boolean unregister() {
        return unregister(true);
    }

    @Override
    public String toString() {
        return "SIRCommand{name='" + getName() + "'}";
    }

    protected static List<String> getOnlineNames() {
        return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(HumanEntity::getName).toList();
    }

    protected static TabBuilder createBasicTabBuilder() {
        return new TabBuilder().setPermissionPredicate(UserManager::hasPermission);
    }

    @Accessors(makeFinal = false)
    @Getter
    public enum Key implements AspectKey {
        SIR(3, 1,
                "The main command of this plugin.",
                "It can't be disabled."
        ),
        PRINT(4, 1,
                "Sent direct messages to players",
                "with color format, placeholders,",
                "and more."),
        ANNOUNCER(5, 1,
                "Handles the announcements' preview",
                "of the Announcement module."
        ),
        CHAT_VIEW(6, 1,
                "Toggles any loaded local channel's",
                "view for the player."
        ) {
            @NotNull
            public String getName() {
                return "chatview";
            }

            @NotNull
            public String getTitle() {
                return "Chat View";
            }
        },
        IGNORE(7, 1,
                "Ignores any player from private",
                "messages or chat."
        ),
        CLEAR_CHAT(3, 2,
                "Clear the chat of the server or for",
                "a single player."
        ) {
            @NotNull
            public String getName() {
                return "clearchat";
            }

            @NotNull
            public String getTitle() {
                return "Clear Chat";
            }
        },
        CHAT_COLOR(4, 2,
                "Handles the /color command.",
                "It allows players to change their",
                "chat color."
        ) {
            @NotNull
            public String getName() {
                return "chatcolor";
            }

            @NotNull
            public String getTitle() {
                return "Chat Color";
            }
        },
        MSG_REPLY(5, 2,
                "Handles the /msg & /reply commands.",
                "Sent private message to players."
        ),
        MUTE(6, 2,
                "Handles all mute-related commands",
                "like /mute, /tempmute, /unmute and",
                "/checkmute."
        );

        private final String name;
        private final String[] description;
        private final UUID uuid;
        private final Slot menuSlot;

        @SuppressWarnings("all")
        @Setter
        private Supplier<Boolean> supplier = () -> false;
        private SIRCommand init;

        Key(int x, int y, String... description) {
            name = name()
                    .toLowerCase(Locale.ENGLISH)
                    .replace('_', '-');

            uuid = AspectKey.super.getUuid();
            menuSlot = Slot.fromXY(x, y);

            this.description = description;
        }

        @Override
        public boolean isEnabled() {
            return init == null ? supplier.get() : init.isEnabled();
        }
    }
}
