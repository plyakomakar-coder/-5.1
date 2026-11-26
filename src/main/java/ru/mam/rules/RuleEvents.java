package ru.mam.rules;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Разовые и периодические события. Каждое событие — это Rule. */
public final class RuleEvents {
    private RuleEvents() {
    }

    static ServerWorld worldOf(ServerPlayerEntity player) {
        return (ServerWorld) player.getEntityWorld();
    }

    static void strike(ServerPlayerEntity player) {
        ServerWorld world = worldOf(player);
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.EVENT);
        if (bolt != null) {
            bolt.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
            world.spawnEntity(bolt);
        }
    }

    static Entity spawnAt(ServerWorld world, EntityType<? extends Entity> type, double x, double y, double z) {
        Entity entity = type.create(world, SpawnReason.EVENT);
        if (entity != null) {
            entity.refreshPositionAfterTeleport(x, y, z);
            world.spawnEntity(entity);
        }
        return entity;
    }

    /** Варден злится на игрока, иначе стоит на месте и не преследует. */
    static void angerWarden(Entity entity, ServerPlayerEntity player) {
        if (entity instanceof WardenEntity warden && warden.isAlive()) {
            warden.increaseAngerAt(player, 100, false);
        }
    }

    private static void levelUpSound(ServerPlayerEntity player) {
        worldOf(player).playSound((PlayerEntity) null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /** Стартовый набор. */
    public static final class Kit implements Rule {
        @Override
        public String id() {
            return "kit";
        }

        @Override
        public String description() {
            return "Стартовый набор";
        }

        @Override
        public boolean harmful() {
            return false;
        }

        @Override
        public void apply(ServerPlayerEntity player) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            player.getInventory().insertStack(new ItemStack(Items.IRON_PICKAXE));
            player.getInventory().insertStack(new ItemStack(Items.IRON_SWORD));
            player.getInventory().insertStack(new ItemStack(Items.GOLDEN_APPLE, 1 + r.nextInt(3)));
            player.getInventory().insertStack(new ItemStack(Items.BREAD, 8));
            levelUpSound(player);
        }
    }

    /** Полный сет брони из случайного материала. */
    public static final class ArmorKit implements Rule {
        @Override
        public String id() {
            return "armor_kit";
        }

        @Override
        public String description() {
            return "Полный сет брони";
        }

        @Override
        public boolean harmful() {
            return false;
        }

        @Override
        public void apply(ServerPlayerEntity player) {
            Item[][] sets = {
                    {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
                    {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
                    {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}
            };
            Item[] set = sets[ThreadLocalRandom.current().nextInt(sets.length)];
            player.equipStack(EquipmentSlot.HEAD, new ItemStack(set[0]));
            player.equipStack(EquipmentSlot.CHEST, new ItemStack(set[1]));
            player.equipStack(EquipmentSlot.LEGS, new ItemStack(set[2]));
            player.equipStack(EquipmentSlot.FEET, new ItemStack(set[3]));
            levelUpSound(player);
        }
    }

    /** Удар молнии сразу после возрождения. */
    public static final class LightningOnRespawn implements Rule {
        @Override
        public String id() {
            return "lightning_respawn";
        }

        @Override
        public String description() {
            return "Удар молнии при возрождении";
        }

        @Override
        public boolean harmful() {
            return true;
        }

        @Override
        public void apply(ServerPlayerEntity player) {
            strike(player);
        }
    }

    /** Молния каждые 30 секунд, если игрок двигается. */
    public static final class PeriodicLightning implements Rule {
        private static final int INTERVAL = 20 * 30;
        private int ticks;
        private double lastX = Double.NaN;
        private double lastZ = Double.NaN;

        @Override
        public String id() {
            return "periodic_lightning";
        }

        @Override
        public String description() {
            return "Молния каждые 30 секунд при движении";
        }

        @Override
        public boolean harmful() {
            return true;
        }

        @Override
        public void tick(ServerPlayerEntity player) {
            if (++ticks % INTERVAL != 0) {
                return;
            }
            boolean moved = !Double.isNaN(lastX)
                    && Math.hypot(player.getX() - lastX, player.getZ() - lastZ) > 1.0;
            lastX = player.getX();
            lastZ = player.getZ();
            if (moved) {
                strike(player);
            }
        }
    }

    /** Динамит спавнится рядом с игроком в случайные моменты, без ограничения по времени. */
    public static final class TntBarrage implements Rule {
        private static final int FUSE = 60;
        private static final int CAP = 9;

        private final List<TntEntity> bombs = new ArrayList<>();
        private int ticks;
        private int nextSpawnAt = -1;

        @Override
        public String id() {
            return "tnt_barrage";
        }

        @Override
        public String description() {
            return "Динамит рядом с тобой";
        }

        @Override
        public boolean harmful() {
            return true;
        }

        private static int randomDelay(ThreadLocalRandom r) {
            return 20 * 2 + r.nextInt(20 * 13);
        }

        @Override
        public void tick(ServerPlayerEntity player) {
            ticks++;
            bombs.removeIf(tnt -> !tnt.isAlive());
            ThreadLocalRandom r = ThreadLocalRandom.current();

            if (nextSpawnAt < 0) {
                nextSpawnAt = ticks + randomDelay(r);
            }
            if (ticks < nextSpawnAt) {
                return;
            }
            nextSpawnAt = ticks + randomDelay(r);

            ServerWorld world = worldOf(player);
            int count = Math.min(CAP - bombs.size(), 1 + r.nextInt(3));
            for (int i = 0; i < count; i++) {
                double angle = r.nextDouble() * Math.PI * 2;
                double dist = 2 + r.nextDouble() * 2;
                BlockPos spot = WorldUtil.findFreeSpot(world,
                        player.getX() + Math.cos(angle) * dist,
                        player.getY(),
                        player.getZ() + Math.sin(angle) * dist);
                if (spot == null) {
                    continue;
                }
                TntEntity tnt = EntityType.TNT.create(world, SpawnReason.EVENT);
                if (tnt != null) {
                    tnt.setFuse(FUSE);
                    tnt.refreshPositionAfterTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                    world.spawnEntity(tnt);
                    bombs.add(tnt);
                }
            }
        }
    }

    /** Призыв враждебных мобов рядом с игроком, только на свободном месте. */
    public static final class Summon implements Rule {
        private final String id;
        private final String name;
        private final EntityType<? extends Entity> type;
        private final int min;
        private final int max;
        private final List<Entity> summoned = new ArrayList<>();
        private int ticks;

        public Summon(String id, String name, EntityType<? extends Entity> type, int min, int max) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.min = min;
            this.max = max;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String description() {
            return name;
        }

        @Override
        public boolean harmful() {
            return true;
        }

        @Override
        public void apply(ServerPlayerEntity player) {
            ServerWorld world = worldOf(player);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            int count = min + r.nextInt(max - min + 1);
            for (int i = 0; i < count; i++) {
                double angle = r.nextDouble() * Math.PI * 2;
                double dist = 4 + r.nextDouble() * 3;
                BlockPos spot = WorldUtil.findFreeSpot(world,
                        player.getX() + Math.cos(angle) * dist,
                        player.getY(),
                        player.getZ() + Math.sin(angle) * dist);
                if (spot == null) {
                    continue;
                }
                Entity entity = spawnAt(world, type, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                if (entity != null) {
                    summoned.add(entity);
                    angerWarden(entity, player);
                }
            }
        }

        @Override
        public void tick(ServerPlayerEntity player) {
            ticks++;
            if (ticks % 40 == 0) {
                for (Entity entity : summoned) {
                    angerWarden(entity, player);
                }
            }
        }
    }

    /** Лаки-блок из установленного мода ставится рядом с игроком. */
    public static final class LuckyBlock implements Rule {
        @Override
        public String id() {
            return "lucky_block";
        }

        @Override
        public String description() {
            return "Лаки-блок рядом";
        }

        @Override
        public boolean harmful() {
            return false;
        }

        @Override
        public void apply(ServerPlayerEntity player) {
            Block block = WorldUtil.findLuckyBlock();
            if (block == null) {
                player.sendMessage(Text.literal("Лаки-блок не найден: установите Simple Lucky Block или Lucky Break"), false);
                return;
            }
            ServerWorld world = worldOf(player);
            BlockPos spot = WorldUtil.findFreeSpot(world, player.getX() + 3, player.getY(), player.getZ());
            if (spot != null) {
                world.setBlockState(spot, block.getDefaultState());
            }
        }
    }

    /** При ломании блока он взрывается и ломает соседние блоки, игрок получает урон. */
    public static final class BlastOnBreak implements Rule {
        @Override
        public String id() {
            return "blast_on_break";
        }

        @Override
        public String description() {
            return "Блок взрывается при ломании";
        }

        @Override
        public boolean harmful() {
            return true;
        }

        @Override
        public void onBlockBroken(ServerPlayerEntity player, BlockPos pos) {
            worldOf(player).createExplosion((Entity) null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3.0f, World.ExplosionSourceType.TNT);
        }
    }
}
