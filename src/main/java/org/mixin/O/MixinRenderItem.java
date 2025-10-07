package org.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ItemPhysics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class MixinRenderItem {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(ItemEntity itemEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (Naven.getInstance() != null && Naven.getInstance().getModuleManager() != null) {
            ItemPhysics itemPhysics = (ItemPhysics) Naven.getInstance().getModuleManager().getModule(ItemPhysics.class);
            if (itemPhysics != null && itemPhysics.handleEvents()) {
                ci.cancel();
                renderWithPhysics(itemEntity, f, g, poseStack, multiBufferSource, i, itemPhysics);
            }
        }
    }

    private void renderWithPhysics(ItemEntity itemEntity, float partialTicks, float yaw, PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemPhysics physics) {
        ItemStack itemStack = itemEntity.getItem();
        if (itemStack.isEmpty()) return;
        poseStack.pushPose();
        applyCustomPhysics(itemEntity, physics, poseStack);

        if (itemStack.getItem() instanceof BlockItem) {
            RenderType renderType = RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
            MultiBufferSource.BufferSource specialBuffer = Minecraft.getInstance().renderBuffers().bufferSource();

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    itemStack,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    specialBuffer,
                    itemEntity.level(),
                    itemEntity.getId()
            );
            specialBuffer.endBatch(renderType);
        } else {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    itemStack,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    itemEntity.level(),
                    itemEntity.getId()
            );
        }

        poseStack.popPose();
    }

    private void applyCustomPhysics(ItemEntity itemIn, ItemPhysics physics, PoseStack matrixStack) {
        boolean isGui3d = true;
        float weight = physics.getWeight();
        float rotationSpeed = physics.getRotationSpeed();

        if (isGui3d) {
            matrixStack.translate(0, 0, -0.08);
        }

        if (isGui3d) {
            if (itemIn.onGround()) {
                matrixStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                matrixStack.mulPose(Axis.ZP.rotationDegrees(itemIn.getYRot()));
            } else {
                int age = itemIn.getAge();
                float hoverStart = itemIn.bobOffs;
                float rotationYaw = (age / 20.0F + hoverStart) * (180F / (float) Math.PI);
                rotationYaw *= rotationSpeed;

                for (int a = 0; a < 7; ++a) {
                    matrixStack.mulPose(Axis.XP.rotation(weight * rotationYaw * 0.017453292F));
                    matrixStack.mulPose(Axis.YP.rotation(weight * rotationYaw * 0.017453292F));
                    matrixStack.mulPose(Axis.ZP.rotation(1.35f * rotationYaw * 0.017453292F));
                }
            }
        }
    }
}