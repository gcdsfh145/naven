package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventManager;
import jnic.JNICInclude;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import sun.misc.Unsafe;
import net.minecraft.client.Minecraft;

@JNICInclude
@ModuleInfo(
        name = "Disabler",
        category = Category.MISC,
        description = "Disables some checks of the anti cheat."
)
public class Disabler extends Module {
    public static Disabler Instance;
    private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue acaaimstep = ValueBuilder.create(this, "ACAAimStep").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue acaperfectrotation = ValueBuilder.create(this, "ACAPerfectRotation").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue grimDuplicateRotPlace = ValueBuilder.create(this, "GrimDuplicateRotPlace").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue grimRotPlace = ValueBuilder.create(this, "GrimRotPlace").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue deBug = ValueBuilder.create(this, "DeBug").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue AntiC02 = ValueBuilder.create(this, "AntiC02").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue AntiC07 = ValueBuilder.create(this, "AntiC07").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue AntiC03 = ValueBuilder.create(this, "AntiC03").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue AntiC0F = ValueBuilder.create(this, "AntiC0F").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue AntiC0E = ValueBuilder.create(this, "AntiC0E").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue grim_Post = ValueBuilder.create(this, "Grim-Post").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue grim_fastBreak = ValueBuilder.create(this, "Grim-FastBreak").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue grim_badF = ValueBuilder.create(this, "Grim-BadF").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue inventorySwingBypassed = ValueBuilder.create(this, "Inventory Swing Bypassed").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue BadPacketsA = ValueBuilder.create(this, "BadPacketsA").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue vulcanDisabler = ValueBuilder.create(this, "VulcanDisabler").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue vulcanModulo = ValueBuilder.create(this, "Vulcan-Modulo").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue vulcanVelocity = ValueBuilder.create(this, "Vulcan-Velocity").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue vulcanAutoClicker = ValueBuilder.create(this, "Vulcan-AutoClicker").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue vulcanSprint = ValueBuilder.create(this, "Vulcan-Sprint").setDefaultBooleanValue(false).build().getBooleanValue();
//    private final BooleanValue vulcanScaffold = ValueBuilder.create(this, "Vulcan-Scaffold").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue BadPacketsF = ValueBuilder.create(this, "BadPacketsF").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue BadPacketsG = ValueBuilder.create(this, "BadPacketsG").setDefaultBooleanValue(false).build().getBooleanValue();
    private float playerYaw;
    private float deltaYaw;
    private float lastPlacedDeltaYaw;
    private final TimerUtils swingTimer = new TimerUtils();
    private boolean rotated = false;
    private float lastYaw = 0.0F;
    private float lastPitch = 0.0F;
    private boolean sprinting;
    private boolean sneaking;
//    private boolean lastSprinting = false;
    private float prevSlot = -1.0F;
    private int newLastSlot = -1;
    private final Random random = new Random();
    private static final double[] PERFECT_PATTERNS = new double[]{0.1, 0.25};
//    private static final double EPSILON = 1.0E-10;
    private static final Unsafe unsafe;
    private static final Minecraft mc = Minecraft.getInstance();
    private static boolean lastResult;
    public static List<Packet<PacketListener>> storedPackets;
    public static ConcurrentLinkedDeque<Integer> pingPackets;
    private boolean isPre;
    private final LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<TimePacket> vulcanVelocityPackets = new ConcurrentLinkedQueue<>();
//    private int attackAmount = 0;
//    private long lastMS = 0;
//    private long lastHurtTime = 0;

    public Disabler() {
        Instance = this;
    }

    @Override
    public void onDisable() {
        if (vulcanVelocityPackets.isEmpty() || mc.level == null || mc.player == null) return;
        vulcanVelocityPackets.forEach(packet -> {
            sendNoEvent(packet.getPacket());
            vulcanVelocityPackets.remove(packet);
        });
    }

    public void onEnabled() {
        sprinting = false;
        sneaking = false;
        this.prevSlot = -1.0F;
        this.newLastSlot = -1;
        if (mc.getConnection() != null) {
            this.connection = mc.getConnection().getConnection();
        }
    }

