package ru.mam.rules;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Все возможные правила. Weight — относительная частота выпадения. */
public final class RuleRegistry {
    public record Template(int weight, Supplier<Rule> factory) {
    }

    public static final List<Template> ALL = new ArrayList<>();

    static {
        // ---- Позитивные эффекты ----
        effect("speed", "Скорость", false, StatusEffects.SPEED, 1, 3, 3);
        effect("speed_extreme", "Сверхскорость", false, StatusEffects.SPEED, 10, 254, 1);
        effect("haste", "Ускоренная добыча", false, StatusEffects.HASTE, 0, 2, 3);
        effect("strength", "Сила", false, StatusEffects.STRENGTH, 0, 2, 3);
        effect("jump_boost", "Прыжок вверх", false, StatusEffects.JUMP_BOOST, 1, 3, 3);
        effect("regeneration", "Регенерация", false, StatusEffects.REGENERATION, 0, 2, 3);
        effect("resistance", "Сопротивление", false, StatusEffects.RESISTANCE, 0, 2, 3);
        effect("fire_resistance", "Огнестойкость", false, StatusEffects.FIRE_RESISTANCE, 0, 0, 3);
        effect("water_breathing", "Дыхание под водой", false, StatusEffects.WATER_BREATHING, 0, 0, 3);
        effect("night_vision", "Ночное зрение", false, StatusEffects.NIGHT_VISION, 0, 0, 3);
        effect("invisibility", "Невидимость", false, StatusEffects.INVISIBILITY, 0, 0, 3);
        effect("absorption", "Поглощение урона", false, StatusEffects.ABSORPTION, 0, 3, 3);
        effect("saturation", "Насыщение", false, StatusEffects.SATURATION, 0, 1, 3);
        effect("luck", "Удача", false, StatusEffects.LUCK, 0, 1, 3);
        effect("dolphins_grace", "Милость дельфина", false, StatusEffects.DOLPHINS_GRACE, 0, 0, 3);
        effect("conduit_power", "Сила проводника", false, StatusEffects.CONDUIT_POWER, 0, 0, 3);
        effect("health_boost", "Дополнительное здоровье", false, StatusEffects.HEALTH_BOOST, 0, 2, 3);
        effect("slow_falling", "Плавное падение", false, StatusEffects.SLOW_FALLING, 0, 0, 3);

        // ---- Негативные эффекты ----
        effect("slowness", "Медлительность", true, StatusEffects.SLOWNESS, 0, 3, 3);
        effect("slowness_extreme", "Полная медлительность", true, StatusEffects.SLOWNESS, 10, 254, 1);
        effect("mining_fatigue", "Усталость при добыче", true, StatusEffects.MINING_FATIGUE, 0, 2, 3);
        effect("weakness", "Слабость", true, StatusEffects.WEAKNESS, 0, 2, 3);
        effect("blindness", "Слепота", true, StatusEffects.BLINDNESS, 0, 0, 3);
        effect("nausea", "Тошнота", true, StatusEffects.NAUSEA, 0, 0, 3);
        effect("darkness", "Тьма", true, StatusEffects.DARKNESS, 0, 0, 3);
        effect("glowing", "Свечение (виден сквозь стены)", true, StatusEffects.GLOWING, 0, 0, 3);
        effect("hunger", "Голод", true, StatusEffects.HUNGER, 0, 3, 3);
        effect("poison", "Отравление", true, StatusEffects.POISON, 0, 1, 3);
        effect("levitation", "Левитация (поднимает вверх)", true, StatusEffects.LEVITATION, 0, 0, 3);

        // ---- Атрибуты и комбинированные правила ----
        ALL.add(new Template(2, () -> new CompositeRule("slow_fall", "Медленное падение без урона", false, List.of(
                new AttributeRule("slow_fall_gravity", "", false,
                        EntityAttributes.GRAVITY, -0.9, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                new AttributeRule("slow_fall_safe", "", false,
                        EntityAttributes.SAFE_FALL_DISTANCE, 1024.0, EntityAttributeModifier.Operation.ADD_VALUE)
        ))));

        ALL.add(new Template(2, () -> new CompositeRule("heavy_body", "Тяжёлое тело (гравитация x2)", true, List.of(
                new AttributeRule("heavy_gravity", "", true,
                        EntityAttributes.GRAVITY, 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        ))));

        // Прыжок на 10–30 блоков. Значение атрибута подбирается по формуле высоты прыжка.
        ALL.add(new Template(2, () -> {
            int height = 10 + ThreadLocalRandom.current().nextInt(21);
            double value = Math.sqrt(2 * 0.08 * height) - 0.42;
            return new AttributeRule("high_jump", "Прыжок на " + height + " блоков", false,
                    EntityAttributes.JUMP_STRENGTH, value, EntityAttributeModifier.Operation.ADD_VALUE);
        }));

        ALL.add(new Template(2, () -> new AttributeRule("low_jump", "Низкий прыжок", true,
                EntityAttributes.JUMP_STRENGTH, -0.3, EntityAttributeModifier.Operation.ADD_VALUE)));

        ALL.add(new Template(1, () -> new AttributeRule("no_move", "Нельзя ходить", true,
                EntityAttributes.MOVEMENT_SPEED, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)));

        // ---- События ----
        ALL.add(new Template(2, RuleEvents.Kit::new));
        ALL.add(new Template(1, RuleEvents.ArmorKit::new));
        ALL.add(new Template(2, RuleEvents.LightningOnRespawn::new));
        ALL.add(new Template(2, RuleEvents.PeriodicLightning::new));
        ALL.add(new Template(2, RuleEvents.TntBarrage::new));
        ALL.add(new Template(1, RuleEvents.BlastOnBreak::new));
        ALL.add(new Template(1, () -> new RuleEvents.Summon("summon_wither", "Визер рядом с тобой",
                EntityType.WITHER, 1, 1)));
        ALL.add(new Template(1, () -> new RuleEvents.Summon("summon_warden", "Варден рядом с тобой",
                EntityType.WARDEN, 1, 1)));
        ALL.add(new Template(2, () -> new RuleEvents.Summon("zombie_pack", "Стая зомби",
                EntityType.ZOMBIE, 3, 5)));
        ALL.add(new Template(1, ChestTraps.ChestRule::new));
        ALL.add(new Template(2, RuleEvents.LuckyBlock::new));
    }

    private static void effect(String id, String name, boolean harmful,
                               RegistryEntry<StatusEffect> entry, int min, int max, int weight) {
        ALL.add(new Template(weight, () -> new EffectRule(id, name, harmful, entry,
                ThreadLocalRandom.current().nextInt(min, max + 1))));
    }

    private RuleRegistry() {
    }
}
