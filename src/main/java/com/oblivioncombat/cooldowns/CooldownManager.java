package com.oblivioncombat.cooldowns;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Cooldowns keyed by "weaponId:abilityId", stored as absolute server ticks (in memory only). */
public final class CooldownManager {
    private final Map<UUID, Map<String, Long>> expiry = new HashMap<>();

    public long remainingTicks(UUID player, String key, long nowTick) {
        Map<String, Long> m = expiry.get(player);
        if (m == null) return 0;
        Long until = m.get(key);
        return until == null ? 0 : Math.max(0, until - nowTick);
    }

    public void set(UUID player, String key, long nowTick, long ticks) {
        expiry.computeIfAbsent(player, k -> new HashMap<>()).put(key, nowTick + ticks);
    }

    /** Resets all cooldowns of a weapon (prefix "weaponId:"), or everything when prefix is null. */
    public void reset(UUID player, String weaponId) {
        Map<String, Long> m = expiry.get(player);
        if (m == null) return;
        if (weaponId == null) m.clear();
        else m.keySet().removeIf(k -> k.startsWith(weaponId + ":"));
    }

    public void purge(long nowTick) {
        Iterator<Map.Entry<UUID, Map<String, Long>>> it = expiry.entrySet().iterator();
        while (it.hasNext()) {
            Map<String, Long> m = it.next().getValue();
            m.values().removeIf(t -> t <= nowTick);
            if (m.isEmpty()) it.remove();
        }
    }

    public void clearAll() {
        expiry.clear();
    }
}
