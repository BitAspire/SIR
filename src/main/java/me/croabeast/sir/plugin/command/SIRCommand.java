package me.croabeast.sir.plugin.command;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.command.*;
import me.croabeast.lib.file.Configurable;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.aspect.SIRAspect;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.manager.SIRUserManager;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class SIRCommand extends BukkitCommand implements SIRAspect {

    protected final SIRPlugin plugin;
    private Options options;

    final boolean modifiable;

    static Player fromSender(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    @Getter(AccessLevel.PACKAGE)
    class Options {

        private final String permission;
        private final List<String> subCommands;

        private final boolean enabled;
        private final List<String> aliases;

        private final boolean override;

        Options(String name) throws IllegalStateException {
            ConfigurationSection s = getMainFile().getSection("commands." + name);
            Objects.requireNonNull(s);

            final String path = "permissions.";

            this.permission = s.getString(path + "main", "");
            Exceptions.validate(StringUtils::isNotBlank, permission);

            enabled = s.getBoolean("enabled", true);
            aliases = Configurable.toStringList(s, "aliases");

            subCommands = Configurable.toStringList(s, path + "subcommands");
            override = s.getBoolean("override-existing");
        }
    }

    protected SIRCommand(String name, boolean modifiable) {
        super(SIRPlugin.getInstance(), name);

        this.options = new Options(name);

        this.plugin = SIRPlugin.getInstance();
        this.modifiable = modifiable;

        options.getSubCommands()
                .forEach(s -> registerSubCommand(new SubCommand(this, s) {
                    @Override
                    public boolean isPermitted(CommandSender sender, boolean log) {
                        return SIRUserManager.hasPerm(sender, getWildcardPermission()) ||
                                SIRUserManager.hasPerm(sender, this.getPermission());
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
                    .send(getMainFile().toStringList("lang.wrong-arg"));
            return true;
        });
    }

    /**
     * Constructs a new SIRCommand with the specified name.
     * @param name the name of the command
     */
    protected SIRCommand(String name) {
        this(name, true);
    }

    void loadOptionsFromFile() {
        if (modifiable) options = new Options(getName());
    }

    @Override
    public final boolean isRegistered() {
        return super.isRegistered();
    }

    @NotNull
    public final AspectKey getAspectKey() {
        throw new IllegalStateException("Keys are not supported for commands");
    }

    @NotNull
    public final ConfigurableFile getMainFile() {
        return FileData.Command.getMain();
    }

    /**
     * Gets the configurable file for language settings associated with this command.
     * @return the configurable file for language settings
     */
    @NotNull
    protected abstract ConfigurableFile getLang();

    /**
     * Gets the configurable file for data associated with this command.
     * @return the configurable file for data, or null if not applicable
     */
    @Nullable
    protected ConfigurableFile getData() {
        return null;
    }

    public boolean isEnabled() {
        return !modifiable || options.isEnabled();
    }

    @Override
    public boolean isOverriding() {
        return options.isOverride();
    }

    @NotNull
    public String getPermission() {
        return options.getPermission();
    }

    public final void setPermission(String permission) {
        loadOptionsFromFile();
    }

    @NotNull
    public List<String> getAliases() {
        return options.getAliases();
    }

    /**
     * Sets the aliases for this command from its file.
     *
     * @param aliases the list of aliases to set, file-dependent
     * @return this instance
     */
    @NotNull
    public final SIRCommand setAliases(@NotNull List<String> aliases) {
        loadOptionsFromFile();
        return this;
    }

    protected abstract boolean execute(CommandSender sender, String[] args);

    @NotNull
    public final Executable getExecutable() {
        return (sender, args) -> execute(sender, args) ? Executable.State.TRUE : Executable.State.FALSE;
    }

    public abstract TabBuilder getCompletionBuilder();

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        TabBuilder builder = getCompletionBuilder();
        return () -> builder == null ? new ArrayList<>() : builder.build(sender, arguments);
    }

    class CommandDisplayer extends MessageSender {

        private CommandDisplayer() {
            super(plugin.getLibrary().getLoadedSender());
        }

        @Override
        public boolean send(String... strings) {
            if (strings.length != 1)
                throw new NullPointerException("Needs only a single path");

            return super.send(getLang().toStringList("lang." + strings[0]));
        }
    }

    /**
     * Tests whether the specified command sender has the permission to execute this command.
     *
     * @param target the command sender to test
     * @return true if the command sender has the permission, otherwise false
     */
    public final boolean testPermissionSilent(@NotNull CommandSender target) {
        return SIRUserManager.hasPerm(target, getPermission()) ||
                SIRUserManager.hasPerm(target, getWildcardPermission());
    }

    public final boolean isPermitted(CommandSender sender, boolean log) {
        if (testPermissionSilent(sender))
            return true;

        Player player = sender instanceof Player ? (Player) sender : null;
        if (log)
            plugin.getLibrary().getLoadedSender()
                    .setTargets(player).addPlaceholder("{perm}", getPermission())
                    .send(getMainFile().toStringList("lang.no-permission"));

        return false;
    }

    /**
     * Creates a new MessageSender instance for sending messages from the command sender.
     *
     * @param sender the command sender
     * @return the MessageSender instance
     */
    protected MessageSender createSender(CommandSender sender) {
        return new CommandDisplayer().setTargets(fromSender(sender));
    }

    /**
     * Edits the behavior of a subcommand using a predicate.
     *
     * @param name      the name of the subcommand
     * @param predicate the predicate to apply
     *
     * @return true if the subcommand behavior was successfully edited, otherwise false
     */
    protected boolean editSubCommand(String name, BiPredicate<CommandSender, String[]> predicate) {
        if (StringUtils.isBlank(name) || predicate == null)
            return false;

        final BaseCommand subCommand = getSubCommand(name);
        if (subCommand == null) return false;

        ((SubCommand) subCommand).setExecutable(Executable.from(predicate));
        return true;
    }

    @Override
    public final boolean register() {
        loadOptionsFromFile();
        return super.register();
    }

    @Override
    public final boolean unregister() {
        loadOptionsFromFile();
        return super.unregister();
    }

    protected static List<String> getOnlineNames() {
        return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(HumanEntity::getName).toList();
    }

    protected static TabBuilder createBasicTabBuilder() {
        return new TabBuilder().setPermissionPredicate(SIRUserManager::hasPerm);
    }

    @Override
    public String toString() {
        return "SIRCommand{name='" + getName() + "'}";
    }
}
