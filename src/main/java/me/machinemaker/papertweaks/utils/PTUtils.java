/*
 * GNU General Public License v3
 *
 * PaperTweaks, a performant replacement for the VanillaTweaks datapacks.
 *
 * Copyright (C) 2021-2023 Machine_Maker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package me.machinemaker.papertweaks.utils;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import me.machinemaker.mirror.FieldAccessor;
import me.machinemaker.mirror.MethodInvoker;
import me.machinemaker.mirror.Mirror;
import me.machinemaker.mirror.paper.PaperMirror;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

@DefaultQualifier(NonNull.class)
public final class PTUtils {

    private static final Class<?> CRAFT_PLAYER_CLASS = PaperMirror.getCraftBukkitClass("entity.CraftPlayer");
    private static final Class<?> NMS_PLAYER_CLASS = PaperMirror.findMinecraftClass("world.entity.player.EntityHuman", "world.entity.player.Player");
    private static final Class<?> CRAFT_META_SKULL_CLASS = PaperMirror.getCraftBukkitClass("inventory.CraftMetaSkull");
    private static final FieldAccessor.Typed<GameProfile> CRAFT_META_ITEM_GAME_PROFILE = Mirror.typedFuzzyField(CRAFT_META_SKULL_CLASS, GameProfile.class).names("profile").find();
    private static final MethodInvoker CRAFT_PLAYER_GET_HANDLE = Mirror.fuzzyMethod(CRAFT_PLAYER_CLASS, NMS_PLAYER_CLASS).names("getHandle").find();
    private static final MethodInvoker.Typed<GameProfile> NMS_PLAYER_GET_PLAYER_PROFILE = Mirror.typedFuzzyMethod(NMS_PLAYER_CLASS, GameProfile.class).find();

    private static final Gson GSON = new Gson();

    private PTUtils() {
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable Component sanitizeName(final @Nullable String name) {
        if (name == null) {
            return null;
        }
        final JsonElement tree = GsonComponentSerializer.gson().serializer().fromJson(name, JsonElement.class);
        if (tree instanceof final JsonObject object && object.has("text")) {
            final String text = object.getAsJsonPrimitive("text").getAsString();
            if (text.contains("§")) {
                if (object.size() == 1) {
                    final TextComponent deserialized = LegacyComponentSerializer.legacySection().deserialize(text);
                    if (text.contains("§r")) {
                        return deserialized.decoration(ITALIC, false);
                    }
                    return deserialized;
                } else {
                    throw new IllegalStateException("This is a bug. Report to PaperTweaks Discord or GitHub: %s".formatted(name));
                }
            }
        }
        return GsonComponentSerializer.gson().deserializeFromTree(tree);
    }

    public static GameProfile getGameProfile(final Player player) {
        final GameProfile live = Objects.requireNonNull(NMS_PLAYER_GET_PLAYER_PROFILE.invoke(CRAFT_PLAYER_GET_HANDLE.invoke(player)), () -> "unexpected null GameProfile from " + player);
        final GameProfile copy = new GameProfile(live.getId(), live.getName());
        copy.getProperties().putAll(live.getProperties());
        return copy;
    }

    public static ItemStack getSkull(final Component name, final String texture) {
        return getSkull(name, null, texture, 1);
    }

    public static ItemStack getSkull(final Component name, final @Nullable UUID uuid, final String texture, final int count) {
        return getSkull(name, null, uuid, texture, count);
    }

    public static ItemStack getSkull(final String gameProfileName, final UUID uuid, final String texture) {
        return getSkull(null, gameProfileName, uuid, texture, 1);
    }

    public static ItemStack getSkull(final @Nullable Component name, final @Nullable String gameProfileName, final @Nullable UUID uuid, final @Nullable String texture, final int count) {
        final ItemStack skull = new ItemStack(Material.PLAYER_HEAD, count);
        if (name == null && gameProfileName == null && uuid == null && texture == null) {
            return skull;
        }
        final @Nullable SkullMeta meta = (SkullMeta) Objects.requireNonNull(skull.getItemMeta());
        final GameProfile profile = new GameProfile(uuid == null ? UUID.randomUUID() : uuid, gameProfileName);
        profile.getProperties().put("textures", new Property("textures", texture));
        loadMeta(meta, profile, name);
        skull.setItemMeta(meta);
        return skull;
    }

    public static void sanitizeTextures(final GameProfile profile) {
        final Property textures = Iterables.getFirst(profile.getProperties().get("textures"), null);
        profile.getProperties().removeAll("textures");
        if (textures != null) {
            final JsonObject object = GSON.fromJson(new String(Base64.getDecoder().decode(textures.getValue()), StandardCharsets.UTF_8), JsonObject.class);
            object.remove("timestamp");
            profile.getProperties().put("textures", new Property("textures", Base64.getEncoder().encodeToString(GSON.toJson(object).getBytes(StandardCharsets.UTF_8))));
        }
    }

    public static void loadMeta(final SkullMeta meta, final GameProfile profile) {
        loadMeta(meta, profile, null);
    }

    public static void loadMeta(final SkullMeta meta, final GameProfile profile, final @Nullable Component name) {
        CRAFT_META_ITEM_GAME_PROFILE.set(meta, profile);
        if (name != null) {
            loadMeta(meta, name);
        }
    }

    public static void loadMeta(final ItemMeta meta, final Component displayName) {
        meta.displayName(displayName);
    }

    public static Location toBlockLoc(final Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Location toCenter(final Location location, final boolean includeY) {
        final Location center = location.clone();
        center.setX(location.getBlockX() + 0.5);
        if (includeY) {
            center.setY(location.getBlockY() + 0.5);
        }
        center.setZ(location.getBlockZ() + 0.5);
        return center;
    }

    public static <T> T random(final Collection<T> coll) {
        int num = (int) (Math.random() * coll.size());
        for (final T t : coll) if (--num < 0) return t;
        throw new AssertionError();
    }

    public static <T> Map<CachedHashObjectWrapper<T>, MutableInt> toCachedMapCount(final List<T> list) {
        final Map<CachedHashObjectWrapper<T>, MutableInt> listCount = new HashMap<>();
        for (final T item : list) {
            listCount.computeIfAbsent(new CachedHashObjectWrapper<>(item), (k) -> new MutableInt()).increment();
        }
        return listCount;
    }

    /**
     * Replaces all occurrences of items from {unioned} that are not in {with} with null.
     */
    public static <T> List<@Nullable T> nullUnionList(final List<T> unioned, final List<T> with) {
        final Map<CachedHashObjectWrapper<T>, MutableInt> withCount = toCachedMapCount(with);
        return nullUnionList(unioned, withCount);
    }

    public static <T> List<@Nullable T> nullUnionList(final List<T> unioned,
                                            final Map<CachedHashObjectWrapper<T>, MutableInt> with) {
        final List<@Nullable T> result = new ArrayList<>();
        for (final T item : unioned) {
            final MutableInt x = with.get(new CachedHashObjectWrapper<>(item));
            if (x == null || x.intValue() <= 0) {
                result.add(null);
            } else {
                result.add(item);
                x.decrement();
            }
        }
        return result;
    }

    public static void runIfHasPermission(final String permission, final Consumer<CommandSender> consumer) {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                consumer.accept(player);
            }
        }
        consumer.accept(Bukkit.getConsoleSender());
    }
}
