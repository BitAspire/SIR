package me.croabeast.sir.command;

import lombok.Getter;
import me.croabeast.command.BukkitCommand;
import me.croabeast.command.CommandPredicate;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public abstract class SIRCommand extends BukkitCommand {

    protected static final String LANG_PREFIX = "lang.";
    private static GlobalTask task = null;

    protected final SIRApi api;
    protected final TakionLib lib;

    @Nullable
    private final ConfigurableFile lang;

    @Getter
    private String commandKey;
    private CommandFile file;

    public SIRCommand(String name, @Nullable ConfigurableFile lang) {
        super(SIRApi.instance().getPlugin(), name);
        this.api = SIRApi.instance();
        this.lib = api.getLibrary();
        this.lang = lang;
        this.commandKey = name;

        configureErrorHandlers();
    }

    // ----------------------------------------------------------------------
    // Base metadata
    // ----------------------------------------------------------------------
    private void configureErrorHandlers() {
        setExecutingError((sender, e) -> {
            e.printStackTrace();
            return rawSender(sender).send("<P> &7Error executing the command " + getName() + ": &c" + e.getLocalizedMessage());
        });
        setCompletingError((sender, e) -> {
            e.printStackTrace();
            return rawSender(sender).send("<P> &7Error completing the command " + getName() + ": &c" + e.getLocalizedMessage());
        });
        setWrongArgumentAction((sender, arg) ->
                rawSender(sender).addPlaceholder("{arg}", arg)
                        .send(resolveMessages("wrong-arg", "<P> &cInvalid argument: &f{arg}&c.")));
    }

    @Nullable
    public final ConfigurableFile getLang() {
        return lang;
    }

    // ----------------------------------------------------------------------
    // Bukkit metadata overrides
    // ----------------------------------------------------------------------
    @Override
    @NotNull
    public final String getName() {
        return file == null ? (commandKey == null ? "" : commandKey) : file.getName();
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

    // ----------------------------------------------------------------------
    // Command file-backed properties
    // ----------------------------------------------------------------------
    @NotNull
    public final String getPermission() {
        return file == null ? "" : file.getPermission();
    }

    @NotNull
    public final String getUsage() {
        return file == null ? "" : file.getUsage();
    }

    @NotNull
    public final String getDescription() {
        return file == null ? "" : file.getDescription();
    }

    @NotNull
    public final List<String> getAliases() {
        return file == null ? new ArrayList<>() : file.getAliases();
    }

    @NotNull
    public final String getPermissionMessage() {
        return file == null ? "" : file.getPermissionMessage();
    }

    // ----------------------------------------------------------------------
    // Relationships and state
    // ----------------------------------------------------------------------
    public final boolean hasParent() {
        return file != null && file.hasParent();
    }

    @Nullable
    public final SIRCommand getParent() {
        return api.getCommandManager().getCommand(file == null ? null : file.getParentName());
    }

    public boolean isEnabled() {
        return true;
    }

    public final boolean isOverriding() {
        return file != null && file.isOverride();
    }

    protected abstract boolean execute(CommandSender sender, String[] args);

    // ----------------------------------------------------------------------
    // Execution hooks
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // Permission helpers
    // ----------------------------------------------------------------------
    public final boolean testPermissionSilent(@NotNull CommandSender target) {
        UserManager manager = api.getUserManager();
        return manager.hasPermission(target, getPermission()) ||
                manager.hasPermission(target, getWildcardPermission());
    }

    public final boolean isPermitted(CommandSender sender, boolean log) {
        if (testPermissionSilent(sender))
            return true;

        if (log)
            createSender(sender).addPlaceholder("{perm}", getPermission())
                    .send(resolveMessages("no-permission", "<P> &cYou do not have permission: &f{perm}&c."));

        return false;
    }

    protected TabBuilder createBasicTabBuilder() {
        return new TabBuilder().setPermissionPredicate(api.getUserManager()::hasPermission);
    }

    // ----------------------------------------------------------------------
    // Messaging helpers
    // ----------------------------------------------------------------------
    public final MessageSender createSender(CommandSender sender) {
        return new CommandDisplayer().setLogger(!(sender instanceof Player)).setTargets(sender);
    }

    protected final boolean checkPlayer(CommandSender sender, String name) {
        return createSender(sender)
                .setLogger(false)
                .addPlaceholder("{target}", name)
                .send(resolveMessages("not-player", "<P> &cPlayer not found: &f{target}&c."));
    }

    protected final boolean isSubCommandPermitted(CommandSender sender, String name, boolean log) {
        if (file == null) return isPermitted(sender, log);

        String permission = file.getSubCommands().get(name);
        if (permission == null) return isPermitted(sender, log);

        UserManager manager = api.getUserManager();
        if (manager.hasPermission(sender, permission) ||
                manager.hasPermission(sender, getWildcardPermission()))
            return true;

        if (log)
            createSender(sender).addPlaceholder("{perm}", permission)
                    .send(resolveMessages("no-permission", "<P> &cYou do not have permission: &f{perm}&c."));

        return false;
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------
    @Override
    public boolean register(boolean sync) {
        reloadOptions();
        return super.register(sync);
    }

    @Override
    public boolean unregister(boolean sync) {
        reloadOptions();
        return super.unregister(sync);
    }

    @Override
    public final boolean register() {
        return register(true);
    }

    @Override
    public final boolean unregister() {
        return unregister(true);
    }

    void applyFile(CommandFile file) {
        this.file = Objects.requireNonNull(file, "Command file cannot be null");
        this.commandKey = file.getName();
        reloadOptions();
    }

    private void reloadOptions() {
        try {
            super.setName(file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.setPermission(file.getPermission());
        super.setDescription(file.getDescription());
        super.setUsage(file.getUsage());
        super.setAliases(file.getAliases());
        super.setPermissionMessage(file.getPermissionMessage());
    }

    private List<String> resolveMessages(String key, String fallback) {
        if (lang == null)
            return Collections.singletonList(fallback);

        List<String> resolved = lang.toStringList(LANG_PREFIX + key);
        if (resolved == null || resolved.isEmpty())
            return Collections.singletonList(fallback);

        return resolved;
    }

    private MessageSender rawSender(CommandSender sender) {
        return lib.getLoadedSender().setTargets(sender);
    }

    // ----------------------------------------------------------------------
    // Diagnostics
    // ----------------------------------------------------------------------
    @Override
    public String toString() {
        return "SIRCommand{name='" + getName() + "'}";
    }

    public static void scheduleSync() {
        GlobalScheduler scheduler = SIRApi.instance().getScheduler();

        scheduler.runTask(() -> {
            if (task != null) task.cancel();

            task = scheduler.runTaskLater(() -> {
                task = null;
                syncCommands();
            }, 1L);
        });
    }

    protected static List<String> getOnlineNames() {
        return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(HumanEntity::getName).toList();
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

            return super.send(resolveMessages(strings[0], "<P> &7Missing language message: &f" + strings[0]));
        }
    }
}
