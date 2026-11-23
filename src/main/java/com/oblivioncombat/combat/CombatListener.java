package com.oblivioncombat.combat;

import com.oblivioncombat.OblivionCombat;
import com.oblivioncombat.abilities.Trigger;
import com.oblivioncombat.config.OblivionConfig;
import com.oblivioncombat.util.Compat;
import com.oblivioncombat.weapons.Weapon;
import com.oblivioncombat.weapons.WeaponRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.UUID;

/**
 * Wires Fabric events to weapons. Ability clicks return SUCCESS (cancelling the vanilla action),
 * so a Sneak+Left-click ability never also performs a normal hit.
 */
public final class CombatListener {
    private CombatListener() {}

    public static void register() {
        // Right-click / Sneak + Right-click
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp) || !(world instanceof ServerWorld sw)) return ActionResult.PASS;
            Weapon wp = WeaponRegistry.fromStack(sp.getMainHandStack());
            if (wp == null) return ActionResult.PASS;
            if (!worldAllowed(sw)) { sp.sendMessage(OblivionConfig.text(cfg().messages.weaponDisabledHere), true); return ActionResult.PASS; }
            Trigger t = sp.isSneaking() ? Trigger.SNEAK_RIGHT_CLICK : Trigger.RIGHT_CLICK;
            return wp.activate(sp, sw, t, null) ? ActionResult.SUCCESS : ActionResult.PASS;
        });

        // Left-click on an entity (Sneak + Left-click triggers abilities)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp) || !(world instanceof ServerWorld sw)) return ActionResult.PASS;
            if (sp.isSpectator()) return ActionResult.PASS;
            Weapon wp = WeaponRegistry.fromStack(sp.getMainHandStack());
            if (wp == null || !worldAllowed(sw)) return ActionResult.PASS;
            if (!sp.isSneaking()) return ActionResult.PASS;
            LivingEntity target = entity instanceof LivingEntity le ? le : null;
            return wp.activate(sp, sw, Trigger.SNEAK_LEFT_CLICK, target) ? ActionResult.SUCCESS : ActionResult.PASS;
        });

        // Hit feedback, combos, combat tags, assist bookkeeping
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (Compat.abilityDamage || blocked || damageTaken <= 0) return;
            if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) return;
            if (source.getSource() != attacker) return; // melee only
            ServerWorld w = Compat.world(attacker);
            long now = Compat.server(attacker).getTicks();

            if (entity instanceof ServerPlayerEntity victim) {
                OblivionCombat.combat().playerDamaged(victim.getUuid(), attacker.getUuid(), now);
            }
            Weapon wp = WeaponRegistry.fromStack(attacker.getMainHandStack());
            if (wp == null || !worldAllowed(w)) return;
            int combo = OblivionCombat.combat().hit(attacker.getUuid(), now);
            wp.onHit(attacker, entity, w, combo);
        });

        // Kill effects, streaks, assists
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity dead) {
                OblivionCombat.combat().onDeath(dead.getUuid());
            }
            Entity atk = source.getAttacker();
            if (!(atk instanceof ServerPlayerEntity killer) || entity == killer) return;
            ServerWorld w = Compat.world(killer);
            long now = Compat.server(killer).getTicks();

            Weapon wp = WeaponRegistry.fromStack(killer.getMainHandStack());
            if (wp != null && worldAllowed(w)) wp.onKill(killer, entity, w);

            if (entity instanceof ServerPlayerEntity victim) {
                int streak = OblivionCombat.combat().addKill(killer.getUuid());
                if (streak >= 2) {
                    killer.sendMessage(OblivionConfig.text(cfg().messages.prefix
                            + cfg().messages.killStreak.replace("%count%", String.valueOf(streak))), false);
                }
                for (UUID id : OblivionCombat.combat().assists(victim.getUuid(), killer.getUuid(), now)) {
                    ServerPlayerEntity helper = Compat.server(killer).getPlayerManager().getPlayer(id);
                    if (helper != null) {
                        helper.sendMessage(OblivionConfig.text(cfg().messages.prefix
                                + cfg().messages.assist.replace("%player%", victim.getName().getString())), true);
                    }
                }
            }
        });
    }

    private static OblivionConfig cfg() { return OblivionCombat.config(); }

    public static boolean worldAllowed(ServerWorld w) {
        return !cfg().disabledWorlds.contains(w.getRegistryKey().getValue().toString());
    }
}
