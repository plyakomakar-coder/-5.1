package ru.mam.rules;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class WorldUtil {
    private WorldUtil() {
    }

    /**
     * Ищет свободное место рядом с точкой: воздух внутри и на голове, твёрдый блок под ногами.
     * Возвращает null, если подходящего места нет.
     */
    public static BlockPos findFreeSpot(ServerWorld world, double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos pos = new BlockPos(bx, by + dy, bz);
            if (world.getBlockState(pos).isAir()
                    && world.getBlockState(pos.up()).isAir()
                    && !world.getBlockState(pos.down()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    /** Лаки-блок из установленного мода: сначала Simple Lucky Block, потом Lucky Break. */
    public static Block findLuckyBlock() {
        Block simple = Registries.BLOCK.get(Identifier.of("simple_lucky_block", "lucky_block"));
        if (simple != null && simple != Blocks.AIR) {
            return simple;
        }
        Block luckyBreak = Registries.BLOCK.get(Identifier.of("luckybreak", "lucky_block"));
        if (luckyBreak != null && luckyBreak != Blocks.AIR) {
            return luckyBreak;
        }
        return null;
    }
}
