package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.google.common.collect.Lists;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector4f;

@ModuleInfo(
        name = "EffectDisplay",
        description = "Displays potion effects on the HUD",
        category = Category.RENDER
)
public class EffectDisplay extends Module {
   ModeValue targetHud = ValueBuilder.create(this, "EffectDisplay HUD")
           .setDefaultModeIndex(1)
           .setModes("Naven", "New")
           .build()
           .getModeValue();
   private List<Runnable> list;
   private final Map<MobEffect, EffectDisplay.MobEffectInfo> infos = new ConcurrentHashMap<>();
   private final Color headerColor = new Color(150, 45, 45, 255);
   private final Color bodyColor = new Color(0, 0, 0, 50);
   private final List<Vector4f> blurMatrices = new ArrayList<>();

   @EventTarget(4)
   public void renderIcons(EventRender2D e) {
      if (this.list != null) {
         this.list.forEach(Runnable::run);
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      String currentMode = targetHud.getCurrentMode();

      for (Vector4f matrix : this.blurMatrices) {
         if ("New".equals(currentMode)) {
            RenderUtils.fillBound(e.getStack(), matrix.x(), matrix.y(), matrix.z(), matrix.w(), 1073741824);
         } else if ("Naven".equals(currentMode)) {
            RenderUtils.drawRoundedRect(e.getStack(), matrix.x(), matrix.y(), matrix.z(), matrix.w(), 5.0F, 1073741824);
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      for (MobEffectInstance effect : mc.player.getActiveEffects()) {
         EffectDisplay.MobEffectInfo info;
         if (this.infos.containsKey(effect.getEffect())) {
            info = this.infos.get(effect.getEffect());
         } else {
            info = new EffectDisplay.MobEffectInfo();
            this.infos.put(effect.getEffect(), info);
         }

         info.maxDuration = Math.max(info.maxDuration, effect.getDuration());
         info.duration = effect.getDuration();
         info.amplifier = effect.getAmplifier();
         info.shouldDisappear = false;
      }

      int startY = mc.getWindow().getGuiScaledHeight() / 2 - this.infos.size() * 16;
      this.list = Lists.newArrayListWithExpectedSize(this.infos.size());
      this.blurMatrices.clear();

      String currentMode = targetHud.getCurrentMode();

      for (Entry<MobEffect, EffectDisplay.MobEffectInfo> entry : this.infos.entrySet()) {
         e.getStack().pushPose();
         EffectDisplay.MobEffectInfo effectInfo = entry.getValue();

         String text;
         if ("New".equals(currentMode)) {
            text = getDisplayName(entry.getKey(), effectInfo);
         } else if ("Naven".equals(currentMode)) {
            text = getNavenDisplayName(entry.getKey(), effectInfo);
         } else {
            text = getDisplayName(entry.getKey(), effectInfo);
         }

         if (effectInfo.yTimer.value == -1.0F) {
            effectInfo.yTimer.value = (float)startY;
         }

         CustomTextRenderer harmony = Fonts.harmony;
         effectInfo.width = 25.0F + harmony.getWidth(text, 0.3) + 20.0F;
         float x = effectInfo.xTimer.value;
         float y = effectInfo.yTimer.value;
         effectInfo.shouldDisappear = !mc.player.hasEffect(entry.getKey());

         if (effectInfo.shouldDisappear) {
            effectInfo.xTimer.target = -effectInfo.width - 20.0F;
            if (x <= -effectInfo.width - 20.0F) {
               this.infos.remove(entry.getKey());
            }
         } else {
            effectInfo.durationTimer.target = (float)effectInfo.duration / (float)effectInfo.maxDuration * effectInfo.width;
            if (effectInfo.durationTimer.value <= 0.0F) {
               effectInfo.durationTimer.value = effectInfo.durationTimer.target;
            }

            effectInfo.xTimer.target = 10.0F;
            effectInfo.yTimer.target = (float)startY;
            effectInfo.yTimer.update(true);
         }

         effectInfo.durationTimer.update(true);
         effectInfo.xTimer.update(true);

         if ("New".equals(currentMode)) {
            renderNewMode(e, entry, effectInfo, x, y, harmony, text);
         } else if ("Naven".equals(currentMode)) {
            renderNavenMode(e, entry, effectInfo, x, y, harmony, text);
         } else {
            renderNewMode(e, entry, effectInfo, x, y, harmony, text);
         }

         startY += 34;
         e.getStack().popPose();
      }
   }

   private void renderNewMode(EventRender2D e, Entry<MobEffect, EffectDisplay.MobEffectInfo> entry,
                              EffectDisplay.MobEffectInfo effectInfo, float x, float y,
                              CustomTextRenderer harmony, String text) {
      StencilUtils.write(false);
      RenderUtils.drawRoundedRect(e.getStack(), x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F, 5.0F, -1);
      StencilUtils.erase(true);
      int effectColor = getEffectThemeColor(entry.getKey(), 95);
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.width, 30.0F, effectColor);
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.durationTimer.value, 30.0F, effectColor);

      harmony.render(e.getStack(), text, (double)(x + 27.0F), (double)(y + 7.0F), Color.WHITE, true, 0.3);
      String duration = StringUtil.formatTickDuration(effectInfo.duration);
      harmony.render(e.getStack(), duration, (double)(x + 27.0F), (double)(y + 17.0F), Color.WHITE, true, 0.25);

      MobEffectTextureManager mobeffecttexturemanager = mc.getMobEffectTextures();
      TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(entry.getKey());
      this.list.add(() -> {
         RenderSystem.setShaderTexture(0, textureatlassprite.atlasLocation());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         e.getGuiGraphics().blit((int)(x + 6.0F), (int)(y + 8.0F), 1, 18, 18, textureatlassprite);
      });

      this.blurMatrices.add(new Vector4f(x, y, effectInfo.width, 30.0F));
      StencilUtils.dispose();
   }

   private void renderNavenMode(EventRender2D e, Entry<MobEffect, EffectDisplay.MobEffectInfo> entry,
                                EffectDisplay.MobEffectInfo effectInfo, float x, float y,
                                CustomTextRenderer harmony, String text) {
      StencilUtils.write(false);
      this.blurMatrices.add(new Vector4f(x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F));
      RenderUtils.drawRoundedRect(e.getStack(), x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F, 5.0F, -1);
      StencilUtils.erase(true);
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.width, 30.0F, this.bodyColor.getRGB());
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.durationTimer.value, 30.0F, this.bodyColor.getRGB());
      RenderUtils.drawRoundedRect(e.getStack(), x + effectInfo.width - 10.0F, y + 7.0F, 5.0F, 18.0F, 2.0F, this.headerColor.getRGB());

