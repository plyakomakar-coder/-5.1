package ru.mam.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import ru.mam.DeathRulesMod;
import ru.mam.network.RulesPayload;

public class MamClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RulesPayload.ID, (payload, context) ->
                context.client().execute(() -> RuleOverlay.show(RuleOverlay.parse(payload.text()))));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(DeathRulesMod.MOD_ID, "rules_overlay"),
                (context, tickCounter) -> RuleOverlay.render(context));
    }
}
