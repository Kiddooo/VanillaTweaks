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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.machinemaker.vanillatweaks.LoggerFactory;
import me.machinemaker.vanillatweaks.annotations.ModuleInfo;
import me.machinemaker.vanillatweaks.config.Mixins;
import me.machinemaker.vanillatweaks.modules.ModuleBase;
import me.machinemaker.vanillatweaks.modules.ModuleCommand;
import me.machinemaker.vanillatweaks.modules.ModuleLifecycle;
import me.machinemaker.vanillatweaks.modules.ModuleListener;
import me.machinemaker.vanillatweaks.utils.VTUtils;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleInfo(name = "TreasureGems", configPath = "hermitcraft.treasure-gems", description = "Adds several gems to various loot tables")
public class TreasureGems extends ModuleBase {

    static final Logger LOGGER = LoggerFactory.getModuleLogger(TreasureGems.class);
    static final ObjectMapper JSON_MAPPER = Mixins.registerMixins(new ObjectMapper().registerModule(new ParameterNamesModule()));

    final Map<LootTables, TreasurePool> treasurePoolMap;
    final Map<String, ItemStack> heads;

    @Inject
    TreasureGems(@Named("plugin") ClassLoader loader) {
        Map<LootTables, TreasurePool> tempTreasurePoolMap;
        Map<String, ItemStack> tempHeads = new HashMap<>();
        this.heads = Collections.unmodifiableMap(tempHeads);
        try {
            tempTreasurePoolMap = JSON_MAPPER.readValue(loader.getResourceAsStream("data/treasure_gems/loot_pools.json"), new TypeReference<Map<LootTables, TreasurePool>>() {});
            ObjectNode heads = JSON_MAPPER.readValue(loader.getResourceAsStream("data/treasure_gems/heads.json"), ObjectNode.class);
            var iter = heads.fields();
            while (iter.hasNext()) {
                var entry = iter.next();
                String legacyName = LegacyComponentSerializer.legacySection().serialize(GsonComponentSerializer.gson().deserialize(entry.getValue().get("name").asText()));
                tempHeads.put(entry.getKey(), VTUtils.getSkull(legacyName, UUID.fromString(entry.getValue().get("uuid").asText()), entry.getValue().get("texture").asText(), 1));
            }

        } catch (IOException e) {
            tempTreasurePoolMap = Collections.emptyMap();
            LOGGER.error("Could not load treasure gems from data/treasure_gems.json. This module will not work properly", e);
        }

        this.treasurePoolMap = tempTreasurePoolMap;
    }

    @Override
    protected @NotNull Class<? extends ModuleLifecycle> lifecycle() {
        return ModuleLifecycle.Empty.class;
    }

    @Override
    protected @NotNull Collection<Class<? extends ModuleListener>> listeners() {
        return Set.of(LootListener.class);
    }

    @Override
    protected @NotNull Collection<Class<? extends ModuleCommand>> commands() {
        return Set.of(Commands.class);
    }
}