package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;

@ModuleInfo(
        name = "NoSlow",
        description = "NoSlowDown with GrimTimer",
        category = Category.MOVEMENT
)
public class NoSlow extends Module {
    ModeValue mode = ValueBuilder.create(this, "mode")
            .setDefaultModeIndex(0)
            .setModes("GrimTimer")
            .build()
            .getModeValue();

    FloatValue idleTime_Value = ValueBuilder.create(this, "Idle Time")
            .setDefaultFloatValue(250.0F)
            .setFloatStep(10.0F)
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(500.0F)
            .build()
            .getFloatValue();

    FloatValue downHeight_Value = ValueBuilder.create(this, "Down Height")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    BooleanValue autoJump = ValueBuilder.create(this, "Auto Jump").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue C03 = ValueBuilder.create(this, "Send C03").setDefaultBooleanValue(true).build().getBooleanValue();

    BooleanValue goldenAppleCheck = ValueBuilder.create(this, "Only Golden Apple")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private static final Minecraft mc = Minecraft.getInstance();
    private long lastMsTimer = 0;
    private boolean isActive = false;

    public static Timer getTimer() {
        if (mc == null) return null;
        try {
            Field timerField = Minecraft.class.getDeclaredField("timer");
            timerField.setAccessible(true);
            return (Timer) timerField.get(mc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setMsPerTick(Timer timer, float msPerTick) {
        if (timer == null) return;
        try {
            Field msPerTickField = Timer.class.getDeclaredField("msPerTick");
            msPerTickField.setAccessible(true);
            msPerTickField.setFloat(timer, msPerTick);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isUsingGoldenApple() {
        if (mc.player == null) return false;

        if (mc.player.isUsingItem()) {
            Item useItem = mc.player.getUseItem().getItem();
            return useItem == Items.GOLDEN_APPLE || useItem == Items.ENCHANTED_GOLDEN_APPLE;
        }

        return false;
    }

    private boolean shouldActivateGrimTimer() {
        if (!mode.getCurrentMode().equals("GrimTimer")) {
            return false;
        }

        if (goldenAppleCheck.getCurrentValue()) {
            return isUsingGoldenApple();
        }

        return true;
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix(mode.getCurrentMode());

        if (mc.player == null || mc.level == null) return;

        boolean shouldActivate = shouldActivateGrimTimer();

        if (shouldActivate) {
            if (!isActive) {
                // 刚开始激活
                isActive = true;
                if (autoJump.getCurrentValue() && mc.player.onGround()) {
                    mc.options.keyJump.setDown(true);
                }
            }
            handleGrimTimerLogic();
        } else {
            if (isActive) {
                deactivateGrimTimer();
            }
        }
    }

    private void handleGrimTimerLogic() {
        if (!isActive) return;

        Timer timer = getTimer();
        if (timer == null) return;

        if (mc.player.onGround()) {
            if (autoJump.getCurrentValue()) {
                mc.options.keyJump.setDown(true);
            }
            setMsPerTick(timer, 50F);
        }

        if (!mc.player.onGround()) {
            mc.options.keySprint.setDown(false);
            double fallDistance = mc.player.fallDistance;
            double maxFallDistance = getMaxFallDistance();

            if (fallDistance > 0 && fallDistance < maxFallDistance / 2) {
                setMsPerTick(timer, idleTime_Value.getCurrentValue());
                if (C03.getCurrentValue()) sendC03Packet(true);
            } else if (fallDistance >= maxFallDistance / downHeight_Value.getCurrentValue()) {
                setMsPerTick(timer, 50F);
            }

            if (C03.getCurrentValue()) sendC03Packet(false);
        }

        if (mc.player.onGround() || mc.player.hurtTime > 0) {
            setMsPerTick(timer, 50F);
        }
    }

    private void deactivateGrimTimer() {
        isActive = false;
        Timer timer = getTimer();
        if (timer != null) {
            setMsPerTick(timer, 50F);
        }
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(true);
    }

    private double getMaxFallDistance() {
        return 3.0;
    }

    private void sendC03Packet(boolean onGround) {
        if (mc.getConnection() != null && mc.player != null) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    onGround
            ));
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        isActive = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        deactivateGrimTimer();
    }
}