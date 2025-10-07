package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.MSTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.OldNaming;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import jnic.JNICInclude;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;

@JNICInclude
@ModuleInfo(
        name = "SuperKnockback",
        description = "WTap knockback enhancement",
        category = Category.COMBAT
)
public class SuperKnockback extends Module {
    private boolean wasSprinting = false;
    private int resetSprintTimer = 0;
    private int tick = 0;
    private boolean sprintLegit = true;
    private final Minecraft mc = Minecraft.getInstance();
    private final ModeValue modeValue = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("SprintPacket", "SneakPacket", "Packet", "W-Tap", "W-Tap2", "W-Tap3", "Legit")
            .build()
            .getModeValue();
    private final FloatValue hurtTimeValue = ValueBuilder.create(this, "HurtTime").setDefaultFloatValue(10).setMinFloatValue(0).setMaxFloatValue(10).setVisibility(() ->
            modeValue.isCurrentMode("SprintPacket") ||
                    modeValue.isCurrentMode("SneakPacket") ||
                    modeValue.isCurrentMode("Packet") ||
                    modeValue.isCurrentMode("W-Tap")).build().getFloatValue();
    private final ModeValue wTap3ModeValue = ValueBuilder.create(this, "W-Tap3 Mode")
            .setDefaultModeIndex(0)
            .setModes("Wtap", "Legit", "Packet")
            .setVisibility(() -> modeValue.isCurrentMode("W-Tap3"))
            .build()
            .getModeValue();
    private final BooleanValue onlyGround = ValueBuilder.create(this, "OnlyGround").setDefaultBooleanValue(false).setVisibility(() ->
            modeValue.isCurrentMode("SprintPacket") ||
                    modeValue.isCurrentMode("SneakPacket") ||
                    modeValue.isCurrentMode("Packet") ||
                    modeValue.isCurrentMode("W-Tap")).build().getBooleanValue();
    private final BooleanValue onlyMove = ValueBuilder.create(this, "OnlyMove").setDefaultBooleanValue(false).setVisibility(() ->
            modeValue.isCurrentMode("SprintPacket") ||
                    modeValue.isCurrentMode("SneakPacket") ||
                    modeValue.isCurrentMode("Packet") ||
                    modeValue.isCurrentMode("W-Tap")).build().getBooleanValue();
    private final BooleanValue noMove = ValueBuilder.create(this, "NoMove").setDefaultBooleanValue(false).setVisibility(() ->
            modeValue.isCurrentMode("SprintPacket") ||
                    modeValue.isCurrentMode("SneakPacket") ||
                    modeValue.isCurrentMode("Packet") ||
                    modeValue.isCurrentMode("W-Tap")).build().getBooleanValue();
    private final BooleanValue autoSetSprint = ValueBuilder.create(this, "AutoSetSprint").setDefaultBooleanValue(false).setVisibility(() ->
            modeValue.isCurrentMode("SprintPacket") ||
                    modeValue.isCurrentMode("SneakPacket") ||
                    modeValue.isCurrentMode("Packet") ||
                    modeValue.isCurrentMode("W-Tap")).build().getBooleanValue();
    private final FloatValue delayValue = ValueBuilder.create(this, "Delay").setDefaultFloatValue(100).setVisibility(() ->
            modeValue.isCurrentMode("SprintPacket") ||
                    modeValue.isCurrentMode("SneakPacket") ||
                    modeValue.isCurrentMode("Packet") ||
                    modeValue.isCurrentMode("W-Tap")).setMinFloatValue(0).setMaxFloatValue(500).build().getFloatValue();
    private final BooleanValue onlyGroundLegit = ValueBuilder.create(this, "OnlyGround").setDefaultBooleanValue(false).setVisibility(() -> modeValue.isCurrentMode("Legit")).build().getBooleanValue();
    private final BooleanValue onlyMoveLegit = ValueBuilder.create(this, "OnlyMove").setDefaultBooleanValue(true).setVisibility(() -> modeValue.isCurrentMode("Legit")).build().getBooleanValue();

