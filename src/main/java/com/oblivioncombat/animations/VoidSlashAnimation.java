package com.oblivioncombat.animations;

import com.oblivioncombat.config.OblivionConfig;
import com.oblivioncombat.particles.Fx;
import com.oblivioncombat.particles.Shapes;
import com.oblivioncombat.util.Compat;
import com.oblivioncombat.util.Targets;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Crescent-shaped projectile. Hits each entity once, stops on solid blocks. */
public final class VoidSlashAnimation implements Animation {
    private final ServerWorld world;
    private final ServerPlayerEntity owner;
    private final OblivionConfig.VoidSlashCfg cfg;
    private final Vec3d dir, side;
    private final int maxAge;
    private final Set<UUID> hit = new HashSet<>();
    private Vec3d pos;
    private int age = 0;

    public VoidSlashAnimation(ServerWorld world, ServerPlayerEntity owner, Vec3d start, Vec3d direction,
                              OblivionConfig.VoidSlashCfg cfg) {
        this.world = world; this.owner = owner; this.cfg = cfg; this.pos = start;
        this.dir = direction.normalize();
        Vec3d right = dir.crossProduct(Shapes.UP);
        if (right.lengthSquared() < 1e-4) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(dir).normalize();
        double tilt = Math.toRadians(cfg.tiltDegrees);
        this.side = right.multiply(Math.cos(tilt)).add(up.multiply(Math.sin(tilt)));
        this.maxAge = (int) Math.ceil(cfg.range / cfg.speed);
    }

    @Override public UUID owner() { return owner.getUuid(); }

    @Override
    public boolean tick(MinecraftServer server) {
        if (owner.isRemoved()) return true;
        age++;
        pos = pos.add(dir.multiply(cfg.speed));

        BlockPos bp = BlockPos.ofFloored(pos);
        BlockState state = world.getBlockState(bp);
        if (state.isSolidBlock(world, bp)) {
            impact();
            return true;
        }

        render();

        double s = cfg.size * 2;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, Box.of(pos, s, s, s),
                e -> Targets.valid(owner, e) && !hit.contains(e.getUuid()));
        for (LivingEntity e : targets) {
            hit.add(e.getUuid());
            Compat.hurt(world, e, owner, (float) cfg.damage);
            Compat.setVelocity(e, dir.multiply(cfg.knockback).add(0, 0.25, 0));
            Vec3d mid = Compat.pos(e).add(0, e.getHeight() / 2, 0);
            Fx.particle(world, ParticleTypes.PORTAL, mid, Fx.scale(25), 0.5, 0.8);
            Shapes.sphere(mid, 0.9, Fx.scale(20), 0, p -> Fx.particle(world, Fx.MAGENTA, p));
            Fx.sound(world, mid, "minecraft:entity.warden.attack_impact", 0.7f, 1.3f);
        }
        return age >= maxAge;
    }

    private void render() {
        double half = Math.toRadians(75);
        Shapes.crescent(pos, dir, side, cfg.size, half, Fx.scale(22), Fx.scale(3), cfg.size * 0.45,
                p -> Fx.particle(world, Fx.PURPLE, p));
        Shapes.crescent(pos, dir, side, cfg.size * 0.9, half, Fx.scale(14), 1, 0,
                p -> Fx.particle(world, Fx.BLACK, p));
        Fx.particle(world, ParticleTypes.SQUID_INK, pos.subtract(dir.multiply(0.8)), Fx.scale(2), 0.4, 0.02);
        Fx.particle(world, ParticleTypes.REVERSE_PORTAL, pos.subtract(dir.multiply(1.2)), Fx.scale(3), 0.6, 0.05);
        if (age % 3 == 0) {
            Shapes.ring(pos.subtract(dir.multiply(0.5)), dir, cfg.size * 0.5, Fx.scale(14), age * 0.5,
                    p -> Fx.particle(world, Fx.DEEP_PURPLE, p));
        }
    }

    private void impact() {
        Fx.particle(world, ParticleTypes.PORTAL, pos, Fx.scale(30), 0.8, 0.8);
        Shapes.sphere(pos, 1.4, Fx.scale(30), 0, p -> Fx.particle(world, Fx.PURPLE, p));
        Fx.sound(world, pos, "minecraft:entity.generic.explode", 0.4f, 1.6f);
    }
}
