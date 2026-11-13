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
import net.minecraft.util.Mth;
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

/**
 * Server-side "White Hole" boost.
 *
 * Phase 1 (ticks 0-29, 1.5 s): 15x firework-rocket speed.
 * Phase 2 (ticks 30-39):       eases down to 8x firework-rocket speed.
 * After tick 40 (about the length of a Flight Duration 2 rocket) the boost ends, the white trail stops, and the
 * player simply keeps the 8x momentum, which vanilla elytra physics then bleeds off naturally.
 */
@EventBusSubscriber(modid = AngelOfDeathWings.MODID)
public final class WingBoostHandler {
    /** Blocks per tick a firework rocket pushes an elytra flyer toward (~0.72, i.e. 3.6 / 5). */
    public static final double ROCKET_SPEED = 0.72D;
    public static final double PEAK_SPEED = ROCKET_SPEED * 15.0D;
    public static final double SUSTAIN_SPEED = ROCKET_SPEED * 8.0D;

    public static final int PEAK_TICKS = 30;
    public static final int EASE_TICKS = 4;
    /** Total boost length: 40 ticks, about a Flight Duration 2 rocket. */
    public static final int BOOST_TICKS = 40;
    public static final int COOLDOWN_TICKS = 80;

    /** Stay this far away from a wall in the flight path so the boost never smashes the player into it. */
    private static final double WALL_MARGIN = 2.0D;

    private static final double HELIX_RADIUS = 0.85D;
    private static final double HELIX_TWIST_PER_BLOCK = 0.5D;
    private static final double WING_TIP_SPAN = 2.4D;
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.4F);

    private static final class BoostState {
        int tick = 0;
        double travelled = 0.0D;
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

        level.sendParticles(ParticleTypes.FLASH, back.x, back.y, back.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, back.x, back.y, back.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0F, 0.8F);
        level.playSound(null, back.x, back.y, back.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.8F);
    }

    /** 15x for the first 30 ticks, then a short smooth ease down to 8x. */
    private static double speedFor(int tick) {
        if (tick < PEAK_TICKS) {
            return PEAK_SPEED;
        }
        int into = tick - PEAK_TICKS + 1;
        if (into >= EASE_TICKS) {
            return SUSTAIN_SPEED;
        }
        float t = into / (float) EASE_TICKS;
        float eased = t * t * (3.0F - 2.0F * t);
        return Mth.lerp(eased, PEAK_SPEED, SUSTAIN_SPEED);
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
        double speed = speedFor(state.tick);
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(speed + WALL_MARGIN)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            double allowed = hit.getLocation().distanceTo(eye) - WALL_MARGIN;
            if (allowed < 0.25D) {
                player.setDeltaMovement(look.scale(0.4D));
                player.hurtMarked = true;
                ACTIVE.remove(player.getUUID());
                return;
            }
            speed = Math.min(speed, allowed);
        }

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
        player.setDeltaMovement(look.scale(speed));
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        spawnTrail(level, player, state, look);

        state.lastPos = player.position();
        state.tick++;
        if (state.tick >= BOOST_TICKS) {
            // Boost over: the trail stops here and the player keeps the 8x momentum like normal elytra flight.
            ACTIVE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    /**
     * Light "white hole" trail: a slow double helix of end rod sparks spiralling around the flight line, a soft white
     * core down the middle, and one streak off each wing tip. At most ~13 particles per tick.
     */
    private static void spawnTrail(ServerLevel level, ServerPlayer player, BoostState state, Vec3 look) {
        Vec3 current = player.position();
        Vec3 from = state.lastPos == null ? current : state.lastPos;
        Vec3 path = current.subtract(from);
        double length = path.length();
        int samples = Mth.clamp((int) Math.ceil(length / 2.5D), 1, 4);
        double step = length / samples;

        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        double shoulderHeight = player.getBbHeight() * 0.65D;

        for (int i = 1; i <= samples; i++) {
            Vec3 along = from.add(path.scale((double) i / samples));
            Vec3 center = along.add(0.0D, shoulderHeight, 0.0D).subtract(look.scale(0.9D));
            state.travelled += step;
            double angle = state.travelled * HELIX_TWIST_PER_BLOCK * Math.PI * 2.0D / 3.0D;

            for (int strand = 0; strand < 2; strand++) {
                double a = angle + strand * Math.PI;
                Vec3 p = center
                        .add(right.scale(Math.cos(a) * HELIX_RADIUS))
                        .add(up.scale(Math.sin(a) * HELIX_RADIUS));
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.sendParticles(WHITE_DUST, center.x, center.y, center.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);

            if ((state.tick & 1) == 0 && i == samples) {
                Vec3 tipBase = center.subtract(look.scale(0.4D));
                Vec3 leftTip = tipBase.subtract(right.scale(WING_TIP_SPAN));
                Vec3 rightTip = tipBase.add(right.scale(WING_TIP_SPAN));
                level.sendParticles(ParticleTypes.END_ROD, leftTip.x, leftTip.y, leftTip.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, rightTip.x, rightTip.y, rightTip.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
