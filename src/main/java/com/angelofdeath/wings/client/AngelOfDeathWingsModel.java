package com.angelofdeath.wings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Multi-tiered angel wings, three jointed segments per wing:
 * covert (arched shoulder base) -> secondary (layered, staggered mid feathers) -> primary (fan of tapered blades).
 * Texture is 128x64: the left wing uses u 0..63, the right wing the same layout shifted by +64 u
 * (see tools/generate_textures.py, which paints these exact rectangles).
 */
public class AngelOfDeathWingsModel<T extends LivingEntity> extends EntityModel<T> {
    private static final float ROOT_X = 1.2F;
    private static final float ROOT_Y = 1.5F;
    private static final float ROOT_Z = 2.2F;
    private static final int RIGHT_WING_U_SHIFT = 64;
    private static final int BLADE_COUNT = 5;

    /** Per-blade fan angle (radians, before spread scaling). */
    private static final float[] BLADE_FAN = {0.0F, 0.2F, 0.4F, 0.6F, 0.8F};

    /** Per blade, per stacked box (wide, medium, tip): {width, height}. */
    private static final int[][][] BLADE_SIZE = {
            {{3, 11}, {2, 6}, {1, 4}},
            {{3, 13}, {2, 7}, {1, 4}},
            {{3, 12}, {2, 7}, {1, 4}},
            {{3, 10}, {2, 6}, {1, 3}},
            {{3, 8}, {2, 5}, {1, 3}}
    };

    /** Per blade, per stacked box: {u, v} on the left-wing region of the texture. */
    private static final int[][][] BLADE_UV = {
            {{48, 0}, {58, 15}, {24, 25}},
            {{8, 0}, {46, 15}, {28, 25}},
            {{24, 0}, {52, 15}, {32, 25}},
            {{56, 0}, {0, 25}, {36, 25}},
            {{38, 15}, {6, 25}, {40, 25}}
    };

    private record Wing(ModelPart root, ModelPart covert, ModelPart secondary, ModelPart primary, ModelPart[] blades) {
    }

    private final ModelPart modelRoot;
    private final Wing leftWing;
    private final Wing rightWing;

