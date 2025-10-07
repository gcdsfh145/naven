package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import jnic.JNICInclude;
import org.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayTraceUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;

import java.lang.reflect.Method;
import java.util.*;

import static com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayTraceUtil.isWallBetween;

@JNICInclude
@ModuleInfo(
        name = "Velocity",
        description = "Reduces knockback.",
        category = Category.COMBAT
)
public class Velocity extends Module {
    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("GrimAC", "JumpReset", "Attack Reduce", "Grim Attack", "GrimReduce", "GrimReduceNew")
            .build()
            .getModeValue();
    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("GrimAC"))
            .build()
            .getBooleanValue();
    private final FloatValue attacks = ValueBuilder.create(this, "ShaoYu")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> mode.isCurrentMode("GrimAC"))
            .build()
            .getFloatValue();
    public FloatValue jumpChance = ValueBuilder.create(this, "Chance")
            .setDefaultFloatValue(100.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .setVisibility(() -> mode.isCurrentMode("JumpReset"))
            .build()
            .getFloatValue();
    public BooleanValue delayInAir = ValueBuilder.create(this, "DelayInAir")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("JumpReset"))
            .build()
            .getBooleanValue();

    private final FloatValue attackReduceAttacks = ValueBuilder.create(this, "Attack Count")
            .setDefaultFloatValue(5.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(16.0f)
            .setFloatStep(1.0f)
            .setVisibility(() -> mode.isCurrentMode("Attack Reduce"))
            .build()
            .getFloatValue();
    private final FloatValue attackRange = ValueBuilder.create(this, "Attack Range")
            .setDefaultFloatValue(3.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(6.0f)
            .setFloatStep(0.01f)
            .setVisibility(() -> mode.isCurrentMode("Attack Reduce"))
            .build()
            .getFloatValue();
    private final BooleanValue onlyMove = ValueBuilder.create(this, "Only Move")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Attack Reduce"))
            .build()
            .getBooleanValue();
    private final BooleanValue onlySprint = ValueBuilder.create(this, "Only Sprint")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Attack Reduce"))
            .build()
            .getBooleanValue();
    private final BooleanValue onlyGround = ValueBuilder.create(this, "Only Ground")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Attack Reduce"))
            .build()
            .getBooleanValue();
    private final BooleanValue debugMessageValue = ValueBuilder.create(this, "Flag Debug Message")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Attack Reduce"))
            .build()
            .getBooleanValue();

    private final BooleanValue attackedFlightObject = ValueBuilder.create(this, "Attacked FlightObject")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Grim Attack"))
            .build()
            .getBooleanValue();
    private final FloatValue targetMotion = ValueBuilder.create(this, "Target Motion")
            .setDefaultFloatValue(0.1f)
            .setMinFloatValue(0.01f)
            .setMaxFloatValue(1.0f)
            .setFloatStep(0.001f)
            .setVisibility(() -> mode.isCurrentMode("Grim Attack"))
            .build()
            .getFloatValue();
    private final FloatValue grimAttackCounter = ValueBuilder.create(this, "Counter")
            .setDefaultFloatValue(1.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(1.0f)
            .setVisibility(() -> mode.isCurrentMode("Grim Attack"))
            .build()
            .getFloatValue();
    private final BooleanValue rayCast = ValueBuilder.create(this, "Ray Cast")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("Grim Attack"))
            .build()
            .getBooleanValue();
    private final BooleanValue sprintOnly = ValueBuilder.create(this, "Sprint Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("Grim Attack"))
            .build()
            .getBooleanValue();
    private final FloatValue grimAttackRange = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3.0f)
            .setMinFloatValue(2.0f)
            .setMaxFloatValue(8.0f)
            .setFloatStep(0.1f)
            .setVisibility(() -> mode.isCurrentMode("Grim Attack"))
            .build()
            .getFloatValue();

    // GrimReduce specific settings
    private final ModeValue grimReduceMode = ValueBuilder.create(this, "Grim Mode")
            .setDefaultModeIndex(0)
            .setModes("Reduce", "1.17")
            .setVisibility(() -> mode.isCurrentMode("GrimReduce"))
            .build()
            .getModeValue();
    private final FloatValue grimReduceFactor = ValueBuilder.create(this, "Reduction Factor")
            .setDefaultFloatValue(0.6f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(1.0f)
            .setFloatStep(0.1f)
            .setVisibility(() -> mode.isCurrentMode("GrimReduce") && grimReduceMode.isCurrentMode("Reduce"))
            .build()
            .getFloatValue();
    private final BooleanValue grimReduceLog = ValueBuilder.create(this, "GrimReduce Log")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("GrimReduce"))
            .build()
            .getBooleanValue();

    private final ModeValue grimReduceNewAttackMotionValue = ValueBuilder.create(this, "Attack Motion")
            .setDefaultModeIndex(2)
            .setModes("Pre", "Post", "Update", "Tick")
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getModeValue();
    private final FloatValue grimReduceNewAttackValue = ValueBuilder.create(this, "Attack Count")
            .setDefaultFloatValue(5.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(20.0f)
            .setFloatStep(1.0f)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getFloatValue();
    private final FloatValue grimReduceNewHurtTime = ValueBuilder.create(this, "Hurt Time")
            .setDefaultFloatValue(0.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(1.0f)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getFloatValue();
    private final BooleanValue grimReduceNewSprintValue = ValueBuilder.create(this, "Sprint")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final FloatValue grimReduceNewNoXZ = ValueBuilder.create(this, "No XZ")
            .setDefaultFloatValue(0.6f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(1.0f)
            .setFloatStep(0.1f)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getFloatValue();
    private final BooleanValue grimReduceNewNoUseItem = ValueBuilder.create(this, "No Use Item")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final BooleanValue grimReduceNewNoFakeLag = ValueBuilder.create(this, "No Fake Lag")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final BooleanValue grimReduceNewNoBedBreaker = ValueBuilder.create(this, "No Bed Breaker")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final BooleanValue grimReduceNewNoScaffold = ValueBuilder.create(this, "No Scaffold")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final BooleanValue grimReduceNewAutoFindTarget = ValueBuilder.create(this, "Auto Find Target")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final BooleanValue grimReduceNewReturnFindTeams = ValueBuilder.create(this, "Return Find Teams")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();
    private final BooleanValue grimReduceNewLog = ValueBuilder.create(this, "GrimReduceNew Log")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("GrimReduceNew"))
            .build()
            .getBooleanValue();

    private boolean velocityFlag = false;
    private boolean sprintFlag = false;
    private final TickTimeHelper timer = new TickTimeHelper();
    private int preC0f = 0;
    private int grimAction = 0;
    private LivingEntity targetEntity;
    private int jumpTimer = 0;
    private boolean shouldJump = false;
    private boolean isJumping = false;
    private int slowdownTicks = 0;
    private int cooldownTicks = 0;
    private boolean jrForwardStarted = false;
    private boolean velocityInput = false;
    private boolean damage = false;
    private final List<Packet<?>> packets = new ArrayList<>();
    private boolean delayPackets = false;
    private int delayTicks = 0;
    private Method handlePacketMethod;
    private int flags = 0;
    private boolean attacked = false;
    private boolean wasSprintingBeforeAttack = false;
    private double velocityStrength = 0.0;
    private double[] motionReduction = new double[]{0.6, 0.6};

    private final Queue<Packet<?>> grimAttackPackets = new LinkedList<>();
    private boolean grimAttackAttackedFlightObject = false;
    private boolean grimAttackSlowdownTicks = false;
    private boolean grimAttackVelocityInput = false;
    private boolean grimAttackAttacked = false;
    private boolean grimAttackShouldSend = false;
    private double grimAttackReduceXZ = 0;

    private boolean canSpoof = false;
    private boolean canCancel = false;
    private int lastSprint = -1;
    private long lastVelocityTime = 0;
    private static final long VELOCITY_COOLDOWN = 500;
    private static final double MINIMUM_SPEED = 0.02;

    private boolean grimReduceNewVelocityInput = false;
    private long lastGrimReduceNewAttackTime = 0;
    private static final long GRIM_REDUCE_NEW_COOLDOWN = 100;

    public Velocity() {
        try {
            Class<?> connectionClass = Objects.requireNonNull(mc.getConnection()).getClass();
            handlePacketMethod = connectionClass.getDeclaredMethod("handlePacket", Packet.class);
            handlePacketMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Optional<LivingEntity> findEntity() {
        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == Type.ENTITY) {
            Entity entity = ((EntityHitResult)hitResult).getEntity();
            if (entity instanceof Player) {
                return Optional.of((LivingEntity)entity);
            }
        }
        return Optional.empty();
    }

    private double ensureMinimumSpeed(double speed) {
        if (Math.abs(speed) < MINIMUM_SPEED) {
            return speed > 0 ? MINIMUM_SPEED : -MINIMUM_SPEED;
        }
        return speed;
    }

    private void applyVelocityReduction(double x, double y, double z, double reductionFactor) {
        double newX = x * reductionFactor;
        double newZ = z * reductionFactor;

        newX = ensureMinimumSpeed(newX);
        newZ = ensureMinimumSpeed(newZ);

        mc.player.setDeltaMovement(newX, y, newZ);
    }

    public void reset() {
        this.velocityFlag = false;
        velocityInput = false;
        damage = false;
        packets.clear();
        delayPackets = false;
        delayTicks = 0;
        this.flags = 0;
        this.attacked = false;
        this.wasSprintingBeforeAttack = false;
        this.velocityStrength = 0.0;

        grimAttackVelocityInput = false;
        grimAttackAttacked = false;
        grimAttackAttackedFlightObject = false;
        grimAttackReduceXZ = 0;
        grimAttackPackets.clear();

        canSpoof = false;
        canCancel = false;
        lastSprint = -1;

        grimReduceNewVelocityInput = false;
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private void grimReduceLog(String message) {
        if (this.grimReduceLog.getCurrentValue()) {
            ChatUtils.addChatMessage("[GrimReduce] " + message);
        }
    }

    private void grimReduceNewLog(String message) {
        if (this.grimReduceNewLog.getCurrentValue()) {
            ChatUtils.addChatMessage("[GrimReduceNew] " + message);
        }
    }

    @Override
    public void onEnable() {
        jumpTimer = 0;
        shouldJump = false;
        isJumping = false;
        cooldownTicks = 0;
        jrForwardStarted = false;
        this.reset();

        grimAttackSlowdownTicks = false;
        grimAttackShouldSend = false;
        lastVelocityTime = 0;
        lastGrimReduceNewAttackTime = 0;
    }

    @Override
    public void onDisable() {
        jumpTimer = 0;
        shouldJump = false;
        isJumping = false;
        cooldownTicks = 0;
        if (mc != null && mc.options != null) {
            try {
                mc.options.keyJump.setDown(false);
                if (jrForwardStarted) mc.options.keyUp.setDown(false);
            } catch (Throwable ignored) {}
        }
        jrForwardStarted = false;
        this.setSuffix(mode.getCurrentMode());
        this.reset();

        grimAttackSlowdownTicks = false;
        grimAttackShouldSend = false;
        lastVelocityTime = 0;
        lastGrimReduceNewAttackTime = 0;
    }

    @EventTarget
    public void onWorld(EventRespawn eventRespawn) {
        jumpTimer = 0;
        shouldJump = false;
        isJumping = false;
        cooldownTicks = 0;
        if (mc != null && mc.options != null) {
            try {
                mc.options.keyJump.setDown(false);
                if (jrForwardStarted) mc.options.keyUp.setDown(false);
            } catch (Throwable ignored) {}
        }
        jrForwardStarted = false;
        this.reset();

        grimAttackSlowdownTicks = false;
        grimAttackShouldSend = false;
        lastVelocityTime = 0;
        lastGrimReduceNewAttackTime = 0;
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (mode.isCurrentMode("GrimAC") && this.velocityFlag) {
            if (this.sprintFlag && !((LocalPlayerAccessor)mc.player).isWasSprinting()) {
                this.velocityFlag = false;
                return;
            }

            this.velocityFlag = false;
            if (this.sprintFlag && this.targetEntity != null) {
                float currentYaw = mc.player.getYRot();
                float currentPitch = mc.player.getXRot();
                mc.player.setYRot(RotationManager.rotations.x);
                mc.player.setXRot(RotationManager.rotations.y);

                for (int i = 0; i < this.attacks.getCurrentValue(); i++) {
                    mc.gameMode.attack(mc.player, this.targetEntity);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }

                mc.player.setYRot(currentYaw);
                mc.player.setXRot(currentPitch);
            }
        }
    }

    @EventTarget
    public void onTick(EventRunTicks eventRunTicks) {
        if (mc.player == null) return;

        if (mode.isCurrentMode("GrimReduceNew")) {
            if (grimReduceNewAttackMotionValue.isCurrentMode("Tick")) {
                setGrimReduceNewVelocityInput();
            }
        }

        if (mode.isCurrentMode("GrimReduce")) {
            if (grimReduceMode.isCurrentMode("1.17")) {
                if (canSpoof) {
                    grimReduceLog("Sending spoof packets");
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()
                    ));
                    canSpoof = false;
                }
            }
        }

        if (mode.isCurrentMode("Grim Attack")) {
            while (!grimAttackPackets.isEmpty()) {
                mc.getConnection().send(grimAttackPackets.poll());
            }

            if (grimAttackSlowdownTicks) {
                grimAttackSlowdownTicks = false;
            }

            if (grimAttackVelocityInput) {
                if (grimAttackAttacked) {
                    double currentX = mc.player.getDeltaMovement().x;
                    double currentY = mc.player.getDeltaMovement().y;
                    double currentZ = mc.player.getDeltaMovement().z;
                    applyVelocityReduction(currentX, currentY, currentZ, grimAttackReduceXZ);
                    grimAttackAttacked = false;
                }
                if (grimAttackAttackedFlightObject) {
                    double currentX = mc.player.getDeltaMovement().x;
                    double currentY = mc.player.getDeltaMovement().y;
                    double currentZ = mc.player.getDeltaMovement().z;
                    applyVelocityReduction(currentX, currentY, currentZ, grimAttackReduceXZ);
                    grimAttackAttackedFlightObject = false;
                }
                if (mc.player.hurtTime == 0) {
                    grimAttackVelocityInput = false;
                    grimAttackAttacked = false;
                    grimAttackAttackedFlightObject = false;
                    grimAttackReduceXZ = 0;
                }
            }
        }

        if (mode.isCurrentMode("JumpReset")) {
            if (delayInAir.getCurrentValue()) {
                if (delayPackets && mc.player.onGround()) {
                    try {
                        for (Packet<?> packet : packets) {
                            handlePacketMethod.invoke(mc.getConnection(), packet);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    packets.clear();
                    delayPackets = false;
                    velocityInput = true;
                }
                if (delayPackets) {
                    delayTicks++;
                    if (delayTicks >= 60) {
                        try {
                            for (Packet<?> packet : packets) {
                                handlePacketMethod.invoke(mc.getConnection(), packet);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        packets.clear();
                        delayPackets = false;
                        velocityInput = true;
                        delayTicks = 0;
                    }
                }
                if (velocityInput) {
                    mc.options.keyJump.setDown(true);
                    velocityInput = false;
                }
            } else {
                int ht = mc.player.hurtTime;
                if (ht >= 8) {
                    try {
                        mc.options.keyJump.setDown(true);
                    } catch (Throwable ignored) {
                    }
                    if (ht >= 7 && !mc.options.keyUp.isDown()) {
                        try {
                            mc.options.keyUp.setDown(true);
                        } catch (Throwable ignored) {
                        }
                        jrForwardStarted = true;
                    }
                } else if (ht >= 7) {
                    if (!mc.options.keyUp.isDown()) {
                        try {
                            mc.options.keyUp.setDown(true);
                        } catch (Throwable ignored) {
                        }
                        jrForwardStarted = true;
                    }
                }
                if (ht < 7 && ht > 0) {
                    try {
                        mc.options.keyJump.setDown(false);
                    } catch (Throwable ignored) {
                    }
                    if (jrForwardStarted) {
                        try {
                            mc.options.keyUp.setDown(false);
                        } catch (Throwable ignored) {
                        }
                        jrForwardStarted = false;
                    }
                }
                return;
            }
        }

        if (mode.isCurrentMode("Attack Reduce")) {
            if (this.flags > 0) {
                --this.flags;
                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("[Velocity] Flag countdown: " + this.flags + " ticks remaining");
                }
            }

            if (this.velocityInput) {
                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("[Velocity] Processing velocity input...");
                }
                this.velocityInput = false;
                if (this.debugMessageValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("[Velocity] Velocity input processing completed and reset");
                }
                if (this.onlyGround.getCurrentValue() && !mc.player.onGround() ||
                        this.onlyMove.getCurrentValue() && !MoveUtils.isMoving() ||
                        this.flags > 0) {
                    if (this.debugMessageValue.getCurrentValue()) {
                        StringBuilder reason = new StringBuilder();
                        if (this.onlyGround.getCurrentValue() && !mc.player.onGround()) {
                            reason.append("not on ground, ");
                        }
                        if (this.onlyMove.getCurrentValue() && !MoveUtils.isMoving()) {
                            reason.append("not moving, ");
                        }
                        if (this.flags > 0) {
                            reason.append("flag active (").append(this.flags).append(" ticks), ");
                        }
                        if (reason.length() > 0) {
                            reason.setLength(reason.length() - 2);
                        }
                        ChatUtils.addChatMessage("[Velocity] Skipping processing - Reason: " + reason.toString());
                    }
                    return;
                }
                if (this.attacked) {
                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Applying velocity reduction...");
                    }
                    double velX = mc.player.getDeltaMovement().x * this.motionReduction[0];
                    double velY = mc.player.getDeltaMovement().y;
                    double velZ = mc.player.getDeltaMovement().z * this.motionReduction[1];

                    // 确保最小速度
                    velX = ensureMinimumSpeed(velX);
                    velZ = ensureMinimumSpeed(velZ);

                    mc.player.setDeltaMovement(velX, velY, velZ);
                    if (!this.wasSprintingBeforeAttack) {
                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }
                    this.attacked = false;
                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Velocity reduced by " + String.format("%.1f", (1.0 - this.motionReduction[0]) * 100.0) + "% (XZ-axis)");
                    }
                }
            }
        }

        if (cooldownTicks > 0) cooldownTicks--;
        if (mc.player.onGround()) isJumping = false;
        if (jumpTimer > 0 && !isJumping && cooldownTicks == 0) {
            jumpTimer--;
            if (jumpTimer == 0 && shouldJump) {
                mc.player.jumpFromGround();
                isJumping = true;
                shouldJump = false;
                cooldownTicks = 5;
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion eventMotion) {
        if (eventMotion.getType() == EventType.PRE) {
            if (mode.isCurrentMode("GrimReduceNew")) {
                if (grimReduceNewAttackMotionValue.isCurrentMode("Pre")) {
                    setGrimReduceNewVelocityInput();
                }
            }

            this.slowdownTicks--;
            if (this.slowdownTicks == 0) {
                Naven.TICK_TIMER = 1.0F;
            } else if (this.slowdownTicks > 0) {
                ChatUtils.addChatMessage("Slowdown Ticks: " + this.slowdownTicks);
                Naven.TICK_TIMER = 1.0F / this.slowdownTicks;
            }

            if (mode.isCurrentMode("GrimReduce") && grimReduceMode.isCurrentMode("Reduce")) {
                if (lastSprint == 0) {
                    lastSprint--;
                    if (!MoveUtils.isMoving()) {
                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }
                } else if (lastSprint > 0) {
                    lastSprint--;
                    if (mc.player.onGround() && !MoveUtils.isMoving()) {
                        lastSprint = -1;
                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }
                }
            }
        }

        if (eventMotion.getType() == EventType.POST && mode.isCurrentMode("GrimReduceNew")) {
            if (grimReduceNewAttackMotionValue.isCurrentMode("Post")) {
                setGrimReduceNewVelocityInput();
            }
        }
    }

    @EventTarget
    public void onPacket(EventHandlePacket e) {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;

        try {
            if (mode.isCurrentMode("GrimAC")) {
                if (e.getPacket() instanceof ClientboundSetEntityMotionPacket && this.timer.delay(3)) {
                    ClientboundSetEntityMotionPacket packet = (ClientboundSetEntityMotionPacket)e.getPacket();
                    if (mc.player.getId() == packet.getId()) {
                        double x = packet.getXa() / 8000.0;
                        double z = packet.getZa() / 8000.0;
                        double speed = Math.sqrt(x * x + z * z);

                        if (!Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                                && mc.player.getUseItem().isEmpty()
                                && mc.screen == null) {
                            Optional<LivingEntity> target = this.findEntity();
                            if (target.isPresent()) {
                                this.targetEntity = target.get();
                                this.sprintFlag = ((LocalPlayerAccessor)mc.player).isWasSprinting();
                                if (this.sprintFlag) {
                                    e.setCancelled(true);
                                    if (this.log.getCurrentValue()) {
                                        log("Vl: " + (float)Math.round(speed * 100.0) / 100.0F);
                                    }

                                    x *= Math.pow(0.6, this.attacks.getCurrentValue());
                                    z *= Math.pow(0.6, this.attacks.getCurrentValue());

                                    x = ensureMinimumSpeed(x);
                                    z = ensureMinimumSpeed(z);

                                    mc.player.setDeltaMovement(x, packet.getYa() / 8000.0, z);
                                    this.velocityFlag = true;
                                    this.timer.reset();
                                }
                            }
                        }
                    }
                }

                if (mc.player.tickCount > 120) {
                    Packet<?> packet = e.getPacket();
                    if (packet instanceof ClientboundPingPacket pingPacket) {
                        if (Math.abs(this.preC0f - pingPacket.getId()) == 1) {
                            this.grimAction = pingPacket.getId();
                        }

                        this.preC0f = pingPacket.getId();
                        if (this.grimAction != pingPacket.getId() && Math.abs(this.grimAction - pingPacket.getId()) > 10 && mc.player.hurtTime > 0) {
                            mc.player.hurtTime = 0;
                            e.setCancelled(true);
                        }
                    }
                }
            } else if (mode.isCurrentMode("JumpReset")) {
                Packet<?> packet = e.getPacket();
                if (delayInAir.getCurrentValue()) {
                    if (packet instanceof ClientboundHurtAnimationPacket) {
                        ClientboundHurtAnimationPacket hurtPacket = (ClientboundHurtAnimationPacket) packet;
                        try {
                            Method getEntityIdMethod = hurtPacket.getClass().getMethod("getEntityId");
                            int entityId = (Integer) getEntityIdMethod.invoke(hurtPacket);
                            if (entityId == mc.player.getId()) {
                                damage = true;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (damage && packet instanceof ClientboundSetEntityMotionPacket) {
                        ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) packet;
                        if (motionPacket.getId() == mc.player.getId()) {
                            if (!mc.player.onGround()) {
                                delayPackets = true;
                                e.setCancelled(true);
                                packets.add(packet);
                                delayTicks = 0;
                            } else {
                                velocityInput = true;
                            }
                            damage = false;
                        }
                    }
                    return;
                }
                if (packet instanceof ClientboundSetEntityMotionPacket) {
                    ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket) packet;
                    if (velocityPacket.getId() == mc.player.getId()) {
                        if (mc.player.onGround() && RandomSource.create().nextFloat() * 100 <= jumpChance.getCurrentValue()) {
                            mc.player.jumpFromGround();
                        }
                    }
                }
            } else if (mode.isCurrentMode("Attack Reduce")) {
                Packet<?> packet = e.getPacket();
                if (packet instanceof ClientboundPlayerPositionPacket) {
                    this.flags = 10;
                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Flag detected - delaying velocity processing");
                    }
                } else if (packet instanceof ClientboundSetEntityMotionPacket) {
                    ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket) packet;
                    if (velocityPacket.getId() != mc.player.getId()) {
                        return;
                    }
                    if (this.onlyGround.getCurrentValue() && !mc.player.onGround() ||
                            this.onlyMove.getCurrentValue() && !MoveUtils.isMoving() ||
                            this.flags > 0) {
                        return;
                    }
                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Processing velocity input...");
                    }

                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Executing attack reduce logic...");
                    }

                    HitResult hitResult = RayTraceUtil.rayCast(
                            new Rotation(mc.player.getYRot(), mc.player.getXRot()),
                            this.attackRange.getCurrentValue(),
                            1.0f,
                            mc.player,
                            null,
                            false
                    );

                    if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
                        return;
                    }

                    double x = (double)velocityPacket.getXa() / 8000.0;
                    double z = (double)velocityPacket.getZa() / 8000.0;
                    this.velocityStrength = Math.sqrt(x * x + z * z);
                    if (this.velocityStrength < 0.2) {
                        return;
                    }
                    if (this.velocityStrength >= 3.0) {
                        this.motionReduction[0] = 0.1;
                        this.motionReduction[1] = 0.1;
                    } else if (this.velocityStrength >= 1.0) {
                        this.motionReduction[0] = 0.3;
                        this.motionReduction[1] = 0.3;
                    } else {
                        this.motionReduction[0] = 0.5;
                        this.motionReduction[1] = 0.5;
                    }

                    Entity targetEntity = ((EntityHitResult)hitResult).getEntity();
                    this.wasSprintingBeforeAttack = mc.player.isSprinting();
                    if (!this.wasSprintingBeforeAttack && this.onlySprint.getCurrentValue()) {
                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                        mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround()));
                    }

                    int maxAttacks = (int)this.attackReduceAttacks.getCurrentValue();
                    for (int i = 0; i < maxAttacks; ++i) {
                        mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(targetEntity, mc.player.isShiftKeyDown()));
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    }
                    this.attacked = true;
                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Attack reduce prepared - Strength: " +
                                String.format("%.2f", this.velocityStrength) + ", Reduction: " +
                                String.format("%.1f", this.motionReduction[0] * 100.0) + "%");
                    }

                    this.velocityInput = true;
                    if (this.debugMessageValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("[Velocity] Velocity packet detected - processing next tick");
                    }
                }
            } else if (mode.isCurrentMode("Grim Attack")) {
                Packet<?> packet = e.getPacket();

                if (packet instanceof ClientboundSetEntityMotionPacket packetEntityVelocity) {
                    if (packetEntityVelocity.getId() != mc.player.getId()) return;

                    LivingEntity target = null;
                    if (target != null && target != mc.player && !mc.player.onClimbable()) {
                        final HitResult hitResult = RayTraceUtil.rayCasts(new Rotation(mc.player.getYRot(), mc.player.getXRot()), grimAttackRange.getCurrentValue());
                        if (rayCast.getCurrentValue() && hitResult != null) {
                            if (hitResult.getType() != HitResult.Type.ENTITY || !target.equals(((EntityHitResult) hitResult).getEntity())) {
                                return;
                            }
                        }
                        boolean state = ((LocalPlayerAccessor)mc.player).isWasSprinting();

                        if (!sprintOnly.getCurrentValue() || state) {

                            if (grimAttackAttacked) return;

                            grimAttackVelocityInput = true;

                            grimAttackReduceXZ = 1;

                            if (!state) {
                                grimAttackPackets.offer(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                                grimAttackPackets.offer(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround()));
                                grimAttackSlowdownTicks = true;
                            }

                            final double motionX = packetEntityVelocity.getXa() / 8000.0;
                            final double motionZ = packetEntityVelocity.getZa() / 8000.0;
                            double velocityDistance = Math.sqrt(motionX * motionX + motionZ * motionZ);

                            int counter = 0;
                            while (velocityDistance * grimAttackReduceXZ > targetMotion.getCurrentValue() && counter <= grimAttackCounter.getCurrentValue()) {
                                grimAttackPackets.offer(ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
                                grimAttackPackets.offer(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                                grimAttackReduceXZ *= 0.6;
                                counter++;
                            }

                            if (!state) {
                                grimAttackPackets.offer(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                            }

                            grimAttackAttacked = true;
                        }
                    }

                    if (attackedFlightObject.getCurrentValue()) {
                        for (Entity entity : mc.level.entitiesForRendering()) {
                            if (entity != null
                                    && entity != mc.player
                                    && entity instanceof net.minecraft.world.entity.projectile.Projectile
                                    && RotationUtils.getDistanceToEntity(entity) > 6.0) {
                                final HitResult hitResult = RayTraceUtil.rayCasts(new Rotation(mc.player.getYRot(), mc.player.getXRot()), grimAttackRange.getCurrentValue());
                                if (rayCast.getCurrentValue() && hitResult != null) {
                                    if (hitResult.getType() != HitResult.Type.ENTITY || !entity.equals(((EntityHitResult) hitResult).getEntity())) {
                                        return;
                                    }
                                }

                                if (grimAttackAttackedFlightObject) return;

                                if (entity.onGround()) continue;

                                grimAttackVelocityInput = true;

                                boolean state = ((LocalPlayerAccessor)mc.player).isWasSprinting();

                                grimAttackReduceXZ = 1;

                                if (!state) {
                                    grimAttackPackets.offer(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                                    grimAttackPackets.offer(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround()));
                                    grimAttackSlowdownTicks = true;
                                }

                                final double motionX = packetEntityVelocity.getXa() / 8000.0;
                                final double motionZ = packetEntityVelocity.getZa() / 8000.0;
                                double velocityDistance = Math.sqrt(motionX * motionX + motionZ * motionZ);

                                int counter = 0;
                                while (velocityDistance * grimAttackReduceXZ > targetMotion.getCurrentValue() && counter <= grimAttackCounter.getCurrentValue()) {
                                    grimAttackPackets.offer(ServerboundInteractPacket.createAttackPacket(entity, mc.player.isShiftKeyDown()));
                                    grimAttackPackets.offer(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                                    grimAttackReduceXZ *= 0.6;
                                    counter++;
                                }

                                if (!state) {
                                    grimAttackPackets.offer(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                                }

                                grimAttackAttackedFlightObject = true;
                                break;
                            }
                        }
                    }
                }
            } else if (mode.isCurrentMode("GrimReduce")) {
                Packet<?> packet = e.getPacket();

                if (grimReduceMode.isCurrentMode("Reduce")) {
                    if (packet instanceof ClientboundSetEntityMotionPacket packetEntityVelocity) {
                        if (packetEntityVelocity.getId() == mc.player.getId()) {
                            if (!Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                                    && mc.player.getUseItem().isEmpty()
                                    && mc.screen == null) {

                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastVelocityTime < VELOCITY_COOLDOWN) {
                                    grimReduceLog("Velocity cooldown active, skipping");
                                    return;
                                }

                                Optional<LivingEntity> target = this.findEntity();
                                if (target.isPresent()) {
                                    e.setCancelled(true);
                                    lastVelocityTime = currentTime;
                                    grimReduceLog("Cancelled velocity packet");

                                    boolean serverSprintState = ((LocalPlayerAccessor) mc.player).isWasSprinting();
                                    if (!serverSprintState) {
                                        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                                        lastSprint = 5;
                                    }

                                    double velocityX = packetEntityVelocity.getXa() / 8000.0;
                                    double velocityZ = packetEntityVelocity.getZa() / 8000.0;
                                    double velocityY = packetEntityVelocity.getYa() / 8000.0;
                                    double factor = grimReduceFactor.getCurrentValue();

                                    applyVelocityReduction(velocityX, velocityY, velocityZ, factor);

                                    grimReduceLog("Reduced velocity by factor " + factor + ", original length: " + String.format("%.3f", Math.sqrt(velocityX*velocityX + velocityZ*velocityZ)));
                                }
                            }
                        }
                    }
                } else if (grimReduceMode.isCurrentMode("1.17")) {
                    if (packet instanceof ClientboundSetEntityMotionPacket) {
                        if (canCancel) {
                            canCancel = false;
                            canSpoof = true;
                            e.setCancelled(true);
                            grimReduceLog("Cancelled velocity packet for 1.17 mode");
                        }
                    }

                    if (packet instanceof ClientboundHurtAnimationPacket) {
                        ClientboundHurtAnimationPacket hurtPacket = (ClientboundHurtAnimationPacket) packet;
                        try {
                            Method getEntityIdMethod = hurtPacket.getClass().getMethod("getEntityId");
                            int entityId = (Integer) getEntityIdMethod.invoke(hurtPacket);
                            if (entityId == mc.player.getId()) {
                                canCancel = true;
                                grimReduceLog("Entity status received, can cancel next velocity");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } else if (mode.isCurrentMode("GrimReduceNew")) {
                Packet<?> packet = e.getPacket();

                if (packet instanceof ClientboundSetEntityMotionPacket clientboundsetentitymotionpacket) {
                    if (mc.level.getEntity(clientboundsetentitymotionpacket.getId()) != mc.player) {
                        return;
                    }

                    if (mc.player.isSpectator()
//                            || Naven.getInstance().getModuleManager().getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.world.Timer.class).isEnabled()
                            || Naven.getInstance().getModuleManager().getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck.class).isEnabled()
                            || mc.player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                            || grimReduceNewNoUseItem.getCurrentValue() && mc.player.isUsingItem()
                            || grimReduceNewNoFakeLag.getCurrentValue() && Naven.getInstance().getModuleManager().getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.FakeLag.class).isEnabled()
                            || grimReduceNewNoBedBreaker.getCurrentValue()
//                            && Naven.getInstance().getModuleManager().getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.BedBreaker.class).isEnabled()
                            || grimReduceNewNoScaffold.getCurrentValue() && Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()) {
                        return;
                    }

                    grimReduceNewVelocityInput = true;
                    grimReduceNewLog("Velocity packet received, setting input flag");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setGrimReduceNewVelocityInput() {
        if (mode.isCurrentMode("GrimReduceNew") && grimReduceNewVelocityInput && mc.player != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastGrimReduceNewAttackTime < GRIM_REDUCE_NEW_COOLDOWN) {
                return;
            }

            com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura Aura =
                    (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);

            if (Aura != null && Aura.isEnabled() && Aura.getTarget() != null) {
                if (mc.player.hurtTime <= grimReduceNewHurtTime.getCurrentValue()) {
                    int attackCount = (int) grimReduceNewAttackValue.getCurrentValue();

                    attackCount = Math.min(attackCount, 5);

                    for (int i = 0; i < attackCount; i++) {
                        mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(Aura.getTarget(), mc.player.isShiftKeyDown()));
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

                        if (grimReduceNewSprintValue.getCurrentValue() && !mc.player.isSprinting()) {
                            if (mc.player.isSprinting()) {
                                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                            }

                            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                            mc.player.setSprinting(true);
                        }

                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(
                                (double) grimReduceNewNoXZ.getCurrentValue(), 1.0, (double) grimReduceNewNoXZ.getCurrentValue()));
                    }

                    grimReduceNewVelocityInput = false;
                    lastGrimReduceNewAttackTime = currentTime;
                    grimReduceNewLog("Executed " + attackCount + " attacks with velocity reduction");
                }
            } else if (grimReduceNewAutoFindTarget.getCurrentValue()) {
                Entity entity = null;
                double closestDistance = Double.MAX_VALUE;

                for (Entity entity1 : mc.level.entitiesForRendering()) {
                    if (entity1 instanceof LivingEntity) {
                        double distance = mc.player.distanceTo(entity1);
                        if (distance <= 3.0
                                && entity1 != mc.player
                                && isTargetTypeValid(entity1, false, grimReduceNewReturnFindTeams.getCurrentValue(), grimReduceNewReturnFindTeams.getCurrentValue())
                                && !isWallBetween(entity1)
                                && distance < closestDistance) {
                            closestDistance = distance;
                            entity = entity1;
                        }
                    }
                }

                if (entity != null) {
                    int attackCount = (int) grimReduceNewAttackValue.getCurrentValue();

                    attackCount = Math.min(attackCount, 3);

                    for (int j = 0; j < attackCount; j++) {
                        RotationUtils.setTargetRotation(RotationUtils.getNCPRotations(entity.position(), false), 20);
                        mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(entity, mc.player.isShiftKeyDown()));
                        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

                        if (grimReduceNewSprintValue.getCurrentValue()) {
                            if (mc.player.isSprinting()) {
                                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                            }

                            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                            mc.player.setSprinting(true);
                        }

                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(
                                (double) grimReduceNewNoXZ.getCurrentValue(), 1.0, (double) grimReduceNewNoXZ.getCurrentValue()));
                    }

                    grimReduceNewVelocityInput = false;
                    lastGrimReduceNewAttackTime = currentTime;
                    grimReduceNewLog("Auto found target and executed " + attackCount + " attacks");
                }
            }
        }
    }

    private boolean isTargetTypeValid(Entity entity, boolean ignoreInvisible, boolean checkTeams, boolean checkFriend) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (ignoreInvisible && entity.isInvisible()) return false;

        return true;
    }

}