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
package me.machinemaker.papertweaks.modules.survival.pillagertools;

import com.google.inject.Inject;
import me.machinemaker.papertweaks.modules.ConfiguredModuleCommand;
import me.machinemaker.papertweaks.modules.ModuleCommand;

import java.util.Locale;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

@ModuleCommand.Info(value = "pillagertools", aliases = "ptools", i18n = "pillager-tools", perm = "pillagertools")
class Commands extends ConfiguredModuleCommand {

    private final Config config;

    @Inject
    Commands(Config config) {
        this.config = config;
    }

    @Override
    protected void registerCommands() {
        var builder = this.builder();

        manager.command(literal(builder, "status")
                .handler(context -> {
                    var txtBuilder = text()
                            .append(translatable("modules.pillager-tools.commands.status.success.header", YELLOW, BOLD));
                    for (PillagerTools.ToggleOption option : PillagerTools.ToggleOption.values()) {
                        txtBuilder.append(newline()).append(translatable("modules.pillager-tools.commands.status.success.setting",
                                YELLOW,
                                translatable("modules.pillager-tools.settings." + option.name().toLowerCase(Locale.ENGLISH)),
                                translatable("commands.config.default-value.bool." + this.config.getSettingValue(option), GREEN)
                                ));
                    }
                    context.getSender().sendMessage(txtBuilder);
                })
        );

        this.config.createCommands(this, builder);
    }
}
