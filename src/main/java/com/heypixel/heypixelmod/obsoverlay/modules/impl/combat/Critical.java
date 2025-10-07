package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.StrafeEvent;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.network.NetworkEvent;

@ModuleInfo(
        name = "Critical",
        description = "Critical",
        category = Category.COMBAT
)
public class Critical extends Module {
    // 原有模式 + Grim 模式
    ModeValue mode = ValueBuilder.create(this, "mode")
            .setDefaultModeIndex(0)
            .setModes("Jump", "MiniJump", "Packet", "GrimStuck", "GrimTimer")
            .build()
            .getModeValue();

    // GrimStuck 专用设置
    BooleanValue fire = ValueBuilder.create(this, "Fire").setDefaultBooleanValue(false).build().getBooleanValue();

    // GrimTimer 专用设置
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

    // 原有字段
    private boolean attacking;
    private int criticalTicks = 0;
    private boolean shouldCritical = false;

    // 新增 Grim 字段
    private boolean start = false;
    private long lastMsTimer = 0;

    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix(mode.getCurrentMode());

        // 原有逻辑
        if (shouldCritical && criticalTicks > 0) {
            performCritical();
            criticalTicks--;

            if (criticalTicks <= 0) {
                shouldCritical = false;
            }
        }

        // 新增 Grim 逻辑
        if (mc.player == null || mc.level == null) return;

        switch (mode.getCurrentMode()) {
            case "GrimTimer":
                handleGrimTimerLogic();
                break;
            case "GrimStuck":
                handleGrimStuckLogic();
                break;
        }
    }

    @EventTarget
    public void onAttack(AttackEntityEvent event) {
        if (this.isEnabled()) {
            attacking = true;

            if (event.getTarget() instanceof net.minecraft.world.entity.player.Player) {
                shouldCritical = true;
                criticalTicks = 3;
            }

            // Grim 模式攻击检测
            if (mode.getCurrentMode().equals("GrimStuck") || mode.getCurrentMode().equals("GrimTimer")) {
                start = true;
            }
        }
    }

    @EventTarget
    public void onPacket(NetworkEvent.ServerCustomPayloadEvent event) {
        if (event.getPayload() instanceof ServerboundInteractPacket packet && this.isEnabled()) {
            if (WrapperUtils.isAttackAction(packet)) {
                start = true;
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event){
        if (attacking && mc.player.onGround()) {
            switch (mode.getCurrentMode()) {
                case "Jump":
                    mc.player.jumpFromGround();
                    break;
                case "MiniJump":
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.25, mc.player.getDeltaMovement().z);
                    break;
                case "Packet":
                    mc.player.setOnGround(false);
                    break;
                // Grim 模式不在此处理
            }
            attacking = false;
        }
    }

    private void performCritical() {
        if (mc.player.onGround()) {
            switch (mode.getCurrentMode()) {
                case "Jump":
                    mc.player.jumpFromGround();
                    break;
                case "MiniJump":
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.25, mc.player.getDeltaMovement().z);
                    break;
                case "Packet":
                    mc.player.setOnGround(false);
                    break;
                // Grim 模式不在此处理
            }
        }
    }

    private void handleGrimStuckLogic() {
        // 火焰粒子效果
        if (fire.getValue() && start && WrapperUtils.getOffGroundTicks() > 3) {
            if (System.currentTimeMillis() - lastMsTimer >= 500L) {
                for (int i = 0; i <= 8; i++) {
                    if (KillAura.target != null) {
                        WrapperUtils.spawnLegacyParticles(KillAura.target, ParticleTypes.FLAME);
                    }
                }
                lastMsTimer = System.currentTimeMillis();
            }
        }

        // 位置同步逻辑
        if (KillAura.target != null && start) {
            if (mc.player.fallDistance > 0 || WrapperUtils.getOffGroundTicks() > 3) {
                double d0 = mc.player.getX() - WrapperUtils.getXLast();
                double d1 = mc.player.getY() - WrapperUtils.getYLast();
                double d2 = mc.player.getZ() - WrapperUtils.getZLast();
                double d3 = (KillAura.INSTANCE.rotation_[0] - WrapperUtils.getYRotLast());
                double d4 = (KillAura.INSTANCE.rotation_[1] - WrapperUtils.getXRotLast());

                boolean positionChanged = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D;
                boolean rotationChanged = d3 != 0.0D || d4 != 0.0D;

                if (positionChanged && rotationChanged) {
                    sendPositionPacket(ServerboundMovePlayerPacket.PosRot.class,
                            mc.player.getX(), mc.player.getY() - 0.03, mc.player.getZ(),
                            KillAura.INSTANCE.rotation_[0], KillAura.INSTANCE.rotation_[1]);
                } else if (positionChanged) {
                    sendPositionPacket(ServerboundMovePlayerPacket.Pos.class,
                            mc.player.getX(), mc.player.getY() - 0.03, mc.player.getZ());
                } else if (rotationChanged) {
                    sendPositionPacket(ServerboundMovePlayerPacket.Rot.class,
                            KillAura.INSTANCE.rotation_[0], KillAura.INSTANCE.rotation_[1]);
                } else {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false));
                }
                WrapperUtils.setSkipTicks(WrapperUtils.getSkipTicks() + 1);
            }
        } else {
            start = false;
        }
    }

    private void handleGrimTimerLogic() {
        // 地面自动跳跃
        if (KillAura.target != null && mc.player.onGround()) {
            if (autoJump.getValue()) {
                mc.options.keyJump.setDown(true);
            }
            WrapperUtils.setMsPerTick(WrapperUtils.getTimer(), 50F);
        }

        // 空中速度控制
        if (KillAura.target != null && !mc.player.onGround()) {
            mc.options.keySprint.setDown(false);
            double fallDistance = mc.player.fallDistance;
            double maxFallDistance = getMaxFallDistance();

            if (fallDistance > 0 && fallDistance < maxFallDistance / 2) {
                WrapperUtils.setMsPerTick(WrapperUtils.getTimer(), idleTime_Value.getValue());
                if (C03.getValue()) sendC03Packet(true);
            } else if (fallDistance >= maxFallDistance / downHeight_Value.getValue()) {
                WrapperUtils.setMsPerTick(WrapperUtils.getTimer(), 50F);
            }
            if (C03.getValue()) sendC03Packet(false);
        }

        // 状态恢复
        if (mc.player.onGround() || mc.player.hurtTime > 0) {
            WrapperUtils.setMsPerTick(WrapperUtils.getTimer(), 50F);
        }
    }

    private double getMaxFallDistance() {
        // 简单的最大下落距离计算，可以根据需要调整
        return 3.0;
    }

    private void sendC03Packet(boolean onGround) {
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYRot(),
                mc.player.getXRot(),
                onGround
        ));
    }

    private void sendPositionPacket(Class<?> packetType, Object... params) {
        if (packetType == ServerboundMovePlayerPacket.PosRot.class) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    (Double)params[0], (Double)params[1], (Double)params[2],
                    (Float)params[3], (Float)params[4], false));
        } else if (packetType == ServerboundMovePlayerPacket.Pos.class) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    (Double)params[0], (Double)params[1], (Double)params[2], false));
        } else if (packetType == ServerboundMovePlayerPacket.Rot.class) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    (Float)params[0], (Float)params[1], false));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attacking = false;
        shouldCritical = false;
        criticalTicks = 0;
        start = false;

        // 重置 Grim 相关状态
        if (WrapperUtils.getTimer() != null) {
            WrapperUtils.setMsPerTick(WrapperUtils.getTimer(), 50F); // 重置计时器
        }

        // 重置按键状态
        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(true);
    }
}