package com.angelofdeath.wings.boost;

import com.angelofdeath.wings.AngelOfDeathWings;
import com.angelofdeath.wings.item.AngelOfDeathWingsItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

/** Server-side "White Hole" boost: 60 ticks of 5x-rocket-speed flight, 80 tick cooldown, white trail while active. */
@EventBusSubscriber(modid = AngelOfDeathWings.MODID)
public final class WingBoostHandler {
    public static final int BOOST_TICKS = 60;
    public static final int COOLDOWN_TICKS = 80;
    /** Blocks per tick: 5x the ~0.72 b/t a Flight Duration 3 rocket gives. */
    public static final double BOOST_SPEED = 3.6D;
    /** Stay this far away from a wall that is in the flight path so the boost never causes fly-into-wall damage. */
    private static final double WALL_MARGIN = 1.6D;

    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.0F);
    private static final double[] WING_OFFSETS = {0.5D, 1.2D, 2.0D, 3.0D};

    private static final class BoostState {
        int ticksLeft = BOOST_TICKS;
        Vec3 lastPos;
    }

    /** Only touched from the server thread. */
    private static final Map<UUID, BoostState> ACTIVE = new HashMap<>();

    private WingBoostHandler() {
    }

    public static void tryActivate(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof AngelOfDeathWingsItem)) {
            return;
        }
        if (player.isSpectator() || ACTIVE.containsKey(player.getUUID())) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(chest.getItem())) {
            return;
        }
        // Only while gliding or in mid-air.
        if (!player.isFallFlying() && player.onGround()) {
            return;
        }
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        player.getCooldowns().addCooldown(chest.getItem(), COOLDOWN_TICKS);

        BoostState state = new BoostState();
        state.lastPos = player.position();
        ACTIVE.put(player.getUUID(), state);

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 back = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D).subtract(look.scale(0.8D));

        level.sendParticles(ParticleTypes.FLASH, back.x, back.y, back.z, 2, 0.15D, 0.15D, 0.15D, 0.0D);
        level.playSound(null, back.x, back.y, back.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0F, 0.8F);
        level.playSound(null, back.x, back.y, back.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.8F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BoostState state = ACTIVE.get(player.getUUID());
        if (state == null) {
            return;
        }

        boolean wearingWings = player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AngelOfDeathWingsItem;
        if (!wearingWings || !player.isAlive() || player.isSpectator() || player.onGround()
                || player.isInWaterOrBubble() || player.horizontalCollision) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();

        // Cap the speed so the player stops short of any wall in the flight path.
        double speed = BOOST_SPEED;
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(BOOST_SPEED + WALL_MARGIN)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean blocked = false;
        if (hit.getType() == HitResult.Type.BLOCK) {
            double allowed = hit.getLocation().distanceTo(eye) - WALL_MARGIN;
            if (allowed < 0.25D) {
                blocked = true;
            } else {
                speed = Math.min(speed, allowed);
            }
        }

        if (blocked) {
            player.setDeltaMovement(look.scale(0.4D));
            player.hurtMarked = true;
            ACTIVE.remove(player.getUUID());
            return;
        }

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
        player.setDeltaMovement(look.scale(speed));
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        spawnTrail(level, player, state, look);

        state.lastPos = player.position();
        state.ticksLeft--;
        if (state.ticksLeft <= 0) {
            ACTIVE.remove(player.getUUID()); // trail stops the moment the 60 ticks are over
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    /** Dense pure-white trail behind the wings, filled in along the path travelled since last tick. */
    private static void spawnTrail(ServerLevel level, ServerPlayer player, BoostState state, Vec3 look) {
        Vec3 current = player.position();
        Vec3 from = state.lastPos == null ? current : state.lastPos;
        Vec3 path = current.subtract(from);
        int samples = Math.max(1, Math.min(8, (int) Math.ceil(path.length() / 0.6D)));

        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        double shoulderHeight = player.getBbHeight() * 0.65D;

        for (int i = 0; i <= samples; i++) {
            Vec3 along = from.add(path.scale((double) i / samples));
            Vec3 spine = along.add(0.0D, shoulderHeight, 0.0D).subtract(look.scale(0.9D));

            level.sendParticles(ParticleTypes.END_ROD, spine.x, spine.y, spine.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            level.sendParticles(WHITE_DUST, spine.x, spine.y, spine.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);

            for (double offset : WING_OFFSETS) {
                Vec3 sweep = spine.subtract(look.scale(offset * 0.25D));
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 p = sweep.add(right.scale(offset * side));
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
                    level.sendParticles(WHITE_DUST, p.x, p.y, p.z, 1, 0.06D, 0.06D, 0.06D, 0.0D);
                }
            }
        }
    }
}
