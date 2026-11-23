package com.oblivioncombat.animations;

import com.oblivioncombat.particles.Fx;
import com.oblivioncombat.particles.Shapes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Expanding (or shrinking) portal-like ring. */
public final class PortalRingAnimation implements Animation {
    private final ServerWorld world;
    private final Vec3d center, normal;
    private final double r0, r1;
    private final int ticks;
    private final DustParticleEffect dust;
    private int age = 0;

    public PortalRingAnimation(ServerWorld world, Vec3d center, Vec3d normal, double r0, double r1,
                               int ticks, DustParticleEffect dust) {
        this.world = world; this.center = center; this.normal = normal;
        this.r0 = r0; this.r1 = r1; this.ticks = Math.max(1, ticks); this.dust = dust;
    }

    @Override public UUID owner() { return null; }

    @Override
    public boolean tick(MinecraftServer server) {
        age++;
        double r = r0 + (r1 - r0) * age / ticks;
        Shapes.ring(center, normal, r, Fx.scale(36), age * 0.3, p -> Fx.particle(world, dust, p));
        Shapes.ring(center, normal, r * 0.85, Fx.scale(10), -age * 0.4, p -> Fx.particle(world, ParticleTypes.REVERSE_PORTAL, p));
        return age >= ticks;
    }
}
