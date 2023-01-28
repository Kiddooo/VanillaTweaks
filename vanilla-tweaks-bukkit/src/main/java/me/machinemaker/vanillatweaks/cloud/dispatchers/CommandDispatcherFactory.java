/*
 * GNU General Public License v3
 *
 * PaperTweaks, a performant replacement for the VanillaTweaks datapacks.
 *
 * Copyright (C) 2021 Machine_Maker
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
package me.machinemaker.vanillatweaks.cloud.dispatchers;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Factory for creating implementations of {@link CommandDispatcher}.
 */
public class CommandDispatcherFactory extends CacheLoader<CommandSender, CommandDispatcher> {

    private final BukkitAudiences audiences;

    @Inject
    public CommandDispatcherFactory(final BukkitAudiences audiences) {
        this.audiences = audiences;
    }


    public CommandDispatcher from(final CommandSender sender) {
        if (sender instanceof ConsoleCommandSender consoleCommandSender) {
            return new ConsoleCommandDispatcher(consoleCommandSender, this.audiences.console());
        } else if (sender instanceof Player player) {
            return new PlayerCommandDispatcher(player, this.audiences::player);
        }
        throw new IllegalArgumentException(sender + " is unknown");
    }

    @Override
    public CommandDispatcher load(final CommandSender key) {
        return this.from(key);
    }
}
