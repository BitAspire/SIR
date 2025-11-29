package me.croabeast.sir.command;

import me.croabeast.command.BaseCommand;
import me.croabeast.command.BukkitCommand;
import me.croabeast.command.CommandPredicate;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.reflect.Reflector;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class SIRCommand extends BukkitCommand {

    final SIRApi api = SIRApi.instance();
    final TakionLib lib = api.getLibrary();

    final ConfigurableFile config;
    CommandFile file;

    private MessageSender create(CommandSender sender) {
        return lib.getLoadedSender().setTargets(sender);
    }

    protected SIRCommand(ConfigurableFile config) {
        super(SIRApi.instance().getPlugin(), "");

        this.config = Objects.requireNonNull(config, "Config file cannot be null");
        reloadOptions();

        setExecutingError((sender, e) -> {
            e.printStackTrace();
            return create(sender).send("<P> &7Error executing the command " + getName() + ": &c" + e.getLocalizedMessage());
        });
        setCompletingError((sender, e) -> {
            e.printStackTrace();
            return create(sender).send("<P> &7Error completing the command " + getName() + ": &c" + e.getLocalizedMessage());
        });

        setWrongArgumentAction((sender, arg) ->
                create(sender).addPlaceholder("{arg}", arg).send(getLangConfig().toStringList("lang.wrong-arg")));
    }

    private class SubCommand extends me.croabeast.command.SubCommand {

        public SubCommand(String name, String permission) {
            super(SIRCommand.this, name);
            setPermission(permission);
        }

        @Override
        public boolean isPermitted(CommandSender sender, boolean log) {
            UserManager manager = api.getUserManager();
            return manager.hasPermission(sender, getWildcardPermission()) ||
                    manager.hasPermission(sender, getPermission());
        }
    }

    boolean reloadOptions() {
        try {
            file = new CommandFile(config.getConfiguration());
            Reflector.from(() -> this).set("name", file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        super.setPermission(file.getPermission());

        file.getSubCommands().keySet().forEach(this::removeSubCommand);
        file.getSubCommands().forEach((k, v) ->
                addSubCommand(new SubCommand(k, v)));

        super.setDescription(file.getDescription());
        super.setUsage(file.getUsage());
        super.setAliases(file.getAliases());

        super.setPermissionMessage(file.getPermissionMessage());
        return true;
    }

    @NotNull
    public final SIRCommand setAliases(@NotNull List<String> aliases) {
        return this;
    }

    @NotNull
    public final SIRCommand setDescription(@NotNull String description) {
        return this;
    }

    @NotNull
    public final SIRCommand setUsage(@NotNull String usage) {
        return this;
    }

    @NotNull
    public final SIRCommand setPermissionMessage(@Nullable String permissionMessage) {
        return this;
    }

    @Override
    public final boolean setLabel(@NotNull String name) {
        return false;
    }

    @Override
    public final boolean setName(@NotNull String name) {
        return false;
    }

    @Override
    public final void setPermission(@Nullable String permission) {}

    @Override
    public final void setAliases(String... aliases) {}

    @NotNull
    public final String getName() {
        return file.getName();
    }

    @NotNull
    public final String getPermission() {
        return file.getPermission();
    }

    @NotNull
    public final String getUsage() {
        return file.getUsage();
    }

    @NotNull
    public final String getDescription() {
        return file.getDescription();
    }

    @NotNull
    public final List<String> getAliases() {
        return file.getAliases();
    }

    @NotNull
    public final String getPermissionMessage() {
        return file.getPermissionMessage();
    }

    @NotNull
    protected final ConfigurableFile getRootConfig() {
        return config;
    }

    @NotNull
    protected abstract ConfigurableFile getLangConfig();

    public final boolean hasParent() {
        return file.hasParent();
    }

    public final boolean isEnabled() {
        return file.isEnabled();
    }

    public final boolean isOverriding() {
        return file.isOverride();
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
            super(api.getLibrary().getLoadedSender());
        }

        @NotNull
        public MessageSender copy() {
            return new CommandDisplayer(this);
        }

        @Override
        public boolean send(String... strings) {
            if (strings.length != 1)
                throw new NullPointerException("Needs only a single path");

            return super.send(getLangConfig().toStringList("lang." + strings[0]));
        }
    }

    protected final boolean checkPlayer(CommandSender sender, String name) {
        return createSender(sender)
                .setLogger(false).addPlaceholder("{target}", name)
                .send(getRootConfig().toStringList("lang.not-player"));
    }

    public final boolean testPermissionSilent(@NotNull CommandSender target) {
        UserManager manager = api.getUserManager();
        return manager.hasPermission(target, getPermission()) ||
                manager.hasPermission(target, getWildcardPermission());
    }

    public final boolean isPermitted(CommandSender sender, boolean log) {
        if (testPermissionSilent(sender))
            return true;

        Player player = sender instanceof Player ? (Player) sender : null;
        if (log)
            create(player).addPlaceholder("{perm}", getPermission())
                    .send(getRootConfig().toStringList("lang.no-permission"));

        return false;
    }

    protected TabBuilder createBasicTabBuilder() {
        return new TabBuilder().setPermissionPredicate(api.getUserManager()::hasPermission);
    }

    protected final MessageSender createSender(CommandSender sender) {
        return new CommandDisplayer().setLogger(!(sender instanceof Player)).setTargets(sender);
    }

    protected final boolean editSubCommand(String name, CommandPredicate predicate) {
        if (StringUtils.isBlank(name) || predicate == null)
            return false;

        final BaseCommand subCommand = getSubCommand(name);
        if (subCommand == null) return false;

        ((me.croabeast.command.SubCommand) subCommand).setPredicate(predicate);
        return true;
    }

    @Override
    public boolean register(boolean sync) {
        return reloadOptions() && super.register(sync);
    }

    @Override
    public boolean unregister(boolean sync) {
        return reloadOptions() && super.unregister(sync);
    }

    @Override
    public final boolean register() {
        return register(true);
    }

    @Override
    public final boolean unregister() {
        return unregister(true);
    }

    @Override
    public String toString() {
        return "SIRCommand{name='" + getName() + "'}";
    }

    protected static List<String> getOnlineNames() {
        return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(HumanEntity::getName).toList();
    }
}
