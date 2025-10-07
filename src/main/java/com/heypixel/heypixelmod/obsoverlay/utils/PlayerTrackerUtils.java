package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;

public class PlayerTrackerUtils {
    public static boolean isInLobby() {
        ClientLevel world = mc.level;
        if (world == null) return false;

        return world.players().stream().anyMatch(e -> e.getName().getString().contains("问题反馈")
                || e.getName().getString().contains("练习场")
                || e.getName().getString().contains("单人模式")
        );
    }

    public static boolean isHoldingGodAxe(Player player) {
        return isGodAxe(player.getMainHandItem()) || isGodAxe(player.getOffhandItem());
    }

    public static boolean isGodAxe(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.GOLDEN_AXE)) return false;

        int durability = stack.getMaxDamage() - stack.getDamageValue();

        int sharpnessLevel = getEnchantmentLevel(stack);

        return durability <= 2 && sharpnessLevel > 20;
    }

    private static int getEnchantmentLevel(ItemStack stack) {
        ListTag enchantmentTagList = stack.getEnchantmentTags();

        for (int i = 0; i < enchantmentTagList.size(); i++) {
            CompoundTag nbt = enchantmentTagList.getCompound(i);
            if (nbt.contains("id") && nbt.contains("lvl") &&
                    nbt.getString("id").equals("minecraft:sharpness")) {
                return nbt.getInt("lvl");
            }
        }
        return 0;
    }

    public static boolean isHoldingSlimeball(Player player) {
        return player.getMainHandItem().is(Items.SLIME_BALL) || player.getOffhandItem().is(Items.SLIME_BALL);
    }

    public static boolean isHoldingTotemo(Player player) {
        return player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
    }

    public static boolean isHoldingCrossbow(Player player) {
        return player.getMainHandItem().is(Items.CROSSBOW) || player.getOffhandItem().is(Items.CROSSBOW);
    }

    public static boolean isHoldingBow(Player player) {
        return player.getMainHandItem().is(Items.BOW) || player.getOffhandItem().is(Items.BOW);
    }

    public static boolean isHoldingFireCharge(Player player) {
        return player.getMainHandItem().is(Items.FIRE_CHARGE) || player.getOffhandItem().is(Items.FIRE_CHARGE);
    }

    public static int isRegen(Player player) {
        MobEffectInstance regenPotion = player.getEffect(MobEffects.REGENERATION);
        return regenPotion == null ? -1 : regenPotion.getDuration();
    }

    public static int isStrength(Player player) {
        MobEffectInstance strengthPotion = player.getEffect(MobEffects.DAMAGE_BOOST);
        return strengthPotion == null ? -1 : strengthPotion.getDuration();
    }
}