    private void log(String message) {
        if (this.logging.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private float normalizeYaw(float yaw) {
        while (yaw > 180.0F) {
            yaw -= 360.0F;
        }

        while (yaw < -180.0F) {
            yaw += 360.0F;
        }

        return yaw;
    }

    private boolean shouldModifyRotation(float currentYaw, float currentPitch) {
        if (this.lastYaw == 0.0F && this.lastPitch == 0.0F) {
            return false;
        } else {
            double yawDelta = (double)Math.abs(this.normalizeYaw(currentYaw - this.lastYaw));
            double pitchDelta = (double)Math.abs(currentPitch - this.lastPitch);
            boolean isStepYaw = yawDelta < 1.0E-5 && pitchDelta > 1.0;
            boolean isStepPitch = pitchDelta < 1.0E-5 && yawDelta > 1.0;
            return isStepYaw || isStepPitch;
        }
    }

    private float[] getModifiedRotation(float yaw, float pitch) {
        double yawDelta = (double)Math.abs(this.normalizeYaw(yaw - this.lastYaw));
        double pitchDelta = (double)Math.abs(pitch - this.lastPitch);
        float newYaw = yaw;
        float newPitch = pitch;
        if (yawDelta < 1.0E-5 && pitchDelta > 1.0) {
            newYaw = this.lastYaw + (float)(this.random.nextGaussian() * 0.001);
        }

        if (pitchDelta < 1.0E-5 && yawDelta > 1.0) {
            newPitch = this.lastPitch + (float)(this.random.nextGaussian() * 0.001);
        }

        return new float[]{newYaw, newPitch};
    }

    private float[] getAntiPerfectRotation(float yaw, float pitch) {
        if (this.lastYaw == 0.0F && this.lastPitch == 0.0F) {
            return new float[]{yaw, pitch};
        } else {
            double yawDelta = (double)Math.abs(this.normalizeYaw(yaw - this.lastYaw));
            double pitchDelta = (double)Math.abs(pitch - this.lastPitch);
            float newYaw = yaw;
            float newPitch = pitch;
            if (!this.isNoRotation(yawDelta) && this.isPerfectPattern(yawDelta)) {
                double jitter = this.random.nextGaussian() * 0.005;
                newYaw = yaw + (float)jitter;
            }

            if (!this.isNoRotation(pitchDelta) && this.isPerfectPattern(pitchDelta)) {
                double jitter = this.random.nextGaussian() * 0.005;
                newPitch = pitch + (float)jitter;
            }

            return new float[]{newYaw, newPitch};
        }
    }

    private boolean isNoRotation(double rotation) {
        return Math.abs(rotation) <= 1.0E-10 || this.isIntegerMultiple(360.0, rotation);
    }

    private boolean isPerfectPattern(double rotation) {
        if (!Double.isInfinite(rotation) && !Double.isNaN(rotation)) {
            for (double pattern : PERFECT_PATTERNS) {
                if (this.isIntegerMultiple(pattern, rotation)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private boolean isIntegerMultiple(double reference, double value) {
        if (reference == 0.0) {
            return Math.abs(value) <= 1.0E-10;
        } else {
            double multiple = value / reference;
            return Math.abs(multiple - (double)Math.round(multiple)) <= 1.0E-10;
        }
    }

    public static float getPacketYRot(ServerboundMovePlayerPacket packet) {
        if (mc.gameMode == null) {
            return 0.0F;
        } else {
            Field yRotField = findField(packet.getClass(), "f_134121_");

            try {
                return yRotField.getFloat(packet);
            } catch (Exception var3) {
                FileManager.logger.error("Failed to get yrot field", var3);
                var3.printStackTrace();
                return 0.0F;
            }
        }
    }

    public static float getPacketXRot(ServerboundMovePlayerPacket packet) {
        if (mc.gameMode == null) {
            return 0.0F;
        } else {
            Field xRotField = findField(packet.getClass(), "f_134122_");

            try {
                return xRotField.getFloat(packet);
            } catch (Exception var3) {
                FileManager.logger.error("Failed to get xrot field", var3);
                var3.printStackTrace();
                return 0.0F;
            }
        }
    }

    private static Field findField(Class<?> clazz, String... fieldNames) {
        if (clazz != null && fieldNames != null && fieldNames.length != 0) {
            Exception failed = null;

            for (Class<?> currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass()) {
                for (String fieldName : fieldNames) {
                    if (fieldName != null) {
                        try {
                            Field f = currentClass.getDeclaredField(fieldName);
                            f.setAccessible(true);
                            if ((f.getModifiers() & 16) != 0) {
                                unsafe.putInt(f, (long)unsafe.arrayBaseOffset(boolean[].class), f.getModifiers() & -17);
                            }

                            return f;
                        } catch (Exception var9) {
                            failed = var9;
                        }
                    }
                }
            }

            throw new Disabler.UnableToFindFieldException(failed);
        } else {
            throw new IllegalArgumentException("Class and fieldNames must not be null or empty");
        }
    }

    public static void setPacketYRot(ServerboundMovePlayerPacket packet, float yRot) {
        if (mc.gameMode != null) {
            Field yRotField = findField(packet.getClass(), "f_134121_");

            try {
                yRotField.setFloat(packet, yRot);
            } catch (Exception var4) {
                FileManager.logger.error("Failed to set yrot field", var4);
                var4.printStackTrace();
            }
        }
    }

    public static void setPacketXRot(ServerboundMovePlayerPacket packet, float xRot) {
        if (mc.gameMode != null) {
            Field xRotField = findField(packet.getClass(), "f_134122_");

            try {
                xRotField.setFloat(packet, xRot);
            } catch (Exception var4) {
                FileManager.logger.error("Failed to set xrot field", var4);
                var4.printStackTrace();
            }
        }
    }

    @EventTarget(3)
    public void duplicateRotPlaceDisabler(EventPacket e) {
        if (this.grimDuplicateRotPlace.currentValue && e.getType() == EventType.SEND && !e.isCancelled() && mc.player != null) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
                ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) e.getPacket();
                if (packet.hasRotation()) {
                    if (packet.getYRot(0.0F) < 360.0F && packet.getYRot(0.0F) > -360.0F) {
                        if (packet.hasPosition()) {
                            e.setPacket(
                                    new PosRot(
                                            packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), packet.getYRot(0.0F) + 720.0F, packet.getXRot(0.0F), packet.isOnGround()
                                    )
                            );
                        } else {
                            e.setPacket(new Rot(packet.getYRot(0.0F) + 720.0F, packet.getXRot(0.0F), packet.isOnGround()));
                        }
                    }

                    float lastPlayerYaw = this.playerYaw;
                    this.playerYaw = packet.getYRot(0.0F);
                    this.deltaYaw = Math.abs(this.playerYaw - lastPlayerYaw);
                    this.rotated = true;
                    if (this.deltaYaw > 2.0F) {
                        float xDiff = Math.abs(this.deltaYaw - this.lastPlacedDeltaYaw);
                        if ((double) xDiff < 1.0E-4) {
                            this.log("Disabling DuplicateRotPlace!");
                            if (packet.hasPosition()) {
                                e.setPacket(
                                        new PosRot(
                                                packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), packet.getYRot(0.0F) + 0.002F, packet.getXRot(0.0F), packet.isOnGround()
                                        )
                                );
                            } else {
                                e.setPacket(new Rot(packet.getYRot(0.0F) + 0.002F, packet.getXRot(0.0F), packet.isOnGround()));
                            }
                        }
                    }
                }
            } else if (e.getPacket() instanceof ServerboundUseItemOnPacket && this.rotated) {
                this.lastPlacedDeltaYaw = this.deltaYaw;
                this.rotated = false;
            }
        }
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundPlayerCommandPacket wrapped) {
            if (BadPacketsF.getCurrentValue()) {
                if (wrapped.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
                    if (sprinting)
                        event.setCancelled(true);

                    sprinting = true;
                }

                if (wrapped.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                    if (!sprinting)
                        event.setCancelled(true);

                    sprinting = false;
                }
            }

            if (BadPacketsG.getCurrentValue()) {
                if (wrapped.getAction() == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY) {
                    if (sneaking)
                        event.setCancelled(true);

                    sneaking = true;
                }

                if (wrapped.getAction() == ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY) {
                    if (!sneaking)
                        event.setCancelled(true);

                    sneaking = false;
                }
            }
        }
        if (mc.screen instanceof InventoryScreen && inventorySwingBypassed.getCurrentValue()) {
            if (packet instanceof ServerboundSwingPacket) {
                if (!swingTimer.hasTimeElapsed(250L, true)) {
                    event.setCancelled(true);
                    if (logging.getCurrentValue()) {
                        log("Inventory Swing Bypassed: Cancelled swing packet");
                    }
                }
            }
        }
        if (mc.player != null && !mc.isLocalServer()) {
            if (this.BadPacketsA.getCurrentValue() && packet instanceof ServerboundSetCarriedItemPacket) {
                int slot = ((ServerboundSetCarriedItemPacket) packet).getSlot();
                if (slot == this.newLastSlot && slot != -1) {
                    event.setCancelled(true);
                    if (this.deBug.getCurrentValue()) {
                        this.log("BadPacketsA: Cancelled duplicate slot packet");
                    }
                }
                this.newLastSlot = slot;
            } else if (this.BadPacketsA.getCurrentValue() && packet instanceof ServerboundSetCarriedItemPacket serverboundsetcarrieditempacket) {
                if ((float) serverboundsetcarrieditempacket.getSlot() == this.prevSlot) {
                    event.setCancelled(true);
                    if (this.deBug.getCurrentValue()) {
                        this.log("NoBadPacketsA");
                    }
                } else {
                    this.prevSlot = (float) serverboundsetcarrieditempacket.getSlot();
                }
            }
//            if (this.noNewBadPacketsF.getCurrentValue() && packet instanceof ServerboundPlayerCommandPacket) {
//                ServerboundPlayerCommandPacket actionPacket = (ServerboundPlayerCommandPacket) packet;
//                Action action = actionPacket.getAction();
//
//                if (action == Action.START_SPRINTING) {
//                    if (this.lastSprinting) {
//                        event.setCancelled(true);
//                        if (this.deBug.getCurrentValue()) {
//                            this.log("NoNewBadPacketsF: Cancelled duplicate sprint start");
//                        }
//                    }
//                    this.lastSprinting = true;
//                } else if (action == Action.STOP_SPRINTING) {
//                    if (!this.lastSprinting) {
//                        event.setCancelled(true);
//                        if (this.deBug.getCurrentValue()) {
//                            this.log("NoNewBadPacketsF: Cancelled duplicate sprint stop");
//                        }
//                    }
//                    this.lastSprinting = false;
//                }
//            } else if (this.noBadPacketsF.getCurrentValue()) {
//                if (Naven.moduleManager.getModule(Blink.class).getState()) {
//                    return;
//                }
//
//                if (packet instanceof ServerboundPlayerCommandPacket serverboundplayercommandpacket) {
//                    if (serverboundplayercommandpacket.getAction() == Action.START_SPRINTING) {
//                        if (this.lastSprinting) {
//                            PacketUtilss.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
//                        }
//
//                        this.lastSprinting = true;
//                        if (this.deBug.getCurrentValue()) {
//                            this.log("NoBadPacketsF A");
//                        }
//                    } else if (serverboundplayercommandpacket.getAction() == Action.STOP_SPRINTING) {
//                        if (!this.lastSprinting) {
//                            PacketUtilss.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.START_SPRINTING));
//                        }
//
//                        this.lastSprinting = false;
//                        if (this.deBug.getCurrentValue()) {
//                            this.log("NoBadPacketsF B");
//                       }
//                    }
//               }
//            }
        }

        if (mc.player == null || mc.level == null) return;

        if (grim_fastBreak.getCurrentValue()) {
            if (packet instanceof ServerboundPlayerActionPacket actionPacket && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                if (this.deBug.getCurrentValue()) {
                    this.log("Grim-FastBreak: Sending ABORT_DESTROY_BLOCK packet");
                }
                sendNoEvent(OldNaming.C07PacketPlayerDigging(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, actionPacket.getPos(), actionPacket.getDirection()));
            }
        }
        if (grim_badF.getCurrentValue()) {
            if (packet instanceof ServerboundPlayerCommandPacket) {
                if (!isPre) {
                    if (this.deBug.getCurrentValue()) {
                        this.log("Grim-BadF: Queuing player command packet");
                    }
                    event.setCancelled(true);
                    packets.add(packet);
                }
            }
        }

//        if (vulcanDisabler.getCurrentValue()) {
//            if (vulcanModulo.getCurrentValue() && packet instanceof ServerboundMovePlayerPacket) {
//                try {
//                    Field xField = ServerboundMovePlayerPacket.class.getDeclaredField("x");
//                    Field zField = ServerboundMovePlayerPacket.class.getDeclaredField("z");
//                    xField.setAccessible(true);
//                    zField.setAccessible(true);
//                    double x = xField.getDouble(packet);
//                    double z = zField.getDouble(packet);
//                    xField.setDouble(packet, x + Math.random() / 10000);
//                    zField.setDouble(packet, z + Math.random() / 10000);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//            if (vulcanVelocity.getCurrentValue() && (packet instanceof ServerboundPongPacket || packet instanceof ServerboundKeepAlivePacket)) {
//                vulcanVelocityPackets.offer(new TimePacket(System.currentTimeMillis(), packet));
//                event.setCancelled(true);
//            }
//
//            if (vulcanVelocity.getCurrentValue() && vulcanVelocityPackets.stream().anyMatch(p -> (System.currentTimeMillis() - p.getTime() > 5000) || vulcanVelocityPackets.size() > 50)) {
//                vulcanVelocityPackets.forEach(timePacket -> {
//                    sendNoEvent(timePacket.getPacket());
//                    vulcanVelocityPackets.remove(timePacket);
//                });
//            }
//
//            if (vulcanAutoClicker.getCurrentValue() && packet instanceof ServerboundInteractPacket) {
//                try {
//                    Field actionField = ServerboundInteractPacket.class.getDeclaredField("action");
//                    actionField.setAccessible(true);
//                    Object action = actionField.get(packet);
//
//                    Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ServerboundInteractPacket$Action");
//                    Object attackAction = Enum.valueOf((Class<Enum>)actionClass, "ATTACK");
//
//                    if (action.equals(attackAction)) {
//                        attackAmount++;
//                    }
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//            if (vulcanAutoClicker.getCurrentValue() && attackAmount > 15) {
//                sendNoEvent(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, BlockPos.ZERO, Direction.DOWN));
//                attackAmount = 0;
//            }
//
//            if (vulcanSprint.getCurrentValue() && packet instanceof ServerboundPlayerCommandPacket) {
//                event.setCancelled(true);
//            }
//       }
    }

