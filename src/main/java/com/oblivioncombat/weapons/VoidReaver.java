package com.oblivioncombat.weapons;

import com.oblivioncombat.OblivionCombat;
import com.oblivioncombat.abilities.AbilityInfo;
import com.oblivioncombat.abilities.Trigger;
import com.oblivioncombat.animations.BlackholeAnimation;
import com.oblivioncombat.animations.PortalRingAnimation;
import com.oblivioncombat.animations.VoidExecutionAnimation;
import com.oblivioncombat.animations.VoidSlashAnimation;
import com.oblivioncombat.config.OblivionConfig;
import com.oblivioncombat.gui.Hud;
import com.oblivioncombat.particles.Fx;
import com.oblivioncombat.particles.Shapes;
import com.oblivioncombat.util.Compat;
import com.oblivioncombat.util.Targets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

/**
 * VOID REAVER
 *  Right-click            Void Slash        crescent projectile
 *  Sneak + Right-click    Blackhole         summon; press again while active = Void Collapse (early detonation)
 *  Sneak + Left-click     Dimensional Rift  teleport behind the target and strike
 *  On kill                Void Execution    implosion kill effect (visual only)
 */
public final class VoidReaver extends Weapon {
    private final AbilityInfo slash = new AbilityInfo("void_slash", "Void Slash", Trigger.RIGHT_CLICK,
            "Launches a massive purple crescent.", () -> cfg().voidSlash.cooldownSeconds);
    private final AbilityInfo blackhole = new AbilityInfo("blackhole", "Blackhole", Trigger.SNEAK_RIGHT_CLICK,
            "Summons a gravitational vortex that pulls enemies in.", () -> cfg().blackhole.cooldownSeconds);
    private final AbilityInfo collapse = new AbilityInfo("void_collapse", "Void Collapse", Trigger.SNEAK_RIGHT_CLICK,
            "Press again while the Blackhole is active to detonate it early.", () -> 0);
    private final AbilityInfo rift = new AbilityInfo("rift", "Dimensional Rift", Trigger.SNEAK_LEFT_CLICK,
            "Teleports you behind your target.", () -> cfg().rift.cooldownSeconds);
    private final AbilityInfo execution = new AbilityInfo("void_execution", "Void Execution", Trigger.ON_KILL,
            "Kills implode in a burst of cosmic energy.", () -> 0);

    public VoidReaver() {
        super("void_reaver", "VOID REAVER", Items.NETHERITE_SWORD, Formatting.DARK_PURPLE,
                "A weapon forged beyond reality.");
    }

    private static OblivionConfig cfg() { return OblivionCombat.config(); }

    @Override public Rarity rarity() { return Rarity.parse(cfg().voidReaver.rarity, Rarity.LEGENDARY); }
    @Override public boolean enabled() { return cfg().voidReaver.enabled; }
    @Override public double attackDamage() { return cfg().voidReaver.attackDamage; }
    @Override public double attackSpeed() { return cfg().voidReaver.attackSpeed; }
    @Override public List<AbilityInfo> abilities() { return List.of(slash, blackhole, collapse, rift, execution); }

    @Override
    public boolean activate(ServerPlayerEntity p, ServerWorld w, Trigger t, LivingEntity target) {
        return switch (t) {
            case RIGHT_CLICK -> voidSlash(p, w);
            case SNEAK_RIGHT_CLICK -> blackholeOrCollapse(p, w);
            case SNEAK_LEFT_CLICK -> dimensionalRift(p, w, target);
            default -> false;
        };
    }

    // ---- Void Slash ----
    private boolean voidSlash(ServerPlayerEntity p, ServerWorld w) {
        var c = cfg().voidSlash;
        if (!c.enabled) return false;
        if (!ready(p, slash)) return true;
        Vec3d dir = p.getRotationVector();
        Vec3d start = p.getEyePos().add(dir.multiply(1.2));
        if (!OblivionCombat.animations().start(new VoidSlashAnimation(w, p, start, dir, c))) return true;
        startCooldown(p, slash);
        Fx.sound(w, start, "minecraft:entity.enderman.teleport", 0.7f, 0.5f);
        Fx.sound(w, start, "minecraft:entity.warden.sonic_boom", 0.35f, 1.8f);
        return true;
    }

