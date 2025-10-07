package com.heypixel.heypixelmod.obsoverlay.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;

public class Target {
    private static final DecimalFormat decimalFormat = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static float easingHealth = 0.0F;
    private static LivingEntity lastTarget = null;
    private static long lastUpdateTime = 0L;
    private static final Minecraft mc = Minecraft.getInstance();

    public static void drawTargetInfo(GuiGraphics guiGraphics, LivingEntity target) {
        PoseStack poseStack = guiGraphics.pose();
        float f = target.getHealth();
        float f1 = target.getMaxHealth();
        float f2 = f / f1;
        if (target != lastTarget) {
            lastTarget = target;
            easingHealth = f;
        } else {
            long i = System.currentTimeMillis() - lastUpdateTime;
            easingHealth = easingHealth + (f - easingHealth) * Math.min(1.0F, (float) i / 500.0F);
        }

        lastUpdateTime = System.currentTimeMillis();
        float f6 = (float) mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
        float f3 = (float) mc.getWindow().getGuiScaledHeight() / 2.0F - 60.0F;
        float f4 = 150.0F;
        float f5 = 60.0F;
        RenderUtils.drawRect(poseStack, f6, f3, f6 + f4, f3 + f5, new Color(0, 0, 0, 130).getRGB());
        guiGraphics.drawString(mc.font, target.getName().getString(), (int)(f6 + 40.0F), (int)(f3 + 5.0F), Color.WHITE.getRGB(), true);
        guiGraphics.drawString(mc.font, "Health: " + decimalFormat.format((double) easingHealth) + " / " + decimalFormat.format((double) f1),
                (int)(f6 + 40.0F), (int)(f3 + 20.0F), Color.WHITE.getRGB(), true);
        RenderUtils.drawRect(
                poseStack,
                f6 + 40.0F,
                f3 + 35.0F,
                f6 + 40.0F + easingHealth / f1 * (f4 - 50.0F),
                f3 + 45.0F,
                new Color(
                        Math.max(0, Math.min(255, (int) (255.0F * (1.0F - Math.max(0.0F, Math.min(1.0F, f2)))))),
                        Math.max(0, Math.min(255, (int) (255.0F * Math.max(0.0F, Math.min(1.0F, f2))))),
                        0
                )
                        .getRGB()
        );
        guiGraphics.drawString(mc.font, "Armor: " + target.getArmorValue(), (int)(f6 + 40.0F), (int)(f3 + 50.0F), Color.WHITE.getRGB(), true);
    }
}