    public void send(Packet<?> pPacket) {
        EventPackets packet = new EventPackets(pPacket);
        EventManager.call2(packet);

        if (packet.isCancelled()) {
            return;
        }

        this.connection.send(pPacket);
    }

    private Connection connection;

    public void sendNoEvent(Packet<?> pPacket) {
        this.send(pPacket);
    }

//    @EventTarget
//    private void onPacket(PacketEvent2 event) {
//        Packet<?> packet = event.getPacket();
//        if (packet instanceof ServerboundMovePlayerPacket && this.AntiC03.getCurrentValue()) {
//            event.setCancelled(true);
//            this.log("[AntiC03] Attempted cancel -> " + packet.getClass().getSimpleName());
//        }
//
//        if (packet instanceof ServerboundPlayerActionPacket && this.AntiC07.getCurrentValue()) {
//            event.setCancelled(true);
//            this.log("[AntiC07] Attempted cancel -> " + packet.getClass().getSimpleName());
//        }
//
//        if (packet instanceof ServerboundPongPacket && this.AntiC0F.getCurrentValue()) {
//            event.setCancelled(true);
//            this.log("[AntiC0F] Attempted cancel -> " + packet.getClass().getSimpleName());
//        }
//
//        if (packet instanceof ServerboundInteractPacket && this.AntiC02.getCurrentValue()) {
//            event.setCancelled(true);
//            this.log("[AntiC02] Attempted cancel -> " + packet.getClass().getSimpleName());
//        }
//
//        if (packet instanceof ServerboundContainerClickPacket && this.AntiC0E.getCurrentValue()) {
//            event.setCancelled(true);
//            this.log("[AntiC0E] Attempted cancel -> " + packet.getClass().getSimpleName());
//        }
//    }

