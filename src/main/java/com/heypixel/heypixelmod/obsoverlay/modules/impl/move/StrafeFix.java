package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveInputEvent;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import jnic.JNICInclude;
import net.minecraft.util.Mth;

@JNICInclude
@ModuleInfo(
        name = "StrafeFix",
        description = "Fix you speed",
        category = Category.MOVEMENT
)
public class StrafeFix extends Module {
    private static StrafeFix INSTANCE;

    public StrafeFix() {
        INSTANCE = this;
    }

    public static void FixMove(MoveInputEvent event) {
        if (RotationUtils.targetRotation != null) {
            float f = event.getForward();
            float f1 = event.getStrafe();
            double d0 = (double) Mth.wrapDegrees((float) Math.toDegrees(direction(mc.player.getYRot(), (double) f, (double) f1)));
            if (f == 0.0F && f1 == 0.0F) {
                return;
            }

            float f2 = 0.0F;
            float f3 = 0.0F;
            float f4 = Float.MAX_VALUE;

            for (float f5 = -1.0F; f5 <= 1.0F; f5++) {
                for (float f6 = -1.0F; f6 <= 1.0F; f6++) {
                    if (f6 != 0.0F || f5 != 0.0F) {
                        double d1 = (double) Mth.wrapDegrees((float) Math.toDegrees(direction(RotationUtils.targetRotation.getYaw(), (double) f5, (double) f6)));
                        double d2 = Math.abs(d0 - d1);
                        if (d2 < (double) f4) {
                            f4 = (float) d2;
                            f2 = f5;
                            f3 = f6;
                        }
                    }
                }
            }

            event.setForward(f2);
            event.setStrafe(f3);
        }
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0F;
        }

        float f = 1.0F;
        if (moveForward < 0.0) {
            f = -0.5F;
        } else if (moveForward > 0.0) {
            f = 0.5F;
        }

        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0F * f;
        }

        if (moveStrafing < 0.0) {
            rotationYaw += 90.0F * f;
        }

        return Math.toRadians((double) rotationYaw);
    }

    @EventTarget
    private void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            FixMove(event);
        }
    }

    public static StrafeFix getInstance() {
        return INSTANCE;
    }
}