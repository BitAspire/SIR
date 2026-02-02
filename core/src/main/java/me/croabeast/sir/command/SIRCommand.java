package me.croabeast.sir.command;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import me.croabeast.command.BukkitCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.ConfigurableFile;
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

    @Getter
    private final ConfigurableFile lang;

    protected final SIRApi api;
    protected final TakionLib lib;

    private CommandFile file;

    public SIRCommand(String name, ConfigurableFile lang) {
        super(SIRApi.instance().getPlugin(), name);

        lib = (api = SIRApi.instance()).getLibrary();
        this.lang = lang;

        setSynchronizer(api.getCommandManager().getSynchronizer());

        setExecuteCheck((s, e) -> Utils.create(s).send("<P> &7Error executing the command " + getName() + ": &c" + e.getLocalizedMessage()));
        setCompleteCheck((s, e) -> Utils.create(s).send("<P> &7Error completing the command " + getName() + ": &c" + e.getLocalizedMessage()));
        setArgumentCheck((s, a) -> Utils.create(s).addPlaceholder("{arg}", a).send("wrong-arg", "<P> &cInvalid argument: &f{arg}&c."));
    }

    void applyFile(CommandFile file) {
        this.file = Objects.requireNonNull(file, "Command file cannot be null");
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

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isOverriding() {
        return file != null && file.isOverride();
    }

    public boolean hasParent() {
        return file != null && file.hasParent();
    }

    @Nullable
    public SIRCommand getParent() {
        return api.getCommandManager().getCommand(file == null ? null : file.getParentName());
    }

    @Override
    public final boolean testPermissionSilent(@NotNull CommandSender target) {
        final UserManager manager = api.getUserManager();
        return manager.hasPermission(target, getPermission()) ||
                manager.hasPermission(target, getPermission(true));
    }

    @Override
    public final boolean isPermitted(CommandSender sender, boolean log) {
        return testPermissionSilent(sender) || (log && Utils.create(this, sender)
                .addPlaceholder("{perm}", getPermission())
                .send("no-permission", "<P> &cYou do not have permission: &f{perm}&c."));
    }

    protected boolean checkPlayer(CommandSender sender, String name) {
        return Utils.create(this, sender).setLogger(false)
                .addPlaceholder("{target}", name)
                .send("not-player", "<P> &cPlayer not found: &f{target}&c.");
    }

    protected boolean isSubCommandPermitted(CommandSender sender, String name, boolean log) {
        if (file == null) return isPermitted(sender, log);

        String permission = file.getSubCommands().get(name);
        if (permission == null)
            return isPermitted(sender, log);

        UserManager manager = api.getUserManager();
        if (manager.hasPermission(sender, permission) || manager.hasPermission(sender, getPermission(true)))
            return true;

        return log && Utils.create(this, sender).addPlaceholder("{perm}", permission)
                .send("no-permission", "<P> &cYou do not have permission: &f{perm}&c.");
    }

    @Override
    public abstract TabBuilder getCompletionBuilder();

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        TabBuilder builder = getCompletionBuilder();
        return () -> builder == null ? new ArrayList<>() : builder.build(sender, arguments);
    }

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

    @UtilityClass
    public class Utils {

        private List<String> resolve(ConfigurableFile lang, String... strings) {
            if (strings.length != 1 && strings.length != 2)
                throw new NullPointerException();

            String key = strings[0];
            if (lang == null) return ArrayUtils.toList(key);

            List<String> fallback = strings.length != 2 ?
                    new ArrayList<>() :
                    ArrayUtils.toList(strings[1]);

            List<String> result = lang.toStringList("lang." + key);
            return !result.isEmpty() ? result : fallback;
        }

        private final class SenderImpl extends MessageSender {

            private final ConfigurableFile lang;
            private final SIRCommand command;
            private final CommandSender sender;

            private SenderImpl(SIRCommand command, CommandSender sender) {
                super(command.lib.getLoadedSender());
                this.lang = (this.command = command).lang;
                setLogger(!((this.sender = sender) instanceof Player));
                setTargets(sender);
            }

            private SenderImpl(SenderImpl sender) {
                this(sender.command, sender.sender);
            }

            @NotNull
            public MessageSender copy() {
                return new SenderImpl(this);
            }

            @Override
            public boolean send(String... strings) {
                return super.send(resolve(lang, strings));
            }
        }

        private final class Default extends MessageSender {

            private Default(CommandSender sender) {
                super(SIRApi.instance().getLibrary().getLoadedSender());
                setLogger(!(sender instanceof Player));
                setTargets(sender);
            }

            @Override
            public boolean send(String... strings) {
                return super.send(resolve(null, strings));
            }
        }

        public MessageSender create(SIRCommand command, CommandSender sender) {
            return command != null ? new SenderImpl(command, sender) : new Default(sender);
        }

        public MessageSender create(CommandSender sender) {
            return create(null, sender);
        }

        public TabBuilder newBuilder() {
            return new TabBuilder().setPermissionPredicate(SIRApi.instance().getUserManager()::hasPermission);
        }

        public List<String> getOnlineNames() {
            return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(HumanEntity::getName).toList();
        }
    }
}
