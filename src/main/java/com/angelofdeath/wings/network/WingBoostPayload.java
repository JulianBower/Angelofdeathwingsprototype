package com.angelofdeath.wings.network;

import com.angelofdeath.wings.AngelOfDeathWings;
import com.angelofdeath.wings.boost.WingBoostHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: "the B key was pressed". Carries no data. */
public record WingBoostPayload() implements CustomPacketPayload {
    public static final WingBoostPayload INSTANCE = new WingBoostPayload();

    public static final CustomPacketPayload.Type<WingBoostPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathWings.MODID, "wing_boost"));

    public static final StreamCodec<ByteBuf, WingBoostPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WingBoostPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                WingBoostHandler.tryActivate(player);
            }
        });
    }
}
