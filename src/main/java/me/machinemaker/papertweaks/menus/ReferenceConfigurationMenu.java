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
package me.machinemaker.papertweaks.menus;

import cloud.commandframework.context.CommandContext;
import java.util.List;
import me.machinemaker.papertweaks.cloud.dispatchers.CommandDispatcher;
import me.machinemaker.papertweaks.cloud.dispatchers.PlayerCommandDispatcher;
import me.machinemaker.papertweaks.menus.parts.MenuPartLike;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ReferenceConfigurationMenu<S> extends AbstractConfigurationMenu<S> {

    private final S reference;

    public ReferenceConfigurationMenu(final Component title, final String commandPrefix, final List<MenuPartLike<S>> parts, final S reference) {
        super(title, commandPrefix, parts);
        this.reference = reference;
    }

    public void send(final CommandContext<CommandDispatcher> context) {
        this.send(context.getSender(), this.reference);
    }

    @Override
    public void send(final Audience audience, final S reference) {
        if (audience instanceof Player || audience instanceof PlayerCommandDispatcher) {
            audience.sendMessage(this.build(reference));
        } else {
            throw new IllegalArgumentException(audience + " isn't a valid audience for sending a configuration menu");
        }
    }
}