      harmony.render(e.getStack(), text, (double)(x + 27.0F), (double)(y + 7.0F), this.headerColor, true, 0.3);
      String duration = StringUtil.formatTickDuration(effectInfo.duration);
      harmony.render(e.getStack(), duration, (double)(x + 27.0F), (double)(y + 17.0F), Color.WHITE, true, 0.25);

      MobEffectTextureManager mobeffecttexturemanager = mc.getMobEffectTextures();
      TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(entry.getKey());
      this.list.add(() -> {
         RenderSystem.setShaderTexture(0, textureatlassprite.atlasLocation());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         e.getGuiGraphics().blit((int)(x + 6.0F), (int)(y + 8.0F), 1, 18, 18, textureatlassprite);
      });
      StencilUtils.dispose();
   }

   private int getEffectThemeColor(MobEffect effect, int alpha) {
      String descriptionId = effect.getDescriptionId();

      if (descriptionId.equals("effect.minecraft.strength")) return createColorWithAlpha(0x932423, alpha);
      if (descriptionId.equals("effect.minecraft.weakness")) return createColorWithAlpha(0x484D48, alpha);
      if (descriptionId.equals("effect.minecraft.speed")) return createColorWithAlpha(0x7CAFC6, alpha);
      if (descriptionId.equals("effect.minecraft.slowness")) return createColorWithAlpha(0x5A6C81, alpha);
      if (descriptionId.equals("effect.minecraft.jump_boost")) return createColorWithAlpha(0x22FF4C, alpha);
      if (descriptionId.equals("effect.minecraft.regeneration")) return createColorWithAlpha(0xCD5CAB, alpha);
      if (descriptionId.equals("effect.minecraft.poison")) return createColorWithAlpha(0x4E9331, alpha);
      if (descriptionId.equals("effect.minecraft.fire_resistance")) return createColorWithAlpha(0xE49A3A, alpha);
      if (descriptionId.equals("effect.minecraft.water_breathing")) return createColorWithAlpha(0x2E5299, alpha);
      if (descriptionId.equals("effect.minecraft.invisibility")) return createColorWithAlpha(0x7F8392, alpha);
      if (descriptionId.equals("effect.minecraft.night_vision")) return createColorWithAlpha(0x1F1FA1, alpha);
      if (descriptionId.equals("effect.minecraft.haste")) return createColorWithAlpha(0xD9C043, alpha);
      if (descriptionId.equals("effect.minecraft.mining_fatigue")) return createColorWithAlpha(0x4A4217, alpha);
      if (descriptionId.equals("effect.minecraft.resistance")) return createColorWithAlpha(0x99453A, alpha);
      if (descriptionId.equals("effect.minecraft.absorption")) return createColorWithAlpha(0x2552A5, alpha);
      if (descriptionId.equals("effect.minecraft.health_boost")) return createColorWithAlpha(0xF87D23, alpha);
      if (descriptionId.equals("effect.minecraft.saturation")) return createColorWithAlpha(0xF8AD48, alpha);
      if (descriptionId.equals("effect.minecraft.glowing")) return createColorWithAlpha(0x94A61B, alpha);
      if (descriptionId.equals("effect.minecraft.levitation")) return createColorWithAlpha(0xCE32ED, alpha);
      if (descriptionId.equals("effect.minecraft.luck")) return createColorWithAlpha(0x339900, alpha);
      if (descriptionId.equals("effect.minecraft.unluck")) return createColorWithAlpha(0xBC0000, alpha);
      if (descriptionId.equals("effect.minecraft.slow_falling")) return createColorWithAlpha(0xF7F8CE, alpha);
      if (descriptionId.equals("effect.minecraft.conduit_power")) return createColorWithAlpha(0x1BCAD8, alpha);
      if (descriptionId.equals("effect.minecraft.dolphins_grace")) return createColorWithAlpha(0x86B2CA, alpha);
      if (descriptionId.equals("effect.minecraft.bad_omen")) return createColorWithAlpha(0x0B6138, alpha);
      if (descriptionId.equals("effect.minecraft.hero_of_the_village")) return createColorWithAlpha(0xCDD724, alpha);
      if (descriptionId.equals("effect.minecraft.darkness")) return createColorWithAlpha(0x1E1E23, alpha);

      return createColorWithAlpha(0x000000, alpha);
   }

   private int createColorWithAlpha(int rgb, int alpha) {
      int r = (rgb >> 16) & 0xFF;
      int g = (rgb >> 8) & 0xFF;
      int b = rgb & 0xFF;
      return (alpha << 24) | (r << 16) | (g << 8) | b;
   }

   public String getDisplayName(MobEffect effect, EffectDisplay.MobEffectInfo info) {
      String effectName = getEnglishEffectName(effect);
      String amplifierName;
      if (info.amplifier == 0) {
         amplifierName = "";
      } else if (info.amplifier == 1) {
         amplifierName = " II";
      } else if (info.amplifier == 2) {
         amplifierName = " III";
      } else if (info.amplifier == 3) {
         amplifierName = " IV";
      } else {
         amplifierName = " " + (info.amplifier + 1);
      }

      return effectName + amplifierName;
   }

   public String getNavenDisplayName(MobEffect effect, EffectDisplay.MobEffectInfo info) {
      String effectName = effect.getDisplayName().getString();
      String amplifierName;
      if (info.amplifier == 0) {
         amplifierName = "";
      } else if (info.amplifier == 1) {
         amplifierName = " " + I18n.get("enchantment.level.2");
      } else if (info.amplifier == 2) {
         amplifierName = " " + I18n.get("enchantment.level.3");
      } else if (info.amplifier == 3) {
         amplifierName = " " + I18n.get("enchantment.level.4");
      } else {
         amplifierName = " " + (info.amplifier + 1);
      }

      return effectName + amplifierName;
   }

   private String getEnglishEffectName(MobEffect effect) {
      String descriptionId = effect.getDescriptionId();

      if (descriptionId.equals("effect.minecraft.strength")) return "Strength";
      if (descriptionId.equals("effect.minecraft.weakness")) return "Weakness";
      if (descriptionId.equals("effect.minecraft.speed")) return "Speed";
      if (descriptionId.equals("effect.minecraft.slowness")) return "Slowness";
      if (descriptionId.equals("effect.minecraft.jump_boost")) return "Jump Boost";
      if (descriptionId.equals("effect.minecraft.regeneration")) return "Regeneration";
      if (descriptionId.equals("effect.minecraft.poison")) return "Poison";
      if (descriptionId.equals("effect.minecraft.fire_resistance")) return "Fire Resistance";
      if (descriptionId.equals("effect.minecraft.water_breathing")) return "Water Breathing";
      if (descriptionId.equals("effect.minecraft.invisibility")) return "Invisibility";
      if (descriptionId.equals("effect.minecraft.night_vision")) return "Night Vision";
      if (descriptionId.equals("effect.minecraft.haste")) return "Haste";
      if (descriptionId.equals("effect.minecraft.mining_fatigue")) return "Mining Fatigue";
      if (descriptionId.equals("effect.minecraft.resistance")) return "Resistance";
      if (descriptionId.equals("effect.minecraft.absorption")) return "Absorption";
      if (descriptionId.equals("effect.minecraft.health_boost")) return "Health Boost";
      if (descriptionId.equals("effect.minecraft.saturation")) return "Saturation";
      if (descriptionId.equals("effect.minecraft.glowing")) return "Glowing";
      if (descriptionId.equals("effect.minecraft.levitation")) return "Levitation";
      if (descriptionId.equals("effect.minecraft.luck")) return "Luck";
      if (descriptionId.equals("effect.minecraft.unluck")) return "Bad Luck";
      if (descriptionId.equals("effect.minecraft.slow_falling")) return "Slow Falling";
      if (descriptionId.equals("effect.minecraft.conduit_power")) return "Conduit Power";
      if (descriptionId.equals("effect.minecraft.dolphins_grace")) return "Dolphin's Grace";
      if (descriptionId.equals("effect.minecraft.bad_omen")) return "Bad Omen";
      if (descriptionId.equals("effect.minecraft.hero_of_the_village")) return "Hero of the Village";
      if (descriptionId.equals("effect.minecraft.darkness")) return "Darkness";
      return descriptionId.replace("effect.minecraft.", "");
   }

   public static class MobEffectInfo {
      public SmoothAnimationTimer xTimer = new SmoothAnimationTimer(-60.0F, 0.2F);
      public SmoothAnimationTimer yTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public SmoothAnimationTimer durationTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public int maxDuration = -1;
      public int duration = 0;
      public int amplifier = 0;
      public boolean shouldDisappear = false;
      public float width;
   }
}