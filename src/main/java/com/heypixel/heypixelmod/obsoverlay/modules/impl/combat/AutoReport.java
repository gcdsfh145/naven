package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(
        name = "AutoReport",
        description = "Automatically reports players who kill you",
        category = Category.MISC
)
public class AutoReport extends Module {
    private int stage = 0;
    private final List<Integer> targetSlots = new ArrayList<>();
    private int currentTarget = 0;
    private int reportDelay = 0;
    private int guiLoadDelay = 0;
    private String killerName = null;
    private String playerName = null;
    private long lastPlayerNameCheckTime = 0;
    private static final long PLAYER_NAME_CHECK_INTERVAL = 5000L;
    private boolean isProcessing = false;

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Pattern> deathPatterns = List.of(
            Pattern.compile("\\s*([^\\s]+)\\s*被\\s*([^\\s]+)\\s*击败"),
            Pattern.compile("\\s*([^\\s]+)\\s*被炸成了粉尘, 最终还是被\\s*([^\\s]+)\\s*击败!"),
            Pattern.compile("\\s*([^\\s]+)\\s*消逝了, 最终还是被\\s*([^\\s]+)\\s*击败!"),
            Pattern.compile("\\s*([^\\s]+)\\s*被架在了烧烤架上, 熟透了, 最终还是被\\s*([^\\s]+)\\s*击败!"),
            Pattern.compile("\\s*([^\\s]+)\\s*跑得很快, 但是他还是摔了一跤, 最终被\\s*([^\\s]+)\\s*击败"),
            Pattern.compile("\\s*([^\\s]+)\\s*被\\s*([^\\s]+)\\s*用弓箭射穿了"),
            Pattern.compile("\\s*([^\\s]+)\\s*被重压地无法呼吸, 最终还是被\\s*([^\\s]+)\\s*击败!"),
            Pattern.compile("\\s*([^\\s]+)\\s*被\\s*([^\\s]+)\\s*杀死了"),
            Pattern.compile("\\s*([^\\s]+)\\s*被\\s*([^\\s]+)\\s*终结了"),
            Pattern.compile("\\s*([^\\s]+)\\s*死于\\s*([^\\s]+)\\s*之手"),
            Pattern.compile("(.+?)被(.+?)击败"),
            Pattern.compile("(.+?)被炸成了粉尘, 最终还是被(.+?)击败!"),
            Pattern.compile("(.+?)消逝了, 最终还是被(.+?)击败!"),
            Pattern.compile("(.+?)被架在了烧烤架上, 熟透了, 最终还是被(.+?)击败!"),
            Pattern.compile("(.+?)跑得很快, 但是他还是摔了一跤, 最终被(.+?)击败"),
            Pattern.compile("(.+?)被(.+?)用弓箭射穿了"),
            Pattern.compile("(.+?)被重压地无法呼吸, 最终还是被(.+?)击败!")
    );

    @Override
    public void onEnable() {
        resetState();
        log("自动举报模块已启用", ChatFormatting.GREEN);
    }

    @Override
    public void onDisable() {
        resetState();
        log("自动举报模块已禁用", ChatFormatting.RED);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!this.isEnabled() || mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerNameCheckTime >= PLAYER_NAME_CHECK_INTERVAL) {
            playerName = mc.player.getScoreboardName();
            lastPlayerNameCheckTime = currentTime;
        }

        if (killerName == null) {
            if (isProcessing) {
                resetState();
            }
            return;
        }

        isProcessing = true;

