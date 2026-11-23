package com.oblivioncombat.gui;

import com.oblivioncombat.OblivionCombat;
import com.oblivioncombat.abilities.AbilityInfo;
import com.oblivioncombat.abilities.Trigger;
import com.oblivioncombat.weapons.Weapon;
import com.oblivioncombat.weapons.WeaponRegistry;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Action-bar cooldown indicators and activation titles. */
public final class Hud {
    private Hud() {}

    public static void title(ServerPlayerEntity p, String title, String subtitle, Formatting color) {
        if (!OblivionCombat.config().showTitles) return;
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(4, 22, 10));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle).formatted(Formatting.GRAY)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(color, Formatting.BOLD)));
    }

    /** Called every server tick; only does work every 4th tick for players holding a weapon. */
    public static void tick(MinecraftServer server) {
        if (!OblivionCombat.config().showActionBar || server.getTicks() % 4 != 0) return;
        long now = server.getTicks();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Weapon w = WeaponRegistry.fromStack(p.getMainHandStack());
            if (w == null) continue;
            p.sendMessage(line(p, w, now), true);
        }
    }

    private static Text line(ServerPlayerEntity p, Weapon w, long now) {
        MutableText out = Text.empty();
        boolean first = true;
        for (AbilityInfo ab : w.abilities()) {
            if (ab.trigger() == Trigger.PASSIVE || ab.trigger() == Trigger.ON_KILL) continue;
            if (ab.cooldownSeconds().getAsDouble() <= 0) continue;
            if (!first) out.append(Text.literal("  |  ").formatted(Formatting.DARK_GRAY));
            first = false;
            long left = OblivionCombat.cooldowns().remainingTicks(p.getUuid(), w.id() + ":" + ab.id(), now);
            out.append(Text.literal(ab.name() + " ").formatted(Formatting.GRAY));
            if (left <= 0) out.append(Text.literal("READY").formatted(Formatting.GREEN, Formatting.BOLD));
            else out.append(Text.literal(String.format("%.1fs", left / 20.0)).formatted(Formatting.RED));
        }
        int combo = OblivionCombat.combat().combo(p.getUuid(), now);
        if (combo > 1) {
            out.append(Text.literal("  |  ").formatted(Formatting.DARK_GRAY));
            out.append(Text.literal("Combo x" + combo).formatted(w.color(), Formatting.BOLD));
        }
        return out;
    }
}
