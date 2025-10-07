package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class PlayerWidget extends DraggableWidget {
    final SmoothAnimationTimer height = new SmoothAnimationTimer(0), width = new SmoothAnimationTimer(0);
    final HashMap<String, String> map = new LinkedHashMap<>();
    boolean shouldRender = false;
    BooleanValue value;
    private final CustomTextRenderer fontRenderer;

    public PlayerWidget(BooleanValue value) {
        super("Players");
        this.value = value;
        this.fontRenderer = Fonts.opensans;
    }

    private static class EntityWatcher {
        public static boolean hasGodAxe(Player player) { return false; }
        public static boolean hasEnchantedGApple(Player player) { return false; }
    }

    private static class HackerDetector {
        public static boolean isCheating(Player player) { return false; }
    }

    private static class Teams {
        public static boolean isSameTeam(Player player) { return false; }
    }

    private static class FriendManager {
        public static boolean isFriend(Player player) { return false; }
    }

    public void onUpdate() {
        map.clear();

        if (mc.level != null) {
            ArrayList<Player> players = new ArrayList<>(mc.level.players());
            players.removeIf(entity -> entity.getId() < 0);
            players.sort((o1, o2) -> (int) (mc.player.distanceTo(o1) * 1000 - mc.player.distanceTo(o2) * 1000));

            players.forEach(entity -> {
                if (!Teams.isSameTeam(entity) && !FriendManager.isFriend(entity) && entity != mc.player) {
                    int health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());

                    if (EntityWatcher.hasGodAxe(entity)) {
                        map.put("[" + Math.round(mc.player.distanceTo(entity)) + "m][" + health + "HP] " + entity.getName().getString(), "God Axe");
                    } else if (EntityWatcher.hasEnchantedGApple(entity)) {
                        map.put("[" + Math.round(mc.player.distanceTo(entity)) + "m][" + health + "HP] " + entity.getName().getString(), "Enchanted GApple");
                    } else if (HackerDetector.isCheating(entity)) {
                        map.put("[" + Math.round(mc.player.distanceTo(entity)) + "m][" + health + "HP] " + entity.getName().getString(), "Hacker");
                    }
                }
            });
        }

        shouldRender = (!map.isEmpty() || mc.screen instanceof ChatScreen) && value.getCurrentValue();

        if (!shouldRender) {
            height.target = 0;
            width.target = 0;
        }
    }

    @Override
    public void renderBody() {
        PoseStack poseStack = new PoseStack();
        double scale = 0.5;

        fontRenderer.render(poseStack, name, 2, 2, 0xFFFFFFFF);

        if (shouldRender) {
            height.target = 12;
            width.target = (float) fontRenderer.getWidth(name, scale);

            for (HashMap.Entry<String, String> entry : map.entrySet()) {
                float entryWidth = (float) fontRenderer.getWidth(entry.getKey() + " " + entry.getValue(), scale) + 10;
                width.target = Math.max(width.target, entryWidth);
            }

            float currentY = 2 + height.target;
            for (HashMap.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                fontRenderer.render(poseStack, key, 2, (int) currentY, 0xFFFFFFFF);

                float valueWidth = (float) fontRenderer.getWidth(value, scale);
                float valueX = 2 + width.target - valueWidth;
                fontRenderer.render(poseStack, value, (int) valueX, (int) currentY, 0xFFFFFFFF);

                height.target += 10;
                currentY += 10;
            }
        } else {
            height.target = 0;
            width.target = 0;
        }

        height.update(true);
        width.update(true);
    }

    @Override
    public float getWidth() {
        return width.value + 6;
    }

    @Override
    public float getHeight() {
        return height.value + 6;
    }

    @Override
    public boolean shouldRender() {
        return shouldRender || width.value > 1 || height.value > 1;
    }
}