    public AngelOfDeathWingsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.modelRoot = root;
        this.leftWing = bake(root, "left_");
        this.rightWing = bake(root, "right_");
    }

    private static Wing bake(ModelPart root, String prefix) {
        ModelPart wing = root.getChild(prefix + "wing");
        ModelPart covert = wing.getChild(prefix + "covert");
        ModelPart secondary = covert.getChild(prefix + "secondary");
        ModelPart primary = secondary.getChild(prefix + "primary");
        ModelPart[] blades = new ModelPart[BLADE_COUNT];
        for (int i = 0; i < BLADE_COUNT; i++) {
            blades[i] = primary.getChild(prefix + "blade_" + i);
        }
        return new Wing(wing, covert, secondary, primary, blades);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addWing(root, true);
        addWing(root, false);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addWing(PartDefinition root, boolean left) {
        final float side = left ? 1.0F : -1.0F;
        final int shift = left ? 0 : RIGHT_WING_U_SHIFT;
        final String p = left ? "left_" : "right_";

        PartDefinition wing = root.addOrReplaceChild(p + "wing", CubeListBuilder.create(),
                PartPose.offset(side * ROOT_X, ROOT_Y, ROOT_Z));

        // 1. Inner shoulder covert: arched base reaching up and outward from the shoulder blades.
        CubeListBuilder covertCubes = CubeListBuilder.create();
        box(covertCubes, left, shift, 8, 15, 0.0F, -7.0F, 0.0F, 7, 8);   // cov_a
        box(covertCubes, left, shift, 12, 25, 1.0F, -9.0F, 0.5F, 5, 4);  // cov_b (raised arch layer)
        PartDefinition covert = wing.addOrReplaceChild(p + "covert", covertCubes, PartPose.ZERO);

        // 2. Middle secondary feathers: two layered rows hanging down with staggered bottom edges.
        CubeListBuilder secondaryCubes = CubeListBuilder.create();
        box(secondaryCubes, left, shift, 16, 0, 0.0F, 0.0F, 0.0F, 3, 12);   // sec_a
        box(secondaryCubes, left, shift, 0, 0, 3.0F, 0.0F, 0.0F, 3, 14);    // sec_b
        box(secondaryCubes, left, shift, 32, 0, 6.0F, 0.0F, 0.0F, 3, 11);   // sec_c
        box(secondaryCubes, left, shift, 24, 15, 9.0F, 0.0F, 0.0F, 2, 8);   // sec_d
        box(secondaryCubes, left, shift, 0, 15, 1.0F, 2.0F, 0.9F, 3, 9);    // sec2_a
        box(secondaryCubes, left, shift, 40, 0, 4.0F, 2.0F, 0.9F, 3, 11);   // sec2_b
        box(secondaryCubes, left, shift, 30, 15, 7.0F, 2.0F, 0.9F, 3, 8);   // sec2_c
        PartDefinition secondary = covert.addOrReplaceChild(p + "secondary", secondaryCubes,
                PartPose.offset(side * 7.0F, -3.0F, 0.0F));

        // 3. Sweeping outer primary blades: a fan of long tapered feathers (wide -> medium -> tip).
        PartDefinition primary = secondary.addOrReplaceChild(p + "primary", CubeListBuilder.create(),
                PartPose.offset(side * 8.0F, 8.0F, 0.0F));
        for (int i = 0; i < BLADE_COUNT; i++) {
            CubeListBuilder blade = CubeListBuilder.create();
            float z = 0.4F * i;
            float y = 0.0F;
            for (int s = 0; s < 3; s++) {
                int w = BLADE_SIZE[i][s][0];
                int h = BLADE_SIZE[i][s][1];
                box(blade, left, shift, BLADE_UV[i][s][0], BLADE_UV[i][s][1], -w / 2.0F, y, z, w, h);
                y += h;
            }
            primary.addOrReplaceChild(p + "blade_" + i, blade, PartPose.ZERO);
        }
    }

    /** Adds a 1-pixel-thick feather box; the right wing is mirrored in x and reads its own texture region. */
    private static void box(CubeListBuilder builder, boolean left, int uShift, int u, int v,
                            float x, float y, float z, int w, int h) {
        float originX = left ? x : -(x + w);
        builder.texOffs(u + uShift, v).addBox(originX, y, z, w, h, 1.0F);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - entity.tickCount;
        boolean gliding = entity.isFallFlying();
        boolean crouching = entity.isCrouching() && !gliding;

        float spread;
        float flutter;
        if (gliding) {
            // Spread out dramatically while gliding, even wider and faster-fluttering while boosting.
            float t = Mth.clamp((entity.getFallFlyingTicks() + partialTick) / 8.0F, 0.0F, 1.0F);
            float eased = t * t * (3.0F - 2.0F * t);
            float speed = (float) entity.getDeltaMovement().length();
            float boost = Mth.clamp((speed - 1.8F) / 1.6F, 0.0F, 1.0F);
            spread = Mth.lerp(eased, 0.55F, 1.0F) + 0.22F * boost;
            flutter = Mth.sin(ageInTicks * 0.35F) * (0.025F + 0.03F * boost);
        } else if (crouching) {
            spread = 0.04F; // folded tight while sneaking
            flutter = 0.0F;
        } else {
            // Gently idling while standing, folding inward as the player walks.
            float walk = Mth.clamp(limbSwingAmount * 2.2F, 0.0F, 1.0F);
            float breathe = Mth.sin(ageInTicks * 0.07F);
            spread = Mth.lerp(walk, 0.4F + breathe * 0.05F, 0.1F);
            flutter = breathe * 0.02F * (1.0F - walk);
        }

        pose(leftWing, 1.0F, spread, flutter, crouching);
        pose(rightWing, -1.0F, spread, flutter, crouching);
    }

    private static void pose(Wing wing, float side, float spread, float flutter, boolean crouching) {
        wing.root.setPos(side * ROOT_X, ROOT_Y + (crouching ? 2.0F : 0.0F), ROOT_Z + (crouching ? 0.5F : 0.0F));
        wing.root.xRot = crouching ? 0.5F : 0.0F;
        wing.root.yRot = -side * (0.1F + 0.4F * spread);
        wing.root.zRot = 0.0F;

        wing.covert.zRot = -side * (0.18F + 0.5F * spread + flutter);
        wing.secondary.zRot = -side * (0.04F + 0.86F * spread + flutter * 1.5F);
        wing.primary.zRot = -side * (0.05F + 0.15F * spread + flutter * 2.0F);
        wing.primary.yRot = -side * 0.12F * spread;

        float fan = 0.2F + 0.8F * spread;
        for (int i = 0; i < BLADE_COUNT; i++) {
            wing.blades[i].zRot = -side * BLADE_FAN[i] * fan;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        modelRoot.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
