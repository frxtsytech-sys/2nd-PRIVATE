package com.oblivioncombat.weapons;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WeaponRegistry {
    private static final Map<String, Weapon> WEAPONS = new LinkedHashMap<>();

    private WeaponRegistry() {}

    public static void registerDefaults() {
        WEAPONS.clear();
        register(new VoidReaver());
        // Stage 3+: Inferno Edge, Frostbite, Thunder God, Soul Destroyer, Celestial Divider, axes...
    }

    public static void register(Weapon w) { WEAPONS.put(w.id(), w); }
    public static Weapon get(String id) { return WEAPONS.get(id); }
    public static Collection<Weapon> all() { return WEAPONS.values(); }
    public static Collection<String> ids() { return WEAPONS.keySet(); }

    /** The enabled Oblivion weapon this stack represents, or null. */
    public static Weapon fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        String id = data.copyNbt().getString(Weapon.TAG).orElse("");
        if (id.isEmpty()) return null;
        Weapon w = WEAPONS.get(id);
        return (w != null && w.enabled()) ? w : null;
    }
}
