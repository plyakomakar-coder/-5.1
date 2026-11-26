package ru.mam.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Открывает защищённые поля MobEntity, чтобы добавлять цели атаки мирным мобам. */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor("goalSelector")
    GoalSelector mamGoalSelector();

    @Accessor("targetSelector")
    GoalSelector mamTargetSelector();
}
