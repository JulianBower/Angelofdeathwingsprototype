package com.angelofdeath.wings.client;

import com.angelofdeath.wings.AngelOfDeathWings;
import com.angelofdeath.wings.registry.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Mod-bus, client-only registration: key mapping, model layer, player render layers, armor model override. */
@EventBusSubscriber(modid = AngelOfDeathWings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.WING_BOOST);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.ANGEL_WINGS, AngelOfDeathWingsModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        attach(event, PlayerSkin.Model.WIDE);
        attach(event, PlayerSkin.Model.SLIM);
    }

    private static void attach(EntityRenderersEvent.AddLayers event, PlayerSkin.Model skin) {
        EntityRenderer<? extends Player> renderer = event.getSkin(skin);
        if (renderer instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new AngelOfDeathWingsLayer(playerRenderer, event.getEntityModels()));
        }
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new WingsClientExtensions(), ModItems.ANGEL_OF_DEATH_WINGS.get());
    }
}
