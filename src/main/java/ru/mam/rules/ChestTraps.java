package ru.mam.rules;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сундук-ловушка. При открытии через 4 секунды сундук взрывается вместе с содержимым,
 * а игрок, который открыл сундук, погибает.
 */
public final class ChestTraps {
    private static final int FUSE = 20 * 4;
    private static final Map<BlockPos, Trap> TRAPS = new HashMap<>();

    private ChestTraps() {
    }

    private static final class Trap {
        final ServerWorld world;
        UUID opener;
        int fuse = -1;

        Trap(ServerWorld world) {
            this.world = world;
        }
    }

    /** Регистрируется как UseBlockCallback. Запускает таймер, когда кто-то открывает сундук-ловушку. */
    public static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        Trap trap = TRAPS.get(hit.getBlockPos());
        if (trap != null && trap.fuse < 0) {
            trap.fuse = FUSE;
            trap.opener = serverPlayer.getUuid();
        }
        return ActionResult.PASS;
    }

    /** Вызывается каждый тик сервера. */
    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<BlockPos, Trap>> iterator = TRAPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Trap> entry = iterator.next();
            Trap trap = entry.getValue();
            if (trap.fuse < 0) {
                continue;
            }
            trap.fuse--;
            if (trap.fuse > 0) {
                continue;
            }
            BlockPos pos = entry.getKey();
            iterator.remove();
            trap.world.setBlockState(pos, Blocks.AIR.getDefaultState());
            trap.world.createExplosion((Entity) null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    4.0f, World.ExplosionSourceType.TNT);
            ServerPlayerEntity victim = server.getPlayerManager().getPlayer(trap.opener);
            if (victim != null) {
                victim.damage(trap.world, trap.world.getDamageSources().generic(), 1000.0f);
            }
        }
    }

    /** Правило: ставит сундук-ловушку рядом с игроком. */
    public static final class ChestRule implements Rule {
        @Override
        public String id() {
            return "chest_trap";
        }

        @Override
        public String description() {
            return "Сундук-ловушка рядом (открыл - через 4 секунды смерть)";
        }

        @Override
        public boolean harmful() {
            return true;
        }

        @Override
        public void apply(ServerPlayerEntity player) {
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos pos = WorldUtil.findFreeSpot(world, player.getX() + 3, player.getY(), player.getZ());
            if (pos == null) {
                return;
            }
            world.setBlockState(pos, Blocks.CHEST.getDefaultState());
            if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                List<ItemStack> loot = buildLoot(world);
                for (int i = 0; i < loot.size() && i < chest.size(); i++) {
                    chest.setStack(i, loot.get(i));
                }
            }
            TRAPS.put(pos, new Trap(world));
        }
    }

    private static List<ItemStack> buildLoot(ServerWorld world) {
        Registry<Enchantment> enchants = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        List<ItemStack> loot = new ArrayList<>();

        loot.add(maxAll(new ItemStack(Items.NETHERITE_SWORD), enchants));
        loot.add(maxAll(new ItemStack(Items.DIAMOND_SWORD), enchants));

        loot.add(maxAll(new ItemStack(Items.NETHERITE_HELMET), enchants));
        loot.add(maxAll(new ItemStack(Items.NETHERITE_CHESTPLATE), enchants));
        loot.add(maxAll(new ItemStack(Items.NETHERITE_LEGGINGS), enchants));
        loot.add(maxAll(new ItemStack(Items.NETHERITE_BOOTS), enchants));
        loot.add(maxAll(new ItemStack(Items.DIAMOND_HELMET), enchants));
        loot.add(maxAll(new ItemStack(Items.DIAMOND_CHESTPLATE), enchants));
        loot.add(maxAll(new ItemStack(Items.DIAMOND_LEGGINGS), enchants));
        loot.add(maxAll(new ItemStack(Items.DIAMOND_BOOTS), enchants));

        loot.add(mining(new ItemStack(Items.NETHERITE_PICKAXE), enchants));
        loot.add(mining(new ItemStack(Items.NETHERITE_PICKAXE), enchants));
        loot.add(mining(new ItemStack(Items.NETHERITE_AXE), enchants));
        loot.add(mining(new ItemStack(Items.NETHERITE_AXE), enchants));

        loot.add(new ItemStack(Items.TOTEM_OF_UNDYING, 2));
        loot.add(new ItemStack(Items.ELYTRA));
        loot.add(new ItemStack(Items.FIREWORK_ROCKET, 64));
        loot.add(new ItemStack(Items.FIREWORK_ROCKET, 64));
        loot.add(new ItemStack(Items.FIREWORK_ROCKET, 64));
        loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64));
        loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64));
        return loot;
    }

    /** Все подходящие чары на уровне 255. */
    private static ItemStack maxAll(ItemStack stack, Registry<Enchantment> enchants) {
        enchants.streamEntries().forEach(entry -> {
            if (entry.value().isAcceptableItem(stack)) {
                stack.addEnchantment(entry, 255);
            }
        });
        return stack;
    }

    /** Кирка и топор: эффективность 255, прочность 3, удача 255. */
    private static ItemStack mining(ItemStack stack, Registry<Enchantment> enchants) {
        put(stack, enchants, Enchantments.EFFICIENCY, 255);
        put(stack, enchants, Enchantments.UNBREAKING, 3);
        put(stack, enchants, Enchantments.FORTUNE, 255);
        return stack;
    }

    private static void put(ItemStack stack, Registry<Enchantment> enchants,
                            RegistryKey<Enchantment> key, int level) {
        enchants.getEntry(key.getValue()).ifPresent(entry -> stack.addEnchantment(entry, level));
    }
}
