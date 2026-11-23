package com.oblivioncombat.util;

import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Every Minecraft API call that has changed between 1.21.x releases lives here, so a
 * mapping mismatch can be fixed in ONE place instead of across the whole mod.
 */
public final class Compat {
    private Compat() {}

    /** True while the mod itself is dealing ability damage (so hit effects don't fire for it). */
    public static boolean abilityDamage = false;

    public static ServerWorld world(Entity e) {
        return (ServerWorld) e.getEntityWorld();
    }

    public static MinecraftServer server(Entity e) {
        return world(e).getServer();
    }

    public static Vec3d pos(Entity e) {
        return new Vec3d(e.getX(), e.getY(), e.getZ());
    }

    /** Operator "gamemaster" level (2) or higher. */
    public static boolean isAdmin(ServerCommandSource source) {
        return source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
    }

    public static void hurt(ServerWorld world, LivingEntity target, ServerPlayerEntity source, float amount) {
        abilityDamage = true;
        try {
            target.damage(world, world.getDamageSources().playerAttack(source), amount);
        } finally {
            abilityDamage = false;
        }
    }

    public static void setVelocity(Entity e, Vec3d v) {
        e.setVelocity(v);
        if (e instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
        }
    }

    public static void teleport(ServerPlayerEntity p, Vec3d pos, float yaw, float pitch) {
        p.networkHandler.requestTeleport(pos.x, pos.y, pos.z, yaw, pitch);
    }
}
