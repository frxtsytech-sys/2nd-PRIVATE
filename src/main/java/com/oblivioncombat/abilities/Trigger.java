package com.oblivioncombat.abilities;

/** How an ability is activated. Left-click on air is not reported by vanilla clients, so it is not offered. */
public enum Trigger {
    RIGHT_CLICK("Right-click"),
    SNEAK_RIGHT_CLICK("Sneak + Right-click"),
    LEFT_CLICK("Left-click (on entity)"),
    SNEAK_LEFT_CLICK("Sneak + Left-click (on entity)"),
    PASSIVE("Passive"),
    ON_KILL("On kill");

    private final String label;
    Trigger(String label) { this.label = label; }
    public String label() { return label; }
}
