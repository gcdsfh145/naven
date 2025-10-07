package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMove;

import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;


public class PlayerUtil {
    public static void setSpeed(EventMove moveEvent, double moveSpeed, float pseudoYaw, double pseudoStrafe, double pseudoForward, boolean chase) {
        double forward = pseudoForward;
        double strafe = pseudoStrafe;
        float yaw = pseudoYaw;
        if(pseudoForward == 0.0D && pseudoStrafe == 0.0D) {
            moveEvent.setZ(0.0D);
            moveEvent.setX(0.0D);
        } else {
            if(pseudoForward != 0.0D) {
                if (!chase) {
                    if (pseudoStrafe > 0.0D) {
                        yaw = pseudoYaw + (float) (pseudoForward > 0.0D ? -44 : 44);
                    } else if (pseudoStrafe < 0.0D) {
                        yaw = pseudoYaw + (float) (pseudoForward > 0.0D ? 44 : -44);
                    }
                }
                strafe = 0.0D;
                if(pseudoForward > 0.0D) {
                    forward = 1.0D;
                } else if(pseudoForward < 0.0D) {
                    forward = -1.0D;
                }
            }

            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            moveEvent.setX(forward * moveSpeed * cos + strafe * moveSpeed * sin);
            moveEvent.setZ(forward * moveSpeed * sin - strafe * moveSpeed * cos);
        }
    }


    public void setSpeedWithoutEvent(double moveSpeed, float pseudoYaw, double pseudoStrafe, double pseudoForward, boolean chase) {
        double forward = pseudoForward;
        double strafe = pseudoStrafe;
        float yaw = pseudoYaw;
        if(pseudoForward == 0.0D && pseudoStrafe == 0.0D) {
            this.motionX = (0.0D);
            this.motionZ = (0.0D);
        } else {
            if(pseudoForward != 0.0D) {
                if (!chase) {
                    if (pseudoStrafe > 0.0D) {
                        yaw = pseudoYaw + (float) (pseudoForward > 0.0D ? -45 : 45);
                    } else if (pseudoStrafe < 0.0D) {
                        yaw = pseudoYaw + (float) (pseudoForward > 0.0D ? 45 : -45);
                    }
                }
                strafe = 0.0D;
                if(pseudoForward > 0.0D) {
                    forward = 1.0D;
                } else if(pseudoForward < 0.0D) {
                    forward = -1.0D;
                }
            }

            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            this.motionX = (forward * moveSpeed * cos + strafe * moveSpeed * sin);
            this.motionZ = (forward * moveSpeed * sin - strafe * moveSpeed * cos);
        }
    }
    public double motionX;
    public double motionZ;
}
