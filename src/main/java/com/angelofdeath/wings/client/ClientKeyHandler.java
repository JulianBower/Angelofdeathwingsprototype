package com.angelofdeath.wings.client;

import com.angelofdeath.wings.AngelOfDeathWings;
import com.angelofdeath.wings.item.AngelOfDeathWingsItem;
import com.angelofdeath.wings.network.WingBoostPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Game-bus, client-only: sends the boost packet when B is pressed with the wings equipped. */
@EventBusSubscriber(modid = AngelOfDeathWings.MODID, value = Dist.CLIENT)
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        boolean wearingWings =
                minecraft.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AngelOfDeathWingsItem;
        while (ModKeyMappings.WING_BOOST.consumeClick()) {
            if (wearingWings) {
                PacketDistributor.sendToServer(WingBoostPayload.INSTANCE);
            }
        }
    }
}
