package ru.mam.rules;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Набор правил, который выдаётся одним пунктом. */
public record CompositeRule(String id, String description, boolean harmful, List<Rule> parts) implements Rule {

    @Override
    public void apply(ServerPlayerEntity player) {
        for (Rule rule : parts) {
            rule.apply(player);
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        for (Rule rule : parts) {
            rule.remove(player);
        }
    }

    @Override
    public void tick(ServerPlayerEntity player) {
        for (Rule rule : parts) {
            rule.tick(player);
        }
    }

    @Override
    public void onBlockBroken(ServerPlayerEntity player, BlockPos pos) {
        for (Rule rule : parts) {
            rule.onBlockBroken(player, pos);
        }
    }
}
