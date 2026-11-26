package ru.mam.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.mam.DeathRulesMod;

/**
 * Пакет сервер → клиент. Текст: по одной строке на правило,
 * первый символ строки "1" — негативное правило, "0" — позитивное.
 */
public record RulesPayload(String text) implements CustomPayload {
    public static final CustomPayload.Id<RulesPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DeathRulesMod.MOD_ID, "rules"));

    public static final PacketCodec<RegistryByteBuf, RulesPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, RulesPayload::text, RulesPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
