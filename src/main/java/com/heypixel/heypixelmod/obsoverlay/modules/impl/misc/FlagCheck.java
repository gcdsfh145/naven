package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.events.impl.PacketEvent;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.IntValue;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ModuleInfo(name = "FlagCheck", description = "Alerts you about set backs", category = Category.MISC)
public class FlagCheck extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    private final BooleanValue chatMessage = ValueBuilder.create(this, "ChatMessage")
            .setDefaultBooleanValue(true).build().getBooleanValue();

    private final BooleanValue notification = ValueBuilder.create(this, "Notification")
            .setDefaultBooleanValue(true).build().getBooleanValue();

    private final BooleanValue invalidAttributes = ValueBuilder.create(this, "InvalidAttributes")
            .setDefaultBooleanValue(true).build().getBooleanValue();

    private final IntValue notiTimer = ValueBuilder.create(this, "NotiTimer")
            .setDefaultIntValue(250)
            .setMinIntValue(0)
            .setMaxIntValue(2500)
            .build().getIntValue();

    private final BooleanValue lagChat = ValueBuilder.create(this, "LagChat")
            .setDefaultBooleanValue(true).build().getBooleanValue();

    private final BooleanValue resetFlags = ValueBuilder.create(this, "ResetFlags")
            .setDefaultBooleanValue(true).build().getBooleanValue();

    private final IntValue afterSeconds = ValueBuilder.create(this, "After")
            .setDefaultIntValue(30)
            .setMinIntValue(1)
            .setMaxIntValue(300)
            .build().getIntValue();

    private final BooleanValue render = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(true).build().getBooleanValue();

    private int flagCount = 0;
    private float lastYaw = 0F;
    private float lastPitch = 0F;
    private long lastFlagTime = 0L;

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.player.tickCount <= 25) {
            return;
        }

        Packet<?> packet = event.getPacket2();

        if (packet instanceof ClientboundPlayerPositionPacket) {
            ClientboundPlayerPositionPacket positionPacket = (ClientboundPlayerPositionPacket) packet;

            float newYaw = positionPacket.getYRot();
            float newPitch = positionPacket.getXRot();

            float deltaYaw = calculateAngleDelta(newYaw, lastYaw);
            float deltaPitch = calculateAngleDelta(newPitch, lastPitch);

            flagCount++;

            if (deltaYaw >= 90 || deltaPitch >= 90) {
                alert(AlertReason.FORCEROTATE, String.format("(%.0f° | %.0f°)", deltaYaw, deltaPitch));
            } else {
                alert(AlertReason.LAGBACK, null);
            }

            lastFlagTime = System.currentTimeMillis();
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        } else if (packet instanceof ClientboundDisconnectPacket) {
            flagCount = 0;
        }
    }

    public void onTick() {
        if (!invalidAttributes.getCurrentValue() || mc.player == null) {
            return;
        }

        boolean invalidHealth = mc.player.getHealth() <= 0f && mc.player.isAlive();
        boolean invalidHunger = mc.player.getFoodData().getFoodLevel() <= 0;

        if (!invalidHealth && !invalidHunger) {
            return;
        }

        StringBuilder invalidReasons = new StringBuilder();

        if (invalidHealth) {
            invalidReasons.append("Health");
        }

        if (invalidHunger) {
            if (invalidReasons.length() > 0) {
                invalidReasons.append(", ");
            }
            invalidReasons.append("Hunger");
        }

        if (invalidReasons.length() > 0) {
            flagCount++;
            alert(AlertReason.INVALID, invalidReasons.toString());
        }

        if (resetFlags.getCurrentValue() && flagCount > 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFlagTime >= afterSeconds.getCurrentValue() * 1000L) {
                flagCount = 0;
            }
        }
    }

    private void alert(AlertReason reason, String extra) {
        String message;

        switch (reason) {
            case INVALID:
                if (extra != null && !extra.isEmpty()) {
                    message = String.format("§c[FlagCheck] §7Invalid attributes detected: §f%s §7(Flag #%d)", extra, flagCount);
                } else {
                    message = String.format("§c[FlagCheck] §7Invalid attributes detected (Flag #%d)", flagCount);
                }
                break;

            case FORCEROTATE:
                if (extra != null && !extra.isEmpty()) {
                    message = String.format("§c[FlagCheck] §7Force rotation detected %s §7(Flag #%d)", extra, flagCount);
                } else {
                    message = String.format("§c[FlagCheck] §7Force rotation detected (Flag #%d)", flagCount);
                }
                break;

            case LAGBACK:
                message = String.format("§c[FlagCheck] §7Lagback detected (Flag #%d)", flagCount);
                break;

            default:
                message = String.format("§c[FlagCheck] §7Unknown flag detected (Flag #%d)", flagCount);
                break;
        }

        if (notification.getCurrentValue()) {
            NotificationManager manager = getNotificationManager();
            if (manager != null) {
                Notification noti = new Notification(NotificationLevel.WARNING, message, notiTimer.getCurrentValue());
                manager.addNotification(noti);
            }
        }

        if (chatMessage.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private float calculateAngleDelta(float newAngle, float oldAngle) {
        float delta = newAngle - oldAngle;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        return Math.abs(delta);
    }

    private NotificationManager getNotificationManager() {
        try {
            return com.heypixel.heypixelmod.obsoverlay.Naven.getInstance().getNotificationManager();
        } catch (Exception e) {
            return null;
        }
    }

    private enum AlertReason {
        INVALID,
        FORCEROTATE,
        LAGBACK
    }
}