    private final MSTimer timer = new MSTimer();
    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix(modeValue.getCurrentMode());
    }
    @Override
    public void onDisable() {
        resetSprintState();
        sprintLegit = true;
        if (mc.player != null) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onEnable() {
        resetSprintState();
        sprintLegit = true;
    }

    private void resetSprintState() {
        if (modeValue.isCurrentMode("W-Tap2")) {
            resetSprintTimer = 0;
            if (wasSprinting && mc.player != null) {
                mc.player.setSprinting(true);
            }
            wasSprinting = false;
        }
    }

    @EventTarget
    public void onAttack(EventAttackSlowdown event) {
        if (modeValue.isCurrentMode("W-Tap2")) {
            if (mc.player == null) return;

            wasSprinting = mc.player.isSprinting();

            if (wasSprinting) {
                mc.player.setSprinting(false);
                resetSprintTimer = 2;
            }
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (event.getTarget() instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity) event.getTarget();
            if (livingentity.hurtTime <= this.hurtTimeValue.getCurrentValue()
                    && mc.player != null
                    && this.timer.hasTimePassed((long) this.delayValue.getCurrentValue())
                    && (!this.onlyGround.getCurrentValue() || mc.player.onGround())
                    && (!this.onlyMove.getCurrentValue() || MoveUtils.isMoving())
                    && (!this.noMove.getCurrentValue() || !MoveUtils.isMoving())) {
                //&& (!XPowerClient.moduleManager.getModule(Timer.class).getState() || !this.NoTimer.get())) {
                String mode = this.modeValue.getCurrentMode().toLowerCase();
                switch (mode) {
                    case "sprintpacket":
                        if (mc.player.isSprinting()) {
                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        }

                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        if (this.autoSetSprint.getCurrentValue()) {
                            mc.player.setSprinting(true);
                        }
                        break;
                    case "sneakpacket":
                        if (!MoveUtils.isMoving()) {
                            if (mc.player.isSprinting()) {
                                mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                            }

                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                            if (this.autoSetSprint.getCurrentValue()) {
                                mc.player.setSprinting(true);
                            }

                            return;
                        }

                        if (mc.player.isSprinting()) {
                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        }

                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        if (this.autoSetSprint.getCurrentValue()) {
                            mc.player.setSprinting(true);
                        }
                        break;
                    case "packet":
                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        break;
                    case "w-tap":
                        if (mc.player.isSprinting()) {
                            mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        }

                        mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        if (this.autoSetSprint.getCurrentValue()) {
                            mc.player.setSprinting(true);
                        }
                        break;
                    case "w-tap3":
                        String wTap3Mode = this.wTap3ModeValue.getCurrentMode().toLowerCase();
                        switch (wTap3Mode) {
                            case "wtap":
                            case "legit":
                                tick = 2;
                                break;
                            case "packet":
                                if (mc.player.isSprinting()) {
                                    mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                                }

                                mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                                mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                                mc.player.connection.send(OldNaming.C0BPacketEntityAction(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

                                mc.player.setSprinting(true);
                                break;
                        }
                        break;
                    case "legit":
                        if ((!onlyGroundLegit.getCurrentValue() || mc.player.onGround())
                                && (!onlyMoveLegit.getCurrentValue() || MoveUtils.isMoving())) {
                            sprintLegit = !sprintLegit;
                        }
                        break;
                }

                this.timer.reset();
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (modeValue.isCurrentMode("W-Tap2")) {
            if (resetSprintTimer > 0) {
                resetSprintTimer--;

                if (resetSprintTimer == 0 && wasSprinting && mc.player != null) {
                    mc.player.setSprinting(true);
                    wasSprinting = false;
                }
            }
        }

        if (modeValue.isCurrentMode("W-Tap3") && mc.player != null) {
            String wTap3Mode = this.wTap3ModeValue.getCurrentMode().toLowerCase();

            if (wTap3Mode.equals("legit")) {
                if (tick == 2) {
                    mc.player.setSprinting(false);
                    tick = 1;
                } else if (tick == 1) {
                    mc.player.setSprinting(true);
                    tick = 0;
                }
            }
        }

        if (modeValue.isCurrentMode("Legit") && mc.player != null) {
            mc.player.setSprinting(sprintLegit);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (modeValue.isCurrentMode("W-Tap3") && mc.player != null) {
            String wTap3Mode = this.wTap3ModeValue.getCurrentMode().toLowerCase();

            if (wTap3Mode.equals("wtap")) {
                if (tick == 2) {
                    mc.player.setSprinting(false);
                    tick = 1;
                } else if (tick == 1) {
                    mc.player.setSprinting(true);
                    tick = 0;
                }
            }
        }
    }
}