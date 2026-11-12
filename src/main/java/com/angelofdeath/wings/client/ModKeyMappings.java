package com.angelofdeath.wings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.angelofdeathwings";

    public static final KeyMapping WING_BOOST = new KeyMapping(
            "key.angelofdeathwings.wing_boost", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    private ModKeyMappings() {
    }
}
