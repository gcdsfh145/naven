package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import jnic.JNICInclude;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.stats.StatsCounter;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;

import static com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura.target;


@JNICInclude
@ModuleInfo(
        name = "BackTrack",
        description = "Stuck Network,but adversaries",
        category = Category.COMBAT
)
public class BackTrack extends Module {
    public BooleanValue log = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue killAuraEnableCheck = ValueBuilder.create(this, "KillAura Enable Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue OnGroundStop = ValueBuilder.create(this, "On Ground Stop")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public FloatValue maxpacket = ValueBuilder.create(this, "Max Packet number")
            .setDefaultFloatValue(1000.0F)
            .setFloatStep(5.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5000.0F)
            .build()
            .getFloatValue();
    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();
    FloatValue delay = ValueBuilder.create(this, "Delay(Tick)")
            .setDefaultFloatValue(20.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(200.0F)
            .build()
            .getFloatValue();
    public BooleanValue btrender = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public ModeValue btrendermode = ValueBuilder.create(this, "Render Mode")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes(new String[]{"Normal", "LingDong"})
            .build()
            .getModeValue();
    public FloatValue textSize = ValueBuilder.create(this, "Text Size")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultFloatValue(0.35F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public BooleanValue targetNearbyCheck = ValueBuilder.create(this, "Target Nearby Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
    public FloatValue renderPosX = ValueBuilder.create(this, "RenderX")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(-1000.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build().getFloatValue();

    public FloatValue renderPosY = ValueBuilder.create(this, "RenderY")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(-1000.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build().getFloatValue();
    public ModeValue interceptMode = ValueBuilder.create(this, "Intercept Mode")
            .setDefaultModeIndex(1)
            .setModes(new String[]{"Original", "AllExceptSelf", "OnlyInterceptTarget"})
            .build()
            .getModeValue();
    public BooleanValue debugFilter = ValueBuilder.create(this, "Debug Filter")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue releaseWhenTP = ValueBuilder.create(this, "Release When TP/BPS Abnormal")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public FloatValue bpsThreshold = ValueBuilder.create(this, "BPS Threshold")
            .setDefaultFloatValue(12.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(5.0F)
            .setMaxFloatValue(50.0F)
            .build()
            .getFloatValue();
    public BooleanValue botCheck = ValueBuilder.create(this, "Bot Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue teamCheck = ValueBuilder.create(this, "Team Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue onlyCombat = ValueBuilder.create(this, "Only Combat")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public ModeValue renderMode = ValueBuilder.create(this, "RenderMode")
            .setModes("Box", "Wireframe", "None")
            .setDefaultModeIndex(0)
            .build().getModeValue();

    public FloatValue boxColorRed = ValueBuilder.create(this, "Box Red")
            .setDefaultFloatValue(0.3F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .setVisibility(() -> renderMode.isCurrentMode("Box"))
            .build().getFloatValue();

    public FloatValue boxColorGreen = ValueBuilder.create(this, "Box Green")
            .setDefaultFloatValue(0.13F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .setVisibility(() -> renderMode.isCurrentMode("Box"))
            .build().getFloatValue();

    public FloatValue boxColorBlue = ValueBuilder.create(this, "Box Blue")
            .setDefaultFloatValue(0.58F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .setVisibility(() -> renderMode.isCurrentMode("Box"))
            .build().getFloatValue();

    public FloatValue boxColorAlpha = ValueBuilder.create(this, "Box Alpha")
            .setDefaultFloatValue(0.34F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .setVisibility(() -> renderMode.isCurrentMode("Box"))
            .build().getFloatValue();

    public FloatValue wireframeWidth = ValueBuilder.create(this, "Wireframe Width")
            .setDefaultFloatValue(1.5F)
            .setMinFloatValue(0.5F)
            .setMaxFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> renderMode.isCurrentMode("Wireframe"))
            .build().getFloatValue();

    public BooleanValue enablePrediction = ValueBuilder.create(this, "Enable Prediction")
            .setDefaultBooleanValue(true)
            .build().getBooleanValue();

    public FloatValue predictionTicks = ValueBuilder.create(this, "Prediction Ticks")
            .setDefaultFloatValue(3.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.5F)
            .setVisibility(enablePrediction::getCurrentValue)
            .build().getFloatValue();

    public FloatValue renderStability = ValueBuilder.create(this, "Render Stability")
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> renderMode.isCurrentMode("Box") || renderMode.isCurrentMode("Wireframe"))
            .build().getFloatValue();

    public BooleanValue swingCheck = ValueBuilder.create(this, "Swing Check")
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();
    public ModeValue activeMode = ValueBuilder.create(this, "Active Mode")
            .setModes(new String[]{"Hit", "Not Hit", "Always"})
            .setDefaultModeIndex(2)
            .build().getModeValue();
    public BooleanValue releaseOnVelocity = ValueBuilder.create(this, "Release On Velocity")
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();
    public FloatValue minMS = ValueBuilder.create(this, "Min MS")
            .setDefaultFloatValue(50.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5000.0F)
            .setFloatStep(5.0F)
            .build().getFloatValue();
    public FloatValue maxMS = ValueBuilder.create(this, "Max MS")
            .setDefaultFloatValue(200.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5000.0F)
            .setFloatStep(5.0F)
            .build().getFloatValue();

    public BooleanValue sendVelocity = ValueBuilder.create(this, "Send Velocity")
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();

    public boolean btwork = false;
    private final LinkedBlockingDeque<Packet<?>> airKBQueue = new LinkedBlockingDeque<>();
    private final List<Integer> knockbackPositions = new ArrayList<>();
    private static final int mainColor = new Color(150, 45, 45, 255).getRGB();
    private boolean isInterceptingAirKB = false;
    private int interceptedPacketCount = 0;
    private int delayTicks = 0;
    private boolean shouldCheckGround = false;
    public String trackingText = "";
    private float progressBarAnimation = 0f;
    private float textAnimation = 0f;
    private long lastAnimationUpdate = 0;
    private double lastPosX = Double.NaN;
    private double lastPosZ = Double.NaN;
    private Integer currentTargetId = null;
    private LocalPlayer simulatedTarget = null;
    private MSTimers timer = new MSTimers();
    private Vec3 lastMotion = Vec3.ZERO;
    private Vec3 smoothedPrediction = Vec3.ZERO;
    private Vec3 lastActualPos = Vec3.ZERO;
    private long lastRenderTime = 0;
    private int renderStabilityCounter = 0;
    public Vec3 realPosition = new Vec3(0, 0, 0);
    private int ping;

    private Entity oldEntity = null;

    private int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void onEnable() {
        this.reset();
        this.progressBarAnimation = 0f;
        this.textAnimation = 0f;
        this.lastAnimationUpdate = 0;

        if (mc != null && mc.player != null) {
            this.lastPosX = mc.player.getX();
            this.lastPosZ = mc.player.getZ();
        } else {
            this.lastPosX = Double.NaN;
            this.lastPosZ = Double.NaN;
        }
    }

    public void onDisable() {
        this.reset();
        this.progressBarAnimation = 0f;
        this.textAnimation = 0f;
        this.lastAnimationUpdate = 0;
        this.simulatedTarget = null;
        releaseAllPackets();
    }

    public int getPacketCount() {
        return this.airKBQueue.size();
    }

    public void reset() {
        this.releaseAirKBQueue();
        this.isInterceptingAirKB = false;
        this.interceptedPacketCount = 0;
        this.delayTicks = 0;
        this.shouldCheckGround = false;
        this.btwork = false;
        this.knockbackPositions.clear();
        this.lastPosX = Double.NaN;
        this.lastPosZ = Double.NaN;
        this.currentTargetId = null;
        this.simulatedTarget = null;
        this.lastMotion = Vec3.ZERO;
        this.smoothedPrediction = Vec3.ZERO;
        this.lastActualPos = Vec3.ZERO;
        this.lastRenderTime = 0;
        this.renderStabilityCounter = 0;
        this.realPosition = new Vec3(0, 0, 0);
        this.oldEntity = null;
    }

    private void releaseAllPackets() {
        this.simulatedTarget = null;
        this.currentTargetId = null;
        while (!this.airKBQueue.isEmpty()) {
            try {
                Packet<ClientPacketListener> packet = (Packet<ClientPacketListener>) this.airKBQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    packet.handle(mc.getConnection());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isAuraEnabledOrBypassed() {
        if (!this.killAuraEnableCheck.getCurrentValue()) {
            return true;
        } else {
            try {
                return Naven.getInstance().getModuleManager().getModule(Aura.class).isEnabled();
            } catch (Exception var2) {
                return false;
            }
        }
    }

    private boolean shouldWork() {
        if (onlyCombat.getCurrentValue()) {
            try {
                Aura auraModule = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
                if (auraModule == null || !auraModule.isEnabled() || target == null) {
                    if (this.simulatedTarget != null) {
                        this.simulatedTarget = null;
                        this.currentTargetId = null;
                    }
                    return false;
                }
            } catch (Exception e) {
                if (this.simulatedTarget != null) {
                    this.simulatedTarget = null;
                    this.currentTargetId = null;
                }
                return false;
            }
        }

        return true;
    }

    private void updateCurrentTarget() {
        if (mc != null && mc.level != null && mc.player != null) {
            Entity dist = target;
            if (dist instanceof Player) {
                Player p = (Player) dist;
                if (p.isAlive()) {
                    if (botCheck.getCurrentValue() && isBotEntity(p)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a bot, skipping: id=" + p.getId());
                        }
                        this.currentTargetId = null;
                        this.simulatedTarget = null;
                        return;
                    }
                    if (teamCheck.getCurrentValue() && isTeammate(p)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a teammate, skipping: id=" + p.getId());
                        }
                        this.currentTargetId = null;
                        this.simulatedTarget = null;
                        return;
                    }

                    if (oldEntity != p) {
                        releaseAllPackets();
                    }

                    this.currentTargetId = p.getId();
                    updateSimulatedTarget(p);
                    this.oldEntity = p;

                    if (this.debugFilter.getCurrentValue()) {
                        double distance = mc.player.distanceTo(p);
                        this.log("Target Update (Aura.target): id=" + this.currentTargetId + ", dist=" + String.format("%.2f", distance));
                    }
                    return;
                }
            }

            dist = Aura.aimingTarget;
            if (dist instanceof Player) {
                Player p2 = (Player) dist;
                if (p2.isAlive()) {
                    if (botCheck.getCurrentValue() && isBotEntity(p2)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a bot, skipping: id=" + p2.getId());
                        }
                        this.currentTargetId = null;
                        this.simulatedTarget = null;
                        return;
                    }
                    if (teamCheck.getCurrentValue() && isTeammate(p2)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a teammate, skipping: id=" + p2.getId());
                        }
                        this.currentTargetId = null;
                        this.simulatedTarget = null;
                        return;
                    }

                    if (oldEntity != p2) {
                        releaseAllPackets();
                    }

                    this.currentTargetId = p2.getId();
                    updateSimulatedTarget(p2);
                    this.oldEntity = p2;

                    if (this.debugFilter.getCurrentValue()) {
                        double distance = mc.player.distanceTo(p2);
                        this.log("Target Update (Aura.aimingTarget): id=" + this.currentTargetId + ", dist=" + String.format("%.2f", distance));
                    }
                    return;
                }
            }

            this.currentTargetId = null;
            this.simulatedTarget = null;
            this.smoothedPrediction = Vec3.ZERO;
            this.lastActualPos = Vec3.ZERO;
            this.lastMotion = Vec3.ZERO;
            this.oldEntity = null;

            if (this.debugFilter.getCurrentValue()) {
                this.log("Target Update: none - all target data cleared");
            }
        } else {
            this.currentTargetId = null;
            this.simulatedTarget = null;
            this.smoothedPrediction = Vec3.ZERO;
            this.lastActualPos = Vec3.ZERO;
            this.lastMotion = Vec3.ZERO;
            this.oldEntity = null;
        }
    }

    private void updateSimulatedTarget(Player target) {
        if (this.simulatedTarget == null) {
            this.simulatedTarget = mc.gameMode.createPlayer(mc.level, new StatsCounter(), new ClientRecipeBook());
        }

        Vec3 currentPos = new Vec3(target.getX(), target.getY(), target.getZ());
        if (lastActualPos.distanceTo(currentPos) > 0.1) {
            this.simulatedTarget.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            this.lastActualPos = currentPos;
        }

        this.simulatedTarget.setSprinting(target.isSprinting());
        this.simulatedTarget.setShiftKeyDown(target.isShiftKeyDown());
        Vec3 currentMotion = new Vec3(target.getDeltaMovement().x, target.getDeltaMovement().y, target.getDeltaMovement().z);
        if (currentMotion.length() > 0.01) {
            this.lastMotion = currentMotion;
        }
    }

    private boolean isBotEntity(Entity entity) {
        try {
            AntiBots antiBotsModule = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (antiBotsModule != null && antiBotsModule.isEnabled()) {
                return AntiBots.isBot(entity) || AntiBots.isBedWarsBot(entity);
            }
        } catch (Exception e) {
            return isBot(entity);
        }
        return isBot(entity);
    }

    private boolean isBot(Entity entity) {
        try {
            AntiBots antiBotsModule = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (antiBotsModule != null && antiBotsModule.isEnabled()) {
                return AntiBots.isBot(entity) || AntiBots.isBedWarsBot(entity);
            }
        } catch (Exception e) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                String name = player.getName().getString();
                if (name.isEmpty() || name.equalsIgnoreCase(" ")) {
                    return true;
                }
                if (AntiBots.ids.contains(entity.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTeammate(Entity entity) {
        try {
            Teams teamsModule = (Teams) Naven.getInstance().getModuleManager().getModule(Teams.class);
            if (teamsModule != null && teamsModule.isEnabled()) {
                return Teams.isSameTeam(entity);
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isTargetRelatedPacket(Packet<?> packet) {
        if (this.currentTargetId == null) {
            return false;
        } else {
            try {
                if (packet instanceof ClientboundSetEntityMotionPacket) {
                    ClientboundSetEntityMotionPacket motion = (ClientboundSetEntityMotionPacket) packet;
                    return motion.getId() == this.currentTargetId;
                }

                Method m = packet.getClass().getMethod("getId");
                if (m.getReturnType() == Integer.TYPE) {
                    int id = (Integer) m.invoke(packet);
                    boolean match = id == this.currentTargetId;
                    if (this.debugFilter.getCurrentValue()) {
                        this.log("[OnlyInterceptTarget] getId Checker: packet=" + packet.getClass().getSimpleName() + ", id=" + id + ", match=" + match);
                    }
                    return match;
                }
            } catch (NoSuchMethodException var5) {
            } catch (Exception var6) {
                if (this.debugFilter.getCurrentValue()) {
                    this.log("[OnlyInterceptTarget] Reflection acquisition failed: " + var6.getClass().getSimpleName());
                }
            }
            return false;
        }
    }

    private void releaseAirKBQueue() {
        int packetCount = this.airKBQueue.size();

        while (!this.airKBQueue.isEmpty()) {
            try {
                Packet<ClientPacketListener> packet = (Packet<ClientPacketListener>) this.airKBQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    packet.handle(mc.getConnection());
                }
            } catch (Exception var31) {
                var31.printStackTrace();
            }
        }

        if (packetCount > 0) {
            this.log("Release " + packetCount + " Packets");
        }

        this.interceptedPacketCount = 0;
        this.knockbackPositions.clear();
    }

    private boolean hasNearbyPlayers(float range) {
        if (mc.level != null && mc.player != null) {
            for (Player player : mc.level.players()) {
                if (player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }

    private boolean isSelfRelatedPacket(Packet<?> packet) {
        if (packet instanceof ClientboundPlayerPositionPacket) {
            return true;
        } else if (packet instanceof ClientboundSetEntityMotionPacket) {
            return false;
        } else {
            try {
                Method m = packet.getClass().getMethod("getId");
                if (m.getReturnType() == Integer.TYPE && mc.player != null) {
                    int id = (Integer) m.invoke(packet);
                    boolean self = id == mc.player.getId();
                    if (this.debugFilter.getCurrentValue()) {
                        this.log("getId Checker: packet=" + packet.getClass().getSimpleName() + ", id=" + id + ", self=" + self);
                    }
                    if (self) {
                        return true;
                    }
                }
            } catch (NoSuchMethodException var5) {
            } catch (Exception var6) {
                if (this.debugFilter.getCurrentValue()) {
                    this.log("Reflection acquisition failed: " + var6.getClass().getSimpleName());
                }
            }
            return false;
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player != null) {
            if (!shouldWork()) {
                if (this.isInterceptingAirKB || this.shouldCheckGround) {
                    this.releaseAirKBQueue();
                    this.resetAfterRelease();
                }
                this.btwork = false;
                return;
            }

            if (!this.isAuraEnabledOrBypassed()) {
                if (this.isInterceptingAirKB || this.shouldCheckGround) {
                    this.releaseAirKBQueue();
                    this.resetAfterRelease();
                }
                this.btwork = false;
            } else {
                this.btwork = this.isInterceptingAirKB || this.shouldCheckGround;
                this.updateCurrentTarget();

                if (this.simulatedTarget != null && mc.player != null) {
                    double distance = mc.player.distanceTo(this.simulatedTarget);
                    double fallDistanceDiff = Math.abs(mc.player.fallDistance - (this.oldEntity != null ? this.oldEntity.fallDistance : 0));

                    if (distance > range.getCurrentValue() || fallDistanceDiff > 2.5F) {
                        this.log("Target out of range or fall distance too different, releasing packets");
                        this.releaseAllPackets();
                        this.resetAfterRelease();
                    }
                }

                if (this.simulatedTarget != null && this.lastMotion != Vec3.ZERO) {
                    Vec3 newMotion = this.lastMotion.scale(0.95);
                    this.simulatedTarget.setDeltaMovement(newMotion);
                    this.lastMotion = newMotion;
                }

                if (this.isInterceptingAirKB && this.releaseWhenTP.getCurrentValue()) {
                    double curX = mc.player.getX();
                    double curZ = mc.player.getZ();
                    if (!Double.isNaN(this.lastPosX) && !Double.isNaN(this.lastPosZ)) {
                        double dx = curX - this.lastPosX;
                        double dz = curZ - this.lastPosZ;
                        double horizDist = Math.sqrt(dx * dx + dz * dz);
                        double bps = horizDist * 20.0D;
                        if (bps > (double) this.bpsThreshold.getCurrentValue()) {
                            this.log("BPS Error! (" + String.format("%.2f", bps) + ")，Release All Packets");
                            this.isInterceptingAirKB = false;
                            this.shouldCheckGround = false;
                            this.releaseAirKBQueue();
                            this.resetAfterRelease();
                        }
                    }
                    this.lastPosX = curX;
                    this.lastPosZ = curZ;
                } else if (mc.player != null) {
                    this.lastPosX = mc.player.getX();
                    this.lastPosZ = mc.player.getZ();
                }

                if (this.delayTicks > 0) {
                    --this.delayTicks;
                } else {
                    boolean shouldStartByNearby = !this.targetNearbyCheck.getCurrentValue() || this.hasNearbyPlayers(this.range.getCurrentValue());
                    if (!this.isInterceptingAirKB && shouldStartByNearby) {
                        this.isInterceptingAirKB = true;
                        this.shouldCheckGround = false;
                        this.interceptedPacketCount = 0;
                        this.airKBQueue.clear();
                        this.knockbackPositions.clear();
                        if (this.targetNearbyCheck.getCurrentValue()) {
                            this.log("Checker Player(<= " + this.range.getCurrentValue() + ")，Start intercepting");
                        } else {
                            this.log("Start intercepting");
                        }
                    }

                    if (this.isInterceptingAirKB && (float) this.interceptedPacketCount >= this.maxpacket.getCurrentValue()) {
                        if (this.OnGroundStop.getCurrentValue()) {
                            this.shouldCheckGround = true;
                            this.log("Wait Player OnGround");
                        } else {
                            this.log("Release All Packets");
                            this.releaseAirKBQueue();
                            this.resetAfterRelease();
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled()) {
            this.render(event.getGuiGraphics());
        }
    }

    private void resetAfterRelease() {
        this.isInterceptingAirKB = false;
        this.shouldCheckGround = false;
        this.delayTicks = (int) this.delay.getCurrentValue();
        this.log("BackTrack Delay: " + this.delayTicks + " ticks");
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player != null && mc.getConnection() != null) {
            if (event.getType() == EventType.RECEIVE) {
                if (!shouldWork()) {
                    if (this.isInterceptingAirKB || this.shouldCheckGround) {
                        this.releaseAirKBQueue();
                        this.resetAfterRelease();
                    }
                    return;
                }

                if (!this.isAuraEnabledOrBypassed()) {
                    if (this.isInterceptingAirKB || this.shouldCheckGround) {
                        this.releaseAirKBQueue();
                        this.resetAfterRelease();
                    }
                } else {
                    Packet<?> packet = event.getPacket();

                    if (packet instanceof ClientboundPingPacket) {
                        if (this.currentTargetId != null) {
                            event.setCancelled(true);
                            this.airKBQueue.add(packet);
                        }
                    }

                    if (packet instanceof ClientboundPlayerPositionPacket) {
                        this.currentTargetId = null;
                        this.simulatedTarget = null;
                        if (this.releaseWhenTP.getCurrentValue()) {
                            this.isInterceptingAirKB = false;
                            this.shouldCheckGround = false;
                            this.log("Checked TP, Release All Packets");
                            this.releaseAirKBQueue();
                            this.resetAfterRelease();
                        }
                    } else if (packet instanceof ClientboundExplodePacket) {
                        if (this.sendVelocity.getCurrentValue() && this.currentTargetId != null) {
                            event.setCancelled(true);
                            this.airKBQueue.add(packet);
                        }
                    } else if (packet instanceof ClientboundSetEntityMotionPacket) {
                        ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) packet;
                        if (motionPacket.getId() == mc.player.getId() && this.isInterceptingAirKB) {
                            if (this.sendVelocity.getCurrentValue()) {
                                event.setCancelled(true);
                                this.airKBQueue.add(packet);
                                ++this.interceptedPacketCount;
                                this.knockbackPositions.add(this.airKBQueue.size() - 1);
                                this.log("Intercepting KnockBack Packets #" + this.interceptedPacketCount);
                            }
                        } else if (motionPacket.getId() == this.currentTargetId) {
                            if (this.simulatedTarget != null) {
                                this.simulatedTarget.lerpMotion(
                                        (double)motionPacket.getXa() / 8000.0D,
                                        (double)motionPacket.getYa() / 8000.0D,
                                        (double)motionPacket.getZa() / 8000.0D
                                );
                            }
                        }
                    } else if (this.isInterceptingAirKB) {
                        boolean skipSelf = this.interceptMode.isCurrentMode("AllExceptSelf") && this.isSelfRelatedPacket(packet);
                        if (skipSelf) {
                            if (this.debugFilter.getCurrentValue()) {
                                this.log("[AllExceptSelf] Skip Packet Of Myself: " + packet.getClass().getSimpleName());
                            }
                            return;
                        }

                        if (this.interceptMode.isCurrentMode("OnlyInterceptTarget") && !this.isTargetRelatedPacket(packet)) {
                            if (this.debugFilter.getCurrentValue()) {
                                this.log("[OnlyInterceptTarget] Skip Non-Target Packet: " + packet.getClass().getSimpleName());
                            }
                            return;
                        }

                        if (packet instanceof ClientboundMoveEntityPacket) {
                            ClientboundMoveEntityPacket movePacket = (ClientboundMoveEntityPacket) packet;
                            Entity packetEntity = movePacket.getEntity(mc.level);
                            if (packetEntity != null && packetEntity.getId() == this.currentTargetId) {
                                event.setCancelled(true);
                                this.airKBQueue.add(packet);
                                ++this.interceptedPacketCount;
                                if (this.debugFilter.getCurrentValue()) {
                                    this.log("[" + this.interceptMode.getCurrentMode() + "] Intercepting MoveEntityPacket #" + this.interceptedPacketCount + ": " + packet.getClass().getSimpleName());
                                }

                                if (movePacket.hasPosition() && this.simulatedTarget != null) {
                                    double currentX = this.simulatedTarget.getX();
                                    double currentY = this.simulatedTarget.getY();
                                    double currentZ = this.simulatedTarget.getZ();
                                    double deltaX = movePacket.getXa() / 4096.0;
                                    double deltaY = movePacket.getYa() / 4096.0;
                                    double deltaZ = movePacket.getZa() / 4096.0;

                                    double newX = currentX + deltaX;
                                    double newY = currentY + deltaY;
                                    double newZ = currentZ + deltaZ;

                                    Vec3 newPos = new Vec3(newX, newY, newZ);
                                    if (this.simulatedTarget.position().distanceTo(newPos) > 0.05) {
                                        this.simulatedTarget.moveTo(newX, newY, newZ, movePacket.getyRot(), movePacket.getxRot());
                                    }
                                }
                            }
                        } else if (packet instanceof ClientboundTeleportEntityPacket) {
                            ClientboundTeleportEntityPacket teleportPacket = (ClientboundTeleportEntityPacket) packet;
                            if (teleportPacket.getId() == this.currentTargetId) {
                                event.setCancelled(true);
                                this.airKBQueue.add(packet);
                                ++this.interceptedPacketCount;

                                if (this.simulatedTarget != null) {
                                    double x = teleportPacket.getX();
                                    double y = teleportPacket.getY();
                                    double z = teleportPacket.getZ();
                                    this.simulatedTarget.syncPacketPositionCodec(x, y, z);
                                    if (!this.simulatedTarget.isControlledByLocalInstance()) {
                                        float yRot = (float)(teleportPacket.getyRot() * 360) / 256.0F;
                                        float xRot = (float)(teleportPacket.getxRot() * 360) / 256.0F;
                                        this.simulatedTarget.lerpTo(x, y, z, yRot, xRot, 3, true);
                                        this.simulatedTarget.setOnGround(teleportPacket.isOnGround());
                                    }
                                }
                            }
                        } else {
                            event.setCancelled(true);
                            this.airKBQueue.add(packet);
                            ++this.interceptedPacketCount;
                            if (this.debugFilter.getCurrentValue()) {
                                this.log("[" + this.interceptMode.getCurrentMode() + "] Intercepting Normal Packets #" + this.interceptedPacketCount + ": " + packet.getClass().getSimpleName());
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(EventRender e) {
        if (renderMode.isCurrentMode("None") || currentTargetId == null) {
            return;
        }

        Entity entity = mc.level.getEntity(currentTargetId);
        if (entity == null || !entity.isAlive() || !(entity instanceof Player)) {
            this.currentTargetId = null;
            this.simulatedTarget = null;
            this.smoothedPrediction = Vec3.ZERO;
            return;
        }

        if (mc.player != null && mc.player.distanceTo(entity) > 64.0) {
            return;
        }

        renderStabilityCounter++;
        if (renderStabilityCounter % (int) (10 - renderStability.getCurrentValue() * 9) != 0) {
            return;
        }

        PoseStack stack = e.getPMatrixStack();
        float partialTicks = e.getRenderPartialTicks();

        stack.pushPose();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);

        GL11.glEnable(2848);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderUtils.applyRegionalRenderOffset(stack);

        Vec3 predictedPos = calculateStablePredictedPosition(entity, partialTicks);

        AABB boundingBox = entity.getBoundingBox()
                .move(predictedPos.x - entity.getX(), predictedPos.y - entity.getY(), predictedPos.z - entity.getZ());

        if (renderMode.isCurrentMode("Box")) {
            RenderSystem.setShaderColor(
                    boxColorRed.getCurrentValue() * 0.6f,
                    boxColorGreen.getCurrentValue() * 0.4f,
                    boxColorBlue.getCurrentValue() * 0.7f,
                    boxColorAlpha.getCurrentValue() * 1.2f
            );
            RenderUtils.drawSolidBox(boundingBox, stack);
        } else if (renderMode.isCurrentMode("Wireframe")) {
            GL11.glLineWidth(wireframeWidth.getCurrentValue());
            RenderSystem.setShaderColor(0.5F, 0.2F, 0.8F, 1.0F);
            RenderUtils.drawOutlinedBox(boundingBox, stack);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
        stack.popPose();

        lastRenderTime = System.currentTimeMillis();
    }

    private Vec3 calculateStablePredictedPosition(Entity entity, float partialTicks) {
        if (!enablePrediction.getCurrentValue() || simulatedTarget == null) {
            return new Vec3(entity.getX(), entity.getY(), entity.getZ());
        }
        Vec3 basePos = new Vec3(simulatedTarget.getX(), simulatedTarget.getY(), simulatedTarget.getZ());
        int ticks = (int) predictionTicks.getCurrentValue();
        Vec3 predictedMotion = this.lastMotion.scale(ticks + partialTicks);
        Vec3 newPredictedPos = basePos.add(predictedMotion);
        if (smoothedPrediction == Vec3.ZERO) {
            smoothedPrediction = newPredictedPos;
        } else {
            float stability = renderStability.getCurrentValue();
            smoothedPrediction = new Vec3(
                    smoothedPrediction.x * stability + newPredictedPos.x * (1 - stability),
                    smoothedPrediction.y * stability + newPredictedPos.y * (1 - stability),
                    smoothedPrediction.z * stability + newPredictedPos.z * (1 - stability)
            );
        }

        return smoothedPrediction;
    }

    public void render(GuiGraphics guiGraphics) {
        if (!btrender.getCurrentValue()) return;

        boolean shouldRender = (isInterceptingAirKB || shouldCheckGround) && shouldWork();

        if (currentTargetId == null || !shouldWork()) {
            shouldRender = false;
        }

        if (!shouldRender) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        trackingText = "Tracking... " + interceptedPacketCount + "/" + (int) maxpacket.getCurrentValue();

        if (!btrendermode.isCurrentMode("Normal")) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float progressWidth = 100.0F;
        float progressHeight = 5.0F;

        int baseX = screenWidth / 2 - (int) (progressWidth / 2);
        int baseY = screenHeight / 2 + 40;
        int x = (int) (baseX + renderPosX.getCurrentValue());
        int y = (int) (baseY + renderPosY.getCurrentValue());

        progress.update(true);
        float progressValue = Math.min(1.0F, (float) interceptedPacketCount / maxpacket.getCurrentValue()) * progressWidth;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderUtils.drawRoundedRect(poseStack, (float) x, (float) y, progressWidth, progressHeight, 2.0F, 0x80000000);
        RenderUtils.drawRoundedRect(poseStack, (float) x, (float) y, progressValue, progressHeight, 2.0F, mainColor);

        float textScale = 0.35F;
        CustomTextRenderer font = Fonts.harmony;

        String targetInfo = "";
        if (currentTargetId != null) {
            Entity target = mc.level.getEntity(currentTargetId);
            if (target != null) {
                targetInfo = "Target: " + target.getName().getString();
            }
        }

        String statusText = isInterceptingAirKB ? "Intercepting" :
                shouldCheckGround ? "Waiting Ground" : "Ready";

        float currentTextY = y + progressHeight + 8.0F;

        if (!targetInfo.isEmpty()) {
            float targetTextWidth = font.getWidth(targetInfo, textScale);
            float targetTextX = ((float) screenWidth - targetTextWidth) / 2.0F;
            font.render(poseStack, targetInfo, targetTextX, currentTextY, java.awt.Color.WHITE, true, textScale);
            currentTextY += 10.0F;
        }

        float statusTextWidth = font.getWidth(statusText, textScale);
        float statusTextX = ((float) screenWidth - statusTextWidth) / 2.0F;
        font.render(poseStack, statusText, statusTextX, currentTextY, java.awt.Color.WHITE, true, textScale);
        currentTextY += 10.0F;

        float trackingTextWidth = font.getWidth(trackingText, textScale);
        float trackingTextX = ((float) screenWidth - trackingTextWidth) / 2.0F;
        font.render(poseStack, trackingText, trackingTextX, currentTextY, java.awt.Color.WHITE, true, textScale);

        poseStack.popPose();
    }



//    @EventTarget
//    public void onMotion(MotionEvent eventMotion) {
//
//    }


    public boolean shouldActive(Player target){
        return activeMode.isCurrentMode("Always") ||
                (activeMode.isCurrentMode("Hit") && target.hurtTime != 0) ||
                (activeMode.isCurrentMode("Not Hit") && target.hurtTime == 0);
    }
}