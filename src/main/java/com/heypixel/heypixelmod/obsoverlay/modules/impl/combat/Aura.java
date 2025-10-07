package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RayTraceUtil;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import static javax.swing.UIManager.getColor;

@ModuleInfo(
        name = "KillAura",
        description = "Automatically attacks entities",
        category = Category.COMBAT
)
public class Aura extends Module {
    private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
    private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
    private long targetChangeTime = 0;
    private long animationStartTime = 0;
    private float animationProgress = 0f;
    private boolean animationRunning = false;
    private boolean animationDirection = true;
    private Entity previousTarget = null;
    public static Entity target;
    public static Entity aimingTarget;
    public static List<Entity> targets = new ArrayList<>();
    public static Vector2f rotation;
    private final List<SouthSideTargetComponent> southSideTargets = new ArrayList<>();
    private final MSTimers targetUpdateTimer = new MSTimers();

    ModeValue rotationMode = ValueBuilder.create(this, "Rotation Mode")
            .setDefaultModeIndex(0)
            .setModes("Normal", "Simple","HvH","New","CNM","NCP", "New2", "New3","Smart")
            .build()
            .getModeValue();

    FloatValue minCps = ValueBuilder.create(this, "MinCPS")
            .setDefaultFloatValue(8.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    FloatValue maxCps = ValueBuilder.create(this, "MaxCPS")
            .setDefaultFloatValue(12.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();

//    FloatValue fakeLagAddCps = ValueBuilder.create(this, "FakeLagAddCPS")
//            .setDefaultFloatValue(5.0F)
//            .setFloatStep(1.0F)
//            .setMinFloatValue(0.0F)
//            .setMaxFloatValue(10.0F)
//            .build()
//            .getFloatValue();

    FloatValue rotationSpeed = ValueBuilder.create(this, "Turn Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(10.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();

    private boolean isAimingComplete = false;
    private long aimStartTime = 0;
    private final long maxAimTime = 0;

    private int currentCps = 0;
    private Random random = new Random();
    private final MSTimer attackTimer = new MSTimer();
    private int cps;

    BooleanValue predictValue = ValueBuilder.create(this, "Predict").setDefaultBooleanValue(true).build().getBooleanValue();

    ModeValue targetHud = ValueBuilder.create(this, "Target HUD")
            .setDefaultModeIndex(1)
            .setModes("Off", "NavenNew", "NavenTow", "Remix", "LSD", "Chill", "Exhibition","Xylitol","Naven", "Moon", "SouthSide","Moonlite")
            .build()
            .getModeValue();
    ModeValue targetEsp = ValueBuilder.create(this, "Target ESP")
            .setDefaultModeIndex(1)
            .setModes("Off", "Box", "Image", "GlowCircle")
            .build()
            .getModeValue();
    private float circleStep = 0;
    BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue fakeAutoblock = ValueBuilder.create(this, "Fake Autoblock").setDefaultBooleanValue(false).build().getBooleanValue();
    public ModeValue blockMode = ValueBuilder.create(this, "AnimationsMode")
            .setDefaultModeIndex(0)
            .setModes("1.7", "1.8", "Push", "New")
            .setVisibility(() -> fakeAutoblock.getCurrentValue())
            .build()
            .getModeValue();
    BooleanValue noWall = ValueBuilder.create(this, "No Wall").setDefaultBooleanValue(true).build().getBooleanValue();

    BooleanValue throughWalls = ValueBuilder.create(this, "Through Walls").setDefaultBooleanValue(false).build().getBooleanValue();
    FloatValue throughWallsRange = ValueBuilder.create(this, "ThroughWalls Range")
            .setDefaultFloatValue(2.5F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .setVisibility(() -> throughWalls.getCurrentValue())
            .build()
            .getFloatValue();

    FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();
    FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> !this.infSwitch.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(360.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();

    BooleanValue circleValue = ValueBuilder.create(this, "Circle").setDefaultBooleanValue(false).build().getBooleanValue();
    FloatValue circleColorRed = ValueBuilder.create(this, "CircleColor Red")
            .setDefaultFloatValue(255.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> circleValue.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue circleColorGreen = ValueBuilder.create(this, "CircleColor Green")
            .setDefaultFloatValue(255.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> circleValue.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue circleColorBlue = ValueBuilder.create(this, "CircleColor Blue")
            .setDefaultFloatValue(255.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> circleValue.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue circleColorAlpha = ValueBuilder.create(this, "CircleColor Alpha")
            .setDefaultFloatValue(255.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> circleValue.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue circleAccuracy = ValueBuilder.create(this, "CircleAccuracy")
            .setDefaultFloatValue(15.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(60.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> circleValue.getCurrentValue())
            .build()
            .getFloatValue();

    RotationUtils.Data lastRotationData;
    RotationUtils.Data rotationData;
    int attackTimes = 0;
    private int index;
    private Vector4f blurMatrix;
    private final DecimalFormat DF_1 = new DecimalFormat("0.0");
    private Vector2f currentRotation;

    private class SimpleAnimation {
        public float getOutput() {
            return animationProgress;
        }
    }
    public class SouthSideTargetComponent {
        public final MSTimers updateTimer = new MSTimers();
        private float y = 0;
        private float targetY = 0;
        private float health = 1;
        private float targetHealth = 1;
        private int blockTicks = 0;
        private long blockMask = 0;
        private boolean updatedGapple = false;
        private int gappleCount = 0;
        private float color = 0;
        private float targetColor = 0;
        private float alpha = 0;
        private float targetAlpha = 0;

        public Player entity;

        public SouthSideTargetComponent(Player entity) {
            this.entity = entity;
            this.updateTimer.reset();
            this.alpha = 0;
            this.targetAlpha = 1;
        }

        public void update() {
            float diff = targetY - y;
            if (Math.abs(diff) > 0.1f) {
                y += diff * 0.2f;
            } else {
                y = targetY;
            }

            float healthDiff = targetHealth - health;
            if (Math.abs(healthDiff) > 0.01f) {
                health += healthDiff * 0.1f;
            } else {
                health = targetHealth;
            }

            float colorDiff = targetColor - color;
            if (Math.abs(colorDiff) > 0.01f) {
                color += colorDiff * 0.1f;
            } else {
                color = targetColor;
            }

            float alphaDiff = targetAlpha - alpha;
            if (Math.abs(alphaDiff) > 0.05f) {
                alpha += alphaDiff * 0.2f;
            } else {
                alpha = targetAlpha;
            }

            boolean isBlocking = entity.isUsingItem() && entity.getUseItem().getItem() == Items.SHIELD;
            int blockStatus = isBlocking ? 1 : 0;
            blockMask = (blockMask << 1) | blockStatus;
            blockTicks += blockStatus;
            if ((blockMask & (1L << 40)) != 0) {
                blockTicks--;
                blockMask ^= (1L << 40);
            }

            if (entity.getMainHandItem().getItem() == Items.GOLDEN_APPLE) {
                gappleCount = entity.getMainHandItem().getCount();
                updatedGapple = true;
            }

            targetHealth = entity.getHealth() / entity.getMaxHealth();

            if (this.entity == Aura.target) {
                targetColor = 1;
                targetAlpha = 1;
            } else {
                targetColor = 0;
                targetAlpha = 0.7f;
            }
        }

        public void draw(PoseStack poseStack, GuiGraphics guiGraphics, float scale, float x, float yOffset) {
            if (alpha < 0.01f) return;

            float currentY = this.y + yOffset;

            int baseAlpha = (int)(120 * alpha);

            String extraMessage;
            if (updatedGapple) {
                extraMessage = "Health: " + DF_1.format(entity.getHealth()) + " Block Rate: " + (blockTicks * 2.5) + "% Gapple: " + gappleCount;
            } else {
                extraMessage = "Health: " + DF_1.format(entity.getHealth()) + " Block Rate: " + (blockTicks * 2.5) + "%";
            }

            RenderUtils.drawRoundedRect(poseStack, x, currentY, 110 * scale, 30 * scale, 2, new Color(0, 0, 0, baseAlpha).getRGB());

            if (color > 0.1f) {
                int whiteAlpha = (int)((120 + 135 * color) * alpha);
                RenderUtils.drawRoundedRect(poseStack, x, currentY + 2.5f * scale, 1 * scale, 25 * scale, 0, new Color(255, 255, 255, whiteAlpha).getRGB());
            } else {
                int whiteAlpha = (int)((120 * color) * alpha);
                RenderUtils.drawRoundedRect(poseStack, x, currentY + 2.5f * scale, 1 * scale, 25 * scale, 0, new Color(255, 255, 255, whiteAlpha).getRGB());
            }

            int textAlpha = (int)(255 * alpha);
            Color textColor = new Color(255, 255, 255, textAlpha);

            Fonts.harmony.render(poseStack, entity.getName().getString(), (double)(x + 30 * scale), (double)(currentY + 11 * scale), textColor, true, 0.35);
            Fonts.harmony.render(poseStack, extraMessage, (double)(x + 30.5f * scale), (double)(currentY + 22 * scale), textColor, true, 0.25);

            int healthBarAlpha = (int)(30 * alpha);
            RenderUtils.drawRoundedRect(poseStack, x, currentY, 110 * scale * health, 30 * scale, 2, new Color(255, 255, 255, healthBarAlpha).getRGB());
        }

        public void drawBigHead(PoseStack poseStack, GuiGraphics guiGraphics, float scale, float x, float yOffset) {
            if (alpha < 0.01f) return;

            float currentY = this.y + yOffset;

            poseStack.pushPose();
            poseStack.translate(x, currentY, 0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entity.getUUID());
            if (playerInfo != null) {
                ResourceLocation skin = playerInfo.getSkinLocation();
                poseStack.pushPose();
                poseStack.translate(5, 8, 0);
                poseStack.scale(scale * 0.7f, scale * 0.7f, scale * 0.7f);
                guiGraphics.blit(skin, 0, 0, 25, 25, 8.0f, 8.0f, 8, 8, 64, 64);
                poseStack.popPose();
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            poseStack.popPose();
        }

        public void show() {
            targetAlpha = this.entity == Aura.target ? 1f : 0.7f;
        }

        public void hide() {
            targetAlpha = 0f;
        }

        public boolean shouldRemove() {
            return alpha < 0.01f && targetAlpha < 0.01f;
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (this.blurMatrix != null && !targetHud.isCurrentMode("Off")) {
            RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }

    public static Entity getTarget() {
        return target;
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrix = null;
        long currentTime = System.currentTimeMillis();
        if (target != this.previousTarget) {
            this.animationStartTime = currentTime;
            this.previousTarget = target;
            this.animationRunning = true;
            this.animationDirection = target != null;
        }
        if (animationRunning) {
            long elapsed = currentTime - animationStartTime;
            long duration = animationDirection ? 850 : 400;

            if (elapsed >= duration) {
                animationProgress = animationDirection ? 1f : 0f;
                animationRunning = false;

                if (!animationDirection) {
                    previousTarget = null;
                }
            } else {
                float progress = (float) elapsed / duration;
                if (animationDirection) {
                    animationProgress = easeOutElastic(progress);
                } else {
                    animationProgress = 1f - easeInBack(progress);
                }
            }
        }
        if (targetHud.isCurrentMode("SouthSide") && targetUpdateTimer.hasTimePassed(50)) {
            updateSouthSideTargets();
            targetUpdateTimer.reset();
        }

        Entity renderTarget = target != null ? target : this.previousTarget;
        if (renderTarget instanceof LivingEntity && animationProgress > 0) {
            LivingEntity living = (LivingEntity) renderTarget;
            PoseStack stack = e.getStack();
            GuiGraphics guiGraphics = e.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int x = screenWidth / 2 + 10;
            int y = screenHeight / 2 + 10;

            SimpleAnimation animation = new SimpleAnimation();

            switch (targetHud.getCurrentMode()) {
                case "NavenNew":
                    drawOldHUD(stack, living, x, y);
                    break;
                case "NavenTow":
                    drawTowHUD(stack, guiGraphics, living, x, y);
                    break;
                case "Remix":
                    drawRemixHUD(stack, guiGraphics, living, animation, x, y);
                    break;
                case "LSD":
                    drawLSDHUD(stack, living, animation, x, y);
                    break;
                case "Chill":
                    drawChillHUD(stack, living, animation, x, y);
                    break;
                case "Exhibition":
                    drawLoratadineHUD(stack, living, animation, x, y);
                    break;
                case "Xylitol":
                    drawXylitolHUD(stack, guiGraphics, living, animation, x, y);
                    break;
                case "Naven":
                    drawNavenHUD(stack, guiGraphics, living, animation, x, y);
                    break;
                case "Moon":
                    drawMoonHUD(stack, guiGraphics, living, animation, x, y);
                    break;
                case "SouthSide":
                    drawSouthSideHUD(stack, guiGraphics, animation, screenWidth, screenHeight);
                    break;
                case "Moonlite" :
                    renderMoonLightV2Style(guiGraphics, living, x, y);
                    break;
            }
        }

        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            PoseStack poseStack = e.getStack();
            GuiGraphics guiGraphics = e.getGuiGraphics();

            switch (targetEsp.getCurrentMode()) {
                case "GlowCircle":
                    drawGlowCircleESP(poseStack, guiGraphics, living, e.getPartialTicks());
                    break;
                case "Image":
                    drawImageESP(poseStack, guiGraphics, living, e.getPartialTicks() , e );
                    break;
            }
        }
    }

    private void updateSouthSideTargets() {
        southSideTargets.removeIf(component ->
                component.updateTimer.hasTimePassed(10000) || component.shouldRemove()
        );

        for (Entity entity : targets) {
            if (entity instanceof Player player) {
                boolean found = false;
                for (SouthSideTargetComponent component : southSideTargets) {
                    if (component.entity == player) {
                        component.update();
                        component.show();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    SouthSideTargetComponent newComponent = new SouthSideTargetComponent(player);
                    newComponent.show();
                    southSideTargets.add(newComponent);
                }
            }
        }

        for (SouthSideTargetComponent component : southSideTargets) {
            if (!targets.contains(component.entity)) {
                component.hide();
            }
        }

        float targetY = 0;
        float scale = 1.0f;
        for (SouthSideTargetComponent component : southSideTargets) {
            if (component.updateTimer.hasTimePassed(1000)) {
                component.targetY = -50 * scale;
            } else {
                component.targetY = targetY;
                targetY += 36 * scale;
            }
        }
    }

    private void drawSouthSideHUD(PoseStack poseStack, GuiGraphics guiGraphics, SimpleAnimation animation, int screenWidth, int screenHeight) {
        float scale = 1.0f;
        int x = screenWidth / 2 + 10;
        int y = screenHeight / 2 + 10;

        for (SouthSideTargetComponent component : southSideTargets) {
            component.draw(poseStack, guiGraphics, scale, x, y);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (SouthSideTargetComponent component : southSideTargets) {
            component.drawBigHead(poseStack, guiGraphics, scale, x, y);
        }
        RenderSystem.disableBlend();
    }

    private void drawMoonHUD(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, SimpleAnimation animation, int x, int y) {
        String targetName = target != null ? target.getName().getString() : "Player";
        float healthPercentage = target != null ? target.getHealth() / target.getMaxHealth() : 0;
        float space = (120.0f - 48.0f) / 100.0f;
        poseStack.pushPose();
        poseStack.translate((x + 120.0f / 2) * (1 - animation.getOutput()), (y + 45.0f / 2) * (1 - animation.getOutput()), 0.0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);
        RenderUtils.drawRoundedRect(poseStack, x, y, 120.0f, 45.0f, 8.0f, new Color(30, 30, 30, 200).getRGB());
        RenderUtils.drawRoundedRect(poseStack, x + 42.0f, y + 26.5f, 100.0f * space, 8.0f, 4.0f, new Color(0, 0, 0, 150).getRGB());
        float healthWidth = 100.0f * space * Math.min(healthPercentage, 1.0f);
        RenderUtils.drawRoundedRectWithColor(poseStack, x + 42.0f, y + 26.5f, healthWidth, 8.0f, 4.0f, new Color(HUD.headerColor));
        String healthText = DF_1.format(target != null ? target.getHealth() : 0);
        RenderUtils.renderText(poseStack, healthText + "HP", (double)(x + 40.0f), (double)(y + 17.0f), Color.WHITE.getRGB(), true, 0.35f);
        RenderUtils.renderText(poseStack, targetName, (double)(x + 40.0f), (double)(y + 6.0f), Color.WHITE.getRGB(), true, 0.4f);
        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                ResourceLocation skin = playerInfo.getSkinLocation();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                RenderUtils.renderPlayer2D(poseStack, guiGraphics, target, x + 2.5f, y + 2.5f, 35.0f, 10.0f, Color.WHITE.getRGB());

                RenderSystem.disableBlend();
            }
        } else {
            RenderUtils.drawRoundedRect(poseStack, x + 2.5f, y + 2.5f, 35.0f, 35.0f, 4.0f, new Color(50, 50, 50, 200).getRGB());
            float centerX = x + 20.0f;
            float centerY = y + 20.0f;
            float textWidth = Fonts.harmony.getWidth("?", 0.4f);
            float textHeight = (float) Fonts.harmony.getHeight(true, 0.4f);
            Fonts.harmony.render(poseStack, "?", (double)(centerX - textWidth / 2), (double)(centerY - textHeight / 2), Color.WHITE, true, 0.4f);
        }

        poseStack.popPose();
    }

    private void drawNavenHUD(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, SimpleAnimation animation, int x, int y) {
        String targetName = target != null ? target.getName().getString() : "Player";
        float nameWidth = (float) Fonts.harmony.getWidth(targetName, 0.4);
        float hudWidth = Math.max(116, nameWidth + 40);
        float hudHeight = 47;

        Color backgroundColor = new Color(33, 33, 33, (int)(200 * animation.getOutput()));
        Color textColor = Color.WHITE;

        poseStack.pushPose();
        poseStack.translate((x + hudWidth / 2) * (1 - animation.getOutput()), (y + hudHeight / 2) * (1 - animation.getOutput()), 0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);

        RenderUtils.drawRoundedRect(poseStack, x, y, hudWidth, hudHeight, 5, backgroundColor.getRGB());

        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                ResourceLocation skin = playerInfo.getSkinLocation();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(skin, (int)(x + 6), (int)(y + 6), 26, 26, 8.0f, 8.0f, 8, 8, 64, 64);
                RenderSystem.disableBlend();
            }
        } else {
            float centerX = x + 20;
            float textHeight = (float) Fonts.harmony.getHeight(true, 0.4);
            float centerY = y + 17 - textHeight / 2f;
            float textWidth = (float) Fonts.harmony.getWidth("?", 0.4);
            Fonts.harmony.render(poseStack, "?", (double)(centerX - textWidth / 2), (double)centerY, textColor, true, 0.4);
        }

        Fonts.harmony.render(poseStack, targetName, (double)(x + 38), (double)(y + 9), textColor, true, 0.35);

        float healthPercent = target != null ? Math.min((target.getHealth() + target.getAbsorptionAmount()) / (target.getMaxHealth() + target.getAbsorptionAmount()), 1.0f) : 0;
        float healthWidth = 104 * healthPercent;

        Color healthBackgroundColor = new Color(0, 0, 0, 117);
        Color healthColor = new Color(200, 42, 42, 200);

        RenderUtils.drawRoundedRect(poseStack, x + 6, y + hudHeight - 8, 104, 3, 0.8f, healthBackgroundColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, x + 6, y + hudHeight - 8, healthWidth, 3, 0.8f, healthColor.getRGB());

        String healthText = "Health: " + (int)(healthPercent * 20) + ".0";
        String distanceText = "Distance: " + (target != null ? Math.round(target.distanceTo(mc.player)) : 0) + ".0";
        String blockRateText = "Block Rate: 0%";

        Fonts.harmony.render(poseStack, healthText, (double)(x + 38), (double)(y + 19), textColor, true, 0.3);
        Fonts.harmony.render(poseStack, blockRateText, (double)(x + 38), (double)(y + 27), textColor, true, 0.3);

        poseStack.popPose();
    }

    private void drawXylitolHUD(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, SimpleAnimation animation, int x, int y) {
        String targetName = target != null ? target.getName().getString() : "Player";
        float nameWidth = Fonts.harmony.getWidth(targetName, 0.4f);
        float hudWidth = Math.max(120, nameWidth + 70);
        float hudHeight = 39.5f;

        double healthPercentage = target != null ? Math.min((target.getHealth() + target.getAbsorptionAmount()) / (target.getMaxHealth() + target.getAbsorptionAmount()), 1.0) : 0;
        Color backgroundColor = new Color(0, 0, 0, (int) (100 * animation.getOutput()));

        poseStack.pushPose();
        poseStack.translate((x + hudWidth / 2) * (1 - animation.getOutput()), (y + hudHeight / 2) * (1 - animation.getOutput()), 0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);

        RenderUtils.drawRoundedRect(poseStack, x, y, hudWidth, hudHeight, 4, backgroundColor.getRGB());

        this.blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);

        float endWidth = (float) Math.max(0, (hudWidth - 44) * healthPercentage);

        RenderUtils.drawRoundedRect(poseStack, x + 32, y + 28, hudWidth - 44, 4, 2, new Color(0, 0, 0, 150).getRGB());

        if (endWidth > 0) {
            Color healthColor = new Color(HUD.headerColor);
            RenderUtils.drawRoundedRect(poseStack, x + 32, y + 28, endWidth, 4, 2, healthColor.getRGB());
        }

        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                ResourceLocation skin = playerInfo.getSkinLocation();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                float scale = 0.8125F;
                poseStack.pushPose();
                poseStack.translate(x + 6, y + 8, 0);
                poseStack.scale(scale, scale, 1);
                int scaledSize = 32;
                guiGraphics.blit(skin, 0, 0, scaledSize, scaledSize, 8.0f, 8.0f, 8, 8, 64, 64);
                guiGraphics.blit(skin, 0, 0, scaledSize, scaledSize, 40.0f, 8.0f, 8, 8, 64, 64);

                poseStack.popPose();
                RenderSystem.disableBlend();
            }
        }

        Color textColor = Color.WHITE;
        Fonts.harmony.render(poseStack, "name: ", (double)(x + 34), (double)(y + 7), textColor, true, 0.35);
        Fonts.harmony.render(poseStack, targetName, (double)(x + 34 + Fonts.harmony.getWidth("name: ", 0.35f)), (double)(y + 7), textColor, true, 0.35);

        String healthValue = target != null ? DF_1.format(target.getHealth()) : "0.0";
        Fonts.harmony.render(poseStack, "health: ", (double)(x + 34), (double)(y + 18), textColor, true, 0.35);
        Fonts.harmony.render(poseStack, healthValue + "hp", (double)(x + 34 + Fonts.harmony.getWidth("health: ", 0.35f)), (double)(y + 18), textColor, true, 0.35);

        poseStack.popPose();
    }

    private void drawNavenHUD2(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, SimpleAnimation animation, int x, int y) {
        String targetName = target != null ? target.getName().getString() : "Player";
        float nameWidth = (float) Fonts.harmony.getWidth(targetName, 0.4);
        float hudWidth = Math.max(116, nameWidth + 40);
        float hudHeight = 47;

        Color backgroundColor = new Color(33, 33, 33, (int)(200 * animation.getOutput()));
        Color textColor = Color.WHITE;

        poseStack.pushPose();
        poseStack.translate((x + hudWidth / 2) * (1 - animation.getOutput()), (y + hudHeight / 2) * (1 - animation.getOutput()), 0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);

        RenderUtils.drawRoundedRect(poseStack, x, y, hudWidth, hudHeight, 5, backgroundColor.getRGB());

        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                ResourceLocation skin = playerInfo.getSkinLocation();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(skin, (int)(x + 6), (int)(y + 6), 26, 26, 8.0f, 8.0f, 8, 8, 64, 64);
                RenderSystem.disableBlend();
            }
        } else {
            float centerX = x + 20;
            float textHeight = (float) Fonts.harmony.getHeight(true, 0.4);
            float centerY = y + 17 - textHeight / 2f;
            float textWidth = (float) Fonts.harmony.getWidth("?", 0.4);
            Fonts.harmony.render(poseStack, "?", (double)(centerX - textWidth / 2), (double)centerY, textColor, true, 0.4);
        }

        Fonts.harmony.render(poseStack, targetName, (double)(x + 38), (double)(y + 9), textColor, true, 0.35);

        float healthPercent = target != null ? Math.min((target.getHealth() + target.getAbsorptionAmount()) / (target.getMaxHealth() + target.getAbsorptionAmount()), 1.0f) : 0;
        float healthWidth = 104 * healthPercent;

        Color healthBackgroundColor = new Color(0, 0, 0, 117);
        Color healthColor = new Color(200, 42, 42, 200);

        RenderUtils.drawRoundedRect(poseStack, x + 6, y + hudHeight - 8, 104, 3, 0.8f, healthBackgroundColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, x + 6, y + hudHeight - 8, healthWidth, 3, 0.8f, healthColor.getRGB());

        String healthText = "Health: " + (int)(healthPercent * 20) + ".0";
        String distanceText = "Distance: " + (target != null ? Math.round(target.distanceTo(mc.player)) : 0) + ".0";
        String blockRateText = "Block Rate: 0%";

        Fonts.harmony.render(poseStack, healthText, (double)(x + 38), (double)(y + 19), textColor, true, 0.3);
        Fonts.harmony.render(poseStack, blockRateText, (double)(x + 38), (double)(y + 27), textColor, true, 0.3);

        poseStack.popPose();
    }

    private void drawGradientHorizontal(PoseStack poseStack, float x, float y, float width, float height, Color leftColor, Color rightColor) {
        if (width <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float rightX = x + width;
        buffer.vertex(poseStack.last().pose(), x, y, 0).color(leftColor.getRed() / 255.0f, leftColor.getGreen() / 255.0f, leftColor.getBlue() / 255.0f, leftColor.getAlpha() / 255.0f).endVertex();
        buffer.vertex(poseStack.last().pose(), rightX, y, 0).color(rightColor.getRed() / 255.0f, rightColor.getGreen() / 255.0f, rightColor.getBlue() / 255.0f, rightColor.getAlpha() / 255.0f).endVertex();
        buffer.vertex(poseStack.last().pose(), rightX, y + height, 0).color(rightColor.getRed() / 255.0f, rightColor.getGreen() / 255.0f, rightColor.getBlue() / 255.0f, rightColor.getAlpha() / 255.0f).endVertex();
        buffer.vertex(poseStack.last().pose(), x, y + height, 0).color(leftColor.getRed() / 255.0f, leftColor.getGreen() / 255.0f, leftColor.getBlue() / 255.0f, leftColor.getAlpha() / 255.0f).endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
    }

    @EventTarget
    public void onRender(EventRender e) {
        if (targetEsp.isCurrentMode("Box")) {
            PoseStack stack = e.getPMatrixStack();
            float partialTicks = e.getRenderPartialTicks();
            stack.pushPose();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderUtils.applyRegionalRenderOffset(stack);

            for (Entity entity : targets) {
                if (entity instanceof LivingEntity living) {
                    float[] color = target == living ? targetColorRed : targetColorGreen;
                    stack.pushPose();
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                    double motionX = entity.getX() - entity.xo;
                    double motionY = entity.getY() - entity.yo;
                    double motionZ = entity.getZ() - entity.zo;
                    AABB boundingBox = entity.getBoundingBox()
                            .move(-motionX, -motionY, -motionZ)
                            .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ);
                    RenderUtils.drawSolidBox(boundingBox, stack);
                    stack.popPose();
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.popPose();
        }

        if (circleValue.getCurrentValue()) {
            drawCircle(e.getPMatrixStack(), e.getRenderPartialTicks());
        }
    }

    private void drawCircle(PoseStack poseStack, float partialTicks) {
        if (mc.player == null) return;

        double x = mc.player.xOld + (mc.player.getX() - mc.player.xOld) * partialTicks;
        double y = mc.player.yOld + (mc.player.getY() - mc.player.yOld) * partialTicks;
        double z = mc.player.zOld + (mc.player.getZ() - mc.player.zOld) * partialTicks;

        poseStack.pushPose();
        poseStack.translate(-mc.getEntityRenderDispatcher().camera.getPosition().x,
                -mc.getEntityRenderDispatcher().camera.getPosition().y,
                -mc.getEntityRenderDispatcher().camera.getPosition().z);
        poseStack.translate(x, y, z);

        RenderSystem.enableBlend();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.lineWidth(1.0F);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        int color = new Color(
                (int) circleColorRed.getCurrentValue(),
                (int) circleColorGreen.getCurrentValue(),
                (int) circleColorBlue.getCurrentValue(),
                (int) circleColorAlpha.getCurrentValue()
        ).getRGB();

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        int accuracy = 61 - (int) circleAccuracy.getCurrentValue();
        for (int i = 0; i <= 360; i += accuracy) {
            double cos = Math.cos(i * Math.PI / 180.0) * aimRange.getCurrentValue();
            double sin = Math.sin(i * Math.PI / 180.0) * aimRange.getCurrentValue();

            bufferBuilder.vertex(poseStack.last().pose(), (float) cos, (float) sin, 0.0F)
                    .color(red, green, blue, alpha)
                    .endVertex();
        }

        double cos = Math.cos(360 * Math.PI / 180.0) * aimRange.getCurrentValue();
        double sin = Math.sin(360 * Math.PI / 180.0) * aimRange.getCurrentValue();
        bufferBuilder.vertex(poseStack.last().pose(), (float) cos, (float) sin, 0.0F)
                .color(red, green, blue, alpha)
                .endVertex();

        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        poseStack.popPose();
    }

    private void drawOldHUD(PoseStack stack, LivingEntity living, int x, int y) {
        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
        this.blurMatrix = new Vector4f(x, y, width, 30.0F);
        stack.pushPose();
        float centerX = x + width / 2;
        float centerY = y + 15;
        stack.translate(centerX, centerY, 0);
        stack.scale(animationProgress, animationProgress, 1);
        stack.translate(-centerX, -centerY, 0);
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(stack, x, y, width, 30.0F, 5.0F, HUD.headerColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(stack, x, y, width, 30.0F, HUD.bodyColor);
        RenderUtils.fillBound(stack, x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, HUD.headerColor);
        StencilUtils.dispose();

        Fonts.harmony.render(stack, targetName, (double) (x + 5.0F), (double) (y + 6.0F), Color.WHITE, true, 0.35F);
        Fonts.harmony.render(
                stack,
                "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
                (double) (x + 5.0F),
                (double) (y + 17.0F),
                Color.WHITE,
                true,
                0.35F
        );
        stack.popPose();
    }

    private void drawTowHUD(PoseStack stack, GuiGraphics guiGraphics, LivingEntity living, int x, int y) {
        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float nameWidth = Fonts.harmony.getWidth(targetName, 0.4f);
        float hpWidth = Fonts.harmony.getWidth("HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0f ? "+" + Math.round(living.getAbsorptionAmount()) : ""), 0.35f);
        float textWidth = Math.max(nameWidth, hpWidth);
        boolean isPlayer = living instanceof Player;
        float avatarWidth = isPlayer ? 30.0f : 0.0f;
        float hudWidth = Math.max(textWidth + 15.0f, 80.0f) + avatarWidth;
        float hudHeight = 38.0f;
        this.blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(stack, x, y, hudWidth, hudHeight, 5.0f, HUD.headerColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(stack, x, y, hudWidth, hudHeight, HUD.bodyColor);
        RenderUtils.fillBound(stack, x, y, hudWidth * (living.getHealth() / living.getMaxHealth()), 3.0f, HUD.headerColor);
        StencilUtils.dispose();
        if (isPlayer) {
            Player player = (Player) living;
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                ResourceLocation skin = playerInfo.getSkinLocation();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int avatarX = (int) x + 5;
                int avatarY = (int) y + 8;
                guiGraphics.blit(skin, avatarX, avatarY, 25, 25, 8.0f, 8.0f, 8, 8, 64, 64);
                guiGraphics.blit(skin, avatarX, avatarY, 25, 25, 40.0f, 8.0f, 8, 8, 64, 64);
                RenderSystem.disableBlend();
            }
        }
        float textX = x + avatarWidth + 8.0f;
        float nameY = y + 8.0f;
        float hpY = y + 20.0f;
        Fonts.harmony.render(stack, targetName, (double) textX, (double) nameY, Color.WHITE, true, 0.4f);
        Fonts.harmony.render(stack, "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0f ? "+" + Math.round(living.getAbsorptionAmount()) : ""), (double) textX, (double) hpY, Color.WHITE, true, 0.35f);
    }

    private void drawRemixHUD(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, SimpleAnimation animation, int x, int y) {
        int health = target != null ? (int) target.getHealth() : 0;
        float healthPresent = target != null ? health / target.getMaxHealth() : 0;
        int armor = target != null ? target.getArmorValue() : 0;
        float armorPresent = target != null ? (float) armor / 20 : 0;
        final String name = target != null ? target.getName().getString() : "Player";
        int healthWidth = target != null ? (int) Math.max(130, Fonts.harmony.getWidth(target.getName().getString(), 0.4f) + 50) : 130;
        int armorWidth = target != null ? (int) Math.max(95.5F, Fonts.harmony.getWidth(target.getName().getString(), 0.4f) + 50) : (int) 95.5F;
        float presentWidth_health = Math.min(healthPresent, 1) * healthWidth;
        float presentWidth_armor = Math.min(armorPresent, 1) * armorWidth;
        Color bgColor = new Color(30, 30, 30);
        Color healthBgColor = new Color(80, 0, 0);
        Color healthColor = new Color(0, 165, 0);
        Color armorBgColor = new Color(0, 0, 80);
        Color armorColor = new Color(0, 0, 165);
        Color rectBgColor = new Color(70, 70, 70);
        Color rectOutlineColor = new Color(0, 0, 0);
        Color textColor = new Color(200, 200, 200);
        poseStack.pushPose();
        poseStack.translate(((float) x + 70) * (1 - animation.getOutput()), ((float) y + 25) * (1 - animation.getOutput()), 0.0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);
        RenderUtils.drawRoundedRect(poseStack, (float) x, (float) y, 140, 50, 0, bgColor.getRGB());
        try {
            if (target != null && target instanceof Player) {
                Player player = (Player) target;
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
                if (playerInfo != null) {
                    ResourceLocation skin = playerInfo.getSkinLocation();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    guiGraphics.blit(skin, (int) x + 5, (int) y + 5, 32, 32, 8.0f, 8.0f, 8, 8, 64, 64);
                    RenderSystem.disableBlend();
                }
            } else {
                RenderUtils.drawRoundedRect(poseStack, (float) x + 5, (float) y + 5, 32, 32, 0, rectBgColor.getRGB());
            }
        } catch (Exception e) {
            RenderUtils.drawRoundedRect(poseStack, (float) x + 5, (float) y + 5, 32, 32, 0, rectBgColor.getRGB());
        }

        RenderUtils.drawRoundedRect(poseStack, (float) x + 5, (float) y + 40, 130, 5, 0, healthBgColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 5, (float) y + 40, presentWidth_health, 5, 0, healthColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 39, (float) y + 35.5F, 95.5F, 1, 0, armorBgColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 39, (float) y + 35.5F, presentWidth_armor, 1, 0, armorColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 39, (float) y + 16, 18, 18, 0, rectOutlineColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 40, (float) y + 17, 16, 16, 0, rectBgColor.getRGB());

        RenderUtils.drawRoundedRect(poseStack, (float) x + 59, (float) y + 16, 18, 18, 0, rectOutlineColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 60, (float) y + 17, 16, 16, 0, rectBgColor.getRGB());

        RenderUtils.drawRoundedRect(poseStack, (float) x + 79, (float) y + 16, 18, 18, 0, rectOutlineColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 80, (float) y + 17, 16, 16, 0, rectBgColor.getRGB());

        RenderUtils.drawRoundedRect(poseStack, (float) x + 99, (float) y + 16, 18, 18, 0, rectOutlineColor.getRGB());
        RenderUtils.drawRoundedRect(poseStack, (float) x + 100, (float) y + 17, 16, 16, 0, rectBgColor.getRGB());
        Fonts.harmony.render(poseStack, name, (double) (x + 40), (double) (y + 4), textColor, true, 0.4f);

        if (target instanceof Player) {
            Player player = (Player) target;
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info != null) {
                int ping = info.getLatency();
                String pingText = ping + "ms";
                Fonts.harmony.render(poseStack, pingText, (double) (x + 135 - Fonts.harmony.getWidth(pingText, 0.35f)), (double) (y + 28), textColor, true, 0.35f);
                Fonts.harmony.render(poseStack, "P", (double) (x + 128), (double) (y + 22), getPingColor(ping), true, 0.35f);
            }
        }

        poseStack.popPose();
    }

    private void drawLSDHUD(PoseStack poseStack, LivingEntity target, SimpleAnimation animation, int x, int y) {
        int health = target != null ? (int) target.getHealth() : 0;
        float maxHealth = target != null ? target.getMaxHealth() : 20;

        int armor = target != null ? target.getArmorValue() : 5;

        final String name = target != null ? target.getName().getString() : "Player";

        int width = target != null ? (int) Math.max(140, Fonts.harmony.getWidth(target.getName().getString(), 0.4f) + 50) : 140;
        float barWidth = width - 60;

        poseStack.pushPose();
        poseStack.translate((x + (double) width / 2) * (1 - animation.getOutput()), (y + 20) * (1 - animation.getOutput()), 0.0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);

        RenderUtils.drawRoundedRect(poseStack, x, y, width, 40, 2, new Color(31, 30, 29).getRGB());

        try {
            if (target != null && target instanceof Player) {
                Player player = (Player) target;
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
                if (playerInfo != null) {
                    ResourceLocation skin = playerInfo.getSkinLocation();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderUtils.drawRoundedRect(poseStack, x + 6, y + 6, 28, 28, 6, Color.BLACK.getRGB());
                }
            } else {
                RenderUtils.drawRoundedRect(poseStack, x + 6, y + 6, 28, 28, 6, Color.BLACK.getRGB());
            }
        } catch (Exception e) {
            RenderUtils.drawRoundedRect(poseStack, x + 6, y + 6, 28, 28, 6, Color.BLACK.getRGB());
        }

        Fonts.harmony.render(poseStack, name, (double) (x + 41), (double) (y + 4), new Color(200, 200, 200, 255), true, 0.4f);

        Fonts.harmony.render(poseStack, "H", (double) (x + 41), (double) (y + 17), new Color(HUD.headerColor), true, 0.35f);

        Fonts.harmony.render(poseStack, "A", (double) (x + 41), (double) (y + 28.5F), Color.WHITE, true, 0.35f);

        Color healthBgColor = new Color(70, 70, 70);
        Color healthColor = new Color(HUD.headerColor);

        drawSmoothArmorBar(
                poseStack,
                x + 50,
                y + 18.5F,
                barWidth,
                3,
                health,
                maxHealth,
                healthBgColor,
                healthColor
        );

        drawSmoothArmorBar(
                poseStack,
                x + 50,
                y + 30,
                barWidth,
                3,
                armor,
                20,
                new Color(70, 70, 70),
                Color.WHITE
        );

        poseStack.popPose();
    }

    private void drawChillHUD(PoseStack poseStack, LivingEntity target, SimpleAnimation animation, int x, int y) {

        final String name = target != null ? target.getName().getString() : "Player";

        float health = target != null ? target.getHealth() : 0;
        float maxHealth = target != null ? target.getMaxHealth() : 20;

        float tWidth = Math.max(45F + Math.max(Fonts.harmony.getWidth(name, 0.4f), Fonts.harmony.getWidth(String.format("%.2f", health), 0.4f)), 120F);

        poseStack.pushPose();
        poseStack.translate((x + (double) tWidth / 2) * (1 - animation.getOutput()), (y + 20) * (1 - animation.getOutput()), 0.0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);

        RenderUtils.drawRoundedRect(poseStack, x, y - 1, tWidth, 1, 0, HUD.headerColor);
        RenderUtils.drawRoundedRect(poseStack, x, y, tWidth, 48F, 0, new Color(23, 23, 23).getRGB());

        if (target != null && target instanceof Player) {
            try {
                Player player = (Player) target;
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
                if (playerInfo != null) {
                    ResourceLocation skin = playerInfo.getSkinLocation();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderUtils.drawRoundedRect(poseStack, x + 6, y + 6, 30, 30, 0, Color.BLACK.getRGB());
                }
            } catch (Exception e) {
                RenderUtils.drawRoundedRect(poseStack, x + 6, y + 6, 30, 30, 0, Color.BLACK.getRGB());
            }
        } else {
            RenderUtils.drawRoundedRect(poseStack, x + 6, y + 6, 30, 30, 0, Color.BLACK.getRGB());
        }

        Fonts.harmony.render(poseStack, name, (double) (x + 38F), (double) (y + 5F), new Color(200, 200, 200, 255), true, 0.4f);
        Fonts.harmony.render(poseStack, String.format("%.1f", health), (double) (x + 38F), (double) (y + 17F), new Color(200, 200, 200, 255), true, 0.35f);

        RenderUtils.drawRoundedRect(poseStack, x + 4, y + 38, tWidth - 8, 6, 0, new Color(0, 0, 0, 100).getRGB());

        Color barColor;
        float healthPercent = health / maxHealth;

        if (healthPercent > 0.66f) {
            barColor = new Color(30, 220, 30);
        } else if (healthPercent > 0.33f) {
            barColor = new Color(220, 220, 30);
        } else {
            barColor = new Color(220, 30, 30);
        }

        RenderUtils.drawRoundedRect(poseStack, x + 4, y + 38, (health / maxHealth) * (tWidth - 8), 6, 0, barColor.getRGB());

        poseStack.popPose();
    }

    private void drawLoratadineHUD(PoseStack poseStack, LivingEntity target, SimpleAnimation animation, int x, int y) {
        Color darkest = new Color(0, 0, 0);
        Color lineColor = new Color(104, 104, 104);
        Color dark = new Color(70, 70, 70);

        poseStack.pushPose();
        poseStack.translate((x + 70) * (1 - animation.getOutput()), (y + 25) * (1 - animation.getOutput()), 0.0);
        poseStack.scale((float) animation.getOutput(), (float) animation.getOutput(), 0);

        RenderUtils.drawRectangle(poseStack, x, y, 140, 50, darkest.getRGB());
        RenderUtils.drawRectangle(poseStack, x + 0.5F, y + 0.5F, 139, 49, lineColor.getRGB());
        RenderUtils.drawRectangle(poseStack, x + 1.5F, y + 1.5F, 137, 47, darkest.getRGB());
        RenderUtils.drawRectangle(poseStack, x + 2, y + 2, 136, 46, dark.getRGB());

        String targetName = target != null ? target.getName().getString() : "Player";
        RenderUtils.draw(poseStack, targetName, x + 40, y + 6, Color.WHITE.getRGB());

        int healthScore = target != null ? (int) target.getHealth() : 0;

        String name = "HP: " + healthScore + " | Dist: " + (target != null ? Math.round(RotationUtils.getDistanceToEntity(target)) : 0);
        poseStack.pushPose();
        poseStack.scale(0.7F, 0.7F, 0.7F);
        RenderUtils.draw(poseStack, name, (x + 40F) * (1F / 0.7F), (y + 17F) * (1F / 0.7F), Color.WHITE.getRGB());
        poseStack.popPose();

        double health = Math.min(healthScore, target != null ? target.getMaxHealth() : 20);
        int healthColor = target != null ? getColor(target).getRGB() : new Color(120, 0, 0).getRGB();

        float x2 = x + 40F;
        RenderUtils.drawRectangle(poseStack, x2, y + 25, (float) ((100 - 9) * (health / (target != null ? target.getMaxHealth() : 20))), 6, healthColor);
        RenderUtils.drawRectangle(poseStack, x2, y + 25, 91, 1, darkest.getRGB());
        RenderUtils.drawRectangle(poseStack, x2, y + 30, 91, 1, darkest.getRGB());

        for (int i = 0; i < 10; i++) {
            RenderUtils.drawRectangle(poseStack, x2 + 10 * i, y + 25, 1, 6, darkest.getRGB());
        }

        if (target != null) {
            RenderUtils.renderItemIcon(poseStack, x2, y + 31, target.getMainHandItem());
            RenderUtils.renderItemIcon(poseStack, x2 + 15, y + 31, target.getItemBySlot(EquipmentSlot.HEAD));
            RenderUtils.renderItemIcon(poseStack, x2 + 30, y + 31, target.getItemBySlot(EquipmentSlot.CHEST));
            RenderUtils.renderItemIcon(poseStack, x2 + 45, y + 31, target.getItemBySlot(EquipmentSlot.LEGS));
            RenderUtils.renderItemIcon(poseStack, x2 + 60, y + 31, target.getItemBySlot(EquipmentSlot.FEET));
        }

        if (target != null) {
            poseStack.pushPose();
            poseStack.scale(0.4F, 0.4F, 0.4F);
            poseStack.translate((x + 20) * (1 / 0.4), (y + 44) * (1 / 0.4), 40f * (1 / 0.4));
            RenderUtils.drawModel(poseStack, target.getYRot(), target.getXRot(), target);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void drawRiseHUD(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, SimpleAnimation animation, int x, int y) {
        if (!(target instanceof Player)) return;

        Player player = (Player) target;
        String name = player.getName().getString();

        float health = Math.min(player.getHealth(), player.getMaxHealth());
        float healthPercent = health / player.getMaxHealth();

        int EDGE_OFFSET = 8;
        int PADDING = 7;
        int INDENT = 4;
        int faceScale = 32;

        float nameWidth = Fonts.harmony.getWidth(name, 0.4f);
        float healthTextWidth = Fonts.harmony.getWidth(String.valueOf(Math.round(health)), 0.35f);
        float healthBarWidth = Math.max(nameWidth + 35 - healthTextWidth, 65);

        float width = EDGE_OFFSET + faceScale + EDGE_OFFSET + healthBarWidth + INDENT + healthTextWidth + EDGE_OFFSET;
        float height = faceScale + EDGE_OFFSET * 2;

        float scale = animation.getOutput();

        poseStack.pushPose();
        poseStack.translate((x + width / 2) * (1 - scale), (y + height / 2) * (1 - scale), 0);
        poseStack.scale(scale, scale, 1);

        Color backgroundColor = new Color(0, 0, 0, 180);
        Color accentColor = new Color(HUD.headerColor);
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(poseStack, (float)x, (float)y, width, height, 8, backgroundColor.getRGB());
        StencilUtils.erase(true);
        RenderUtils.drawRect(poseStack, (float)x, (float)y, width, 1.5f, accentColor.getRGB());
        RenderUtils.drawRect(poseStack, (float)x, (float)(y + height - 1.5f), width, 1.5f, accentColor.getRGB());
        RenderUtils.drawRect(poseStack, (float)x, (float)y, 1.5f, height, accentColor.getRGB());
        RenderUtils.drawRect(poseStack, (float)(x + width - 1.5f), (float)y, 1.5f, height, accentColor.getRGB());

        StencilUtils.dispose();

        Fonts.harmony.render(poseStack, "Name", (double) (x + EDGE_OFFSET + faceScale + PADDING), (double) (y + EDGE_OFFSET + INDENT + 2), Color.WHITE, true, 0.35f);
        Fonts.harmony.render(poseStack, name, (double) (x + EDGE_OFFSET + faceScale + PADDING + Fonts.harmony.getWidth("Name", 0.35f) + 3), (double) (y + EDGE_OFFSET + INDENT + 2.5), accentColor, true, 0.4f);

        float hurtTime = player.hurtTime > 0 ? player.hurtTime - mc.getFrameTime() : 0;

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(poseStack, (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET), (float)faceScale, (float)faceScale, 6, Color.BLACK.getRGB());
        StencilUtils.erase(true);

        PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
        if (playerInfo != null) {
            ResourceLocation skin = playerInfo.getSkinLocation();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            guiGraphics.blit(
                    skin,
                    (int)(x + EDGE_OFFSET),
                    (int)(y + EDGE_OFFSET),
                    faceScale,
                    faceScale,
                    8.0f, 8.0f, 8, 8, 64, 64
            );
            guiGraphics.blit(
                    skin,
                    (int)(x + EDGE_OFFSET),
                    (int)(y + EDGE_OFFSET),
                    faceScale,
                    faceScale,
                    40.0f, 8.0f, 8, 8, 64, 64
            );

            RenderSystem.disableBlend();
        }

        StencilUtils.dispose();

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(poseStack, (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET), (float)faceScale, (float)faceScale, 6, Color.BLACK.getRGB());
        StencilUtils.erase(true);

        RenderUtils.drawRect(poseStack, (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET), (float)faceScale, 1.0f, accentColor.getRGB());
        RenderUtils.drawRect(poseStack, (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET + faceScale - 1.0f), (float)faceScale, 1.0f, accentColor.getRGB());
        RenderUtils.drawRect(poseStack, (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET), 1.0f, (float)faceScale, accentColor.getRGB());
        RenderUtils.drawRect(poseStack, (float)(x + EDGE_OFFSET + faceScale - 1.0f), (float)(y + EDGE_OFFSET), 1.0f, (float)faceScale, accentColor.getRGB());

        StencilUtils.dispose();

        RenderUtils.drawRoundedRect(poseStack,
                (float)(x + EDGE_OFFSET + faceScale + PADDING),
                (float)(y + EDGE_OFFSET + faceScale - INDENT - 7),
                healthBarWidth, 6, 3,
                new Color(40, 40, 40).getRGB()
        );

        float healthRemainingWidth = healthPercent * healthBarWidth;
        RenderUtils.drawRoundedRect(poseStack,
                (float)(x + EDGE_OFFSET + faceScale + PADDING),
                (float)(y + EDGE_OFFSET + faceScale - INDENT - 7),
                healthRemainingWidth, 6, 3,
                accentColor.getRGB()
        );

        Fonts.harmony.render(poseStack,
                String.valueOf(Math.round(health)),
                (double) (x + EDGE_OFFSET + faceScale + PADDING + healthBarWidth + INDENT),
                (double) (y + EDGE_OFFSET + faceScale - INDENT - 8),
                accentColor, true, 0.35f
        );

        if (hurtTime > 0) {
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(poseStack, (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET), (float)faceScale, (float)faceScale, 6, Color.BLACK.getRGB());
            StencilUtils.erase(true);

            float alpha = Math.min(hurtTime / 10f * 0.4f, 0.4f);
            RenderUtils.drawRoundedRect(poseStack,
                    (float)(x + EDGE_OFFSET), (float)(y + EDGE_OFFSET),
                    (float)faceScale, (float)faceScale, 6,
                    new Color(1.0f, 0.0f, 0.0f, alpha).getRGB()
            );

            StencilUtils.dispose();
        }

        poseStack.popPose();
    }
    private static Vector4f renderMoonLightV2Style(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float mlHudWidth = 150.0F;
        float mlHudHeight = 35.0F;
        Vector4f blurMatrix = new Vector4f(x, y, mlHudWidth, mlHudHeight);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, mlHudWidth, mlHudHeight, 4.0F, 0x80000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, mlHudWidth, mlHudHeight, 0x80000000);
        StencilUtils.dispose();

        String mlTargetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float mlNameX = x + 8.0F;
        float mlNameY = y + 8.0F;
        Fonts.harmony.render(graphics.pose(), mlTargetName, (double) mlNameX, (double) mlNameY, Color.WHITE, true, 0.30F);

        String mlHealthText = Math.round(living.getHealth()) + "/" + Math.round(living.getMaxHealth());
        float mlHealthTextX = x + 8.0F;
        float mlHealthTextY = y + 20.0F;
        Fonts.harmony.render(graphics.pose(), mlHealthText, (double) mlHealthTextX, (double) mlHealthTextY, Color.WHITE, true, 0.30F);

        float mlCircleX = x + mlHudWidth - 20.0F;
        float mlCircleY = y + mlHudHeight / 2.0F;
        float mlCircleRadius = 10.0F;
        float mlHealthPercent = Math.min(1.0f, Math.max(0.0f, living.getHealth() / living.getMaxHealth()));

        RenderUtils.drawHealthRing(
                graphics.pose(),
                mlCircleX,
                mlCircleY,
                mlCircleRadius,
                2.5F,
                mlHealthPercent
        );

        return blurMatrix;
    }

    private void drawGlowCircleESP(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, float partialTicks) {
        Vec3 targetPos = target.getPosition(partialTicks);
        Vector2f screenPos = ProjectionUtils.project(targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z, partialTicks);

        if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
            float size = 80.0f;
            float imageX = screenPos.x - size/2;
            float imageY = screenPos.y - size/2;

            ResourceLocation glowCircle = new ResourceLocation("shaoyu:esp/glow_circle.png");
            RenderSystem.setShaderTexture(0, glowCircle);

            long time = System.currentTimeMillis();
            float r = (float)(Math.sin(time * 0.001) * 0.3 + 0.7);
            float g = (float)(Math.sin(time * 0.001 + 2.0) * 0.3 + 0.7);
            float b = (float)(Math.sin(time * 0.001 + 4.0) * 0.3 + 0.7);
            float a = 0.8f;

            RenderSystem.setShaderColor(r, g, b, a);
            guiGraphics.blit(glowCircle, (int)imageX, (int)imageY, 0, 0, (int)size, (int)size, (int)size, (int)size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawQuadStappleESP(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, float partialTicks) {
        Vec3 targetPos = target.getPosition(partialTicks);
        Vector2f screenPos = ProjectionUtils.project(targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z, partialTicks);

        if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
            float size = 64.0f;
            float imageX = screenPos.x - size/2;
            float imageY = screenPos.y - size/2;

            ResourceLocation quadStapple = new ResourceLocation("shaoyu:esp/quadstapple.png");
            RenderSystem.setShaderTexture(0, quadStapple);

            long time = System.currentTimeMillis();
            float rotation = (time % 10000) * 0.036f;

            poseStack.pushPose();
            poseStack.translate(imageX + size/2, imageY + size/2, 0);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
            poseStack.translate(-(imageX + size/2), -(imageY + size/2), 0);

            RenderSystem.setShaderColor(1.0F, 0.5F, 0.2F, 0.7F);
            guiGraphics.blit(quadStapple, (int)imageX, (int)imageY, 0, 0, (int)size, (int)size, (int)size, (int)size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            poseStack.popPose();
        }
    }

    private void drawTriangleStappleESP(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, float partialTicks) {
        Vec3 targetPos = target.getPosition(partialTicks);
        Vector2f screenPos = ProjectionUtils.project(targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z, partialTicks);

        if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
            float size = 72.0f;
            float imageX = screenPos.x - size/2;
            float imageY = screenPos.y - size/2;

            ResourceLocation triangleStapple = new ResourceLocation("shaoyu:esp/trianglestapple.png");
            RenderSystem.setShaderTexture(0, triangleStapple);

            long time = System.currentTimeMillis();
            float rotation = -(time % 8000) * 0.045f;

            poseStack.pushPose();
            poseStack.translate(imageX + size/2, imageY + size/2, 0);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
            poseStack.translate(-(imageX + size/2), -(imageY + size/2), 0);

            RenderSystem.setShaderColor(0.2F, 0.8F, 0.3F, 0.75F);
            guiGraphics.blit(triangleStapple, (int)imageX, (int)imageY, 0, 0, (int)size, (int)size, (int)size, (int)size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            poseStack.popPose();
        }
    }

    private void drawTriangleStippleESP(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, float partialTicks) {
        Vec3 targetPos = target.getPosition(partialTicks);
        Vector2f screenPos = ProjectionUtils.project(targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z, partialTicks);

        if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
            float size = 68.0f;
            float imageX = screenPos.x - size/2;
            float imageY = screenPos.y - size/2;

            ResourceLocation triangleStipple = new ResourceLocation("shaoyu:esp/trianglestipple.png");
            RenderSystem.setShaderTexture(0, triangleStipple);

            long time = System.currentTimeMillis();
            float rotation = (time % 12000) * 0.03f;

            poseStack.pushPose();
            poseStack.translate(imageX + size/2, imageY + size/2, 0);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
            poseStack.translate(-(imageX + size/2), -(imageY + size/2), 0);

            RenderSystem.setShaderColor(0.8F, 0.3F, 0.8F, 0.65F);
            guiGraphics.blit(triangleStipple, (int)imageX, (int)imageY, 0, 0, (int)size, (int)size, (int)size, (int)size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            poseStack.popPose();
        }
    }

    private void drawImageESP(PoseStack poseStack, GuiGraphics guiGraphics, LivingEntity target, float partialTicks , EventRender2D e) {
        LivingEntity living = (LivingEntity) target;

        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        Vec3 eyePos = mc.player.getEyePosition(partialTicks);

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                Vec3 targetPos = entity.getPosition(partialTicks);
                Vector2f screenPos = ProjectionUtils.project(targetPos.x, targetPos.y + entity.getBbHeight() / 2, targetPos.z, partialTicks);

                if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
                    double distance = eyePos.distanceTo(targetPos);

                    float baseDistance = 5.0f;
                    float baseSize = 80.0f;
                    float minSize = 20.0f;
                    float maxSize = 120.0f;

                    float size = baseSize * (float)(baseDistance / Math.max(1.0, distance));

                    size = Math.max(minSize, Math.min(maxSize, size));

                    float imageX = screenPos.x - size/2;
                    float imageY = screenPos.y - size/2;
                    float rotationAngle = (System.currentTimeMillis() % 10000) * 0.036f;

                    long time = System.currentTimeMillis();
                    float r = (float)(Math.sin(time * 0.001) * 0.3 + 0.7);
                    float g = (float)(Math.sin(time * 0.001 + 2.0) * 0.3 + 0.7);
                    float b = (float)(Math.sin(time * 0.001 + 4.0) * 0.3 + 0.7);
                    float a = 0.7f;

                    RenderSystem.setShaderColor(r, g, b, a);

                    ResourceLocation renderImage = new ResourceLocation("shaoyu:esp/rectangle.png");
                    RenderSystem.setShaderTexture(0, renderImage);

                    poseStack.pushPose();
                    poseStack.translate(imageX + size/2, imageY + size/2, 0);
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle));
                    poseStack.translate(-(imageX + size/2), -(imageY + size/2), 0);

                    e.getGuiGraphics().blit(
                            renderImage,
                            (int)imageX,
                            (int)imageY,
                            0,
                            0,
                            (int)size,
                            (int)size,
                            (int)size,
                            (int)size
                    );

                    poseStack.popPose();
                }
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }


    @Override
    public void onEnable() {
        rotation = null;
        this.index = 0;
        target = null;
        aimingTarget = null;
        targets.clear();
        this.currentCps = (int) this.minCps.getCurrentValue();
        this.isAimingComplete = false;
        this.currentRotation = null;
    }

    @Override
    public void onDisable() {
        target = null;
        aimingTarget = null;
        super.onDisable();
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        target = null;
        aimingTarget = null;
        this.toggle();
    }

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        e.setCancelled(true);
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        if (event.getType() == EventType.PRE && mc.player != null) {
            if (mc.screen instanceof AbstractContainerScreen
                    || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                    || InventoryUtils.shouldDisableFeatures()) {
                target = null;
                aimingTarget = null;
                this.rotationData = null;
                rotation = null;
                this.lastRotationData = null;
                targets.clear();
                this.isAimingComplete = false;
                return;
            }

            this.cps = random.nextInt((int)(maxCps.getCurrentValue() - minCps.getCurrentValue() + 1)) + (int)minCps.getCurrentValue();
            /*
            if (Naven.getInstance().getModuleManager().getModule(FakeLag.class).isEnabled() ||
                Naven.getInstance().getModuleManager().getModule(Timer.class).isEnabled()) {
                this.cps += (int) fakeLagAddCps.getCurrentValue();
            }
            */

            boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
            this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
            this.updateAttackTargets();
            aimingTarget = this.shouldPreAim();
            this.lastRotationData = this.rotationData;
            this.rotationData = null;

            if (aimingTarget == null) {
                this.isAimingComplete = false;
                rotation = null;
                this.currentRotation = null;
            } else {
                String currentMode = rotationMode.getCurrentMode();

                if ("Normal".equals(currentMode)) {
                    this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
                    if (this.rotationData.getRotation() != null) {
                        if (this.currentRotation == null) {
                            this.currentRotation = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
                            this.aimStartTime = System.currentTimeMillis();
                            this.isAimingComplete = false;
                        }
                        Vector2f targetRotation = this.rotationData.getRotation();
                        float speed = rotationSpeed.getCurrentValue();
                        float maxDelta = speed * 0.15F;
                        float newYaw = updateRotation(this.currentRotation.x, targetRotation.x, maxDelta);
                        float newPitch = updateRotation(this.currentRotation.y, targetRotation.y, maxDelta);
                        this.currentRotation = new Vector2f(newYaw, newPitch);
                        rotation = this.currentRotation;
                        float yawDiff = Math.abs(wrapDegrees(targetRotation.x - this.currentRotation.x));
                        float pitchDiff = Math.abs(wrapDegrees(targetRotation.y - this.currentRotation.y));
                        long currentTime2 = System.currentTimeMillis();

                        if ((yawDiff < 5.0F && pitchDiff < 5.0F) || (currentTime2 - this.aimStartTime) > this.maxAimTime) {
                            this.isAimingComplete = true;
                        } else {
                            this.isAimingComplete = false;
                        }
                    } else {
                        rotation = null;
                        this.isAimingComplete = false;
                    }
                } else {
                    float[] rotationArray = null;

                    switch (currentMode) {
                        case "Simple":
                            rotationArray = RotationUtils.getSimpleRotations(aimingTarget);
                            break;
                        case "NormalNew":
                            Rotation rotations = RotationUtils.getAngless(aimingTarget);
                            rotationArray = new float[]{rotations.getYaw(), rotations.getPitch()};
                            break;
                        case "HvH":
                            rotationArray = RotationUtils.getHVHRotations(aimingTarget , aimRange.getCurrentValue());
                            break;
                        case "New":
                            Vector2f newRot = RotationUtils.getNewRotation(aimingTarget);
                            rotationArray = new float[]{newRot.x, newRot.y};
                            break;
                        case "CNM":
                            rotationArray = RotationUtils.getAnglesss(aimingTarget);
                            break;
                        case "NCP":
                            Vec3 center = RotationUtils.getCenters(aimingTarget.getBoundingBox());
                            Rotation ncpRot = RotationUtils.getNCPRotationss(center, predictValue.getCurrentValue());
                            Vector2f limitedRot = limitAngleChange(RotationManager.rotations, new Vector2f(ncpRot.getYaw(), ncpRot.getPitch()), rotationSpeed.getCurrentValue());
                            rotationArray = new float[]{limitedRot.x, limitedRot.y};
                            break;
                        case "New2":
                            Vec3 center2 = RotationUtils.getCenters(aimingTarget.getBoundingBox());
                            Rotation xkRot1 = RotationUtils.getNewRotations(center2);
                            Vector2f limitedRot1 = limitAngleChange(RotationManager.rotations, new Vector2f(xkRot1.getYaw(), xkRot1.getPitch()), rotationSpeed.getCurrentValue());
                            rotationArray = new float[]{limitedRot1.x, limitedRot1.y};
                            break;
                        case "New3":
                            Vec3 center3 = RotationUtils.getCenters(aimingTarget.getBoundingBox());
                            Rotation xkRot2 = RotationUtils.toRotation(center3, predictValue.getCurrentValue());
                            Vector2f limitedRot2 = limitAngleChange(RotationManager.rotations, new Vector2f(xkRot2.getYaw(), xkRot2.getPitch()), rotationSpeed.getCurrentValue());
                            rotationArray = new float[]{limitedRot2.x, limitedRot2.y};
                            break;
                        case "Smart":
                            org.joml.Vector3d targetPos;
                            final double yDist = aimingTarget.getY() - mc.player.getY();
                            if (yDist >= 1.7) {
                                targetPos = new Vector3d(aimingTarget.getX(), aimingTarget.getY(), aimingTarget.getZ());
                            } else if (yDist <= -1.7) {
                                targetPos = new Vector3d(aimingTarget.getX(), aimingTarget.getY() + aimingTarget.getEyeHeight(), aimingTarget.getZ());
                            } else {
                                targetPos = new Vector3d(aimingTarget.getX(), aimingTarget.getY() + aimingTarget.getEyeHeight() / 2, aimingTarget.getZ());
                            }
                            Vector2f temp = RotationUtils.getRotationFromEyeToPoints(targetPos);
                            rotationArray = new float[]{temp.x, temp.y};
                            break;
                    }

                    if (rotationArray != null) {
                        Vector2f targetRotation = new Vector2f(rotationArray[0], rotationArray[1]);
                        Vector2f currentRot = RotationManager.rotations;
                        Vector2f limitedRotation = limitAngleChange(currentRot, targetRotation, rotationSpeed.getCurrentValue());
                        rotation = limitedRotation;
                        this.isAimingComplete = true;
                    } else {
                        rotation = null;
                        this.isAimingComplete = false;
                    }
                }
            }

            if (targets.isEmpty()) {
                target = null;
                return;
            }

            if (this.index > targets.size() - 1) {
                this.index = 0;
            }

            if (targets.size() > 1
                    && ((float) this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
                this.attackTimes = 0;

                for (int i = 0; i < targets.size(); i++) {
                    this.index++;
                    if (this.index > targets.size() - 1) {
                        this.index = 0;
                    }

                    Entity nextTarget = targets.get(this.index);
                    RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
                    if (data.getDistance() < 3.0) {
                        break;
                    }
                }
            }

            if (this.index > targets.size() - 1 || !isSwitch) {
                this.index = 0;
            }

            target = targets.get(this.index);
        }
    }

    private float updateRotation(float current, float target, float maxDelta) {
        float delta = wrapDegrees(target - current);

        if (Math.abs(delta) > 90.0F) {
            maxDelta *= 2.0F;
        }

        if (delta > maxDelta) {
            delta = maxDelta;
        } else if (delta < -maxDelta) {
            delta = -maxDelta;
        }

        return current + delta;
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (mc.player.getUseItem().isEmpty()
                && mc.screen == null
                && Naven.skipTasks.isEmpty()
                && !NetworkUtils.isServerLag()
                && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()
                && this.isAimingComplete) {
            long attackInterval = (long)(1000.0 / Math.max(1.0, this.cps));
            if (this.attackTimer.hasTimePassed(attackInterval)) {
                this.doAttack();
                this.attackTimer.reset();
            }
        }
    }

    public boolean shouldAutoBlock() {
        return this.isEnabled() && this.fakeAutoblock.getCurrentValue() && aimingTarget != null;
    }

    public Entity shouldPreAim() {
        Entity target = Aura.target;
        if (target == null) {
            List<Entity> aimTargets = this.getTargets();
            if (!aimTargets.isEmpty()) {
                target = aimTargets.get(0);
            }
        }

        return target;
    }

    public void doAttack() {
        if (!targets.isEmpty()) {
            HitResult hitResult = mc.hitResult;
            if (hitResult.getType() == Type.ENTITY) {
                EntityHitResult result = (EntityHitResult)hitResult;
                if (AntiBots.isBot(result.getEntity())) {
                    ChatUtils.addChatMessage("Attacking Bot!");
                    return;
                }
            }

            if (this.multi.getCurrentValue()) {
                int attacked = 0;

                for (Entity entity : targets) {
                    if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                        this.attackEntity(entity);
                        if (++attacked >= 2) {
                            break;
                        }
                    }
                }
            } else if (hitResult.getType() == Type.ENTITY) {
                EntityHitResult result = (EntityHitResult)hitResult;
                this.attackEntity(result.getEntity());
            }
        }
    }

    public void updateAttackTargets() {
        targets = this.getTargets();
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == mc.player) {
            return false;
        } else if (entity instanceof LivingEntity living) {
            if (living instanceof BlinkingPlayer) {
                return false;
            } else {
                AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
                if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
                    if (Teams.isSameTeam(living)) {
                        return false;
                    } else if (FriendManager.isFriend(living)) {
                        return false;
                    } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                        return false;
                    } else if (entity instanceof ArmorStand) {
                        return false;
                    } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                        return false;
                    } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                        return false;
                    } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                        if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                                && !this.attackMobs.getCurrentValue()) {
                            return false;
                        } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                            return false;
                        } else {
                            return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean isValidAttack(Entity entity) {
        if (!this.isValidTarget(entity)) {
            return false;
        } else if (entity instanceof LivingEntity && (float)((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
            return false;
        } else {
            Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
            double distance = closestPoint.distanceTo(mc.player.getEyePosition());

            boolean inWall = RayTraceUtil.isWallBetween(entity);
            boolean hasLineOfSight = mc.player.hasLineOfSight(entity);

            if (inWall) {
                if (throughWalls.getCurrentValue()) {
                    if (distance > (double) throughWallsRange.getCurrentValue()) {
                        return false;
                    }
                } else {
                    if (noWall.getCurrentValue()) {
                        return false;
                    } else {
                        if (distance > (double) this.aimRange.getCurrentValue()) {
                            return false;
                        }
                    }
                }
            } else {
                if (distance > (double) this.aimRange.getCurrentValue()) {
                    return false;
                }
            }

            return RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F)
                    && (!noWall.getCurrentValue() || !RayTraceUtil.isWallBetween(entity) || throughWalls.getCurrentValue());
        }
    }

    public void attackEntity(Entity entity) {
        this.attackTimes++;
        if (rotationMode.isCurrentMode("Normal")) {
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            mc.player.setYRot(RotationManager.rotations.x);
            mc.player.setXRot(RotationManager.rotations.y);

            if (entity instanceof Player && !AntiBots.isBot(entity)) {
                KillSay.attackedPlayers.add(entity.getName().getString());
            }

            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (this.moreParticles.getCurrentValue()) {
                mc.player.magicCrit(entity);
                mc.player.crit(entity);
            }

            mc.player.setYRot(currentYaw);
            mc.player.setXRot(currentPitch);
        } else {
            if (entity instanceof Player && !AntiBots.isBot(entity)) {
                KillSay.attackedPlayers.add(entity.getName().getString());
            }

            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (this.moreParticles.getCurrentValue()) {
                mc.player.magicCrit(entity);
                mc.player.crit(entity);
            }
        }
    }

    private List<Entity> getTargets() {
        Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
                .filter(entity -> entity instanceof Entity)
                .filter(this::isValidAttack);
        List<Entity> possibleTargets = stream.collect(Collectors.toList());
        if (this.priority.isCurrentMode("Range")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
        } else if (this.priority.isCurrentMode("FoV")) {
            possibleTargets.sort(
                    Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
            );
        } else if (this.priority.isCurrentMode("Health")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
        }

        if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())) {
            possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity)entity).isBaby());
        }

        possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
        return this.infSwitch.getCurrentValue()
                ? possibleTargets
                : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
    }

    private Vector2f limitAngleChange(Vector2f current, Vector2f target, float maxTurnSpeed) {
        float yawDiff = wrapDegrees(target.x - current.x);
        float pitchDiff = wrapDegrees(target.y - current.y);
        yawDiff = clamp(yawDiff, -maxTurnSpeed, maxTurnSpeed);
        pitchDiff = clamp(pitchDiff, -maxTurnSpeed, maxTurnSpeed);

        return new Vector2f(current.x + yawDiff, current.y + pitchDiff);
    }

    private float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }
        if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    private float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    private float easeOutElastic(float x) {
        float c4 = (float) ((2 * Math.PI) / 3);
        return x == 0 ? 0 : x == 1 ? 1 : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
    }

    private float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * x * x * x - c1 * x * x;
    }

