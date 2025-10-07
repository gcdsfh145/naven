package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import jnic.JNICInclude;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.List;

@JNICInclude
@ModuleInfo(
        name = "Speed",
        description = "Make you move speed faster",
        category = Category.MOVEMENT
)
public class Speed extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final ModeValue modeValue = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("GrimEntity", "Normal")
            .build()
            .getModeValue();
    private final ModeValue eventModeValue = ValueBuilder.create(this, "Event Mode")
            .setDefaultModeIndex(0)
            .setModes("Pre", "Post", "Motion", "Update", "Tick")
            .build()
            .getModeValue();
    private final BooleanValue noMove = ValueBuilder.create(this, "NoMove").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue targetStrafe = ValueBuilder.create(this, "TargetStrafe").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue targetStrafeOnlyKillaura = ValueBuilder.create(this, "TargetStrafeOnlyKillaura").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue noGroundTargetStrafe = ValueBuilder.create(this, "NoGroundTargetStrafe").setDefaultBooleanValue(false).build().getBooleanValue();
    private final FloatValue minSpeed = ValueBuilder.create(this, "MinSpeed").setDefaultFloatValue(0.3F).setFloatStep(0.1F).setMinFloatValue(0.0F).setMaxFloatValue(1.0F).build().getFloatValue();
    private final FloatValue speedXZ = ValueBuilder.create(this, "SpeedXZ").setDefaultFloatValue(0.3F).setFloatStep(0.1F).setMinFloatValue(0.0F).setMaxFloatValue(1.0F).build().getFloatValue();
    private final FloatValue speedY = ValueBuilder.create(this, "SpeedY").setDefaultFloatValue(0.6F).setFloatStep(0.1F).setMinFloatValue(0.0F).setMaxFloatValue(1.0F).build().getFloatValue();
    private final FloatValue speed = ValueBuilder.create(this, "Speed").setDefaultFloatValue(0.08F).setFloatStep(0.01F).setMinFloatValue(0.01F).setMaxFloatValue(0.15F).build().getFloatValue();

    @EventTarget
    private void onMotion(EventMotion event) {
        String eventMode = this.eventModeValue.getCurrentMode().toLowerCase();
        switch (eventMode) {
            case "pre":
                if (event.getType() == com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType.PRE) {
                    this.runSpeed();
                }
                break;
            case "post":
                if (event.getType() == com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType.POST) {
                    this.runSpeed();
                }
                break;
            case "motion":
                this.runSpeed();
                break;
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (this.eventModeValue.getCurrentMode().equalsIgnoreCase("update")) {
            this.runSpeed();
        }
    }

    @EventTarget
    private void onTick(TickEvent event) {
        if (this.eventModeValue.getCurrentMode().equalsIgnoreCase("tick")) {
            this.runSpeed();
        }
    }

    private void runSpeed() {
        if (!Naven.moduleManager.getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink.class).getState()
                && mc.player != null
                && mc.level != null
                && !mc.player.isSpectator()) {
            if (mc.player.input.forwardImpulse != 0.0F || mc.player.input.leftImpulse != 0.0F || !this.noMove.getCurrentValue()) {
                int entityCount = 0;
                AABB aabb = mc.player.getBoundingBox().inflate((double) this.speedXZ.getCurrentValue(), (double) this.speedY.getCurrentValue(), (double) this.speedXZ.getCurrentValue());
                List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, aabb);
                LivingEntity closestEntity = null;
                double closestDistance = Double.MAX_VALUE;

                for (LivingEntity entity : entities) {
                    if (entity.getId() != mc.player.getId() && !(entity instanceof ArmorStand)) {
                        entityCount++;
                        double distance = mc.player.distanceTo(entity);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestEntity = entity;
                        }
                    }
                }

                double direction;
                if (!this.targetStrafe.getCurrentValue() || closestEntity == null) {
                    direction = Math.toRadians((double) this.getMoveYaw(mc.player));
                } else if (mc.player.onGround() && this.noGroundTargetStrafe.getCurrentValue()) {
                    direction = Math.toRadians((double) this.getMoveYaw(mc.player));
                } else if (this.targetStrafeOnlyKillaura.getCurrentValue()) {
                    direction = Math.toRadians((double) this.getMoveYaw(mc.player));
                } else {
                    direction = Math.toRadians((double) this.getTargetStrafeYaw(mc.player, closestEntity));
                }

                double boostAmount;
                String speedMode = this.modeValue.getCurrentMode().toLowerCase();

                switch (speedMode) {
                    case "grimentity":
                        boostAmount = Math.min(entityCount, 3) * this.speed.getCurrentValue();
                        break;
                    case "normal":
                    default:
                        boostAmount = Math.min(1, 3) * this.speed.getCurrentValue();
                        break;
                }

                Vec3 velocity = new Vec3(-Math.sin(direction) * boostAmount, 0.0, Math.cos(direction) * boostAmount);
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(velocity));
            }
        }
    }

    private float getMoveYaw(net.minecraft.client.player.LocalPlayer player) {
        float yaw = player.getYRot();
        if (player.input.forwardImpulse != 0.0F && player.input.leftImpulse == 0.0F) {
            yaw += player.input.forwardImpulse > 0.0F ? 0.0F : 180.0F;
        } else if (player.input.forwardImpulse != 0.0F) {
            if (player.input.forwardImpulse > 0.0F) {
                yaw += player.input.leftImpulse > 0.0F ? -45.0F : 45.0F;
            } else {
                yaw -= player.input.leftImpulse > 0.0F ? -45.0F : 45.0F;
            }
            yaw += player.input.forwardImpulse > 0.0F ? 0.0F : 180.0F;
        } else if (player.input.leftImpulse != 0.0F) {
            yaw += player.input.leftImpulse > 0.0F ? -90.0F : 90.0F;
        }

        return yaw;
    }

    private float getTargetStrafeYaw(net.minecraft.client.player.LocalPlayer player, LivingEntity target) {
        double deltaX = target.getX() - player.getX();
        double deltaZ = target.getZ() - player.getZ();
        return deltaX * deltaX + deltaZ * deltaZ < (double) (this.minSpeed.getCurrentValue() * this.minSpeed.getCurrentValue())
                ? (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX) + (Math.PI / 2)) - 90.0)
                : (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI) - 90.0);
    }
}