package ru.mam.rules;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.mam.mixin.MobEntityAccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Мирный моб, которого ударил игрок, начинает атаковать его.
 * Злится только тот моб, которого ударили.
 */
public final class PeacefulMobs {
    private static final Set<UUID> ANGRY = new HashSet<>();

    private PeacefulMobs() {
    }

    public static void onDamage(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof PassiveEntity mob)) {
            return;
        }
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!ANGRY.add(mob.getUuid())) {
            return;
        }
        MobEntityAccessor access = (MobEntityAccessor) mob;
        GoalSelector goals = access.mamGoalSelector();
        GoalSelector targets = access.mamTargetSelector();
        goals.add(1, new MeleeAttackGoal(mob, 1.0, true));
        targets.add(1, new ActiveTargetGoal<>(mob, PlayerEntity.class, true));
        mob.setTarget(player);
    }
}
