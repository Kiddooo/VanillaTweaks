/*
 * GNU General Public License v3
 *
 * VanillaTweaks, a performant replacement for the VanillaTweaks datapacks.
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
package me.machinemaker.vanillatweaks.modules.hermitcraft.treasuregems;

import cloud.commandframework.arguments.standard.IntegerArgument;
import com.google.inject.Inject;
import me.machinemaker.vanillatweaks.cloud.arguments.PseudoEnumArgument;
import me.machinemaker.vanillatweaks.cloud.dispatchers.CommandDispatcher;
import me.machinemaker.vanillatweaks.modules.ConfiguredModuleCommand;
import org.bukkit.inventory.ItemStack;

class Commands extends ConfiguredModuleCommand {

    private final TreasureGems treasureGems;

    @Inject
    Commands(TreasureGems treasureGems) {
        super("treasure-gems", "treasuregems");
        this.treasureGems = treasureGems;
    }

    @Override
    protected void registerCommands() {
        var builder = playerCmd("treasuregems", "modules.treasure-gems.commands.root", "tgems");

        manager.command(literal(builder, "give")
                .argument(PseudoEnumArgument.single("head", this.treasureGems.heads.keySet()))
                .argument(IntegerArgument.<CommandDispatcher>newBuilder("count").asOptionalWithDefault(1).withMin(1))
                .handler(sync((context, player) -> {
                    ItemStack head = this.treasureGems.heads.get((String) context.get("head")).clone();
                    head.setAmount(context.get("count"));
                    player.getInventory().addItem(head).values().forEach(extraHead -> player.getWorld().dropItem(player.getLocation(), extraHead, item -> {
                        item.setOwner(player.getUniqueId());
                    }));
                }))
        );
    }
}