package ru.mam.rules;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Правило, которое действует на игрока до следующей смерти или сброса. */
public interface Rule {
    String id();

    String description();

    /** true — негативный эффект (красный), false — позитивный (зелёный). */
    boolean harmful();

    /** Вызывается один раз при выдаче. */
    default void apply(ServerPlayerEntity player) {
    }

    /** Снимает действие правила (используется при /mamreset). */
    default void remove(ServerPlayerEntity player) {
    }

    /** Вызывается каждый тик сервера, пока правило активно. */
    default void tick(ServerPlayerEntity player) {
    }

    /** Вызывается, когда игрок ломает блок. */
    default void onBlockBroken(ServerPlayerEntity player, BlockPos pos) {
    }
}
