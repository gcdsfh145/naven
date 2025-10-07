package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import net.minecraft.client.Camera;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   public static void drawBoundRoundedRect(float left, float top, float width, float height, float radius, int color) {
      float right = left + width;
      float bottom = top + height;

      final int semicircle = 18;
      final float f = 90.0f / semicircle;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);

      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferBuilder = tesselator.getBuilder();
      bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

      float r = ((color >> 16) & 0xFF) / 255.0f;
      float g = ((color >> 8) & 0xFF) / 255.0f;
      float b = (color & 0xFF) / 255.0f;
      float a = ((color >> 24) & 0xFF) / 255.0f;

      drawQuad(bufferBuilder, left + radius, top, right - radius, bottom, r, g, b, a);

      drawRoundedCorner(bufferBuilder, right - radius, top + radius, radius, 270, 360, semicircle, r, g, b, a); // 右上角
      drawRoundedCorner(bufferBuilder, left + radius, top + radius, radius, 180, 270, semicircle, r, g, b, a);  // 左上角
      drawRoundedCorner(bufferBuilder, left + radius, bottom - radius, radius, 90, 180, semicircle, r, g, b, a); // 左下角
      drawRoundedCorner(bufferBuilder, right - radius, bottom - radius, radius, 0, 90, semicircle, r, g, b, a);  // 右下角

      tesselator.end();
      RenderSystem.disableBlend();
   }

   private static void drawQuad(BufferBuilder bufferBuilder, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
      bufferBuilder.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(x1, y2, 0).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(x2, y2, 0).color(r, g, b, a).endVertex();

      bufferBuilder.vertex(x2, y2, 0).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(x2, y1, 0).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
   }

   private static void drawRoundedCorner(BufferBuilder bufferBuilder, float centerX, float centerY, float radius,
                                         float startAngle, float endAngle, int segments, float r, float g, float b, float a) {
      float angleStep = (endAngle - startAngle) / segments;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float) Math.toRadians(startAngle + i * angleStep);
         float angle2 = (float) Math.toRadians(startAngle + (i + 1) * angleStep);

         float x1 = centerX + radius * (float) Math.cos(angle1);
         float y1 = centerY + radius * (float) Math.sin(angle1);
         float x2 = centerX + radius * (float) Math.cos(angle2);
         float y2 = centerY + radius * (float) Math.sin(angle2);

         bufferBuilder.vertex(centerX, centerY, 0).color(r, g, b, a).endVertex();
         bufferBuilder.vertex(x1, y1, 0).color(r, g, b, a).endVertex();
         bufferBuilder.vertex(x2, y2, 0).color(r, g, b, a).endVertex();
      }
   }

   public static void drawRectBound(float x, float y, float width, float height, int color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);

      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferBuilder = tesselator.getBuilder();
      bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

      float r = ((color >> 16) & 0xFF) / 255.0f;
      float g = ((color >> 8) & 0xFF) / 255.0f;
      float b = (color & 0xFF) / 255.0f;
      float a = ((color >> 24) & 0xFF) / 255.0f;

      drawQuad(bufferBuilder, x, y, x + width, y + height, r, g, b, a);

      tesselator.end();
      RenderSystem.disableBlend();
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
   public static void drawHealthRing(PoseStack poseStack, float centerX, float centerY,
                                     float radius, float thickness, float progress) {
      if (progress <= 0) return;

      Matrix4f matrix = poseStack.last().pose();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);

      float r = 1.0f;
      float g = 1.0f;
      float b = 1.0f;
      float a = 1.0f;

      float sweepAngle = progress * 360.0f;

      int segments = (int) (Math.min(360, Math.max(36, sweepAngle)));
      float angleStep = sweepAngle / segments;

      float startAngle = -90.0f;

      buffer.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i <= segments; i++) {
         float angle = (float) Math.toRadians(startAngle + i * angleStep);

         float outerX = centerX + (float)Math.cos(angle) * radius;
         float outerY = centerY + (float)Math.sin(angle) * radius;
         buffer.vertex(matrix, outerX, outerY, 0)
                 .color(r, g, b, a)
                 .endVertex();

         float innerX = centerX + (float)Math.cos(angle) * (radius - thickness);
         float innerY = centerY + (float)Math.sin(angle) * (radius - thickness);
         buffer.vertex(matrix, innerX, innerY, 0)
                 .color(r, g, b, a)
                 .endVertex();
      }

      tessellator.end();
      RenderSystem.disableBlend();
   }
   public static void renderPlayer2D(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity player, float x, float y, float size, float radius, int color) {
      if (player instanceof net.minecraft.world.entity.player.Player) {
         PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
         if (playerInfo != null) {
            ResourceLocation skin = playerInfo.getSkinLocation();
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(poseStack, x, y, size, size, radius, -1);
            StencilUtils.erase(true);
            float a = (float)(color >> 24 & 255) / 255.0F;
            float r = (float)(color >> 16 & 255) / 255.0F;
            float g = (float)(color >> 8 & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;
            RenderSystem.setShaderColor(r, g, b, a);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(skin, (int)x, (int)y, (int)size, (int)size, 8.0F, 8.0F, 8, 8, 64, 64);
            guiGraphics.blit(skin, (int)x, (int)y, (int)size, (int)size, 40.0F, 8.0F, 8, 8, 64, 64);
            RenderSystem.disableBlend();
            StencilUtils.dispose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   public static int getThreeColorsGradient(int index, float speed, Color color1, Color color2, Color color3) {

      float time = (System.currentTimeMillis() + (long)index) % (long)((int)speed);
      float progress = (float)Math.sin(time * Math.PI * 2 / speed) + 1.0F;

      Color startColor, endColor;
      float localProgress;

      if (progress < 1.0F) {
         startColor = color1;
         endColor = color2;
         localProgress = progress;
      } else {
         startColor = color2;
         endColor = color3;
         localProgress = progress - 1.0F;
      }

      int red = (int)(startColor.getRed() + (endColor.getRed() - startColor.getRed()) * localProgress);
      int green = (int)(startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * localProgress);
      int blue = (int)(startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * localProgress);

      return new Color(red, green, blue).getRGB();
   }
   public static int getTwoColorsGradient(int index, float speed, Color color1, Color color2) {
      float time = (System.currentTimeMillis() + (long)index) % (long)((int)speed);
      float progress = (float)Math.sin(time * Math.PI * 2 / speed) * 0.5F + 0.5F;

      int red = (int)(color1.getRed() + (color2.getRed() - color1.getRed()) * progress);
      int green = (int)(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * progress);
      int blue = (int)(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * progress);

      return new Color(red, green, blue).getRGB();
   }
   public static void renderText(PoseStack poseStack, String text, double x, double y, int color, boolean shadow, float scale) {
      Color colorObj = new Color(color);
      Fonts.harmony.render(poseStack, text, x, y, colorObj, shadow, scale);
   }

   public static void drawRoundedRectWithColor(PoseStack poseStack, float x, float y, float width, float height, float radius, Color color) {
      RenderUtils.drawRoundedRect(poseStack, x, y, width, height, radius, color.getRGB());
   }

   public static int reAlpha(int color, float alpha) {
      int col = MathUtils.clamp((int)(alpha * 255.0F), 0, 255) << 24;
      col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
      col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
      return col | MathUtils.clamp(color & 0xFF, 0, 255);
   }
   public static void drawModel(PoseStack poseStack, float yaw, float pitch, LivingEntity target) {
      Lighting.setupFor3DItems();
      MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

      float originalYaw = target.getYRot();
      float originalPitch = target.getXRot();
      float originalBodyYaw = target.yBodyRot;

      target.yBodyRot = yaw - 0.4f;
      target.setYRot(yaw - 0.2f);
      target.setXRot(pitch);

      poseStack.pushPose();
      poseStack.mulPose(new org.joml.Quaternionf().rotationZ((float)Math.toRadians(180F)));
      poseStack.scale(-50, 50, 50);

      mc.getEntityRenderDispatcher().render(
              target,
              0.0D, 0.0D, 0.0D,
              0.0F,
              1.0F,
              poseStack,
              buffer,
              15728880
      );

      buffer.endBatch();
      poseStack.popPose();

      target.setYRot(originalYaw);
      target.setXRot(originalPitch);
      target.yBodyRot = originalBodyYaw;

      Lighting.setupForFlatItems();
   }

   public static void renderItemIcon(PoseStack poseStack, double x, double y, ItemStack itemStack) {
      if (!itemStack.isEmpty()) {
         renderGuiItem(poseStack, itemStack, (int) x, (int) y);
      }
   }
   public static void drawRectangle(PoseStack poseStack, float x, float y, float width, float height, int color) {
      float endX = x + width;
      float endY = y + height;

      drawQuads(poseStack, x, endY, endX, endY, endX, y, x, y, color);
   }

   public static void draw(PoseStack poseStack, String text, float x, float y, int color) {
      Matrix4f matrix = poseStack.last().pose();
      MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

      int lightLevel = 0xF000F0;

      mc.font.drawInBatch(text, x, y, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, lightLevel);

      bufferSource.endBatch();
   }

   private static void drawQuads(PoseStack poseStack, float x, float y, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
      float red = (float) (color >> 16 & 0xFF) / 255.0f;
      float green = (float) (color >> 8 & 0xFF) / 255.0f;
      float blue = (float) (color & 0xFF) / 255.0f;
      float alpha = (float) (color >> 24 & 0xFF) / 255.0f;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.setShaderColor(red, green, blue, alpha);

      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      Matrix4f matrix = poseStack.last().pose();

      bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(red, green, blue, alpha).endVertex();
      bufferBuilder.vertex(matrix, x3, y3, 0.0F).color(red, green, blue, alpha).endVertex();
      bufferBuilder.vertex(matrix, x4, y4, 0.0F).color(red, green, blue, alpha).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());

      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
   }

   public static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, int x, int y, BakedModel model) {
      mc.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.pushPose();

      poseStack.translate(x, y, 100.0F);
      poseStack.translate(8.0, 8.0, 0.0);
      poseStack.scale(1.0F, -1.0F, 1.0F);
      poseStack.scale(16.0F, 16.0F, 16.0F);
      RenderSystem.applyModelViewMatrix();

      MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
      boolean flag = !model.usesBlockLight();
      if (flag) {
         Lighting.setupForFlatItems();
      }

      mc.getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, model);
      bufferSource.endBatch();
      RenderSystem.enableDepthTest();
      if (flag) {
         Lighting.setupFor3DItems();
      }

      poseStack.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   public static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, int x, int y) {
      renderGuiItem(poseStack, itemStack, x, y, mc.getItemRenderer().getModel(itemStack, null, null, 0));
   }


   public static void drawRect(PoseStack poseStack, float x, float y, float x2, float y2, int color) {
      int i = color >> 24 & 0xFF;
      int j = color >> 16 & 0xFF;
      int k = color >> 8 & 0xFF;
      int l = color & 0xFF;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(poseStack.last().pose(), x2, y, 0.0F).color(j, k, l, i).endVertex();
      bufferbuilder.vertex(poseStack.last().pose(), x, y, 0.0F).color(j, k, l, i).endVertex();
      bufferbuilder.vertex(poseStack.last().pose(), x, y2, 0.0F).color(j, k, l, i).endVertex();
      bufferbuilder.vertex(poseStack.last().pose(), x2, y2, 0.0F).color(j, k, l, i).endVertex();
      tesselator.end();
      RenderSystem.disableBlend();
   }


   public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glEnable(2848);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix = poseStack.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
      Tesselator.getInstance().end();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(2848);
   }

   public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
      float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static BlockPos getCameraBlockPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getBlockPosition();
   }

   public static Vec3 getCameraPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getPosition();
   }

   public static RegionPos getCameraRegion() {
      return RegionPos.of(getCameraBlockPos());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack) {
      applyRegionalRenderOffset(matrixStack, getCameraRegion());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
      Vec3 offset = region.toVec3().subtract(getCameraPos());
      matrixStack.translate(offset.x, offset.y, offset.z);
   }

   public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
   }

   private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      if (pMinX < pMaxX) {
         float i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      if (pMinY < pMaxY) {
         float j = pMinY;
         pMinY = pMaxY;
         pMaxY = j;
      }

      float f3 = (float)(pColor >> 24 & 0xFF) / 255.0F;
      float f = (float)(pColor >> 16 & 0xFF) / 255.0F;
      float f1 = (float)(pColor >> 8 & 0xFF) / 255.0F;
      float f2 = (float)(pColor & 0xFF) / 255.0F;
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      Tesselator.getInstance().end();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x + width, y, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
      tesselator.end();
   }

   private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
   }

   public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
      if (color == 16777215) {
         color = ARGB32.color(255, 255, 255, 255);
      }

      if (edgeRadius < 0.0F) {
         edgeRadius = 0.0F;
      }

      if (edgeRadius > width / 2.0F) {
         edgeRadius = width / 2.0F;
      }

      if (edgeRadius > height / 2.0F) {
         edgeRadius = height / 2.0F;
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(1.0F);
      drawRectBound(poseStack, x + edgeRadius, y + edgeRadius, width - edgeRadius * 2.0F, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + edgeRadius, y, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x + edgeRadius, y + height - edgeRadius, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + width - edgeRadius, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      float centerX = x + edgeRadius;
      float centerY = y + edgeRadius;
      int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 180) / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 90) / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 270) / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)i / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      RenderSystem.disableBlend();
   }

   public static void drawSolidBox(PoseStack matrixStack) {
      drawSolidBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
      Tesselator tessellator = RenderSystem.renderThreadTesselator();
      BufferBuilder bufferBuilder = tessellator.getBuilder();
      Matrix4f matrix = matrixStack.last().pose();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawOutlinedBox(PoseStack matrixStack) {
      drawOutlinedBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
      Matrix4f matrix = matrixStack.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      drawSolidBox(bb, bufferBuilder);
      BufferUploader.reset();
      vertexBuffer.bind();
      RenderedBuffer buffer = bufferBuilder.end();
      vertexBuffer.upload(buffer);
      VertexBuffer.unbind();
   }

   public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      drawOutlinedBox(bb, bufferBuilder);
      vertexBuffer.upload(bufferBuilder.end());
   }

   public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
      return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
      return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
   }

   public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
      float right = left + width;
      float bottom = top + height;
      fill(stack, left, top, right, bottom, color);
   }

   public static void 装女人(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

    public static void drawSolidBox(AABB box, PoseStack stack, float[] color) {
        if (color.length < 4) return;

        GL11.glPushMatrix();
        GL11.glColor4f(color[0], color[1], color[2], color[3]);

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        GL11.glBegin(GL11.GL_QUADS);
        // 六个面省略，可以复制上次方法
        GL11.glEnd();

        GL11.glPopMatrix();
    }
}
