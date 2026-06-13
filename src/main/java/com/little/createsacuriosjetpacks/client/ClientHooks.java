package com.little.createsacuriosjetpacks.client;

import com.little.createsacuriosjetpacks.CreateSaCuriosJetpacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateSaCuriosJetpacks.MOD_ID, value = Dist.CLIENT)
public final class ClientHooks {
    private static boolean lastFlyingState = false;

    private ClientHooks() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        // If the jetpack is in the normal chest armor slot, leave CSA completely alone.
        // CSA already handles its own keybind and flight state for that case.
        if (CreateSaCuriosJetpacks.hasVanillaChestJetpack(minecraft.player)) {
            lastFlyingState = false;
            return;
        }

        boolean hasCuriosJetpack = CreateSaCuriosJetpacks.hasCuriosJetpack(minecraft.player);
        boolean flyingKeyDown = hasCuriosJetpack && isCsaFlyingKeyDown();

        CreateSaCuriosJetpacks.setClientFlyingState(minecraft.player, flyingKeyDown);

        if (flyingKeyDown != lastFlyingState) {
            PacketDistributor.sendToServer(new CreateSaCuriosJetpacks.FlyingStatePayload(flyingKeyDown));
            lastFlyingState = flyingKeyDown;
        }
    }

    private static boolean isCsaFlyingKeyDown() {
        try {
            Class<?> keyMappingsClass = Class.forName("net.mcreator.createstuffadditions.init.CreateSaModKeyMappings");
            Object keyMapping = keyMappingsClass.getField("FLYING").get(null);
            if (keyMapping instanceof KeyMapping mapping) {
                return mapping.isDown();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return false;
    }
}
