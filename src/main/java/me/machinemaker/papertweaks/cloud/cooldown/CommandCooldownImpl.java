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
package me.machinemaker.papertweaks.cloud.cooldown;

import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

record CommandCooldownImpl<C>(CloudKey<Void> key, CommandCooldown.DurationFunction<C> duration, CommandCooldown.@Nullable Notifier<C> notifier) implements CommandCooldown<C> {

    static final class BuilderImpl<C> implements CommandCooldown.Builder<C> {

        private final CommandCooldown.DurationFunction<C> cooldownDuration;
        private CommandCooldown.@Nullable Notifier<C> notifier;
        private @Nullable CloudKey<Void> cooldownKey;

        BuilderImpl(final CommandCooldown.DurationFunction<C> cooldownDuration) {
            this.cooldownDuration = cooldownDuration;
        }

        @Override
        public CommandCooldown.Builder<C> notifier(final CommandCooldown.Notifier<C> notifier) {
            this.notifier = notifier;
            return this;
        }

        @Override
        public CommandCooldown.Builder<C> key(final CloudKey<Void> cooldownKey) {
            this.cooldownKey = cooldownKey;
            return this;
        }

        private CloudKey<Void> getOrCreateKey() {
            @Nullable CloudKey<Void> key = this.cooldownKey;
            if (key == null) {
                key = SimpleCloudKey.of(UUID.randomUUID().toString());
            }
            return key;
        }

        @Override
        public CommandCooldown<C> build() {
            return new CommandCooldownImpl<>(this.getOrCreateKey(), this.cooldownDuration, this.notifier);
        }
    }
}
