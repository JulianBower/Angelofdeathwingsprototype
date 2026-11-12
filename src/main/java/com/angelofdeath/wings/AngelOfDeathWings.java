package com.angelofdeath.wings;

import com.angelofdeath.wings.network.WingBoostPayload;
import com.angelofdeath.wings.registry.ModArmorMaterials;
import com.angelofdeath.wings.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(AngelOfDeathWings.MODID)
public class AngelOfDeathWings {
    public static final String MODID = "angelofdeathwings";
    private static final String NETWORK_VERSION = "1";

    public AngelOfDeathWings(IEventBus modEventBus) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addCreativeTabItems);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(WingBoostPayload.TYPE, WingBoostPayload.STREAM_CODEC, WingBoostPayload::handle);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.ANGEL_OF_DEATH_WINGS);
        }
    }
}
