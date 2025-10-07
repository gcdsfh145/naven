package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraft.core.particles.ParticleOptions;

import java.lang.reflect.Field;

@ModuleInfo(
        name = "Critical",
        description = "Automatically performs critical hits",
        category = Category.COMBAT
)
public class Critical extends Module {
    ModeValue mode = ValueBuilder.create(this, "mode")
            .setDefaultModeIndex(0)
            .setModes("Jump", "MiniJump", "Packet", "GrimStuck", "GrimTimer")
            .build()
            .getModeValue();

    BooleanValue fire = ValueBuilder.create(this, "Fire").setDefaultBooleanValue(false).build().getBooleanValue();

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

    private boolean attacking;
    private int criticalTicks = 0;
    private boolean shouldCritical = false;

    private boolean start = false;
    private long lastMsTimer = 0;

    private static final Minecraft mc = Minecraft.getInstance();

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

    public static double getXLast() {
        if (mc.player == null) return 0.0D;
        try {
            Field xLastField = net.minecraft.world.entity.player.Player.class.getDeclaredField("xOld");
            xLastField.setAccessible(true);
            return xLastField.getDouble(mc.player);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0D;
        }
    }

    public static double getYLast() {
        if (mc.player == null) return 0.0D;
        try {
            Field yLastField = net.minecraft.world.entity.player.Player.class.getDeclaredField("yOld");
            yLastField.setAccessible(true);
            return yLastField.getDouble(mc.player);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0D;
        }
    }

    public static double getZLast() {
        if (mc.player == null) return 0.0D;
        try {
            Field zLastField = net.minecraft.world.entity.player.Player.class.getDeclaredField("zOld");
            zLastField.setAccessible(true);
            return zLastField.getDouble(mc.player);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0D;
        }
    }

    public static float getYRotLast() {
        if (mc.player == null) return 0.0F;
        try {
            Field yRotLastField = net.minecraft.world.entity.player.Player.class.getDeclaredField("yRotO");
            yRotLastField.setAccessible(true);
            return yRotLastField.getFloat(mc.player);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0F;
        }
    }

    public static float getXRotLast() {
        if (mc.player == null) return 0.0F;
        try {
            Field xRotLastField = net.minecraft.world.entity.player.Player.class.getDeclaredField("xRotO");
            xRotLastField.setAccessible(true);
            return xRotLastField.getFloat(mc.player);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0F;
        }
    }

    public static int getSkipTicks() {
        return 0;
    }

    public static void setSkipTicks(int skipTicks) {
    }

    public static int getOffGroundTicks() {
        if (mc.player == null) return 0;
        return mc.player.onGround() ? 0 : 1;
    }

    public static void spawnLegacyParticles(Entity entity, ParticleOptions particleType) {
        RandomSource random = entity.level().getRandom();
        for (int i = 0; i < 16; i++) {
            double offsetX = (random.nextDouble() * 2 - 1) * entity.getBbWidth() / 4.0;
            double offsetY = (random.nextDouble() * 2 - 1) * entity.getBbHeight() / 4.0;
            double offsetZ = (random.nextDouble() * 2 - 1) * entity.getBbWidth() / 4.0;

            if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ <= 1.0) {
                double posX = entity.getX() + offsetX;
                double posY = entity.getY() + entity.getBbHeight() / 2 + offsetY;
                double posZ = entity.getZ() + offsetZ;

                double speedX = offsetX;
                double speedY = offsetY + 0.2;
                double speedZ = offsetZ;

                entity.level().addParticle(particleType, posX, posY, posZ, speedX, speedY, speedZ);
            }
        }
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix(mode.getCurrentMode());

        if (shouldCritical && criticalTicks > 0) {
            performCritical();
            criticalTicks--;

            if (criticalTicks <= 0) {
                shouldCritical = false;
            }
        }

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

            if (mode.getCurrentMode().equals("GrimStuck") || mode.getCurrentMode().equals("GrimTimer")) {
                start = true;
            }
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
            }
        }
    }

    private void handleGrimStuckLogic() {
        if (fire.getCurrentValue() && start && getOffGroundTicks() > 3) {
            if (System.currentTimeMillis() - lastMsTimer >= 500L) {
                for (int i = 0; i <= 8; i++) {
                    if (Aura.target != null) {
                        spawnLegacyParticles(Aura.target, ParticleTypes.FLAME);
                    }
                }
                lastMsTimer = System.currentTimeMillis();
            }
        }

        if (Aura.target != null && start) {
            if (mc.player.fallDistance > 0 || getOffGroundTicks() > 3) {
                double d0 = mc.player.getX() - getXLast();
                double d1 = mc.player.getY() - getYLast();
                double d2 = mc.player.getZ() - getZLast();
                double d3 = (mc.player.getYRot() - getYRotLast());
                double d4 = (mc.player.getXRot() - getXRotLast());

                boolean positionChanged = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D;
                boolean rotationChanged = d3 != 0.0D || d4 != 0.0D;

                if (positionChanged && rotationChanged) {
                    sendPositionPacket(ServerboundMovePlayerPacket.PosRot.class,
                            mc.player.getX(), mc.player.getY() - 0.03, mc.player.getZ(),
                            mc.player.getYRot(), mc.player.getXRot());
                } else if (positionChanged) {
                    sendPositionPacket(ServerboundMovePlayerPacket.Pos.class,
                            mc.player.getX(), mc.player.getY() - 0.03, mc.player.getZ());
                } else if (rotationChanged) {
                    sendPositionPacket(ServerboundMovePlayerPacket.Rot.class,
                            mc.player.getYRot(), mc.player.getXRot());
                } else {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false));
                }
                setSkipTicks(getSkipTicks() + 1);
            }
        } else {
            start = false;
        }
    }

    private void handleGrimTimerLogic() {
        if (Aura.target != null && mc.player.onGround()) {
            if (autoJump.getCurrentValue()) {
                mc.options.keyJump.setDown(true);
            }
            setMsPerTick(getTimer(), 50F);
        }

        if (Aura.target != null && !mc.player.onGround()) {
            mc.options.keySprint.setDown(false);
            double fallDistance = mc.player.fallDistance;
            double maxFallDistance = getMaxFallDistance();

            if (fallDistance > 0 && fallDistance < maxFallDistance / 2) {
                setMsPerTick(getTimer(), idleTime_Value.getCurrentValue());
                if (C03.getCurrentValue()) sendC03Packet(true);
            } else if (fallDistance >= maxFallDistance / downHeight_Value.getCurrentValue()) {
                setMsPerTick(getTimer(), 50F);
            }
            if (C03.getCurrentValue()) sendC03Packet(false);
        }

        if (mc.player.onGround() || mc.player.hurtTime > 0) {
            setMsPerTick(getTimer(), 50F);
        }
    }

    private double getMaxFallDistance() {
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

        if (getTimer() != null) {
            setMsPerTick(getTimer(), 50F);
        }

        mc.options.keyJump.setDown(false);
        mc.options.keySprint.setDown(true);
    }
}