package com.oblivioncombat.animations;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** One scheduled effect. Ticked once per server tick by AnimationManager (no per-effect tasks). */
public interface Animation {
    /** Owning player, or null. Used to cancel animations when a player disconnects. */
    UUID owner();

    /** @return true when finished and ready to be removed. */
    boolean tick(MinecraftServer server);

    default void onEnd() {}
}
