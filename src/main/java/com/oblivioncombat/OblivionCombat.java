package com.oblivioncombat;

import com.oblivioncombat.animations.AnimationManager;
import com.oblivioncombat.combat.CombatListener;
import com.oblivioncombat.combat.CombatTracker;
import com.oblivioncombat.commands.OblivionCommand;
import com.oblivioncombat.config.OblivionConfig;
import com.oblivioncombat.cooldowns.CooldownManager;
import com.oblivioncombat.gui.Hud;
import com.oblivioncombat.particles.Fx;
import com.oblivioncombat.weapons.WeaponRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class OblivionCombat implements ModInitializer {
    public static final String MOD_ID = "oblivioncombat";
    public static final Logger LOGGER = LoggerFactory.getLogger("OblivionCombat");

    private static OblivionConfig config = new OblivionConfig();
    private static final AnimationManager ANIMATIONS = new AnimationManager();
    private static final CooldownManager COOLDOWNS = new CooldownManager();
    private static final CombatTracker COMBAT = new CombatTracker();

    public static OblivionConfig config() { return config; }
    public static AnimationManager animations() { return ANIMATIONS; }
    public static CooldownManager cooldowns() { return COOLDOWNS; }
    public static CombatTracker combat() { return COMBAT; }

    /** Re-reads config files. Keeps the old config if the new file is invalid. */
    public static boolean reloadConfig() {
        OblivionConfig fresh = OblivionConfig.read();
        if (fresh == null) return false;
        fresh.sanitize();
        fresh.save();
        config = fresh;
        return true;
    }

    @Override
    public void onInitialize() {
        config = OblivionConfig.load();
        WeaponRegistry.registerDefaults();
        CombatListener.register();
        OblivionCommand.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Fx.resetBudget();
            ANIMATIONS.tick(server);
            Hud.tick(server);
            if (server.getTicks() % 1200 == 0) {
                COOLDOWNS.purge(server.getTicks());
                COMBAT.purge(server.getTicks());
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            ANIMATIONS.cancelOwner(id);
            COMBAT.clearTransient(id);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ANIMATIONS.clear();
            COOLDOWNS.clearAll();
            COMBAT.clearAll();
        });

        LOGGER.info("OblivionCombat loaded ({} weapon(s)).", WeaponRegistry.all().size());
    }
}
