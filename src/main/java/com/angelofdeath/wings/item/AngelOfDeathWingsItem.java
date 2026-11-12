package com.angelofdeath.wings.item;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AngelOfDeathWingsItem extends ArmorItem {
    private static final String LAST_GLIDE_START_KEY = "angelofdeathwings_last_glide_start";
    private static final int FLAP_INTERVAL_TICKS = 18;

    public AngelOfDeathWingsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, ArmorItem.Type.CHESTPLATE, properties);
    }

    /** Infinite durability. (Deliberately without @Override so it stays harmless if the hook is ever absent.) */
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        Level level = entity.level();
        if (!level.isClientSide) {
            long now = level.getGameTime();
            long lastStart = entity.getPersistentData().getLong(LAST_GLIDE_START_KEY);

            // Deep, heavy Ender Dragon flap when the wings spread to begin gliding.
            if (flightTicks <= 1 && now - lastStart >= 20L) {
                entity.getPersistentData().putLong(LAST_GLIDE_START_KEY, now);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, entity.getSoundSource(), 1.6F, 0.7F);
            } else if (flightTicks > 1 && flightTicks % FLAP_INTERVAL_TICKS == 0) {
                // Rhythmic, softer flaps while gliding.
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, entity.getSoundSource(), 0.4F, 1.1F);
            }
        }
        return true;
    }
}
