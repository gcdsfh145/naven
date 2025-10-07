package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EffectsUtil {
    public static int isRegen(Player player) {
        return player.getEffect(MobEffects.REGENERATION) != null ? player.getEffect(MobEffects.REGENERATION).getDuration() : -1;
    }

    public static int isStrength(Player player) {
        return player.getEffect(MobEffects.DAMAGE_BOOST) != null ? player.getEffect(MobEffects.DAMAGE_BOOST).getDuration() : -1;
    }

    public static int isSpeed(Player player) {
        return player.getEffect(MobEffects.MOVEMENT_SPEED) != null ? player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() : -1;
    }

    public static int isJump(Player player) {
        return player.getEffect(MobEffects.JUMP) != null ? player.getEffect(MobEffects.JUMP).getDuration() : -1;
    }

    public static int isVanish(Player player) {
        return player.getEffect(MobEffects.INVISIBILITY) != null ? player.getEffect(MobEffects.INVISIBILITY).getDuration() : -1;
    }

    public static int isResistance(Player player) {
        return player.getEffect(MobEffects.DAMAGE_RESISTANCE) != null ? player.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() : -1;
    }

    public static boolean isHoldingGodAxe(Player player) {
        return isGodAxe(player.getItemBySlot(EquipmentSlot.MAINHAND));
    }

    public static boolean isHoldingAxe(Player player) {
        return isAxe(player.getItemBySlot(EquipmentSlot.MAINHAND));
    }

    public static boolean isHoldingEnchantedGoldenApple(Player player) {
        ItemStack itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemstack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static boolean isKBPlusBool(Player player) {
        ItemStack itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        return itemstack.getItem() == Items.SLIME_BALL && itemstack.isEnchanted();
    }

    private static boolean isGodAxe(ItemStack stack) {
        if (stack != null && stack.getItem() == Items.GOLDEN_AXE) {
            for (int i = 0; i < stack.getEnchantmentTags().size(); i++) {
                CompoundTag compoundtag = stack.getEnchantmentTags().getCompound(i);
                if (compoundtag.contains("id") && compoundtag.contains("lvl") && compoundtag.getInt("id") == 16 && compoundtag.getInt("lvl") > 128) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private static boolean isAxe(ItemStack stack) {
        if (!(stack.getItem() instanceof AxeItem)) {
            return false;
        } else {
            for (int i = 0; i < stack.getEnchantmentTags().size(); i++) {
                CompoundTag compoundtag = stack.getEnchantmentTags().getCompound(i);
                if (compoundtag.contains("id") && compoundtag.contains("lvl") && compoundtag.getInt("id") == 16 && compoundtag.getInt("lvl") > 5) {
                    return true;
                }
            }

            return false;
        }
    }
}

