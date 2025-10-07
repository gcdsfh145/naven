package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MSTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
        name = "HackerDetector",
        description = "HackerDetector",
        category = Category.COMBAT
)
public class HackerDetector extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final BooleanValue debugModeValue = ValueBuilder.create(this, "Debug").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue notifyValue = ValueBuilder.create(this, "Notify").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue noHacker = ValueBuilder.create(this, "noHacker").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue vlValue = ValueBuilder.create(this, "VL").setDefaultFloatValue(30).setMinFloatValue(10).setMaxFloatValue(50).build().getFloatValue();
    private final BooleanValue reachCheck = ValueBuilder.create(this, "Reach").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue timerCheck = ValueBuilder.create(this, "Timer").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue velocityCheck = ValueBuilder.create(this, "Velocity").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue autoClickCheck = ValueBuilder.create(this, "AutoClick").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue flightCheck = ValueBuilder.create(this, "Flight").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue speedCheck = ValueBuilder.create(this, "Speed").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue noSlowCheck = ValueBuilder.create(this, "NoSlow").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue legitScaffoldCheck = ValueBuilder.create(this, "LegitScaffold").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue attackUseCheck = ValueBuilder.create(this, "AttackUseCheck").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue detailedVL = ValueBuilder.create(this, "DetailedVL").setDefaultBooleanValue(false).build().getBooleanValue();

    private final ConcurrentHashMap<Player, HackerData> hackerDataMap = new ConcurrentHashMap<>();
    private final MSTimer timePassed = new MSTimer();
    public static final CopyOnWriteArrayList<String> hackers = new CopyOnWriteArrayList<>();
    private short sneakFlag = 0;
    private NotificationManager notificationManager;

    @Override
    public void onEnable() {
        this.hackerDataMap.clear();
        hackers.clear();
        this.notificationManager = new NotificationManager();
    }

    @Override
    public void onDisable() {
        hackers.clear();
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        this.hackerDataMap.keySet().removeIf(LivingEntity::isDeadOrDying);
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        Packet<?> packet = event.getPacket();
        if (!event.isCancelled() && mc.level != null) {
            if (packet instanceof ClientboundEntityEventPacket) {
                Entity entity = ((ClientboundEntityEventPacket) packet).getEntity(mc.level);
                if (entity instanceof LivingEntity) {
                    if (!(entity instanceof Player) || mc.player == null || mc.level == null) {
                        return;
                    }

                    Player player = null;
                    int i = 0;

                    for (Player abstractclientplayer : mc.level.players()) {
                        if (!(abstractclientplayer.distanceTo(entity) > 7.0F) && !abstractclientplayer.equals(entity)) {
                            i++;
                            player = abstractclientplayer;
                        }
                    }

                    if (i != 1) {
                        return;
                    }

                    HackerData hackerData = this.hackerDataMap.get(player);
                    if (hackerData == null) {
                        return;
                    }

                    if (this.reachCheck.getCurrentValue()) {
                        double distance = (double) player.distanceTo(entity);
                        if (distance > 3.3 && distance < 3.7) {
                            hackerData.flag("Reach (A)", 5, this.detailedVL.getCurrentValue() ? "(Reach: " + distance + ")" : "");
                        } else if (distance > 3.7) {
                            hackerData.flag("Reach (B)", 10, this.detailedVL.getCurrentValue() ? "(Reach: " + distance + ")" : "");
                        }
                    }
                }
            } else if (packet instanceof ClientboundAnimatePacket) {
                Entity entity1 = mc.level.getEntity(((ClientboundAnimatePacket) packet).getId());
                if (entity1 instanceof Player && ((ClientboundAnimatePacket) packet).getAction() == 0) {
                    HackerData hackerData = this.hackerDataMap.get(entity1);
                    if (hackerData != null) {
                        hackerData.tempCps++;
                    }
                }
            } else if (packet instanceof ClientboundTeleportEntityPacket) {
                Entity entity2 = mc.level.getEntity(((ClientboundTeleportEntityPacket) packet).getId());
                if (entity2 instanceof Player) {
                    this.checkPlayer((Player) entity2);
                }
            } else if (packet instanceof ClientboundMoveEntityPacket) {
                ClientboundMoveEntityPacket movePacket = (ClientboundMoveEntityPacket) packet;
                try {
                    java.lang.reflect.Field entityIdField = ClientboundMoveEntityPacket.class.getDeclaredField("entityId");
                    entityIdField.setAccessible(true);
                    int entityId = (int) entityIdField.get(movePacket);

                    Entity entity3 = mc.level.getEntity(entityId);
                    if (entity3 instanceof Player) {
                        this.checkPlayer((Player) entity3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void checkPlayer(Player player) {
        if (player != mc.player) {
            this.hackerDataMap.computeIfAbsent(player, HackerData::new);
            HackerData hackerData = this.hackerDataMap.get(player);
            if (hackerData != null) {
                hackerData.update();
                if (hackerData.aliveTicks < 20) {
                    return;
                }

                boolean flag = true;
                if (this.timerCheck.getCurrentValue()) {
                    long currentTime = System.currentTimeMillis();
                    double balance = hackerData.packetBalance;
                    long timeDiff = currentTime - hackerData.lastMovePacket;
                    balance += 50.0;
                    balance -= (double) timeDiff;
                    if (balance >= 100.0) {
                        int overshoot = (int) (balance / 50.0);
                        balance = -50.0;
                        hackerData.flag("Timer", 1, this.detailedVL.getCurrentValue() ? "(Overshot Timer " + overshoot + ")" : "");
                    } else if (balance < -1000.0) {
                        balance = -1000.0;
                    }

                    if (balance < 100.0) {
                        hackerData.packetBalance = balance;
                        hackerData.lastMovePacket = currentTime;
                    }
                }

                if (this.velocityCheck.getCurrentValue() && player.hurtTime > 0) {
                    double deltaX = Math.abs(player.xOld - player.getX());
                    double deltaZ = Math.abs(player.zOld - player.getZ());
                    if (player.hurtTime > 6 && player.hurtTime < 12 && deltaX <= 0.1 && deltaZ <= 0.01 &&
                            !player.level().getBlockCollisions(player, player.getBoundingBox().inflate(0.05, 0.0, 0.05)).iterator().hasNext()) {
                        hackerData.flag("Velocity (A)", 5, this.detailedVL.getCurrentValue() ? "No movement detected" : "");
                    }
                }

                if (this.autoClickCheck.getCurrentValue()) {
                    if (hackerData.cps > 12 && hackerData.cps < 20) {
                        hackerData.flag("AutoClick (A)", 2, this.detailedVL.getCurrentValue() ? "High CPS(CPS: " + hackerData.cps + ")" : "High CPS");
                        flag = false;
                    } else if (hackerData.cps >= 20) {
                        hackerData.flag("AutoClick (A)", 5, this.detailedVL.getCurrentValue() ? "High CPS(CPS: " + hackerData.cps + ")" : "High CPS");
                    }

                    if (hackerData.cps > 6 && hackerData.cps == hackerData.preCps) {
                        hackerData.flag("AutoClick (B)", 2, this.detailedVL.getCurrentValue() ? "Strange CPS(CPS: " + hackerData.cps + ")" : "Strange CPS");
                        flag = false;
                    }

                    if (Math.abs(player.yHeadRot - player.yRotO) > 50.0F && player.attackAnim != 0.0F && hackerData.cps >= 3) {
                        hackerData.flag("AutoClick (C)", 2, this.detailedVL.getCurrentValue() ?
                                "Yaw Rate(CPS: " + hackerData.cps + ", yawRot: " + Math.abs(player.yHeadRot - player.yRotO) + ")" : "");
                        flag = false;
                    }
                }

                if (this.attackUseCheck.getCurrentValue() && player.isUsingItem() && player.attackAnim != 0.0F) {
                    hackerData.flag("AttackUse", 5, this.detailedVL.getCurrentValue() ? "Using item" : "");
                }

                if (this.flightCheck.getCurrentValue() && player.getVehicle() == null && hackerData.airTicks > 5) {
                    if (Math.abs(hackerData.motionY - hackerData.lastMotionY) < (hackerData.airTicks >= 115 ? 0.001 : 0.005)) {
                        hackerData.flag("Flight (A)", 5, this.detailedVL.getCurrentValue() ?
                                "Glide(diff: " + Math.abs(hackerData.motionY - hackerData.lastMotionY) + ")" : "Glide");
                        flag = false;
                    }

                    if (hackerData.motionY > 0.52) {
                        hackerData.flag("Flight (B)", 5, this.detailedVL.getCurrentValue() ? "Yaxis(motY: " + hackerData.motionY + ")" : "Yaxis(A)");
                        flag = false;
                    }

                    if (hackerData.airTicks > 10 && hackerData.motionY > 0.0) {
                        hackerData.flag("Flight (C)", 5, this.detailedVL.getCurrentValue() ? "Yaxis(motY: " + hackerData.motionY + ")" : "Yaxis(B)");
                        flag = false;
                    }
                }

                if (this.speedCheck.getCurrentValue()) {
                    double speed = Math.abs(hackerData.motionXZ);
                    if (hackerData.airTicks == 0) {
                        double limit = 0.42;
                        if (hackerData.groundTicks < 5) {
                            limit += 0.1;
                        }

                        if (player.isCrouching()) {
                            limit *= 0.68;
                        }

                        if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                            limit += (double) player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier();
                            limit *= 1.5;
                        }

                        if (speed > limit) {
                            hackerData.flag("Speed (A)", 5, this.detailedVL.getCurrentValue() ? "Ground Speed(Speed: " + speed + ", Limit: " + limit + ")" : "Ground Speed");
                        }
                    } else {
                        double predictedSpeed = 0.36 * Math.pow(0.985, (double) (hackerData.airTicks + 1));
                        if (hackerData.airTicks >= 115) {
                            predictedSpeed = Math.max(0.08, predictedSpeed);
                        }

                        double threshold = 0.05;
                        if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                            predictedSpeed += (double) player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() * 0.05;
                            threshold *= 1.2;
                        }

                        if (player.hasEffect(MobEffects.SLOW_FALLING)) {
                            predictedSpeed += (double) player.getEffect(MobEffects.SLOW_FALLING).getAmplifier() * 0.05;
                        }

                        if (speed - predictedSpeed > threshold) {
                            hackerData.flag("Speed (B)", 5, this.detailedVL.getCurrentValue() ?
                                    "Air Speed(Speed: " + speed + ", Limit: " + threshold + ", Predict: " + predictedSpeed + ")" : "Air Speed");
                        }
                    }
                }

                if (this.noSlowCheck.getCurrentValue() && player.isUsingItem() &&
                        (hackerData.motionX > 0.7 || hackerData.motionZ > 0.7) && player.hurtTime == 0) {
                    hackerData.flag("NoSlow (A)", 2, this.detailedVL.getCurrentValue() ?
                            "UseItem X:" + hackerData.motionX + " Z:" + hackerData.motionZ : "");
                    flag = false;
                }

                if (this.legitScaffoldCheck.getCurrentValue()) {
                    if (player.isCrouching()) {
                        this.timePassed.reset();
                        this.sneakFlag++;
                    }

                    if (this.timePassed.hasTimePassed(140L)) {
                        this.sneakFlag = 0;
                    }

                    if (player.getXRot() > 75.0F && player.getXRot() < 90.0F && player.attackAnim != 0.0F &&
                            player.getMainHandItem().getItem() instanceof BlockItem) {
                        if (MoveUtils.getSpeed(player) >= 0.1 && player.onGround() && this.sneakFlag > 5) {
                            hackerData.flag("LegitScaffold", 5, this.detailedVL.getCurrentValue() ? "LegitScaffold fast sneak" : "");
                        }

                        if (MoveUtils.getSpeed(player) >= 0.21 && !player.onGround() && this.sneakFlag > 5) {
                            hackerData.flag("LegitScaffold", 5, this.detailedVL.getCurrentValue() ? "LegitScaffold fast sneak" : "");
                        }
                    }
                }

                if (flag) {
                    hackerData.vl = Math.max(0, hackerData.vl - 1);
                }

                if (hackerData.vl >= vlValue.getCurrentValue()) {
                    String hacks = String.join(",", hackerData.useHacks);
                    ChatUtils.addChatMessage("[HackerDetector] §f" + player.getName().getString() + " §ffailed §c" + hacks);
                    hackers.add(player.getName().getString());

                    if (notifyValue.getCurrentValue() && notificationManager != null) {
                        notificationManager.addNotification(new Notification(
                                NotificationLevel.WARNING,
                                player.getName().getString() + " might use hack (" + hacks + ")",
                                2000
                        ));
                    }

                    hackerData.vl = 0;
                }
            }
        }
    }

    public class HackerData {
        private final Player player;
        private final MSTimer cpsTimer = new MSTimer();
        private final CopyOnWriteArrayList<String> useHacks = new CopyOnWriteArrayList<>();
        private double packetBalance = 0.0;
        private long lastMovePacket = System.currentTimeMillis();
        private int aliveTicks = 0;
        private int airTicks = 0;
        private int groundTicks = 0;
        private double motionX = 0.0;
        private double motionY = 0.0;
        private double motionZ = 0.0;
        private double motionXZ = 0.0;
        private double lastMotionY = 0.0;
        private int cps = 0;
        private int preCps = 0;
        private int tempCps = 0;
        private int vl = 0;

        public HackerData(Player player) {
            this.player = player;
        }

        public void update() {
            this.aliveTicks++;
            if (this.cpsTimer.hasTimePassed(1000L)) {
                this.preCps = this.cps;
                this.cps = this.tempCps;
                this.tempCps = 0;
            }

            AABB aabb = this.player.getBoundingBox();
            if (this.player.level().getBlockCollisions(this.player,
                    new AABB(aabb.maxX, this.player.getY() - 1.0, aabb.maxZ, aabb.minX, this.player.getY(), aabb.minZ)).iterator().hasNext()) {
                this.groundTicks++;
                this.airTicks = 0;
            } else {
                this.airTicks++;
                this.groundTicks = 0;
            }

            this.lastMotionY = this.motionY;
            this.motionX = this.player.getX() - this.player.xOld;
            this.motionY = this.player.getY() - this.player.yOld;
            this.motionZ = this.player.getZ() - this.player.zOld;
            this.motionXZ = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        }

        public void flag(String type, int vlIncrement, String msg) {
            if (!this.useHacks.contains(type)) {
                this.useHacks.add(type);
            }

            if (debugModeValue.getCurrentValue()) {
                ChatUtils.addChatMessage("[HackerDetector] §f" + this.player.getName().getString() +
                        " §ffailed §a" + type + " §c" + msg + " §e" + this.vl + "+" + vlIncrement);
            }

            if (this.vl < vlValue.getCurrentValue()) {
                this.vl += vlIncrement;
            }

            if (this.vl >= vlValue.getCurrentValue()) {
                String hacks = String.join(",", this.useHacks);
                ChatUtils.addChatMessage("[HackerDetector] §f" + this.player.getName().getString() + " §ffailed §c" + hacks);
                hackers.add(this.player.getName().getString());

                if (notifyValue.getCurrentValue() && HackerDetector.this.notificationManager != null) {
                    HackerDetector.this.notificationManager.addNotification(new Notification(
                            NotificationLevel.WARNING,
                            this.player.getName().getString() + " might use hack (" + hacks + ")",
                            2000
                    ));
                }

                this.vl = 0;
            }
        }
    }
    @EventTarget
    public void onRender2D(com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D event) {
        if (notificationManager != null) {
            notificationManager.onRender(event);
        }
    }

    @EventTarget
    public void onShader(com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader event) {
        if (notificationManager != null) {
            notificationManager.onRenderShadow(event);
        }
    }
}