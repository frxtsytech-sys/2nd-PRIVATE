package com.oblivioncombat.weapons;

import net.minecraft.util.Formatting;

public enum Rarity {
    COMMON("Common", Formatting.WHITE),
    RARE("Rare", Formatting.BLUE),
    EPIC("Epic", Formatting.DARK_PURPLE),
    LEGENDARY("Legendary", Formatting.GOLD),
    MYTHIC("Mythic", Formatting.RED),
    DIVINE("DIVINE", Formatting.LIGHT_PURPLE);

    private final String label;
    private final Formatting color;

    Rarity(String label, Formatting color) { this.label = label; this.color = color; }

    public String label() { return label; }
    public Formatting color() { return color; }

    public static Rarity parse(String s, Rarity fallback) {
        if (s == null) return fallback;
        try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}