    @EventTarget(3)
    public void grimRotPlace(EventPacket e) {
        if (this.grimRotPlace.currentValue && e.getType() == EventType.SEND && !e.isCancelled() && mc.player != null) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
                ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) e.getPacket();
                if (packet.hasRotation()) {
                    if (packet.getYRot(0.0F) < 360.0F && packet.getYRot(0.0F) > -360.0F) {
                        if (packet.hasPosition()) {
                            e.setPacket(
                                    new PosRot(
                                            packet.getX(0.0),
                                            packet.getY(0.0),
                                            packet.getZ(0.0),
                                            packet.getYRot(0.0F) + 720.0F,
                                            packet.getXRot(0.0F),
                                            packet.isOnGround()
                                    )
                            );
                        } else {
                            e.setPacket(new Rot(packet.getYRot(0.0F) + 720.0F, packet.getXRot(0.0F), packet.isOnGround()));
                        }
                    }

                    float lastPlayerYaw = this.playerYaw;
                    this.playerYaw = packet.getYRot(0.0F);
                    this.deltaYaw = Math.abs(this.playerYaw - lastPlayerYaw);
                    this.rotated = true;
                    if (this.deltaYaw > 2.0F) {
                        float xDiff = Math.abs(this.deltaYaw - this.lastPlacedDeltaYaw);
                        if (xDiff < 1.0E-4) {
                            this.log("Perfect repair when turning the head!");
                            if (packet.hasPosition()) {
                                e.setPacket(
                                        new PosRot(
                                                packet.getX(0.0),
                                                packet.getY(0.0),
                                                packet.getZ(0.0),
                                                packet.getYRot(0.0F) + 0.002F,
                                                packet.getXRot(0.0F),
                                                packet.isOnGround()
                                        )
                                );
                            } else {
                                e.setPacket(new Rot(packet.getYRot(0.0F) + 0.002F, packet.getXRot(0.0F), packet.isOnGround()));
                            }
                        }
                    }
                }
            } else if (e.getPacket() instanceof ServerboundUseItemOnPacket && this.rotated) {
                this.lastPlacedDeltaYaw = this.deltaYaw;
                this.rotated = false;
            }
        }

        if ((this.acaaimstep.currentValue || this.acaperfectrotation.currentValue) && e.getPacket() instanceof ServerboundMovePlayerPacket movePacket) {
            float currentYaw = getPacketYRot(movePacket);
            float currentPitch = getPacketXRot(movePacket);
            boolean modified = false;
            if (this.acaaimstep.currentValue && this.shouldModifyRotation(currentYaw, currentPitch)) {
                float[] modifiedRotation = this.getModifiedRotation(currentYaw, currentPitch);
                currentYaw = modifiedRotation[0];
                currentPitch = modifiedRotation[1];
                modified = true;
            }

            if (this.acaperfectrotation.currentValue) {
                float[] antiPerfectRotation = this.getAntiPerfectRotation(currentYaw, currentPitch);
                if (antiPerfectRotation[0] != currentYaw || antiPerfectRotation[1] != currentPitch) {
                    currentYaw = antiPerfectRotation[0];
                    currentPitch = antiPerfectRotation[1];
                    modified = true;
                    this.log("PerfectRotation: Modified rotation");
                }
            }

            if (modified) {
                setPacketYRot(movePacket, currentYaw);
                setPacketXRot(movePacket, MathUtils.clampPitch_To90(currentPitch));
            }

            this.lastYaw = getPacketYRot(movePacket);
            this.lastPitch = getPacketXRot(movePacket);
        }
    }

    @EventTarget
    private void onMotion(EventMotion eventMotion) {
        if (mc.player == null || mc.level == null) return;

        isPre = eventMotion.getType() == EventType.PRE;

        if (eventMotion.getType() == EventType.POST) {
            try {
                while (!packets.isEmpty()) {
                    sendNoEvent(packets.take());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

//        if (vulcanDisabler.getCurrentValue() && eventMotion.getType() == EventType.PRE) {
//            if (vulcanScaffold.getCurrentValue()) {
//                if (mc.player.tickCount % 10 == 0) {
//                    sendNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
//                }
//                if (mc.player.tickCount % 10 == 2) {
//                    sendNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
//                }
//            }

//            if (vulcanSprint.getCurrentValue()) {
//                if (MoveUtils.isMoving()) {
//                    if (mc.player.tickCount % 2 == 0) {
//                        sendNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
//                    } else {
//                        sendNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
//                    }
//                } else {
//                    sendNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
//                }
//            }
//        }
//        }
    }

    public boolean getGrimPost() {
        boolean result = isValidPostCondition();

        if (lastResult && !result) {
            lastResult = false;
            if (this.deBug.getCurrentValue()) {
                this.log("Grim-Post: Condition changed, processing stored packets");
            }
            mc.execute(this::processPackets);
        }

        lastResult = result;
        return result;
    }

    private boolean isValidPostCondition() {
        return Disabler.Instance != null
                && Disabler.Instance.getState()
                && grim_Post.getCurrentValue()
                && mc.player != null
                && mc.level != null
                && mc.player.isAlive()
                && mc.player.tickCount >= 10
                && !Naven.moduleManager.getModule(Blink.class).getState();
    }

    public synchronized void processPackets() {
        if (!storedPackets.isEmpty()) {
            for (Packet<PacketListener> packet : storedPackets) {
                if (this.deBug.getCurrentValue()) {
                    this.log("Grim-Post: Processing " + storedPackets.size() + " stored packets");
                }
                EventPackets event = new EventPackets(packet);
                EventManager.instance.call2(event);

                if (event.isCancelled() || mc.player == null || mc.player.isRemoved() || mc.getConnection() == null || mc.level == null || !mc.getConnection().getConnection().isConnected()) {
                    if (this.deBug.getCurrentValue()) {
                        this.log("Grim-Post: Sending stored packet " + packet.getClass().getSimpleName());
                    }
                    continue;
                }

                packet.handle(mc.getConnection());
            }
            storedPackets.clear();
        } else if (this.deBug.getCurrentValue()) {
            this.log("Grim-Post: No stored packets to process");
        }
    }

    public boolean grimPostDelay(Packet<?> packet) {
        if (mc.player == null) {
            return false;
        }

        if (packet instanceof ClientboundSetEntityMotionPacket sPacketEntityVelocity) {
            return sPacketEntityVelocity.getId() == mc.player.getId();
        }

        return packet instanceof ClientboundExplodePacket
                || packet instanceof ClientboundPingPacket
                || packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundSetEquipmentPacket
                || packet instanceof ClientboundKeepAlivePacket
                || packet instanceof ClientboundSetHealthPacket
                || packet instanceof ClientboundMoveEntityPacket
                || packet instanceof ClientboundAddPlayerPacket
                || packet instanceof ClientboundCustomPayloadPacket;
    }

    public void fixC0F(ServerboundPongPacket packet) {
        int id = packet.getId();
        if (id >= 0 || pingPackets.isEmpty()) {
            this.sendNoEvent(packet);
        } else {
            do {
                int current = pingPackets.peekFirst();
                this.sendNoEvent(new ServerboundPongPacket(current));
                pingPackets.pollFirst();
                if (current == id) {
                    break;
                }
            } while (!pingPackets.isEmpty());
        }
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }

        lastResult = false;
        storedPackets = new CopyOnWriteArrayList<Packet<PacketListener>>();
        pingPackets = new ConcurrentLinkedDeque<Integer>();
    }

    private static class UnableToFindFieldException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UnableToFindFieldException(Exception e) {
            super(e);
        }
    }

    private static class TimePacket {
        private final long time;
        private final Packet<?> packet;

        public TimePacket(long time, Packet<?> packet) {
            this.time = time;
            this.packet = packet;
        }

        public long getTime() {
            return time;
        }

        public Packet<?> getPacket() {
            return packet;
        }
    }
}