        switch (stage) {
            case 0:
                log("正在发送举报命令...", ChatFormatting.GRAY);
                mc.player.connection.sendCommand("report " + killerName);
                stage = 1;
                guiLoadDelay = 2;
                break;

            case 1:
                if (guiLoadDelay > 0) {
                    guiLoadDelay--;
                    break;
                }

                if (!(mc.screen instanceof ContainerScreen)) {
                    resetState();
                    break;
                }

                ContainerScreen screen = (ContainerScreen) mc.screen;
                String title = screen.getTitle().getString();

                if (!title.contains("举报") && !title.contains("Report")) {
                    resetState();
                    break;
                }

                targetSlots.clear();
                for (int i = 0; i < screen.getMenu().slots.size(); i++) {
                    var currentSlot = screen.getMenu().getSlot(i);
                    var stack = currentSlot.getItem();
                    if (!stack.isEmpty() && (stack.getItem() == Items.PLAYER_HEAD || stack.getItem() == Items.SKELETON_SKULL)) {
                        targetSlots.add(i);
                    }
                }

                if (targetSlots.isEmpty()) {
                    log("未找到举报对象", ChatFormatting.RED);
                    resetState();
                    break;
                }

                stage = 2;
                currentTarget = 0;
                break;

            case 2:
                if (!(mc.screen instanceof ContainerScreen)) {
                    resetState();
                    break;
                }

                ContainerScreen currentScreen = (ContainerScreen) mc.screen;

                if (currentTarget >= targetSlots.size()) {
                    log("未找到匹配的玩家: " + killerName, ChatFormatting.RED);
                    resetState();
                    break;
                }

                int slotIndex = targetSlots.get(currentTarget);
                var targetSlot = currentScreen.getMenu().getSlot(slotIndex);

                if (targetSlot.hasItem()) {
                    Component displayName = targetSlot.getItem().getHoverName();
                    String name = displayName.getString();

                    if (name.contains(killerName)) {
                        log("已选择举报对象: " + killerName, ChatFormatting.GOLD);
                        clickSlot(currentScreen, slotIndex);
                        reportDelay = 10;
                        stage = 3;
                    }
                }

                currentTarget++;
                break;

            case 3:
                if (reportDelay > 0) {
                    reportDelay--;
                    break;
                }

                stage = 4;
                break;

            case 4:
                if (!(mc.screen instanceof ContainerScreen)) {
                    resetState();
                    break;
                }

                ContainerScreen gui = (ContainerScreen) mc.screen;
                boolean foundSword = false;

                for (var reportSlot : gui.getMenu().slots) {
                    if (reportSlot.hasItem() && reportSlot.getItem().getItem() == Items.DIAMOND_SWORD) {
                        log("正在提交举报...", ChatFormatting.LIGHT_PURPLE);
                        clickSlot(gui, reportSlot.index);
                        foundSword = true;
                        break;
                    }
                }

                if (foundSword) {
                    log("成功举报玩家: " + killerName, ChatFormatting.GREEN);
                    mc.setScreen(null);
                    resetState();
                } else {
                    log("未找到举报选项", ChatFormatting.RED);
                    resetState();
                }
                break;
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (!this.isEnabled() || event.getMessage() == null || mc.player == null) return;

        String message = event.getMessage().getString();

        if (playerName == null) {
            playerName = mc.player.getScoreboardName();
        }

        for (Pattern pattern : deathPatterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String victim = matcher.group(1).trim();
                String killer = matcher.group(2).trim();
                if (victim.equals(playerName) && !FriendManager.isFriend(killer)) {
                    log("检测到被击杀: " + killer, ChatFormatting.RED);
                    resetState();
                    killerName = killer;
                    stage = 0;
                    return;
                }
            }
        }
        if (message.contains(playerName) && message.contains("被") && message.contains("击败")) {
            int victimIndex = message.indexOf(playerName);
            int killedIndex = message.indexOf("被", victimIndex);

            if (killedIndex != -1) {
                int killerStart = killedIndex + 1;
                int killerEnd = message.indexOf("击败", killerStart);

                if (killerEnd != -1) {
                    String potentialKiller = message.substring(killerStart, killerEnd).trim();
                    potentialKiller = potentialKiller.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "");

                    if (!potentialKiller.isEmpty() && !FriendManager.isFriend(potentialKiller)) {
                        log("检测到被击杀: " + potentialKiller, ChatFormatting.RED);
                        resetState();
                        killerName = potentialKiller;
                        stage = 0;
                    }
                }
            }
        }
    }

    private void clickSlot(ContainerScreen screen, int slotIndex) {
        if (mc.gameMode == null) return;
        if (mc.player == null) return;

        int containerId = screen.getMenu().containerId;
        mc.gameMode.handleInventoryMouseClick(containerId, slotIndex, 0, ClickType.PICKUP, mc.player);
    }

    private void resetState() {
        stage = 0;
        currentTarget = 0;
        targetSlots.clear();
        reportDelay = 0;
        guiLoadDelay = 0;
        killerName = null;
        isProcessing = false;
    }

    private void log(String message, ChatFormatting color) {
        if (mc.player == null) return;

        Component text = Component.literal("[自动举报] " + message).withStyle(color);
        mc.player.displayClientMessage(text, false);
    }
}