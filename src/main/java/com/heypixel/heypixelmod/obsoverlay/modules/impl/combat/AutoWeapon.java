package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttack;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantments;

@ModuleInfo(name="AutoWeapon", description="Automatically switches to preferred weapon type when attacking entities", category=Category.COMBAT  )
public class AutoWeapon extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private WeaponType selectedWeapon = WeaponType.NONE;
    private final List<WeaponSlotInfo> weaponSlots = new ArrayList<>();
    private boolean wasRightButtonDown = false;
    private final BooleanValue enableSword = ValueBuilder.create(this, "Enable Sword").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue enableSharpAxe = ValueBuilder.create(this, "Enable Sharp Axe").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue enableSlimeball = ValueBuilder.create(this, "Enable Slimeball").setDefaultBooleanValue(true).build().getBooleanValue();
    @Override
    public String getSuffix() {
        if (this.selectedWeapon != null) {
            return this.selectedWeapon.toString();
        }
        WeaponType bestWeapon = this.getBestWeaponType();
        return bestWeapon != null ? bestWeapon.toString() : "None";
    }
    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        this.weaponSlots.clear();
        this.scanInventoryForWeapons();
        this.handleRightClick();
        if (this.selectedWeapon == null) {
            this.selectedWeapon = this.getBestWeaponType();
        }
    }
    @EventTarget
    public void onAttack(EventAttack event) {
        if (this.selectedWeapon == WeaponType.NONE) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof LivingEntity) {
            this.switchToPreferredWeapon();
        }
    }
    private void scanInventoryForWeapons() {
        for (int slot = 0; slot < 36; ++slot) {
            float damage;
            ItemStack stack = this.mc.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;

            if (this.isSword(stack)) {
                damage = this.calculateWeaponDamage(stack);
                this.weaponSlots.add(new WeaponSlotInfo(WeaponType.SWORD, slot, damage));
                continue;
            }
            if (this.isSharpAxe(stack)) {
                damage = this.calculateWeaponDamage(stack);
                this.weaponSlots.add(new WeaponSlotInfo(WeaponType.SHARP_AXE, slot, damage));
                continue;
            }
            if (!this.isKnockbackSlimeball(stack)) continue;
            this.weaponSlots.add(new WeaponSlotInfo(WeaponType.KNOCKBACK_SLIMEBALL, slot, 0.0f));
        }
    }
    private float calculateWeaponDamage(ItemStack stack) {
        float baseDamage = 0.0f;
        if (stack.getItem() instanceof SwordItem) {
            baseDamage = ((SwordItem) stack.getItem()).getDamage();
        } else
            if (stack.getItem() instanceof AxeItem) {
          baseDamage = ((AxeItem) stack.getItem()).getAttackDamage();
        }
        int sharpnessLevel = stack.getEnchantmentLevel(Enchantments.SHARPNESS);
        if (sharpnessLevel > 0) {
            baseDamage += 1.0f + (float) Math.max(0, sharpnessLevel - 1) * 0.5f;
        }
        return baseDamage;
    }
    private void handleRightClick() {
        boolean isRightDown = this.mc.player.isUsingItem();
        if (isRightDown && !this.wasRightButtonDown) {
            this.cycleWeaponSelection();
        }
        this.wasRightButtonDown = isRightDown;
    }
    private void cycleWeaponSelection() {
        ArrayList<WeaponType> enabledTypes = new ArrayList<>();
        enabledTypes.add(WeaponType.NONE);
        if (this.enableSword.getCurrentValue()) {
            enabledTypes.add(WeaponType.SWORD);
        }
        if (this.enableSharpAxe.getCurrentValue()) {
            enabledTypes.add(WeaponType.SHARP_AXE);
        }
        if (this.enableSlimeball.getCurrentValue()) {
            enabledTypes.add(WeaponType.KNOCKBACK_SLIMEBALL);
        }
        if (enabledTypes.isEmpty()) {
            this.selectedWeapon = WeaponType.NONE;
            return;
        }
        if (this.selectedWeapon == null) {
            this.selectedWeapon = enabledTypes.get(0);
            return;
        }
        int currentIndex = enabledTypes.indexOf(this.selectedWeapon);
        this.selectedWeapon = currentIndex == -1 || currentIndex == enabledTypes.size() - 1 ? enabledTypes.get(0) : enabledTypes.get(currentIndex + 1);
    }
    private WeaponType getBestWeaponType() {
        if (this.selectedWeapon == WeaponType.NONE) {
            return WeaponType.NONE;
        }
        if (this.selectedWeapon != null && this.isWeaponAvailable(this.selectedWeapon)) {
            return this.selectedWeapon;
        }
        if (this.enableSword.getCurrentValue() && this.isWeaponAvailable(WeaponType.SWORD)) {
            return WeaponType.SWORD;
        }
        if (this.enableSharpAxe.getCurrentValue() && this.isWeaponAvailable(WeaponType.SHARP_AXE)) {
            return WeaponType.SHARP_AXE;
        }
        if (this.enableSlimeball.getCurrentValue() && this.isWeaponAvailable(WeaponType.KNOCKBACK_SLIMEBALL)) {
            return WeaponType.KNOCKBACK_SLIMEBALL;
        }
        return WeaponType.NONE;
    }
    private boolean isWeaponAvailable(WeaponType type) {
        if (type == WeaponType.NONE) {
            return true;
        }
        for (WeaponSlotInfo info : this.weaponSlots) {
            if (info.type != type) continue;
            return true;
        }
        return false;
    }
    public void switchToPreferredWeapon() {
        WeaponType currentType = this.getBestWeaponType();
        if (currentType == null || currentType == WeaponType.NONE) {
            return;
        }
        ItemStack currentItem = this.mc.player.getMainHandItem();
        if (this.isPreferredWeapon(currentItem, currentType)) {
            return;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = this.mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !this.isPreferredWeapon(stack, currentType)) continue;
            this.mc.player.getInventory().selected = slot;
            return;
        }
        this.moveWeaponFromInventory(currentType);
    }
    private void moveWeaponFromInventory(WeaponType type) {
        if (type == WeaponType.NONE) {
            return;
        }
        WeaponSlotInfo bestWeapon = null;
        for (WeaponSlotInfo info : this.weaponSlots) {
            if (info.type != type || bestWeapon != null && !(info.damage > bestWeapon.damage)) continue;
            bestWeapon = info;
        }
        if (bestWeapon == null) {
            return;
        }
        int weaponSlot = bestWeapon.slot;
        if (weaponSlot < 9) {
            this.mc.player.getInventory().selected = weaponSlot;
            return;
        }
        int emptySlot = this.findEmptyHotbarSlot();
        if (emptySlot != -1) {
            this.swapItems(weaponSlot, emptySlot);
            this.mc.player.getInventory().selected = emptySlot;
            return;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = this.mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || this.isPreferredWeapon(stack, type)) continue;
            this.swapItems(weaponSlot, slot);
            this.mc.player.getInventory().selected = slot;
            return;
        }
    }
    private void swapItems(int slot1, int slot2) {
        ItemStack temp = this.mc.player.getInventory().getItem(slot1);
        this.mc.player.getInventory().setItem(slot1, this.mc.player.getInventory().getItem(slot2));
        this.mc.player.getInventory().setItem(slot2, temp);
    }
    private int findEmptyHotbarSlot() {
        for (int slot = 0; slot < 9; ++slot) {
            if (!this.mc.player.getInventory().getItem(slot).isEmpty()) continue;
            return slot;
        }
        return -1;
    }
    private boolean isPreferredWeapon(ItemStack stack, WeaponType type) {
        switch (type) {
            case NONE: {
                return true;
            }
            case SWORD: {
                return this.isSword(stack);
            }
            case SHARP_AXE: {
                return this.isSharpAxe(stack);
            }
            case KNOCKBACK_SLIMEBALL: {
                return this.isKnockbackSlimeball(stack);
            }
        }
        return false;
    }
    private boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }
    private boolean isSharpAxe(ItemStack stack) {
        return stack.getItem() instanceof AxeItem && stack.getEnchantmentLevel(Enchantments.SHARPNESS) > 0;
    }
    private boolean isKnockbackSlimeball(ItemStack stack) {
        return stack.getItem() == Items.SLIME_BALL && stack.getEnchantmentLevel(Enchantments.KNOCKBACK) > 0;
    }
    public static enum WeaponType {
        NONE("None"),
        SWORD("Sword"),
        SHARP_AXE("Sharpness Axe"),
        KNOCKBACK_SLIMEBALL("Knockback Slimeball");

        private final String name;

        private WeaponType(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }
    private static class WeaponSlotInfo {
        final WeaponType type;
        final int slot;
        final float damage;

        WeaponSlotInfo(WeaponType type, int slot, float damage) {
            this.type = type;
            this.slot = slot;
            this.damage = damage;
        }
    }
}
