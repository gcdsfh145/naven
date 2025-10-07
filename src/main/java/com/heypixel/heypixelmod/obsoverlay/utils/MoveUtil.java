package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;

public class MoveUtil {

    public static double getBaseMoveSpeed() {
        return getBaseMoveSpeed(0.2873, 0.2);
    }

    public static double getBaseMoveSpeed(double value, double effect) {
        double baseSpeed = value;
        if (mc.player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            baseSpeed = value * (1.0D + effect * (double)(mc.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1));
        }
        return baseSpeed;
    }

    public static boolean isBlockUnder() {
        return isBlockUnder(1.0);
    }

    public static boolean isBlockUnder(double distance) {
        for (int i = 1; i <= distance; i++) {
            BlockPos pos = new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() - i), (int) mc.player.getZ());
            if (!mc.level.getBlockState(pos).isAir()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVecOverVoid(double posX, double posY, double posZ) {
        Level world = mc.level;
        if (world == null) return false;

        while (posY > 0) {
            Vec3 posBefore = new Vec3(posX, posY, posZ);
            Vec3 posAfter = new Vec3(posX, posY - 1, posZ);

            BlockHitResult result = world.clip(new ClipContext(posBefore, posAfter,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

            if (result.getType() != HitResult.Type.MISS) {
                return false;
            }

            posY -= 1;
        }

        return true;
    }
}