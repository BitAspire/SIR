package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.plugin.hook.VaultHolder;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@UtilityClass
class HolderUtils {

    @Getter
    static final class BasicHolder implements VaultHolder<Chat> {

        private final Chat source;

        BasicHolder() {
            ServicesManager manager = Bukkit.getServer().getServicesManager();

            RegisteredServiceProvider<Chat> chat = manager.getRegistration(Chat.class);
            if (chat == null)
                throw new IllegalStateException();

            source = chat.getProvider();
        }

        public Plugin getPlugin() {
            return Bukkit.getPluginManager().getPlugin(source.getName());
        }

        @Override
        public boolean isEnabled() {
            return source.isEnabled();
        }

        @Nullable
        public String getPrimaryGroup(Player player) {
            return source.getPrimaryGroup(player);
        }

        @Override
        public boolean isInGroup(Player player, String group) {
            return source.playerInGroup(player, group);
        }

        @NotNull
        public List<String> getGroups(Player player) {
            return ArrayUtils.toList(source.getPlayerGroups(player));
        }

        @Nullable
        public String getPrefix(Player player) {
            return source.getPlayerPrefix(player);
        }

        @Nullable
        public String getSuffix(Player player) {
            return source.getPlayerSuffix(player);
        }

        @Nullable
        public String getGroupPrefix(World world, String group) {
            return source.getGroupPrefix(world, group);
        }

        @Nullable
        public String getGroupSuffix(World world, String group) {
            return source.getGroupSuffix(world, group);
        }

        @NotNull
        public List<String> getGroups() {
            return ArrayUtils.toList(source.getGroups());
        }

        @Override
        public String toString() {
            return "VaultHolder{provider='VaultAPI', plugin='" + source.getName() + "'}";
        }
    }

    @Getter
    static final class LuckHolder implements VaultHolder<LuckPerms> {

        private final LuckPerms source;

        LuckHolder() {
            source = Objects.requireNonNull(Bukkit.getServer().getServicesManager().getRegistration(LuckPerms.class)).getProvider();
        }

        @Override
        public Plugin getPlugin() {
            return Bukkit.getPluginManager().getPlugin("LuckPerms");
        }

        @Override
        public boolean isEnabled() {
            return Exceptions.isPluginEnabled("LuckPerms");
        }

        <T> T getAsUser(Player player, Function<User, T> function) {
            User user = source.getUserManager().getUser(player.getUniqueId());
            return user != null ? function.apply(user) : null;
        }

        @Nullable
        public String getPrimaryGroup(Player player) {
            return getAsUser(player, User::getPrimaryGroup);
        }

        @Override
        public boolean isInGroup(Player player, String group) {
            return player.hasPermission("group." + group);
        }

        @NotNull
        public List<String> getGroups(Player player) {
            CollectionBuilder<Node> builder = getAsUser(player, u -> CollectionBuilder.of(u.getNodes()));
            return builder != null ?
                    builder.filter(n -> n instanceof InheritanceNode)
                            .map(n -> ((InheritanceNode) n).getGroupName())
                            .toList() :
                    new ArrayList<>();
        }

        @Nullable
        public String getPrefix(Player player) {
            return getAsUser(player, u -> u.getCachedData().getMetaData().getPrefix());
        }

        @Nullable
        public String getSuffix(Player player) {
            return getAsUser(player, u -> u.getCachedData().getMetaData().getSuffix());
        }

        @Nullable
        public String getGroupPrefix(World world, String group) {
            return getGroupPrefix(group);
        }

        @Nullable
        public String getGroupPrefix(String group) {
            Group g = source.getGroupManager().getGroup(group);
            return g != null ?
                    g.getCachedData().getMetaData().getPrefix() :
                    null;
        }

        @Nullable
        public String getGroupSuffix(World world, String group) {
            return getGroupSuffix(group);
        }

        @Nullable
        public String getGroupSuffix(String group) {
            Group g = source.getGroupManager().getGroup(group);
            return g != null ?
                    g.getCachedData().getMetaData().getSuffix() :
                    null;
        }

        @NotNull
        public List<String> getGroups() {
            return CollectionBuilder
                    .of(source.getGroupManager().getLoadedGroups())
                    .map(Group::getName).toList();
        }

        @Override
        public String toString() {
            return "VaultHolder{provider='LuckPerms', version=" + getPlugin().getDescription().getVersion() + "}";
        }
    }

    static final class NoHolder implements VaultHolder<Object> {

        @NotNull
        public Object getSource() {
            throw new IllegalStateException("No source was found");
        }

        @Override
        public Plugin getPlugin() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public <V> V fromSource(Function<Object, V> function) {
            throw new IllegalStateException("No source was found");
        }

        @Nullable
        public String getPrimaryGroup(Player player) {
            return null;
        }

        @Override
        public boolean isInGroup(Player player, String group) {
            return false;
        }

        @NotNull
        public List<String> getGroups(Player player) {
            return new ArrayList<>();
        }

        @Nullable
        public String getPrefix(Player player) {
            return null;
        }

        @Nullable
        public String getSuffix(Player player) {
            return null;
        }

        @Nullable
        public String getGroupPrefix(World world, String group) {
            return null;
        }

        @Nullable
        public String getGroupSuffix(World world, String group) {
            return null;
        }

        @Override
        public @NotNull List<String> getGroups() {
            return new ArrayList<>();
        }

        @Override
        public String toString() {
            return "VaultHolder{provider='NONE'}";
        }
    }

    VaultHolder<?> loadHolder() {
        if (Exceptions.isPluginEnabled("LuckPerms"))
            return new LuckHolder();

        try {
            return new BasicHolder();
        } catch (Exception e1) {
            return new NoHolder();
        }
    }
}
