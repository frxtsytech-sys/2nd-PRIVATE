package com.oblivioncombat.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.oblivioncombat.OblivionCombat;
import com.oblivioncombat.abilities.AbilityInfo;
import com.oblivioncombat.config.OblivionConfig;
import com.oblivioncombat.util.Compat;
import com.oblivioncombat.weapons.Rarity;
import com.oblivioncombat.weapons.Weapon;
import com.oblivioncombat.weapons.WeaponRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class OblivionCommand {
    private OblivionCommand() {}

    private static final SuggestionProvider<ServerCommandSource> WEAPONS =
            (ctx, builder) -> CommandSource.suggestMatching(WeaponRegistry.ids(), builder);

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(build()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("oblivion")
                .executes(ctx -> help(ctx.getSource()))
                .then(CommandManager.literal("weapons").executes(ctx -> weapons(ctx.getSource())))
                .then(CommandManager.literal("abilities").executes(ctx -> abilities(ctx.getSource())))
                .then(CommandManager.literal("version").executes(ctx -> version(ctx.getSource())))
                .then(CommandManager.literal("give").requires(Compat::isAdmin)
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("weapon", StringArgumentType.word()).suggests(WEAPONS)
                                        .executes(ctx -> give(ctx,
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "weapon"))))))
                .then(CommandManager.literal("cooldown").requires(Compat::isAdmin)
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> resetCooldown(ctx, EntityArgumentType.getPlayer(ctx, "player"), null))
                                .then(CommandManager.argument("weapon", StringArgumentType.word()).suggests(WEAPONS)
                                        .executes(ctx -> resetCooldown(ctx,
                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "weapon"))))))
                .then(CommandManager.literal("reload").requires(Compat::isAdmin).executes(ctx -> reload(ctx.getSource())))
                .then(CommandManager.literal("debug").requires(Compat::isAdmin).executes(ctx -> debug(ctx.getSource())));
    }

    private static void say(ServerCommandSource s, Text t) { s.sendFeedback(() -> t, false); }

    private static Text prefixed(String legacy) {
        return OblivionConfig.text(OblivionCombat.config().messages.prefix + legacy);
    }

    private static int help(ServerCommandSource s) {
        say(s, prefixed("&7Commands: &f/oblivion weapons&7, &fabilities&7, &fversion"));
        if (Compat.isAdmin(s)) say(s, prefixed("&7Admin: &f/oblivion give <player> <weapon>&7, &fcooldown&7, &freload&7, &fdebug"));
        say(s, prefixed("&8(The weapon GUI arrives in a later stage.)"));
        return 1;
    }

    private static Text weaponLabel(Weapon w) {
        return Text.literal(w.name()).formatted(w.color(), Formatting.BOLD);
    }

    private static int weapons(ServerCommandSource s) {
        for (Weapon w : WeaponRegistry.all()) {
            MutableText line = Text.literal("\u2022 ").formatted(Formatting.DARK_GRAY)
                    .append(weaponLabel(w))
                    .append(Text.literal(" [" + w.rarity().label() + "]").formatted(w.rarity().color()))
                    .append(Text.literal(w.enabled() ? "" : " (disabled)").formatted(Formatting.RED));
            say(s, line);
        }
        return WeaponRegistry.all().size();
    }

    private static int abilities(ServerCommandSource s) {
        for (Weapon w : WeaponRegistry.all()) {
            say(s, weaponLabel(w));
            for (AbilityInfo ab : w.abilities()) {
                double cd = ab.cooldownSeconds().getAsDouble();
                say(s, Text.literal("  " + ab.name()).formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal(" - " + ab.trigger().label()
                                + (cd > 0 ? ", " + (int) cd + "s cooldown" : "")).formatted(Formatting.GRAY))
                        .append(Text.literal("\n    " + ab.description()).formatted(Formatting.DARK_GRAY)));
            }
        }
        return 1;
    }

    private static int version(ServerCommandSource s) {
        String v = FabricLoader.getInstance().getModContainer(OblivionCombat.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        say(s, prefixed("&7OblivionCombat &fv" + v + " &7(Fabric, Minecraft 1.21.11)"));
        return 1;
    }

    private static int give(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, String id) {
        OblivionConfig cfg = OblivionCombat.config();
        Weapon w = WeaponRegistry.get(id);
        if (w == null) {
            ctx.getSource().sendError(prefixed(cfg.messages.unknownWeapon.replace("%weapon%", id)));
            return 0;
        }
        target.getInventory().offerOrDrop(w.createStack());
        say(ctx.getSource(), prefixed(cfg.messages.given.replace("%weapon%", w.name()).replace("%player%", target.getName().getString())));
        target.sendMessage(prefixed(cfg.messages.received.replace("%weapon%", w.name())), false);
        if (cfg.announceRareWeapons && w.rarity().ordinal() >= Rarity.LEGENDARY.ordinal()) {
            ctx.getSource().getServer().getPlayerManager().broadcast(
                    prefixed(cfg.messages.announce.replace("%player%", target.getName().getString()).replace("%weapon%", w.name())), false);
        }
        return 1;
    }

    private static int resetCooldown(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, String id) {
        if (id != null && WeaponRegistry.get(id) == null) {
            ctx.getSource().sendError(prefixed(OblivionCombat.config().messages.unknownWeapon.replace("%weapon%", id)));
            return 0;
        }
        OblivionCombat.cooldowns().reset(target.getUuid(), id);
        say(ctx.getSource(), prefixed("&7Cooldowns reset for &f" + target.getName().getString() + "&7."));
        return 1;
    }

    private static int reload(ServerCommandSource s) {
        boolean ok = OblivionCombat.reloadConfig();
        if (ok) say(s, prefixed(OblivionCombat.config().messages.reloaded));
        else s.sendError(prefixed(OblivionCombat.config().messages.reloadFailed));
        return ok ? 1 : 0;
    }

    private static int debug(ServerCommandSource s) {
        OblivionConfig c = OblivionCombat.config();
        say(s, prefixed("&7Effects: &f" + c.effectsEnabled + " &7Quality: &f" + c.quality
                + " &7Density: &f" + c.particleDensity));
        say(s, prefixed("&7Active animations: &f" + OblivionCombat.animations().size() + "&7/&f" + c.maxActiveAnimations
                + " &7Particle budget/tick: &f" + c.maxParticlesPerTick));
        say(s, prefixed("&7Weapons registered: &f" + WeaponRegistry.all().size() + " &7PvP abilities: &f" + c.allowPvp));
        return 1;
    }
}
