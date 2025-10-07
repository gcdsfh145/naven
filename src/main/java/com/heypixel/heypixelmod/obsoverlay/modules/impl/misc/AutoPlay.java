package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.EffectsUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketEvent2;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@ModuleInfo(
        name = "AutoPlay",
        description = "AutoPlay",
        category = Category.MISC
)
public class AutoPlay extends Module {
    private static final Set<Player> flaggedEntity = new HashSet<>();
    private static final Set<Player> flaggedEntity2 = new HashSet<>();
    public static int GG = 0;
    public static int Start = 0;
    public static boolean game = false;
    private boolean hasSentAgainCommand = false;
    private long lastCommandTime = 0;

    private final BooleanValue fakeServer = ValueBuilder.create(this, "FakeServe")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue fakeNameValue = ValueBuilder.create(this, "FakeName")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue EffectsValue = ValueBuilder.create(this, "Effects")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue autoPlayAgain = ValueBuilder.create(this, "AutoPlayAgain")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final NotificationManager notificationManager = new NotificationManager();

    private final String[] GAME_END_KEYWORDS = {
            "游戏结束", "游戏终结", "胜利", "获胜", "VICTORY", "WIN", "恭喜", "赢得了游戏", "Winner", "冠军"
    };

    private final String[] GAME_START_KEYWORDS = {
            "游戏开始", "战斗开始", "Starting", "Game starting", "准备开始"
    };

    public AutoPlay() {
        updateSuffix();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetAgainCommandState();
        updateSuffix();
        ChatUtils.addChatMessage("§a自动游戏已启用 - 等待检测游戏结束消息");
        System.out.println("AutoPlay: 模块已启用");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        setSuffix("");
        resetAgainCommandState();
        ChatUtils.addChatMessage("§c自动游戏已禁用");
        System.out.println("AutoPlay: 模块已禁用");
    }

    private void updateSuffix() {
        setSuffix("Win " + GG + " game");
    }

    private void resetAgainCommandState() {
        hasSentAgainCommand = false;
        game = false;
    }

