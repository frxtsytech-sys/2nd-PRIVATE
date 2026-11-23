package com.oblivioncombat.particles;

import com.oblivioncombat.OblivionCombat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** All particle + sound output goes through here: quality scaling, global toggle, per-tick budget. */
public final class Fx {
    private Fx() {}

    public static final DustParticleEffect BLACK = new DustParticleEffect(0x0A0010, 1.6f);
    public static final DustParticleEffect DEEP_PURPLE = new DustParticleEffect(0x4A148C, 1.4f);
    public static final DustParticleEffect PURPLE = new DustParticleEffect(0x8E24AA, 1.3f);
    public static final DustParticleEffect MAGENTA = new DustParticleEffect(0xD500F9, 1.0f);
    public static final DustParticleEffect LIGHT = new DustParticleEffect(0xE1BEE7, 1.0f);

    private static int budget = 2500;

    public static void resetBudget() {
        budget = OblivionCombat.config().maxParticlesPerTick;
    }

    public static boolean on() {
        return OblivionCombat.config().effectsEnabled;
    }

    private static double multiplier() {
        return switch (OblivionCombat.config().quality.toUpperCase(Locale.ROOT)) {
            case "LOW" -> 0.25;
            case "MEDIUM" -> 0.55;
            case "ULTRA" -> 1.8;
            default -> 1.0;
        };
    }

    /** Scales a "HIGH quality" particle count by quality + density settings. */
    public static int scale(int base) {
        return Math.max(1, (int) Math.round(base * multiplier() * OblivionCombat.config().particleDensity));
    }

    public static void particle(ServerWorld w, ParticleEffect p, Vec3d pos) {
        particle(w, p, pos, 1, 0, 0);
    }

    public static void particle(ServerWorld w, ParticleEffect p, Vec3d pos, int count, double spread, double speed) {
        if (!on() || budget < count) return;
        budget -= count;
        w.spawnParticles(p, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    /** id like "minecraft:entity.warden.sonic_boom". Unknown ids are ignored silently. */
    public static void sound(ServerWorld w, Vec3d pos, String id, float volume, float pitch) {
        if (!on()) return;
        Identifier ident = Identifier.tryParse(id);
        if (ident == null) return;
        SoundEvent ev = Registries.SOUND_EVENT.get(ident);
        if (ev == null) return;
        w.playSound((PlayerEntity) null, pos.x, pos.y, pos.z, ev, SoundCategory.PLAYERS, volume, pitch);
    }
}
