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
package me.machinemaker.papertweaks.modules.teleportation.homes;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import me.machinemaker.papertweaks.cloud.cooldown.CommandCooldownManager;
import me.machinemaker.papertweaks.cloud.dispatchers.CommandDispatcher;
import me.machinemaker.papertweaks.modules.teleportation.back.Back;
import me.machinemaker.papertweaks.utils.runnables.TeleportRunnable;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;

class HomeTeleportRunnable extends TeleportRunnable {

    @Inject private static CommandCooldownManager<CommandDispatcher, UUID> cooldownManager;
    @Inject private static Plugin plugin;
    static final Map<UUID, BukkitTask> AWAITING_TELEPORT = Maps.newHashMap();

    private final Audience audience;

    HomeTeleportRunnable(@NotNull Player player, @NotNull Location teleportLoc, long tickDelay, Audience audience) {
        super(player, teleportLoc, tickDelay);
        this.audience = audience;
    }

    public void start() {
        AWAITING_TELEPORT.put(player.getUniqueId(), this.runTaskTimer(plugin, 1L, 1L));
    }

    @Override
    public void onTeleport() {
        Back.setBackLocation(this.player, this.player.getLocation()); // Set back location
    }

    @Override
    public void onMove() {
        this.audience.sendMessage(translatable("modules.homes.commands.home.moved", RED));
        cooldownManager.invalidate(this.player.getUniqueId(), Commands.HOME_COMMAND_COOLDOWN_KEY);
    }

    @Override
    public void onEnd() {
        AWAITING_TELEPORT.remove(this.player.getUniqueId());
    }
}
