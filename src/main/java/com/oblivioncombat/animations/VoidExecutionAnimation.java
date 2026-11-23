package com.oblivioncombat.animations;

import com.oblivioncombat.particles.Fx;
import com.oblivioncombat.particles.Shapes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Kill effect: the victim is pulled into a collapsing sphere, then bursts into cosmic dust. Visual only. */
public final class VoidExecutionAnimation implements Animation {
    private static final int IMPLODE = 12;
    private static final int TOTAL = 18;

    private final ServerWorld world;
    private final Vec3d c;
    private final UUID owner;
    private int age = 0;

    public VoidExecutionAnimation(ServerWorld world, Vec3d center, UUID owner) {
        this.world = world; this.c = center; this.owner = owner;
    }

    @Override public UUID owner() { return owner; }

    @Override
    public boolean tick(MinecraftServer server) {
        age++;
        if (age == 1) {
            Fx.sound(world, c, "minecraft:entity.enderman.teleport", 0.8f, 0.5f);
            Fx.sound(world, c, "minecraft:block.end_portal.spawn", 0.25f, 1.6f);
        }
        if (age <= IMPLODE) {
            double r = 3.2 * (1 - age / (double) IMPLODE) + 0.3;
            Shapes.sphere(c, r, Fx.scale(30), age * 0.4, p -> Fx.particle(world, Fx.BLACK, p));
            Shapes.spiral(c, Shapes.UP, r + 0.8, 0.2, 2.5, 1.5, Fx.scale(16), age * 0.5,
                    p -> Fx.particle(world, ParticleTypes.REVERSE_PORTAL, p));
        } else if (age == IMPLODE + 1) {
            Fx.particle(world, ParticleTypes.EXPLOSION_EMITTER, c);
            Fx.particle(world, ParticleTypes.PORTAL, c, Fx.scale(60), 0.6, 1.2);
            Shapes.sphere(c, 1.2, Fx.scale(60), 0, p -> Fx.particle(world, Fx.MAGENTA, p));
            Fx.sound(world, c, "minecraft:entity.warden.sonic_boom", 0.6f, 1.4f);
        } else {
            double r = (age - IMPLODE) * 0.8;
            Shapes.sphere(c, r, Fx.scale(50), age * 0.2, p -> Fx.particle(world, Fx.PURPLE, p));
        }
        return age >= TOTAL;
    }
}