    private Color getPingColor(int ping) {
        if (ping < 100) return new Color(0, 165, 0);
        if (ping < 200) return new Color(165, 165, 0);
        if (ping < 300) return new Color(165, 80, 0);
        if (ping < 400) return new Color(165, 0, 0);
        return new Color(80, 0, 0);
    }

    private void drawSmoothArmorBar(PoseStack poseStack, float x, float y, float width, float height, float health, float maxHealth, Color bgColor, Color barColor) {
        RenderUtils.drawRoundedRect(poseStack, x, y, width, height, 0, bgColor.getRGB());
        float healthPercent = health / maxHealth;
        float barWidth = Math.min(healthPercent * width, width);
        RenderUtils.drawRoundedRect(poseStack, x, y, barWidth, height, 0, barColor.getRGB());
    }

    private void drawSmoothHealthBar(PoseStack poseStack, float x, float y, float width, float height, float health, float maxHealth, Color bgColor, Color barColor) {
        RenderUtils.drawRoundedRect(poseStack, x, y, width, height, 0, bgColor.getRGB());
        float healthPercent = health / maxHealth;
        float barWidth = Math.min(healthPercent * width, width);
        RenderUtils.drawRoundedRect(poseStack, x, y, barWidth, height, 0, barColor.getRGB());
    }

    private Color getColor(LivingEntity entity) {
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float ratio = health / maxHealth;

        if (ratio > 0.5f) {
            return new Color((int)((1 - ratio) * 2 * 255), 255, 0);
        } else {
            return new Color(255, (int)(ratio * 2 * 255), 0);
        }
    }
}