package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderAfterWorld;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManagers;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.client.Minecraft;

@ModuleInfo(name = "LagChecker", description = "Detects server lag and provides notifications", category = Category.MISC)
public class LagChecker extends Module {
    IntValue lagThreshold = ValueBuilder.create(this, "Lag")
            .setDefaultIntValue(1000)
            .setMinIntValue(0)
            .setMaxIntValue(10000)
            .build()
            .getIntValue();
    IntValue notiDelay = ValueBuilder.create(this, "NotiDelay")
            .setDefaultIntValue(1000)
            .setMinIntValue(0)
            .setMaxIntValue(10000)
            .build()
            .getIntValue();
    IntValue notiTimer = ValueBuilder.create(this, "NotiTimer")
            .setDefaultIntValue(250)
            .setMinIntValue(0)
            .setMaxIntValue(2500)
            .build()
            .getIntValue();
    BooleanValue lagChat = ValueBuilder.create(this, "LagChat").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue noti = ValueBuilder.create(this, "Noti").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue lagClear= ValueBuilder.create(this, "lagclear").setDefaultBooleanValue(true).build().getBooleanValue();

    private NotificationManagers notificationManager;
    private final MSTimer timer = new MSTimer();
    private int lag = 0;
    private Minecraft mc = Minecraft.getInstance();

    @Override
    public void onDisable() {
        super.onDisable();
        this.lag = 0;
    }

    @EventTarget
    public void onPacket(PacketEvent2 event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundPlayerPositionPacket) {
            this.lag++;
            if (this.noti.getCurrentValue() && this.timer.hasTimePassed(this.notiDelay.getCurrentValue()) && this.lag > 0) {
                if (notificationManager != null) {
                    notificationManager.addNotification("LagChecker", "Lag! (Count: " + this.lag + ")",
                            NotificationManagers.NotifyType.WARNING, this.notiTimer.getCurrentValue());
                }
                ChatUtils.addChatMessage("Lag!");
                this.timer.reset();
            }

            if (this.lag >= this.lagThreshold.getCurrentValue()) {
                if (this.lagChat.getCurrentValue() && mc.player != null) {
                    mc.player.connection.sendCommand("hub");
                }

                ChatUtils.addChatMessage("Lag " + this.lagThreshold.getCurrentValue() + " AutoClear!");
                this.lag = 0;
            }
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        if (this.lagClear.getCurrentValue()) {
            this.lag = 0;
        }
    }

    public String getTag() {
        return "Lag " + this.lag;
    }

    public void setNotificationManager(NotificationManagers notificationManager) {
        this.notificationManager = notificationManager;
    }
}