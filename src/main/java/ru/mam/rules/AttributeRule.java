package ru.mam.rules;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.mam.DeathRulesMod;

/** Изменение атрибута игрока (прыжок, гравитация, скорость, безопасное падение). */
public record AttributeRule(String id, String description, boolean harmful,
                            RegistryEntry<EntityAttribute> attribute,
                            double value, EntityAttributeModifier.Operation operation) implements Rule {

    @Override
    public void apply(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.addTemporaryModifier(new EntityAttributeModifier(
                Identifier.of(DeathRulesMod.MOD_ID, id), value, operation));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(Identifier.of(DeathRulesMod.MOD_ID, id));
        }
    }
}
