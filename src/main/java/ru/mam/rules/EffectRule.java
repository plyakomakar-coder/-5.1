package ru.mam.rules;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

/** Статус-эффект, который держится до смерти или сброса. */
public record EffectRule(String id, String description, boolean harmful,
                         RegistryEntry<StatusEffect> effect, int amplifier) implements Rule {

    private static final int LONG_DURATION = 20 * 60 * 60 * 24;

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(effect, LONG_DURATION, amplifier, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(effect);
    }
}
