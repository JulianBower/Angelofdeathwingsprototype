package com.angelofdeath.wings.registry;

import com.angelofdeath.wings.AngelOfDeathWings;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, AngelOfDeathWings.MODID);

    /** Chest slot grants +3 armor points. The textures for this layer are fully transparent: the wings are drawn by AngelOfDeathWingsLayer. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ANGEL_OF_DEATH = ARMOR_MATERIALS.register(
            "angel_of_death",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 0);
                        map.put(ArmorItem.Type.LEGGINGS, 0);
                        map.put(ArmorItem.Type.CHESTPLATE, 3);
                        map.put(ArmorItem.Type.HELMET, 0);
                        map.put(ArmorItem.Type.BODY, 0);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_ELYTRA,
                    () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(AngelOfDeathWings.MODID, "angel_wings"))),
                    0.0F,
                    0.0F));

    private ModArmorMaterials() {
    }
}
