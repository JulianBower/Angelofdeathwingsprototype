package com.angelofdeath.wings.registry;

import com.angelofdeath.wings.AngelOfDeathWings;
import com.angelofdeath.wings.item.AngelOfDeathWingsItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AngelOfDeathWings.MODID);

    /** No durability is set at all and the unbreakable component is applied, so the wings can never be damaged. */
    public static final DeferredItem<AngelOfDeathWingsItem> ANGEL_OF_DEATH_WINGS = ITEMS.register(
            "angel_of_death_wings",
            () -> new AngelOfDeathWingsItem(
                    ModArmorMaterials.ANGEL_OF_DEATH,
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()
                            .component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    private ModItems() {
    }
}
