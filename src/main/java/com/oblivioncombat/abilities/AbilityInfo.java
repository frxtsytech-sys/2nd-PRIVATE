package com.oblivioncombat.abilities;

import java.util.function.DoubleSupplier;

/** Describes one ability for lore, HUD and /oblivion abilities. Cooldown is read live from config. */
public record AbilityInfo(String id, String name, Trigger trigger, String description, DoubleSupplier cooldownSeconds) {}