    private void sendAgainCommand() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommandTime < 5000) {
            System.out.println("AutoPlay: 指令发送过于频繁，跳过");
            return;
        }

        if (mc.player != null && !hasSentAgainCommand) {
            System.out.println("AutoPlay: 准备发送 /again 指令");

            mc.player.connection.sendCommand("again");
            hasSentAgainCommand = true;
            lastCommandTime = currentTime;

            notificationManager.addNotification(new Notification(NotificationLevel.INFO, "已自动发送 /again 指令", 3000));
            ChatUtils.addChatMessage("§a自动发送指令: §e/again");
            System.out.println("AutoPlay: 已发送 /again 指令");
        }
    }

    @EventTarget
    private void onPacket(PacketEvent2 event) {
        if (!autoPlayAgain.getCurrentValue()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundSystemChatPacket) {
            ClientboundSystemChatPacket chatPacket = (ClientboundSystemChatPacket) packet;
            String message = chatPacket.content().getString();

            System.out.println("AutoPlay - 收到聊天消息: " + message);

            for (String keyword : GAME_START_KEYWORDS) {
                if (message.contains(keyword)) {
                    if (!game) {
                        game = true;
                        Start++;
                        resetAgainCommandState();
                        notificationManager.addNotification(new Notification(NotificationLevel.INFO, "检测到游戏开始!", 3000));
                        ChatUtils.addChatMessage("§a检测到游戏开始: " + keyword);
                        System.out.println("AutoPlay: 检测到游戏开始 - " + keyword);
                    }
                    break;
                }
            }

            for (String keyword : GAME_END_KEYWORDS) {
                if (message.contains(keyword)) {
                    System.out.println("AutoPlay: 检测到游戏结束关键词 - " + keyword);

                    game = false;
                    GG++;
                    updateSuffix();
                    notificationManager.addNotification(new Notification(NotificationLevel.SUCCESS, "检测到游戏结束!", 3000));
                    ChatUtils.addChatMessage("§a检测到游戏结束关键词: " + keyword);
                    System.out.println("AutoPlay: 检测到游戏结束，准备发送指令");

                    sendAgainCommand();
                    break;
                }
            }
        }

        if (packet instanceof ClientboundSetTitleTextPacket) {
            ClientboundSetTitleTextPacket titlePacket = (ClientboundSetTitleTextPacket) packet;
            String title = titlePacket.getText().getString();

            System.out.println("AutoPlay - 收到标题消息: " + title);

            for (String keyword : GAME_END_KEYWORDS) {
                if (title.contains(keyword)) {
                    System.out.println("AutoPlay: 标题检测到游戏结束关键词 - " + keyword);

                    game = false;
                    GG++;
                    updateSuffix();
                    notificationManager.addNotification(new Notification(NotificationLevel.SUCCESS, "标题检测到游戏结束!", 3000));
                    ChatUtils.addChatMessage("§a标题检测到游戏结束: " + keyword);
                    System.out.println("AutoPlay: 标题检测到游戏结束，准备发送指令");

                    sendAgainCommand();
                    break;
                }
            }
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (this.EffectsValue.getCurrentValue()) {
            if (mc.level == null) {
                return;
            }

            for (Player player : mc.level.players()) {
                if (player != mc.player) {
                    if (this.checkHoldingItem(player, EffectsUtil::isHoldingGodAxe)) {
                        this.flagPlayer(player, "God Axe");
                    } else if (this.checkHoldingItem(player, EffectsUtil::isHoldingAxe)) {
                        this.flagPlayer(player, "Sharpness 5+");
                    } else if (this.checkHoldingItem(player, EffectsUtil::isHoldingEnchantedGoldenApple)) {
                        this.flagEffect(player, "Enchanted Golden Apple");
                    } else if (this.checkHoldingItem(player, EffectsUtil::isKBPlusBool)) {
                        this.flagPlayer(player, "Knockback 3");
                    } else if (EffectsUtil.isRegen(player) > 0) {
                        this.flagEffect(player, "Regeneration");
                    } else if (EffectsUtil.isStrength(player) > 0) {
                        this.flagEffect(player, "Strength");
                    } else if (EffectsUtil.isSpeed(player) > 0) {
                        this.flagEffect(player, "Speed");
                    } else if (EffectsUtil.isJump(player) > 0) {
                        this.flagEffect(player, "Jump Boost");
                    } else if (EffectsUtil.isVanish(player) > 0) {
                        this.flagEffect(player, "Invisibility");
                    } else if (EffectsUtil.isResistance(player) > 0) {
                        this.flagEffect(player, "Resistance Boost");
                    }
                }
            }
        }
    }

    private boolean checkHoldingItem(Player player, ItemPredicate predicate) {
        return predicate.test(player);
    }

    private void flagPlayer(Player player, String message) {
        if (!flaggedEntity.contains(player)) {
            flaggedEntity.add(player);
            this.notifyPlayer(player, message);
        }
    }

    private void flagEffect(Player player, String message) {
        if (!flaggedEntity2.contains(player)) {
            flaggedEntity2.add(player);
            this.notifyPlayer(player, message);
        }
    }

    private void notifyPlayer(Player player, String message) {
        String s = player.getName().getString();
        if (FriendManager.isFriend(s)) {
            notificationManager.addNotification(new Notification(NotificationLevel.SUCCESS, s + " has " + message, 5000));
            ChatUtils.addChatMessage("§bYour friend §e" + s + " §bhas §e" + message);
        } else {
            notificationManager.addNotification(new Notification(NotificationLevel.ERROR, s + " has " + message, 5000));
            ChatUtils.addChatMessage("§cLoser §f" + s + " §cis a §f" + message + " §cplayer");
        }
    }

    public String getTag() {
        return "Game " + Start + "/" + GG;
    }

    @FunctionalInterface
    private interface ItemPredicate {
        boolean test(Player var1);
    }
}