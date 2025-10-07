package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.world.entity.LivingEntity;

import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;

public class MoveUtilss {
    private static double x = 0.0;
    private static double y = 0.0;
    private static double z = 0.0;

    public static void stuckMove() {
        if (mc.player != null) {
            x = mc.player.getDeltaMovement().x;
            y = mc.player.getDeltaMovement().y;
            z = mc.player.getDeltaMovement().z;
        }
    }

    public static void resMove() {
        if (mc.player != null) {
            mc.player.setDeltaMovement(x, y, z);
            x = 0.0;
            y = 0.0;
            z = 0.0;
        }
    }

    public static void stopMove() {
        if (mc.player != null) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
        }
    }

    public static void strafe(float speed) {
        if (mc.player != null && isMoving()) {
            double d0 = getDirection();
            mc.player.setDeltaMovement(-Math.sin(d0) * (double) speed, mc.player.getDeltaMovement().y, Math.cos(d0) * (double) speed);
        }
    }

    public static float getSpeed(LivingEntity e) {
        double d0 = e.getX() - e.xOld;
        double d1 = e.getZ() - e.zOld;
        return (float) Math.sqrt(d0 * d0 + d1 * d1);
    }

    public static double getDirection() {
        float f = mc.player.getYRot();
        if (mc.player.input.forwardImpulse < 0.0F) {
            f += 180.0F;
        }

        float f1 = 1.0F;
        if (mc.player.input.forwardImpulse < 0.0F) {
            f1 = -0.5F;
        } else if (mc.player.input.forwardImpulse > 0.0F) {
            f1 = 0.5F;
        }

        if (mc.player.input.leftImpulse > 0.0F) {
            f -= 90.0F * f1;
        }

        if (mc.player.input.leftImpulse < 0.0F) {
            f += 90.0F * f1;
        }

        return Math.toRadians((double) f);
    }

    public static boolean isMoving() {
        return mc.player != null
                && (mc.player.input.forwardImpulse != 0.0F || mc.player.input.leftImpulse != 0.0F);
    }
}

