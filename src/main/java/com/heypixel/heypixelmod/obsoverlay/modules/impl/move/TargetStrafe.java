package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(name = "TargetStrafe", description = "Strafes towards the target", category = Category.MOVEMENT)
public class TargetStrafe extends Module {
    private final TimeHelper timer = new TimeHelper();
    private final BooleanValue jumpKeyOnly = ValueBuilder.create(this, "Jump Key Only").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue range = ValueBuilder.create(this, "Range").setMinFloatValue(0.1f).setMaxFloatValue(4.5f).setDefaultFloatValue(2.0f).setFloatStep(0.1f).build().getFloatValue();
    private final FloatValue switchDelay = ValueBuilder.create(this, "Switch Delay").setMinFloatValue(100).setMaxFloatValue(5000).setDefaultFloatValue(1000).setFloatStep(100).build().getFloatValue();
    private final BooleanValue voidCheck = ValueBuilder.create(this, "Void Check").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue behind = ValueBuilder.create(this, "Behind").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue change = ValueBuilder.create(this, "Direction").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue render = ValueBuilder.create(this, "Render").setDefaultBooleanValue(true).build().getBooleanValue();

    public static int direction = 1;
    public static LivingEntity target;
    private final TimeHelper switchTimer = new TimeHelper();
    private int strafe = -1;

    public static float getRange() {
        TargetStrafe targetStrafe = (TargetStrafe) Naven.getInstance().getModuleManager().getModule(TargetStrafe.class);
        return targetStrafe.range.getCurrentValue();
    }

    public static boolean getJumpKeyOnly() {
        TargetStrafe targetStrafe = (TargetStrafe) Naven.getInstance().getModuleManager().getModule(TargetStrafe.class);
        return targetStrafe.jumpKeyOnly.getCurrentValue();
    }

    public static boolean canStrafe() {
        TargetStrafe targetStrafe = (TargetStrafe) Naven.getInstance().getModuleManager().getModule(TargetStrafe.class);
        boolean press = !targetStrafe.jumpKeyOnly.getCurrentValue() || mc.options.keyJump.isDown();
        return target != null && press && Naven.getInstance().getModuleManager().getModule(Aura.class).isEnabled() &&
                (Naven.getInstance().getModuleManager().getModule("Speed").isEnabled() ||
                        Naven.getInstance().getModuleManager().getModule("Fly").isEnabled());
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (Aura.target == null) {
                target = null;
            } else {
                if (switchTimer.delay(switchDelay.getCurrentValue()) || target == null) {
                    List<LivingEntity> targets = Aura.targets.stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .map(entity -> (LivingEntity) entity)
                            .collect(Collectors.toList());

                    targets.sort((o1, o2) -> {
                        float distance1 = mc.player.distanceTo(o1);
                        float distance2 = mc.player.distanceTo(o2);
                        return Float.compare(distance1, distance2);
                    });

                    if (!targets.isEmpty()) {
                        target = targets.get(0);
                        switchTimer.reset();
                    }
                }
            }

            if (canStrafe() && target != null) {
                handleStrafe();
            }
        }
    }

    private void handleStrafe() {
        AABB boundingBox = mc.player.getBoundingBox();

        boolean isInVoid = MoveUtil.isVecOverVoid(boundingBox.minX, boundingBox.minY, boundingBox.minZ) ||
                MoveUtil.isVecOverVoid(boundingBox.minX, boundingBox.minY, boundingBox.maxZ) ||
                MoveUtil.isVecOverVoid(boundingBox.maxX, boundingBox.minY, boundingBox.minZ) ||
                MoveUtil.isVecOverVoid(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);

        float yaw = RotationUtils.getRotationFromEyeHasPrev(target).getYaw();
        boolean behindTarget = RotationUtils.getRotationDifferences(Mth.wrapDegrees(yaw), Mth.wrapDegrees(target.getYRot())) <= 10.0F;

        if (mc.player.horizontalCollision || (isInVoid && voidCheck.getCurrentValue()) || (behindTarget && behind.getCurrentValue())) {
            strafe *= -1;
        }

        float targetStrafe = (change.getCurrentValue() && mc.player.xxa != 0.0F) ? mc.player.xxa * (float)strafe : (float)strafe;

        if (MoveUtil.isBlockUnder()) {
            targetStrafe = 0.0F;
        }

        double rotAssist = 45.0D / getEnemyDistance(target);
        double moveAssist = (double)(45.0F / getStrafeDistance(target));
        float mathStrafe = 0.0F;

        if (targetStrafe > 0.0F) {
            if ((target.getBoundingBox().minY > mc.player.getBoundingBox().maxY || target.getBoundingBox().maxY < mc.player.getBoundingBox().minY) &&
                    getEnemyDistance(target) < range.getCurrentValue()) {
                yaw = (float)((double)yaw + -rotAssist);
            }
            mathStrafe = (float)((double)mathStrafe + -moveAssist);
        } else if (targetStrafe < 0.0F) {
            if ((target.getBoundingBox().minY > mc.player.getBoundingBox().maxY || target.getBoundingBox().maxY < mc.player.getBoundingBox().minY) &&
                    getEnemyDistance(target) < range.getCurrentValue()) {
                yaw = (float)((double)yaw + rotAssist);
            }
            mathStrafe = (float)((double)mathStrafe + moveAssist);
        }

        double moveSpeed = MoveUtil.getBaseMoveSpeed();
        double[] doSomeMath = new double[]{Math.cos(Math.toRadians((double)(yaw + 90.0F + mathStrafe))), Math.sin(Math.toRadians((double)(yaw + 90.0F + mathStrafe)))};
        double[] asLast = new double[]{moveSpeed * doSomeMath[0], moveSpeed * doSomeMath[1]};

        mc.player.setDeltaMovement(asLast[0], mc.player.getDeltaMovement().y, asLast[1]);
    }

    private double getEnemyDistance(LivingEntity target) {
        return mc.player.distanceTo(target);
    }

    private float getStrafeDistance(LivingEntity target) {
        return (float)Math.max(getEnemyDistance(target) - range.getCurrentValue(),
                getEnemyDistance(target) - (getEnemyDistance(target) - (range.getCurrentValue() / (range.getCurrentValue() * 2.0F))));
    }

    public void esp(LivingEntity entity, float partialTicks, double rad) {
        if (!render.getCurrentValue()) return;

        float points = 90.0F;
        RenderSystem.enableDepthTest();

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(3.5F);

        double x = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
        double y = entity.yOld + (entity.getY() - entity.yOld) * partialTicks;
        double z = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;

        double pix2 = 6.283185307179586D;
        float speed = 5000.0F;

        float baseHue;
        for(baseHue = (float)(System.currentTimeMillis() % (long)((int)speed)); baseHue > speed; baseHue -= speed) {
            ;
        }

        baseHue = baseHue / speed;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for(int i = 0; i <= 90; ++i) {
            float max = (float)i / points;
            float hue;
            for(hue = max + baseHue; hue > 1.0F; --hue) {
                ;
            }

            int color = Color.HSBtoRGB(hue, 0.75F, 1.0F);
            float r = (float)((color >> 16) & 255) / 255.0F;
            float g = (float)((color >> 8) & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;

            double pointX = x + rad * Math.cos((double)i * 6.283185307179586D / (double)points);
            double pointZ = z + rad * Math.sin((double)i * 6.283185307179586D / (double)points);

            buffer.vertex(pointX, y, pointZ).color(r, g, b, 1.0F).endVertex();
        }

        BufferUploader.drawWithShader(buffer.end());

        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}