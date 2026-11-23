package com.oblivioncombat.animations;

import com.oblivioncombat.OblivionCombat;
import com.oblivioncombat.config.OblivionConfig;
import com.oblivioncombat.gui.Hud;
import com.oblivioncombat.particles.Fx;
import com.oblivioncombat.particles.Shapes;
import com.oblivioncombat.util.Compat;
import com.oblivioncombat.util.Targets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Signature ability. Visual + gameplay only: no blocks are touched, no permanent entity changes
 * (the only side effect is a 6-tick Slowness refresh, which expires by itself).
 *
 * Phase 1 (growing/pull): sphere grows, vortex rotates, entities are pulled in and slowed.
 * Phase 2 (collapse, 10 ticks): shockwave expands, entities are thrown outward, final burst.
 */
public final class BlackholeAnimation implements Animation {
    private static final int COLLAPSE_TICKS = 10;

    private final ServerWorld world;
    private final ServerPlayerEntity owner;
    private final Vec3d center;
    private final OblivionConfig.BlackholeCfg cfg;
    private final int duration;
    private int age = 0;
    private int collapseAge = -1;
    private boolean collapseRequested = false;

    public BlackholeAnimation(ServerWorld world, ServerPlayerEntity owner, Vec3d center, OblivionConfig.BlackholeCfg cfg) {
        this.world = world; this.owner = owner; this.center = center; this.cfg = cfg;
        this.duration = Math.max(20, (int) (cfg.durationSeconds * 20));
    }

    @Override public UUID owner() { return owner.getUuid(); }

    /** Void Collapse: detonate early. Ignored during the first half second. */
    public boolean collapseNow() {
        if (collapseAge >= 0 || age < 10) return false;
        collapseRequested = true;
        return true;
    }

    @Override
    public boolean tick(MinecraftServer server) {
        if (owner.isRemoved()) return true;

        if (collapseAge < 0) {
            age++;
            double progress = Math.min(1.0, age / (double) duration);
            if (age == 1) {
                Fx.sound(world, center, "minecraft:block.end_portal.spawn", 0.8f, 0.5f);
                Fx.sound(world, center, "minecraft:entity.ender_dragon.growl", 0.35f, 0.5f);
                Hud.title(owner, "BLACKHOLE", "Reality is collapsing...", Formatting.DARK_PURPLE);
            }
            if (age % 20 == 0) {
                Fx.sound(world, center, "minecraft:block.beacon.ambient", (float) (0.6 + 0.6 * progress), 0.5f);
                Fx.sound(world, center, "minecraft:block.portal.ambient", 0.5f, (float) (0.5 + 0.3 * progress));
            }
            pullAndDamage(progress);
            renderGrowing(progress);
            if (age >= duration || collapseRequested) {
                collapseAge = 0;
                Fx.sound(world, center, "minecraft:entity.warden.sonic_boom", 1.0f, 0.6f);
            }
            return false;
        }

        collapseAge++;
        if (collapseAge == 1) detonate();
        renderCollapse();
        return collapseAge >= COLLAPSE_TICKS;
    }

    private List<LivingEntity> gather() {
        double d = cfg.radius * 2;
        double r2 = cfg.radius * cfg.radius;
        List<LivingEntity> list = world.getEntitiesByClass(LivingEntity.class, Box.of(center, d, d, d),
                e -> Targets.validForBlackhole(owner, e) && Compat.pos(e).squaredDistanceTo(center) <= r2);
        if (list.size() > cfg.maxEntities) list = new ArrayList<>(list.subList(0, cfg.maxEntities));
        return list;
    }

