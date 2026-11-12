package com.angelofdeath.wings.client;

import com.angelofdeath.wings.AngelOfDeathWings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation ANGEL_WINGS = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathWings.MODID, "angel_wings"), "main");

    private ModModelLayers() {
    }
}
