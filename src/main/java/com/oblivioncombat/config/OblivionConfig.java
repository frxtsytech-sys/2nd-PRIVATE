package com.oblivioncombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oblivioncombat.OblivionCombat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * config/oblivioncombat/config.json. Missing keys fall back to the defaults below.
 * All values are documented in README.md.
 */
public class OblivionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String _readme = "See README.md for every option. Edit, then run /oblivion reload.";

    // ---- global ----
    public boolean effectsEnabled = true;          // master toggle for particles + sounds
    public String quality = "HIGH";                // LOW | MEDIUM | HIGH | ULTRA
    public double particleDensity = 1.0;           // extra multiplier on top of quality
    public int maxParticlesPerTick = 2500;         // hard budget for the whole server per tick
    public int maxActiveAnimations = 64;           // hard cap on simultaneous animations
    public boolean allowPvp = true;                // false: abilities never hurt players
    public boolean creativeBypassCooldown = true;  // creative players ignore cooldowns
    public boolean showActionBar = true;
    public boolean showTitles = true;
    public boolean announceRareWeapons = true;     // broadcast when LEGENDARY+ weapons are given
    public int comboWindowTicks = 50;
    public int combatTagSeconds = 15;
    public int assistWindowSeconds = 10;
    public List<String> disabledWorlds = new ArrayList<>(); // e.g. ["minecraft:the_end"]

    public Messages messages = new Messages();
    public VoidReaverCfg voidReaver = new VoidReaverCfg();
    public VoidSlashCfg voidSlash = new VoidSlashCfg();
    public BlackholeCfg blackhole = new BlackholeCfg();
    public RiftCfg rift = new RiftCfg();

    public static class Messages {
        public String prefix = "&5&l[Oblivion] &r";
        public String onCooldown = "&c%ability% &7is on cooldown: &f%time%s";
        public String noRoom = "&cThere is no room behind that target.";
        public String weaponDisabledHere = "&cOblivion weapons are disabled in this world.";
        public String given = "&7Gave %weapon% &7to &f%player%&7.";
        public String received = "&7You received %weapon%&7.";
        public String announce = "&f%player% &7obtained %weapon%&7!";
        public String unknownWeapon = "&cUnknown weapon: &f%weapon%";
        public String reloaded = "&aConfiguration reloaded.";
        public String reloadFailed = "&cConfig is invalid, keeping the previous one. Check the server log.";
        public String killStreak = "&5Kill streak &fx%count%";
        public String assist = "&7Assist on &f%player%";
    }

    public static class VoidReaverCfg {
        public boolean enabled = true;
        public String rarity = "LEGENDARY";
        public double attackDamage = 10.0;   // total damage of a full-charge swing
        public double attackSpeed = 1.6;
    }

    public static class VoidSlashCfg {
        public boolean enabled = true;
        public double cooldownSeconds = 6;
        public double damage = 10;
        public double range = 24;            // blocks
        public double speed = 1.3;           // blocks per tick
        public double size = 2.2;            // crescent radius
        public double tiltDegrees = 25;
        public double knockback = 0.8;
    }

    public static class BlackholeCfg {
        public boolean enabled = true;
        public double cooldownSeconds = 30;
        public double radius = 9;
        public double durationSeconds = 6;
        public double pullStrength = 0.35;
        public double damagePerSecond = 6;
        public double explosionDamage = 14;
        public double explosionStrength = 2.2;
        public int maxEntities = 24;
        public double castDistance = 7;
        public boolean affectPlayers = true;
        public boolean affectMobs = true;
    }

    public static class RiftCfg {
        public boolean enabled = true;
        public double cooldownSeconds = 12;
        public double damage = 6;
        public double behindDistance = 1.8;
    }

    // ---- io ----
    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("oblivioncombat").resolve("config.json");
    }

    public static OblivionConfig load() {
        OblivionConfig c = read();
        if (c == null) c = new OblivionConfig();
        c.sanitize();
        c.save();
        return c;
    }

    /** Returns null when the file exists but cannot be parsed. */
    public static OblivionConfig read() {
        Path p = path();
        if (!Files.exists(p)) return new OblivionConfig();
        try (Reader r = Files.newBufferedReader(p)) {
            OblivionConfig c = GSON.fromJson(r, OblivionConfig.class);
            return c == null ? new OblivionConfig() : c;
        } catch (Exception e) {
            OblivionCombat.LOGGER.error("Could not read {}: {}", p, e.toString());
            return null;
        }
    }

    /** Only called at startup / reload, never during combat. */
    public void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            OblivionCombat.LOGGER.error("Could not write {}: {}", p, e.toString());
        }
    }

    public void sanitize() {
        if (messages == null) messages = new Messages();
        if (voidReaver == null) voidReaver = new VoidReaverCfg();
        if (voidSlash == null) voidSlash = new VoidSlashCfg();
        if (blackhole == null) blackhole = new BlackholeCfg();
        if (rift == null) rift = new RiftCfg();
        if (disabledWorlds == null) disabledWorlds = new ArrayList<>();
        if (quality == null) quality = "HIGH";
        particleDensity = Math.max(0.05, Math.min(4.0, particleDensity));
        maxParticlesPerTick = Math.max(50, maxParticlesPerTick);
        maxActiveAnimations = Math.max(1, maxActiveAnimations);
        blackhole.radius = Math.max(2, Math.min(32, blackhole.radius));
        blackhole.durationSeconds = Math.max(1, Math.min(30, blackhole.durationSeconds));
        blackhole.maxEntities = Math.max(1, Math.min(200, blackhole.maxEntities));
        blackhole.castDistance = Math.max(2, Math.min(30, blackhole.castDistance));
        voidSlash.speed = Math.max(0.2, Math.min(3, voidSlash.speed));
        voidSlash.range = Math.max(2, Math.min(64, voidSlash.range));
        voidSlash.size = Math.max(0.5, Math.min(6, voidSlash.size));
    }

    public static Text text(String legacy) {
        return Text.literal(legacy.replace('&', '\u00a7'));
    }
}
