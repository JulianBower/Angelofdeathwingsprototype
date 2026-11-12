package com.angelofdeath.wings.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Replaces the chestplate armor model with an empty one so the wings add no body silhouette
 * (and no enchantment glint shape): the wings are drawn only by AngelOfDeathWingsLayer.
 */
public class WingsClientExtensions implements IClientItemExtensions {
    private HumanoidModel<LivingEntity> emptyModel;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                  HumanoidModel<?> original) {
        if (emptyModel == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            for (String name : new String[]{"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
                root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
            }
            ModelPart baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
            emptyModel = new HumanoidModel<>(baked);
        }
        return emptyModel;
    }
}
