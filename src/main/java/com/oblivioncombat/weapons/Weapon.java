package com.oblivioncombat.weapons;

import com.oblivioncombat.OblivionCombat;
import com.oblivioncombat.util.Compat;
import com.oblivioncombat.abilities.AbilityInfo;
import com.oblivioncombat.abilities.Trigger;
import com.oblivioncombat.config.OblivionConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public abstract class Weapon {
    public static final String TAG = "oblivion_weapon";

    protected final String id;
    protected final String name;
    protected final Item baseItem;
    protected final Formatting color;
    protected final String blurb;

    protected Weapon(String id, String name, Item baseItem, Formatting color, String blurb) {
        this.id = id; this.name = name; this.baseItem = baseItem; this.color = color; this.blurb = blurb;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Formatting color() { return color; }
    public abstract Rarity rarity();
    public abstract boolean enabled();
    public abstract double attackDamage();
    public abstract double attackSpeed();
    public abstract List<AbilityInfo> abilities();

    /** @return true when the click was consumed by an ability (cancels the vanilla action). */
    public abstract boolean activate(ServerPlayerEntity p, ServerWorld w, Trigger t, LivingEntity target);

    public void onHit(ServerPlayerEntity attacker, LivingEntity victim, ServerWorld w, int combo) {}
    public void onKill(ServerPlayerEntity attacker, LivingEntity victim, ServerWorld w) {}

    // ---- cooldown helpers ----
    protected boolean ready(ServerPlayerEntity p, AbilityInfo ab) {
        OblivionConfig cfg = OblivionCombat.config();
        if (cfg.creativeBypassCooldown && p.isCreative()) return true;
        long now = Compat.server(p).getTicks();
        long left = OblivionCombat.cooldowns().remainingTicks(p.getUuid(), id + ":" + ab.id(), now);
        if (left <= 0) return true;
        String msg = cfg.messages.onCooldown
                .replace("%ability%", ab.name())
                .replace("%time%", String.format("%.1f", left / 20.0));
        p.sendMessage(OblivionConfig.text(msg), true);
        return false;
    }

    protected void startCooldown(ServerPlayerEntity p, AbilityInfo ab) {
        if (OblivionCombat.config().creativeBypassCooldown && p.isCreative()) return;
        OblivionCombat.cooldowns().set(p.getUuid(), id + ":" + ab.id(), Compat.server(p).getTicks(),
                Math.round(ab.cooldownSeconds().getAsDouble() * 20));
    }

    // ---- item ----
    public Text displayName() {
        return Text.literal(name).styled(s -> s.withColor(color).withBold(true).withItalic(false));
    }

    private static MutableText plain(String s, Formatting f) {
        return Text.literal(s).styled(st -> st.withColor(f).withItalic(false));
    }

    public ItemStack createStack() {
        ItemStack s = new ItemStack(baseItem);
        s.set(DataComponentTypes.CUSTOM_NAME, displayName());

        List<Text> lore = new ArrayList<>();
        lore.add(plain(blurb, Formatting.GRAY));
        lore.add(Text.empty());
        lore.add(plain("Abilities:", Formatting.LIGHT_PURPLE));
        for (AbilityInfo ab : abilities()) {
            lore.add(plain("\u2022 " + ab.name(), Formatting.GRAY)
                    .append(plain(" (" + ab.trigger().label() + ")", Formatting.DARK_GRAY)));
        }
        lore.add(Text.empty());
        lore.add(plain("Rarity: ", Formatting.DARK_GRAY).append(plain(rarity().label(), rarity().color())));
        s.set(DataComponentTypes.LORE, new LoreComponent(lore));

        NbtCompound tag = new NbtCompound();
        tag.putString(TAG, id);
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        s.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(Identifier.ofVanilla("base_attack_damage"),
                                attackDamage() - 1.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(Identifier.ofVanilla("base_attack_speed"),
                                attackSpeed() - 4.0, EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .build());
        return s;
    }
}
