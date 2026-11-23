package com.oblivioncombat.util;

import com.oblivioncombat.OblivionCombat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Central target restrictions shared by all abilities. */
public final class Targets {
    private Targets() {}

    public static boolean valid(ServerPlayerEntity owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isRemoved() || e.isInvulnerable()) return false;
        if (e instanceof ArmorStandEntity) return false;
        if (owner.isTeammate(e)) return false;
        if (e instanceof PlayerEntity p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (!OblivionCombat.config().allowPvp) return false;
        }
        return true;
    }

    public static boolean validForBlackhole(ServerPlayerEntity owner, LivingEntity e) {
        if (!valid(owner, e)) return false;
        var cfg = OblivionCombat.config().blackhole;
        if (e instanceof PlayerEntity) return cfg.affectPlayers;
        return cfg.affectMobs;
    }
}