    // ---- Blackhole / Void Collapse ----
    private boolean blackholeOrCollapse(ServerPlayerEntity p, ServerWorld w) {
        var c = cfg().blackhole;
        if (!c.enabled) return false;

        BlackholeAnimation active = OblivionCombat.animations().blackholeOf(p.getUuid());
        if (active != null) {
            if (active.collapseNow()) Hud.title(p, "VOID COLLAPSE", "Everything ends here.", Formatting.LIGHT_PURPLE);
            return true;
        }
        if (!ready(p, blackhole)) return true;

        Vec3d look = p.getRotationVector();
        Vec3d start = p.getEyePos();
        Vec3d end = start.add(look.multiply(c.castDistance));
        BlockHitResult hit = w.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        Vec3d center = hit.getType() == HitResult.Type.MISS ? end : hit.getPos().subtract(look.multiply(1.0));

        if (!OblivionCombat.animations().start(new BlackholeAnimation(w, p, center, c))) return true;
        startCooldown(p, blackhole);
        return true;
    }

    // ---- Dimensional Rift ----
    private boolean dimensionalRift(ServerPlayerEntity p, ServerWorld w, LivingEntity target) {
        var c = cfg().rift;
        if (!c.enabled || target == null || !Targets.valid(p, target)) return false;
        if (!ready(p, rift)) return true;

        Vec3d facing = Vec3d.fromPolar(0, target.getYaw());
        Vec3d dest = Compat.pos(target).subtract(facing.multiply(c.behindDistance));
        BlockPos feet = BlockPos.ofFloored(dest);
        if (!free(w, feet) || !free(w, feet.up())) {
            p.sendMessage(OblivionConfig.text(cfg().messages.noRoom), true);
            return true;
        }

        Vec3d origin = Compat.pos(p);
        Vec3d toTarget = target.getEyePos().subtract(dest.add(0, p.getStandingEyeHeight(), 0));
        float yaw = (float) (MathHelper.atan2(toTarget.z, toTarget.x) * 180.0 / Math.PI) - 90f;
        float pitch = (float) -(MathHelper.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z)) * 180.0 / Math.PI);

        // origin ring, curved energy line (Bezier), destination ring
        Vec3d o = origin.add(0, 1, 0), d = dest.add(0, 1, 0);
        OblivionCombat.animations().start(new PortalRingAnimation(w, o, Shapes.UP, 1.4, 0.2, 8, Fx.PURPLE));
        Vec3d c1 = o.add(0, 2.0, 0), c2 = d.add(0, 2.0, 0);
        int pts = Fx.scale(24);
        for (int i = 0; i <= pts; i++) {
            Vec3d b = Shapes.bezier(o, c1, c2, d, i / (double) pts);
            Fx.particle(w, ParticleTypes.REVERSE_PORTAL, b, 1, 0.05, 0.02);
            if (i % 2 == 0) Fx.particle(w, Fx.MAGENTA, b);
        }
        Fx.sound(w, o, "minecraft:entity.enderman.teleport", 1.0f, 0.7f);

        Compat.teleport(p, dest, yaw, pitch);

        OblivionCombat.animations().start(new PortalRingAnimation(w, d, Shapes.UP, 0.2, 1.6, 8, Fx.MAGENTA));
        Fx.sound(w, d, "minecraft:entity.enderman.teleport", 1.0f, 1.2f);
        Compat.hurt(w, target, p, (float) c.damage);
        startCooldown(p, rift);
        Hud.title(p, "DIMENSIONAL RIFT", "Behind you...", Formatting.DARK_PURPLE);
        return true;
    }

    private static boolean free(ServerWorld w, BlockPos pos) {
        return w.getBlockState(pos).getCollisionShape(w, pos).isEmpty();
    }

    // ---- passive hit feedback ----
    @Override
    public void onHit(ServerPlayerEntity attacker, LivingEntity victim, ServerWorld w, int combo) {
        Vec3d mid = Compat.pos(victim).add(0, victim.getHeight() / 2, 0);
        Fx.particle(w, ParticleTypes.PORTAL, mid, Fx.scale(12), 0.4, 0.6);
        Fx.particle(w, Fx.PURPLE, mid, Fx.scale(8), 0.3, 0);
        Fx.sound(w, mid, "minecraft:entity.evoker.cast_spell", 0.4f, 1.6f);
        if (combo > 0 && combo % 3 == 0) {
            OblivionCombat.animations().start(new PortalRingAnimation(w, mid, Shapes.UP, 0.3, 2.0, 6, Fx.MAGENTA));
            Fx.sound(w, mid, "minecraft:block.end_portal_frame.fill", 0.6f, 0.6f);
        }
    }

    // ---- Void Execution ----
    @Override
    public void onKill(ServerPlayerEntity attacker, LivingEntity victim, ServerWorld w) {
        Vec3d mid = Compat.pos(victim).add(0, victim.getHeight() / 2, 0);
        OblivionCombat.animations().start(new VoidExecutionAnimation(w, mid, attacker.getUuid()));
    }
}
