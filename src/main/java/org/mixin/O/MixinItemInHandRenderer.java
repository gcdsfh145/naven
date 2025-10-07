package org.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class MixinItemInHandRenderer {
   private static final Minecraft mc = Minecraft.getInstance();

   @Redirect(
           method = {"tick"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
           )
   )
   public ItemStack hookMainHand(LocalPlayer player) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.MAIN_HAND, player.getMainHandItem());
      if (player == Minecraft.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }

   @Redirect(
           method = {"tick"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"
           )
   )
   public ItemStack hookOffHand(LocalPlayer player) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.OFF_HAND, player.getOffhandItem());
      if (player == Minecraft.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }

   @Shadow
   public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int light );

   @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
   public void onRenderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
      Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
      boolean autoBlock = aura.shouldAutoBlock();
      LivingEntity target = (LivingEntity) aura.aimingTarget;
      if (hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof SwordItem && autoBlock && target != null) {
         ci.cancel();
         int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
         poseStack.pushPose();
         String mode = aura.blockMode.getCurrentMode();
         switch (mode) {
            case "1.7":
               poseStack.translate((double) ((float) side * 0.56F), (double) (-0.52F), -0.72F);
               float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
               float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               poseStack.mulPose(Axis.YP.rotation((float) side * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation((float) side * f22 * -20.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.YP.rotation((float) side * -45.0F * (float) Math.PI / 180.0F));
               poseStack.scale(0.9F, 0.9F, 0.9F);
               poseStack.translate(-0.2F, 0.126F, 0.2F);
               poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.YP.rotation((float) side * 15.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation((float) side * 80.0F * (float) Math.PI / 180.0F));
               break;
            case "1.8":
               poseStack.translate((double) ((float) side * 0.56F), (double) (-0.52F), -0.72F);
               poseStack.translate((double) ((float) side * -0.1414214F), 0.08F, 0.1414214F);
               poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.YP.rotation((float) side * 13.365F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation((float) side * 78.05F * (float) Math.PI / 180.0F));
               float f16 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
               float f21 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               poseStack.mulPose(Axis.YP.rotation(f16 * -20.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation(f21 * -20.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.XP.rotation(f21 * -80.0F * (float) Math.PI / 180.0F));
               break;

            case "Push":
               poseStack.translate((double) ((float) side * 0.56F), (double) (-0.52F), -0.72F);
               poseStack.translate((double) ((float) side * -0.1414214F), 0.08F, 0.1414214F);
               poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.YP.rotation((float) side * 13.365F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation((float) side * 78.05F * (float) Math.PI / 180.0F));
               float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
               float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
               poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
               poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
               break;
            case "New":
               poseStack.translate((float) side * 0.56f, -0.52f + equippedProgress * -0.6f, -0.72);
               poseStack.translate((float) side * -0.1414214f, 0.08f, 0.1414213925600052);
               poseStack.mulPose(Axis.XP.rotationDegrees(-102.25f));
               poseStack.mulPose(Axis.YP.rotationDegrees((float) side * 13.365f));
               poseStack.mulPose(Axis.ZP.rotationDegrees((float) side * 78.05f));
               double f = Math.sin((double)(swingProgress * swingProgress) * Math.PI);
               double f1 = Math.sin(Math.sqrt(swingProgress) * Math.PI);
               poseStack.mulPose(Axis.YP.rotationDegrees((float)(f * -20.0)));
               poseStack.mulPose(Axis.ZP.rotationDegrees((float)(f1 * -20.0)));
               poseStack.mulPose(Axis.XP.rotationDegrees((float)(f1 * -80.0)));
               poseStack.scale(1.0f, 1.0f, 1.0f);
               break;
         }
         boolean isRightHand = player.getMainArm() == HumanoidArm.RIGHT;
         renderItem(player, stack, isRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightHand, poseStack, buffer, combinedLight);

         poseStack.popPose();
      }
   }
}