package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name="VelocityNoXZ", description="Reduces Knock Back.", category=Category.COMBAT)
public class VelocityNoXZ extends Module {
    private final BooleanValue NoXZ = ValueBuilder.create(this, "NoXZ").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue attackCountValue = ValueBuilder.create(this, "Attack Count")
            .setDefaultFloatValue(2.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(12.0f)
            .setFloatStep(1.0f)
            .build()
            .getFloatValue();
    private final BooleanValue jumpReset = ValueBuilder.create(this, "Jump Reset").setDefaultBooleanValue(false).build().getBooleanValue();
    private final FloatValue jumpTick = ValueBuilder.create(this, "JumpResetTick").setDefaultFloatValue(0.0f).setMinFloatValue(0.0f).setMaxFloatValue(9.0f).setFloatStep(1.0f).build().getFloatValue();
    private final BooleanValue debugMessageValue = ValueBuilder.create(this, "Debug Message").setDefaultBooleanValue(false).build().getBooleanValue();
    private final FloatValue fovLimitValue = ValueBuilder.create(this, "FOV Limit").setDefaultFloatValue(45.0f).setMinFloatValue(30.0f).setMaxFloatValue(180.0f).setFloatStep(1.0f).build().getFloatValue();
    private final FloatValue speedThresholdValue = ValueBuilder.create(this, "Speed Threshold").setDefaultFloatValue(0.45f).setMinFloatValue(0.0f).setMaxFloatValue(1.0f).setFloatStep(0.01f).build().getFloatValue();

    private Entity targetEntity;
    private boolean velocityInput = false;
    private boolean attacked = false;
    private int jumpResetTicks = 0;
    private double currentKnockbackSpeed = 0.0;

    @Override
    public void onDisable() {
        this.velocityInput = false;
        this.attacked = false;
        this.jumpResetTicks = 0;
        this.targetEntity = null;
        this.currentKnockbackSpeed = 0.0;
    }

    @Override
    public void onEnable() {
        this.setSuffix("GrimNoXZ");
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSetEntityMotionPacket) {
            ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket) packet;
            if (velocityPacket.getId() != mc.player.getId()) {
                return;
            }

            double x = (double) velocityPacket.getXa() / 8000.0;
            double z = (double) velocityPacket.getZa() / 8000.0;
            this.currentKnockbackSpeed = Math.sqrt(x * x + z * z);

            float currentSpeedThreshold = this.speedThresholdValue.getCurrentValue();
            if (this.currentKnockbackSpeed < (double) currentSpeedThreshold) {
                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("Knockback too weak: " + String.format("%.2f", this.currentKnockbackSpeed) + " < " + String.format("%.2f", currentSpeedThreshold));
                }
                return;
            }

            this.velocityInput = true;
            this.targetEntity = Aura.target;

            boolean inFOV = false;
            if (this.targetEntity != null) {
                float currentFovLimit = this.fovLimitValue.getCurrentValue();
                Vec3 playerLookVec = mc.player.getLookAngle();
                Vec3 toTargetVec = new Vec3(this.targetEntity.getX() - mc.player.getX(), 0.0, this.targetEntity.getZ() - mc.player.getZ()).normalize();
                double dot = playerLookVec.x * toTargetVec.x + playerLookVec.z * toTargetVec.z;
                double angleRad = Math.acos(Mth.clamp(dot, -1.0, 1.0));
                double angleDeg = Math.toDegrees(angleRad);
                inFOV = angleDeg <= (double) currentFovLimit;
                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("FOV Check: " + String.format("%.1f°", angleDeg) + (inFOV ? " <= " + String.format("%.1f°", currentFovLimit) : " > " + String.format("%.1f°", currentFovLimit)));
                }
            }

            if (this.NoXZ.getCurrentValue() && this.targetEntity != null && inFOV) {
                boolean wasSprinting = mc.player.isSprinting();

                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));

                int attackCount = (int) this.attackCountValue.getCurrentValue();
                for (int i = 0; i < attackCount; i++) {
                    if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                        mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, mc.player.isShiftKeyDown()));
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    }
                }

                if (wasSprinting) {
                    mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                }

                this.attacked = true;

                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("Reduced knockback with hybrid strategy (" + attackCount + " attacks). Speed: " + String.format("%.2f", this.currentKnockbackSpeed));
                }
            }

            if (this.jumpReset.getCurrentValue()) {
                this.jumpResetTicks = (int) this.jumpTick.getCurrentValue();
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            return;
        }
        if (mc.player.hurtTime == 0) {
            this.velocityInput = false;
            this.currentKnockbackSpeed = 0.0;
        }
        if (this.jumpResetTicks > 0) {
            --this.jumpResetTicks;
        }
        if (this.velocityInput && this.attacked) {
            if (this.targetEntity != null && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.07776, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * 0.07776);
                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("Applied client-side velocity reduction");
                }
            }
            this.attacked = false;
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player != null && this.jumpReset.getCurrentValue() && mc.player.onGround() && this.jumpResetTicks == 1) {
            event.setJump(true);
            this.jumpResetTicks = 0;
            if (this.debugMessageValue.getCurrentValue()) {
                ChatUtils.addChatMessage("Jump reset activated");
            }
        }
    }
}