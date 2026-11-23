package com.oblivioncombat.combat;

import com.oblivioncombat.OblivionCombat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Combos, kill streaks, assists and combat tags. In-memory, cleaned on disconnect/shutdown. */
public final class CombatTracker {
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private final Map<UUID, Integer> combo = new HashMap<>();
    private final Map<UUID, Integer> streak = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> damagedBy = new HashMap<>(); // victim -> attacker -> tick
    private final Map<UUID, Long> tagged = new HashMap<>();

    /** Registers a weapon hit and returns the new combo count. */
    public int hit(UUID attacker, long now) {
        long last = lastHit.getOrDefault(attacker, -100000L);
        int c = (now - last <= OblivionCombat.config().comboWindowTicks) ? combo.getOrDefault(attacker, 0) + 1 : 1;
        combo.put(attacker, c);
        lastHit.put(attacker, now);
        return c;
    }

    public int combo(UUID player, long now) {
        Long last = lastHit.get(player);
        if (last == null || now - last > OblivionCombat.config().comboWindowTicks) return 0;
        return combo.getOrDefault(player, 0);
    }

    /** Only call for player victims. */
    public void playerDamaged(UUID victim, UUID attacker, long now) {
        damagedBy.computeIfAbsent(victim, k -> new HashMap<>()).put(attacker, now);
        tagged.put(victim, now);
        tagged.put(attacker, now);
    }

    public boolean isTagged(UUID id, long now) {
        Long t = tagged.get(id);
        return t != null && now - t <= OblivionCombat.config().combatTagSeconds * 20L;
    }

    public int addKill(UUID killer) {
        return streak.merge(killer, 1, Integer::sum);
    }

    public int streak(UUID id) { return streak.getOrDefault(id, 0); }

    /** Players who damaged the victim recently, excluding the killer. */
    public List<UUID> assists(UUID victim, UUID killer, long now) {
        List<UUID> out = new ArrayList<>();
        Map<UUID, Long> m = damagedBy.get(victim);
        if (m == null) return out;
        long window = OblivionCombat.config().assistWindowSeconds * 20L;
        for (Map.Entry<UUID, Long> e : m.entrySet()) {
            if (!e.getKey().equals(killer) && now - e.getValue() <= window) out.add(e.getKey());
        }
        return out;
    }

    public void onDeath(UUID victim) {
        streak.remove(victim);
        combo.remove(victim);
        damagedBy.remove(victim);
        tagged.remove(victim);
    }

    /** Disconnect: drop everything except nothing that could be abused (tags/streak reset is fine). */
    public void clearTransient(UUID id) {
        combo.remove(id);
        lastHit.remove(id);
        damagedBy.remove(id);
        streak.remove(id);
    }

    public void purge(long now) {
        long window = Math.max(OblivionCombat.config().comboWindowTicks, 1200);
        lastHit.entrySet().removeIf(e -> now - e.getValue() > window);
        combo.keySet().removeIf(k -> !lastHit.containsKey(k));
        tagged.values().removeIf(t -> now - t > 1200);
        damagedBy.values().forEach(m -> m.values().removeIf(t -> now - t > 1200));
        damagedBy.values().removeIf(Map::isEmpty);
    }

    public void clearAll() {
        lastHit.clear(); combo.clear(); streak.clear(); damagedBy.clear(); tagged.clear();
    }
}
