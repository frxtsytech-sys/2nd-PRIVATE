package com.oblivioncombat.animations;

import com.oblivioncombat.OblivionCombat;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Single list, single tick hook. Failed animations are dropped instead of crashing the server. */
public final class AnimationManager {
    private final List<Animation> active = new ArrayList<>();

    public boolean start(Animation a) {
        if (active.size() >= OblivionCombat.config().maxActiveAnimations) return false;
        active.add(a);
        return true;
    }

    public void tick(MinecraftServer server) {
        for (int i = 0; i < active.size(); ) {
            Animation a = active.get(i);
            boolean done;
            try {
                done = a.tick(server);
            } catch (Throwable t) {
                OblivionCombat.LOGGER.error("Animation {} failed and was removed", a.getClass().getSimpleName(), t);
                done = true;
            }
            if (done) {
                try { a.onEnd(); } catch (Throwable ignored) {}
                active.remove(i);
            } else {
                i++;
            }
        }
    }

    public BlackholeAnimation blackholeOf(UUID owner) {
        for (Animation a : active) {
            if (a instanceof BlackholeAnimation b && owner.equals(b.owner())) return b;
        }
        return null;
    }

    public void cancelOwner(UUID owner) {
        active.removeIf(a -> owner.equals(a.owner()));
    }

    public void clear() {
        active.clear();
    }

    public int size() {
        return active.size();
    }
}