    private void pullAndDamage(double progress) {
        boolean damageTick = age % 10 == 0;
        int amp = Math.min(3, (int) (progress * 4));
        for (LivingEntity e : gather()) {
            if (damageTick) Compat.hurt(world, e, owner, (float) (cfg.damagePerSecond * 0.5));
            Vec3d mid = Compat.pos(e).add(0, e.getHeight() / 2.0, 0);
            Vec3d to = center.subtract(mid);
            double dist = to.length();
            if (dist < 0.01) continue;
            double falloff = 0.4 + 0.6 * (1 - Math.min(1, dist / cfg.radius));
            double strength = cfg.pullStrength * (0.35 + 0.65 * progress) * falloff;
            Vec3d base = damageTick ? Vec3d.ZERO : e.getVelocity().multiply(0.55);
            Vec3d vel = base.add(to.normalize().multiply(strength));
            if (dist < 1.2) vel = vel.multiply(0.3);
            double speed = vel.length();
            if (speed > 1.1) vel = vel.multiply(1.1 / speed);
            Compat.setVelocity(e, vel);
            e.fallDistance = 0;
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, amp, false, false, false));
        }
    }

    private void detonate() {
        for (LivingEntity e : gather()) {
            Vec3d off = Compat.pos(e).add(0, e.getHeight() / 2.0, 0).subtract(center);
            double dist = off.length();
            double f = 1 - Math.min(1, dist / cfg.radius);
            Vec3d dir = dist < 0.01 ? Shapes.UP : off.normalize();
            Compat.hurt(world, e, owner, (float) (cfg.explosionDamage * (0.5 + 0.5 * f)));
            Vec3d vel = dir.multiply(cfg.explosionStrength * (0.5 + 0.5 * f)).add(0, 0.35 + 0.4 * f, 0);
            Compat.setVelocity(e, vel);
        }
        Fx.sound(world, center, "minecraft:entity.generic.explode", 1.0f, 0.5f);
        Fx.particle(world, ParticleTypes.EXPLOSION_EMITTER, center);
        Fx.particle(world, ParticleTypes.PORTAL, center, Fx.scale(80), 1.5, 1.0);
        Fx.particle(world, ParticleTypes.LARGE_SMOKE, center, Fx.scale(20), 1.2, 0.1);
        Shapes.sphere(center, 1.8, Fx.scale(80), 0, p -> Fx.particle(world, Fx.MAGENTA, p));
    }

    private void renderGrowing(double progress) {
        double t = age;
        double grow = Math.min(1.0, age / 20.0);
        double coreR = (0.35 + 0.9 * grow) * (1 + 0.25 * progress);

        Shapes.sphere(center, coreR, Fx.scale(18), t * 0.15, p -> Fx.particle(world, Fx.BLACK, p));
        if (age % 2 == 0) {
            Shapes.sphere(center, coreR * 1.35, Fx.scale(10), -t * 0.2, p -> Fx.particle(world, Fx.PURPLE, p));
        }

        Vec3d tilt = new Vec3d(Math.sin(t * 0.05) * 0.4, 1, Math.cos(t * 0.05) * 0.4).normalize();
        double discR = coreR * (2.2 + 1.5 * grow);
        Shapes.ring(center, tilt, discR, Fx.scale(28), t * 0.25, p -> Fx.particle(world, Fx.MAGENTA, p));
        Shapes.ring(center, tilt, discR * 0.7, Fx.scale(20), -t * 0.35, p -> Fx.particle(world, Fx.PURPLE, p));

        for (int arm = 0; arm < 3; arm++) {
            double phase = t * 0.18 + arm * 2 * Math.PI / 3;
            Shapes.spiral(center, tilt, discR * 1.8, coreR * 1.1, 0, 1.25, Fx.scale(14), phase,
                    p -> Fx.particle(world, Fx.DEEP_PURPLE, p));
        }
        if (age % 2 == 0) {
            Fx.particle(world, ParticleTypes.REVERSE_PORTAL, center, Fx.scale(6), discR, 0.15);
        }
        if (age % 3 == 0) {
            Shapes.ring(center, Shapes.UP, cfg.radius * grow, Fx.scale(30), t * 0.05,
                    p -> Fx.particle(world, Fx.DEEP_PURPLE, p));
        }
        if (progress > 0.5 && age % 2 == 0) {
            Fx.particle(world, ParticleTypes.END_ROD, center, Fx.scale(2), coreR, 0.05);
        }
    }

    private void renderCollapse() {
        double prog = collapseAge / (double) COLLAPSE_TICKS;
        double r = cfg.radius * prog;
        Shapes.ring(center, Shapes.UP, r, Fx.scale(60), 0, p -> Fx.particle(world, Fx.LIGHT, p));
        Shapes.ring(center, new Vec3d(0.4, 1, 0.2).normalize(), r * 0.9, Fx.scale(40), collapseAge * 0.3,
                p -> Fx.particle(world, Fx.MAGENTA, p));
        Shapes.sphere(center, r, Fx.scale(70), collapseAge * 0.3, p -> Fx.particle(world, Fx.PURPLE, p));
        if (collapseAge == COLLAPSE_TICKS) {
            Fx.particle(world, ParticleTypes.PORTAL, center, Fx.scale(60), cfg.radius * 0.4, 0.8);
        }
    }
}
