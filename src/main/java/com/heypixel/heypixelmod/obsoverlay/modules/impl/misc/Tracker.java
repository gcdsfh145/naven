package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerTrackerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.WorldEvent;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Tracker", description = "Tracker", category = Category.MISC)
public class Tracker extends Module  {
    BooleanValue lightning = ValueBuilder.create(this, "Lightning").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue allPlayer = ValueBuilder.create(this, "allPlayer").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue bannedCheck = ValueBuilder.create(this, "Banned Check").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue autoHub = ValueBuilder.create(this, "Auto Hub").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue autoDisconnected = ValueBuilder.create(this, "Auto Disconnected").setDefaultBooleanValue(true).build().getBooleanValue();
    public static List<Entity> flaggedEntity = new ArrayList<>();
    public static int banned = 0;
    private final NotificationManager notificationManager = new NotificationManager();

    @Override
    public void onDisable() {
        flaggedEntity.clear();
        super.onDisable();
    }

    @EventTarget
    public void onWorld(WorldEvent e) {
        flaggedEntity.clear();
    }

    @EventTarget
    public void onChatReceived(EventPacket event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundSystemChatPacket wrapper) {

            if (wrapper.content().getString().isEmpty()) return;

            if (bannedCheck.getCurrentValue()) {
                String message = wrapper.content().getString();
                String playerName = mc.player.getGameProfile().getName();
                if (message.startsWith("<" + playerName + ">")) {
                    return;
                }

                if (message.contains("违规")) {

                    banned++;
                    ChatUtils.addChatMessage("检测到违规信息！有一个黑客被妖猫击落了，本局游戏封禁人数：" + banned);

                    if (autoHub.getCurrentValue() && bannedCheck.getCurrentValue())
                        mc.gui.getChat().addMessage(Component.literal("/hub"));

                    if (mc.getConnection() != null && autoDisconnected.getCurrentValue() && bannedCheck.getCurrentValue()) {
                        Connection networkManager = mc.getConnection().getConnection();
                        networkManager.disconnect(Component.literal("检测到违规信息，自动退出服务器"));
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacketEvent(EventPacket e) {
        if (mc.player == null || mc.level == null) return;

        if (lightning.getCurrentValue()) {
            if (e.getPacket() instanceof ClientboundAddEntityPacket packet) {
                if (packet.getType() != EntityType.LIGHTNING_BOLT) {
                    return;
                }

                final Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());

                final Vec3 playerPos = mc.player.position();
                final double distance = playerPos.distanceTo(pos);

                ChatUtils.addChatMessage("闪电击中 | " + "X: " +
                        String.format("%.2f", pos.x) + ", " + "Y: " +
                        String.format("%.2f", pos.y) + ", " + "Z: " +
                        String.format("%.2f", pos.z) + " | 距离玩家 (" +
                        (int) distance + " 米)"
                );
            }
        }
    }


    @EventTarget
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.level == null || mc.level.players().isEmpty()) return;

        if (mc.player.tickCount % 6 != 0) return;

        for (Player player : mc.level.players()) {

            boolean me = player != mc.player;
            boolean flagged = flaggedEntity.contains(player);
            boolean sameTeam = Teams.instance.isSameTeam(player);

            if (!allPlayer.getCurrentValue() && me && !sameTeam && !flagged) {
                checkAndFlag(player);
            }

            if (allPlayer.getCurrentValue() && !flagged) {
                checkAndFlag(player);
            }

        }
    }

    private void checkAndFlag(Player player) {
        if (PlayerTrackerUtils.isStrength(player) > 0) {
            flagPlayer(player, "攻击伤害异常");
        }
        if (PlayerTrackerUtils.isRegen(player) > 0) {
            flagPlayer(player, "恢复速度异常");
        }
        if (PlayerTrackerUtils.isHoldingGodAxe(player)) {
            flagPlayer(player, "持有秒人斧");
        }
        if (PlayerTrackerUtils.isHoldingSlimeball(player)) {
            flagPlayer(player, "持有击退球");
        }
        if (PlayerTrackerUtils.isHoldingTotemo(player)) {
            flagPlayer(player, "持有不死图腾");
        }
        if (PlayerTrackerUtils.isHoldingCrossbow(player)) {
            flagPlayer(player, "持有弩");
        }
        if (PlayerTrackerUtils.isHoldingBow(player)) {
            flagPlayer(player, "持有弓");
        }
        if (PlayerTrackerUtils.isHoldingFireCharge(player)) {
            flagPlayer(player, "持有火焰弹");
        }
    }

    private void flagPlayer(Player player, String message) {
        flaggedEntity.add(player);
        String notificationMessage = player.getName().getString() + " " + message;
        Notification notification = new Notification(NotificationLevel.WARNING, notificationMessage, 5000);
        notificationManager.addNotification(notification);
    }
}