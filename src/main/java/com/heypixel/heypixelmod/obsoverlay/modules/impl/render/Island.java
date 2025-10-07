package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.ez.EaseCube;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.ez.EaseOutExpo;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import dev.yalan.live.LiveClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

@ModuleInfo(
        name = "Island",
        description = "ISLAND~~~",
        category = Category.RENDER
)
public class Island extends Module {
    private static Island INSTANCE;
    private final FloatValue x = ValueBuilder.create(this, "X").setDefaultFloatValue(0.0F).setMinFloatValue(-1000.0F).setMaxFloatValue(1000.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue y = ValueBuilder.create(this, "Y").setDefaultFloatValue(8.0F).setMinFloatValue(-1000.0F).setMaxFloatValue(1000.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue width = ValueBuilder.create(this, "Width").setDefaultFloatValue(220.0F).setMinFloatValue(80.0F).setMaxFloatValue(600.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue height = ValueBuilder.create(this, "Height").setDefaultFloatValue(30.0F).setMinFloatValue(14.0F).setMaxFloatValue(200.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue radius = ValueBuilder.create(this, "Radius").setDefaultFloatValue(10.0F).setMinFloatValue(0.0F).setMaxFloatValue(10.0F).setFloatStep(0.1F).build().getFloatValue();
    private final BooleanValue centerX = ValueBuilder.create(this, "Center X").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue centerTitle = ValueBuilder.create(this, "Center Title").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue blurMask = ValueBuilder.create(this, "Blur Mask").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue bloomMask = ValueBuilder.create(this, "Bloom Mask").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue bloomStrength = ValueBuilder.create(this, "Bloom Strength").setVisibility(this.bloomMask::getCurrentValue).setDefaultFloatValue(1.0F).setMinFloatValue(0.0F).setMaxFloatValue(1.0F).setFloatStep(0.05F).build().getFloatValue();
    private final FloatValue backgroundAlpha = ValueBuilder.create(this, "Background Alpha").setDefaultFloatValue(120.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final BooleanValue autoWidth = ValueBuilder.create(this, "Auto Width").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue padding = ValueBuilder.create(this, "Padding").setDefaultFloatValue(10.0F).setMinFloatValue(0.0F).setMaxFloatValue(40.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue textScale = ValueBuilder.create(this, "Text Scale").setDefaultFloatValue(0.45F).setMinFloatValue(0.2F).setMaxFloatValue(1.0F).setFloatStep(0.01F).build().getFloatValue();
    private final BooleanValue showHeaderBar = ValueBuilder.create(this, "Show Header Bar").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue headerBarHeight = ValueBuilder.create(this, "Header Bar Height").setVisibility(this.showHeaderBar::getCurrentValue).setDefaultFloatValue(4.0F).setMinFloatValue(1.0F).setMaxFloatValue(20.0F).setFloatStep(1.0F).build().getFloatValue();
    private final BooleanValue autoHeight = ValueBuilder.create(this, "Auto Height").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue vPadding = ValueBuilder.create(this, "Vertical Padding").setDefaultFloatValue(6.0F).setMinFloatValue(0.0F).setMaxFloatValue(40.0F).setFloatStep(1.0F).build().getFloatValue();
    private final BooleanValue noticesEnabled = ValueBuilder.create(this, "Notices").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue arrayVertical = ValueBuilder.create(this, "Array Vertical").setDefaultFloatValue(4.0F).setMinFloatValue(0.0F).setMaxFloatValue(40.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue noticeHoldSeconds = ValueBuilder.create(this, "Notice Hold (s)").setDefaultFloatValue(2.0F).setMinFloatValue(0.5F).setMaxFloatValue(10.0F).setFloatStep(0.1F).build().getFloatValue();
    private final FloatValue noticeEnterMs = ValueBuilder.create(this, "Notice Enter (ms)").setDefaultFloatValue(200.0F).setMinFloatValue(50.0F).setMaxFloatValue(1000.0F).setFloatStep(10.0F).build().getFloatValue();
    private final FloatValue noticeExitMs = ValueBuilder.create(this, "Notice Exit (ms)").setDefaultFloatValue(250.0F).setMinFloatValue(50.0F).setMaxFloatValue(1500.0F).setFloatStep(10.0F).build().getFloatValue();
    private final FloatValue noticeScale = ValueBuilder.create(this, "Notice Text Scale").setDefaultFloatValue(0.4F).setMinFloatValue(0.2F).setMaxFloatValue(1.0F).setFloatStep(0.01F).build().getFloatValue();
    private final FloatValue noticeSpacing = ValueBuilder.create(this, "Notice Spacing").setDefaultFloatValue(2.0F).setMinFloatValue(0.0F).setMaxFloatValue(20.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue noticeInnerPadX = ValueBuilder.create(this, "Notice Inner Padding X").setDefaultFloatValue(8.0F).setMinFloatValue(0.0F).setMaxFloatValue(40.0F).setFloatStep(1.0F).build().getFloatValue();
    private final List<Notice> notices = new ArrayList<>();
    private final BooleanValue scaffoldHudEnabled = ValueBuilder.create(this, "Scaffold HUD").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue scaffoldTextScale = ValueBuilder.create(this, "Scaffold Text Scale").setDefaultFloatValue(0.7F).setMinFloatValue(0.4F).setMaxFloatValue(0.9F).setFloatStep(0.01F).build().getFloatValue();
    private final FloatValue scaffoldIconSize = ValueBuilder.create(this, "Scaffold Icon Size").setDefaultFloatValue(16.0F).setMinFloatValue(12.0F).setMaxFloatValue(24.0F).setFloatStep(1.0F).build().getFloatValue();
    private final BooleanValue scaffoldIconSyncColor = ValueBuilder.create(this, "Scaffold Icon Sync Color").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue scaffoldIconCount = ValueBuilder.create(this, "Scaffold Icon Count").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue scaffoldSyncBarColor = ValueBuilder.create(this, "Scaffold Sync Bar Color").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue scaffoldBarHeight = ValueBuilder.create(this, "Scaffold Bar Height").setDefaultFloatValue(12.0F).setMinFloatValue(8.0F).setMaxFloatValue(20.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldPaddingX = ValueBuilder.create(this, "Scaffold Padding X").setDefaultFloatValue(6.0F).setMinFloatValue(2.0F).setMaxFloatValue(16.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldPaddingY = ValueBuilder.create(this, "Scaffold Padding Y").setDefaultFloatValue(6.0F).setMinFloatValue(2.0F).setMaxFloatValue(16.0F).setFloatStep(1.0F).build().getFloatValue();
    private final BooleanValue scaffoldShowPercent = ValueBuilder.create(this, "Show Percent Text").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue scaffoldBpsSmoothing = ValueBuilder.create(this, "BPS Smoothing").setDefaultFloatValue(0.7F).setMinFloatValue(0.0F).setMaxFloatValue(1.0F).setFloatStep(0.05F).build().getFloatValue();
    private final FloatValue scaffoldBpsSampleMs = ValueBuilder.create(this, "BPS Sample (ms)").setDefaultFloatValue(250.0F).setMinFloatValue(100.0F).setMaxFloatValue(1000.0F).setFloatStep(10.0F).build().getFloatValue();
    private final FloatValue scaffoldBarRadius = ValueBuilder.create(this, "Scaffold Bar Radius").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultFloatValue(6.0F).setMinFloatValue(0.0F).setMaxFloatValue(12.0F).setFloatStep(0.5F).build().getFloatValue();
    private final FloatValue scaffoldBarBgAlpha = ValueBuilder.create(this, "Scaffold Bar BG Alpha").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultFloatValue(35.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldBarFillA = ValueBuilder.create(this, "Scaffold Bar Fill A").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultFloatValue(200.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final ModeValue scaffoldFillDirection = ValueBuilder.create(this, "Scaffold Fill Direction").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultModeIndex(0).setModes("Right", "Left").build().getModeValue();
    private final FloatValue scaffoldFillSmooth = ValueBuilder.create(this, "Scaffold Fill Smooth").setVisibility(this.scaffoldHudEnabled::getCurrentValue).setDefaultFloatValue(0.3F).setMinFloatValue(0.0F).setMaxFloatValue(0.95F).setFloatStep(0.05F).build().getFloatValue();
    private final BooleanValue scaffoldShowBps = ValueBuilder.create(this, "Scaffold Show BPS").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue scaffoldBpsR = ValueBuilder.create(this, "Scaffold BPS R").setDefaultFloatValue(255.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldBpsG = ValueBuilder.create(this, "Scaffold BPS G").setDefaultFloatValue(255.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldBpsB = ValueBuilder.create(this, "Scaffold BPS B").setDefaultFloatValue(255.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldBpsA = ValueBuilder.create(this, "Scaffold BPS A").setDefaultFloatValue(255.0F).setMinFloatValue(0.0F).setMaxFloatValue(255.0F).setFloatStep(1.0F).build().getFloatValue();
    private final FloatValue scaffoldPctTextScale = ValueBuilder.create(this, "Scaffold Percent Text Scale").setVisibility(this.scaffoldShowPercent::getCurrentValue).setDefaultFloatValue(0.7F).setMinFloatValue(0.4F).setMaxFloatValue(0.9F).setFloatStep(0.01F).build().getFloatValue();
    private boolean scaffoldLastEnabled = false;
    private int scaffoldStartBlocks = 0;
    private long scaffoldLastSampleMs = 0L;
    private int scaffoldLastBlocksSampled = 0;
    private float scaffoldBps = 0.0F;
    private float scaffoldFillPct = 0.0F;
    private final SmoothAnimationTimer animW = new SmoothAnimationTimer(240.0F);
    private final SmoothAnimationTimer animH = new SmoothAnimationTimer(240.0F);
    private boolean lastOverride = false;
    private long transitionStartMs = 0L;
    private float prevTargetW = 0.0F;
    private float prevTargetH = 0.0F;
    private boolean hasLastBox = false;
    private float lastBoxW = 0.0F;
    private float lastBoxH = 0.0F;
    private long activeTransDurMs = 320L;
    private static final SimpleDateFormat TITLE_TIME_FMT = new SimpleDateFormat("HH:mm:ss");
    private static final int POS_DUR_MS = 220;
    private final Minecraft mc = Minecraft.getInstance();

    public Island() {
        INSTANCE = this;
    }

    public static Island getInstance() {
        return INSTANCE;
    }

    public static void postNotice(String msg) {
        if (INSTANCE != null) {
            INSTANCE.addNotice(msg);
        }
    }

    public void addNotice(String msg) {
        if (this.noticesEnabled.getCurrentValue()) {
            this.notices.add(new Notice(msg, System.currentTimeMillis(), (long)this.noticeEnterMs.getCurrentValue(), (long)(this.noticeHoldSeconds.getCurrentValue() * 1000.0F), (long)this.noticeExitMs.getCurrentValue()));
        }
    }

    private String getIslandTitle() {
        String fps = "";

        try {
            fps = StringUtils.split(mc.fpsString, " ")[0] + " FPS | ";
        } catch (Throwable var3) {
        }
        String time = TITLE_TIME_FMT.format(new Date());
        return "ShaoYu | " + Version.getVersion() + " | " + LiveClient.INSTANCE.autoUsername + " | " + " | " + fps + time;
    }

    private float easeOutExpo(float x) {
        return x >= 1.0F ? 1.0F : (float)(1.0D - Math.pow(2.0D, (double)(-10.0F * x)));
    }

    private float easeInExpo(float x) {
        return x <= 0.0F ? 0.0F : (float)Math.pow(2.0D, (double)(10.0F * (x - 1.0F)));
    }

    private float easeCube(float x) {
        return x * x * x;
    }

    private int clamp255(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private int getRainbowColor(float speed, int alpha) {
        float clamped = Math.max(1.0F, Math.min(20.0F, speed));
        double periodMs = 8000.0D / (double)clamped;
        double t = (double)System.currentTimeMillis() % periodMs / periodMs;
        float hue = (float)t;
        int rgb = Color.HSBtoRGB(hue, 0.9F, 1.0F);
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        return (new Color(r, g, b, Math.max(0, Math.min(255, alpha)))).getRGB();
    }

    private int getItemMainColor(ItemStack stack) {
        try {
            int c = Minecraft.getInstance().getItemColors().getColor(stack, 0);
            if (c != -1) {
                return c & 0xFFFFFF | 0xFF000000;
            }
        } catch (Throwable var3) {
        }

        return (new Color(200, 200, 200, 255)).getRGB();
    }

    private int withAlpha(int rgb, int alpha) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        return (new Color(r, g, b, Math.max(0, Math.min(255, alpha)))).getRGB();
    }

    private int darken(int rgb, float factor) {
        factor = Math.max(0.0F, factor);
        int r = Math.max(0, Math.min(255, Math.round((float)(rgb >> 16 & 255) * (1.0F - factor))));
        int g = Math.max(0, Math.min(255, Math.round((float)(rgb >> 8 & 255) * (1.0F - factor))));
        int b = Math.max(0, Math.min(255, Math.round((float)(rgb & 255) * (1.0F - factor))));
        int a = rgb >>> 24;
        return (new Color(r, g, b, a)).getRGB();
    }

    private int lighten(int rgb, float factor) {
        factor = Math.max(0.0F, Math.min(1.0F, factor));
        int r0 = rgb >> 16 & 255;
        int g0 = rgb >> 8 & 255;
        int b0 = rgb & 255;
        int r = Math.max(0, Math.min(255, Math.round((float)r0 + (float)(255 - r0) * factor)));
        int g = Math.max(0, Math.min(255, Math.round((float)g0 + (float)(255 - g0) * factor)));
        int b = Math.max(0, Math.min(255, Math.round((float)b0 + (float)(255 - b0) * factor)));
        int a = rgb >>> 24;
        return (new Color(r, g, b, a)).getRGB();
    }

    private float bounce(float x) {
        return (float)(Math.sin((double)x * Math.PI * 2.0D) * Math.pow(1.0D - Math.min(1.0D, (double)x), 2.0D) * 0.08D);
    }

    private float computeLeft(float boxWidth) {
        return this.centerX.getCurrentValue() ? (float)mc.getWindow().getGuiScaledWidth() / 2.0F - boxWidth / 2.0F + this.x.getCurrentValue() : this.x.getCurrentValue();
    }

    @EventTarget
    public void onShader(EventShader e) {
        CustomTextRenderer font = Fonts.harmony;
        float[] wh = this.computeAnimatedBoxWH(font);
        float w = wh[0];
        float h = wh[1];
        float left = this.computeLeft(w);
        float top = this.y.getCurrentValue();
        float r = Math.max(0.0F, Math.min(this.radius.getCurrentValue(), Math.min(w, h) / 2.0F - 0.5F));
        if (e.getType() == EventType.BLUR && this.blurMask.getCurrentValue()) {
            RenderUtils.drawRoundedRect(e.getStack(), left, top, w, h, r, Integer.MIN_VALUE);
        }

        if (e.getType() == EventType.SHADOW && this.bloomMask.getCurrentValue()) {
            float s = Math.max(0.0F, Math.min(1.0F, this.bloomStrength.getCurrentValue()));
            int bgA = (int)Math.max(0.0F, Math.min(255.0F, this.backgroundAlpha.getCurrentValue()));
            int aBase = (int)((float)bgA * s);
            if (aBase > 0) {
                RenderUtils.drawRoundedRect(e.getStack(), left, top, w, h, r, (new Color(0, 0, 0, aBase)).getRGB());
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        CustomTextRenderer font = Fonts.harmony;
        float[] wh = this.computeAnimatedBoxWH(font);
        float w = wh[0];
        float h = wh[1];
        String text = this.getIslandTitle();
        double scale = (double)this.textScale.getCurrentValue();
        float textWidth = font.getWidth(text, scale);
        float textHeight = (float)font.getHeight(true, scale);
        boolean overrideByNotices = this.noticesActive() && this.getActiveNoticeCount() > 0;
        float left = this.computeLeft(w);
        float top = this.y.getCurrentValue();
        float r = Math.max(0.0F, Math.min(this.radius.getCurrentValue(), Math.min(w, h) / 2.0F - 0.5F));
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), left, top, w, h, r, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        int bgA = (int)Math.max(0.0F, Math.min(255.0F, this.backgroundAlpha.getCurrentValue()));
        RenderUtils.fillBound(e.getStack(), left, top, w, h, (new Color(0, 0, 0, bgA)).getRGB());
        if (this.showHeaderBar.getCurrentValue()) {
            float hb = Math.min(this.headerBarHeight.getCurrentValue(), h);
            RenderUtils.fill(e.getStack(), left, top, left + w, top + hb, (new Color(150, 45, 45, 160)).getRGB());
        }

        boolean scaffoldEnabled = false;
        Scaffold scaffold = null;

        try {
            scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
            scaffoldEnabled = scaffold != null && scaffold.isEnabled();
        } catch (Throwable var28) {
        }

        int totalBlocksNow = 0;
        if (scaffoldEnabled && this.scaffoldHudEnabled.getCurrentValue()) {
            totalBlocksNow = this.countPlaceableBlocks();
            if (!this.scaffoldLastEnabled) {
                this.scaffoldStartBlocks = Math.max(0, totalBlocksNow);
                this.scaffoldLastBlocksSampled = totalBlocksNow;
                this.scaffoldLastSampleMs = System.currentTimeMillis();
                this.scaffoldBps = 0.0F;
            }

            long now = System.currentTimeMillis();
            long dt = now - this.scaffoldLastSampleMs;
            long needMs = Math.max(50L, (long)this.scaffoldBpsSampleMs.getCurrentValue());
            if (dt >= needMs) {
                int delta = this.scaffoldLastBlocksSampled - totalBlocksNow;
                float inst = dt > 0L ? (float)delta * 1000.0F / (float)dt : 0.0F;
                if (inst < 0.0F) {
                    inst = 0.0F;
                }

                float s = Math.max(0.0F, Math.min(1.0F, this.scaffoldBpsSmoothing.getCurrentValue()));
                this.scaffoldBps = s * this.scaffoldBps + (1.0F - s) * inst;
                this.scaffoldLastBlocksSampled = totalBlocksNow;
                this.scaffoldLastSampleMs = now;
            }
        }

        this.scaffoldLastEnabled = scaffoldEnabled;
        if (!overrideByNotices) {
            if (scaffoldEnabled && this.scaffoldHudEnabled.getCurrentValue()) {
                this.renderScaffoldHUD(e, font, left, top, w, h, totalBlocksNow);
            } else {
                float tx = this.centerTitle.getCurrentValue() ? left + (w - textWidth) / 2.0F : left + this.padding.getCurrentValue();
                float ty = top + (h - textHeight) / 2.0F;
                font.render(e.getStack(), text, (double)tx, (double)ty, Color.WHITE, true, scale);
            }
        }

        if (overrideByNotices) {
            this.renderNotices(e, font, left, top, w, h);
        }

        StencilUtils.dispose();
    }

    private void renderScaffoldHUD(EventRender2D e, CustomTextRenderer font, float left, float top, float w, float h, int totalBlocksNow) {
        float paddingX = Math.max(2.0F, this.scaffoldPaddingX.getCurrentValue());
        float paddingY = Math.max(2.0F, this.scaffoldPaddingY.getCurrentValue());
        float contentLeft = left + paddingX;
        float contentRight = left + w - paddingX;
        float contentTop = top + paddingY;
        float contentBottom = top + h - paddingY;
        float contentH = Math.max(1.0F, contentBottom - contentTop);
        float iconSize = Math.max(12.0F, this.scaffoldIconSize.getCurrentValue());
        float iconY = contentTop + (contentH - iconSize) / 2.0F;
        ItemStack displayStack = this.getDisplayBlockStack();
        if (displayStack != null) {
            GuiGraphics gg = e.getGuiGraphics();
            PoseStack pose = gg.pose();
            pose.pushPose();
            float s = iconSize / 16.0F;
            pose.translate(contentLeft, iconY, 0.0F);
            pose.scale(s, s, 1.0F);
            if (this.scaffoldIconSyncColor.getCurrentValue()) {
                int base = this.getItemMainColor(displayStack);
                int bg = this.withAlpha(base, 60);
                float rBg = iconSize * 0.25F;
                RenderUtils.drawRoundedRect(e.getStack(), 0.0F, 0.0F, 16.0F, 16.0F, rBg / s, bg);
            }

            gg.renderItem(displayStack, 0, 0);
            if (this.scaffoldIconCount.getCurrentValue()) {
                gg.renderItemDecorations(mc.font, displayStack, 0, 0);
            }

            pose.popPose();
        }

        float afterIconX = contentLeft + iconSize + 6.0F;
        String rightText = totalBlocksNow + " | " + String.format(Locale.ROOT, "%.1f", this.scaffoldBps) + " bps";
        double ts = (double)Math.min(0.9F, Math.max(0.4F, this.scaffoldTextScale.getCurrentValue()));
        float rtW = font.getWidth(rightText, ts);
        float rtH = (float)font.getHeight(true, ts);
        float rightTextX = contentRight - rtW;
        float rightTextY = contentTop + (contentH - rtH) / 2.0F + 1.0F;
        font.render(e.getStack(), rightText, (double)rightTextX, (double)rightTextY, Color.WHITE, true, ts);
        float gap = 6.0F;
        float barRight = Math.max(afterIconX, rightTextX - gap);
        float barWidth = Math.max(0.0F, barRight - afterIconX);
        float barHeight = Math.max(6.0F, Math.min(this.scaffoldBarHeight.getCurrentValue(), contentH));
        float barY = contentTop + (contentH - barHeight) / 2.0F;
        float radius = Math.min(barHeight / 2.0F, Math.max(0.0F, this.scaffoldBarRadius.getCurrentValue()));
        int start = Math.max(0, this.scaffoldStartBlocks);
        float pct = start > 0 ? Math.max(0.0F, Math.min(1.0F, (float)totalBlocksNow / (float)start)) : 0.0F;
        int bgCol = (new Color(255, 255, 255, this.clamp255(this.scaffoldBarBgAlpha.getCurrentValue()))).getRGB();
        float s = this.scaffoldFillSmooth.getCurrentValue();
        this.scaffoldFillPct += (pct - this.scaffoldFillPct) * (1.0F - s);
        float drawPct = Math.max(0.0F, Math.min(1.0F, this.scaffoldFillPct));
        int fillCol;
        if (this.scaffoldSyncBarColor.getCurrentValue()) {
            if (displayStack != null) {
                int base = this.getItemMainColor(displayStack);
                bgCol = this.withAlpha(this.lighten(base, 0.35F), this.clamp255(this.scaffoldBarBgAlpha.getCurrentValue()));
                fillCol = this.withAlpha(this.darken(base, 0.15F), this.clamp255(this.scaffoldBarFillA.getCurrentValue()));
            } else {
                float hue = Math.max(0.0F, Math.min(1.0F, pct)) * 0.33F;
                int base = Color.HSBtoRGB(hue, 0.9F, 1.0F);
                bgCol = this.withAlpha(this.lighten(base, 0.35F), this.clamp255(this.scaffoldBarBgAlpha.getCurrentValue()));
                fillCol = this.withAlpha(this.darken(base, 0.15F), this.clamp255(this.scaffoldBarFillA.getCurrentValue()));
            }
        } else {
            fillCol = (new Color(255, 255, 255, this.clamp255(this.scaffoldBarFillA.getCurrentValue()))).getRGB();
        }

        RenderUtils.drawRoundedRect(e.getStack(), afterIconX, barY, barWidth, barHeight, radius, bgCol);
        float fillW = barWidth * drawPct;
        boolean leftDir = "Left".equalsIgnoreCase(this.scaffoldFillDirection.getCurrentMode());
        if (fillW > 0.5F) {
            float fx = leftDir ? afterIconX + barWidth - fillW : afterIconX;
            RenderUtils.drawRoundedRect(e.getStack(), fx, barY, fillW, barHeight, radius, fillCol);
        }

        if (this.scaffoldShowPercent.getCurrentValue()) {
            String pctText = Math.round(pct * 100.0F) + "%";
            double ps = (double)this.scaffoldPctTextScale.getCurrentValue();
            int pctCol = (new Color(255, 255, 255, 230)).getRGB();
            float tx = afterIconX + barWidth / 2.0F - (float)((double)font.getWidth(pctText, ps) / 2.0D);
            float ty = barY + barHeight / 2.0F - (float)(font.getHeight(true, ps) / 2.0D);
            font.render(e.getStack(), pctText, (double)tx, (double)ty, new Color(pctCol, true), true, ps);
        }

        if (this.scaffoldShowBps.getCurrentValue()) {
            String bpsText = String.format("BPS %.2f", Math.max(0.0F, this.scaffoldBps));
            int bpsCol = (new Color(this.clamp255(this.scaffoldBpsR.getCurrentValue()), this.clamp255(this.scaffoldBpsG.getCurrentValue()), this.clamp255(this.scaffoldBpsB.getCurrentValue()), this.clamp255(this.scaffoldBpsA.getCurrentValue()))).getRGB();
            double bs = ts * 0.9D;
            float bx = rightTextX - font.getWidth(bpsText, bs);
            float by = rightTextY + (float)font.getHeight(true, ts) + 1.0F;
            font.render(e.getStack(), bpsText, (double)bx, (double)by, new Color(bpsCol, true), true, bs);
        }
    }

    private ItemStack getDisplayBlockStack() {
        if (mc.player == null) {
            return null;
        } else {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack != null && stack.getItem() instanceof BlockItem && Scaffold.isValidStack(stack)) {
                return stack;
            } else {
                for(int i = 0; i < 9; ++i) {
                    ItemStack s = mc.player.getInventory().getItem(i);
                    if (s != null && s.getItem() instanceof BlockItem && Scaffold.isValidStack(s)) {
                        return s;
                    }
                }

                return null;
            }
        }
    }

    private int countPlaceableBlocks() {
        if (mc.player == null) {
            return 0;
        } else {
            int total = 0;

            for(int i = 0; i < mc.player.getInventory().items.size(); ++i) {
                ItemStack s = mc.player.getInventory().items.get(i);
                if (s != null && s.getItem() instanceof BlockItem && Scaffold.isValidStack(s)) {
                    total += s.getCount();
                }
            }

            return total;
        }
    }

    private float[] computeAnimatedBoxWH(CustomTextRenderer font) {
        float w = this.width.getCurrentValue();
        float h = this.height.getCurrentValue();
        String text = this.getIslandTitle();
        double scale = (double)this.textScale.getCurrentValue();
        float textWidth = font.getWidth(text, scale);
        float textHeight = (float)font.getHeight(true, scale);
        long now = System.currentTimeMillis();
        boolean rawActive = this.noticesActive() && this.getActiveNoticeCount() > 0;
        long holdMs = (long)(this.noticeHoldSeconds.getCurrentValue() * 1000.0F);
        long enterDurMs = (long)this.noticeEnterMs.getCurrentValue();
        long exitDurMs = (long)this.noticeExitMs.getCurrentValue();
        boolean effectiveActive = rawActive;
        if (!rawActive && this.lastOverride) {
            long sinceStart = now - this.transitionStartMs;
            if (sinceStart < holdMs) {
                effectiveActive = true;
            }
        }

        if (effectiveActive != this.lastOverride) {
            this.transitionStartMs = now;
            this.prevTargetW = this.hasLastBox ? this.lastBoxW : w;
            this.prevTargetH = this.hasLastBox ? this.lastBoxH : h;
            this.activeTransDurMs = effectiveActive ? enterDurMs : exitDurMs;
            this.lastOverride = effectiveActive;
        }

        if (effectiveActive) {
            double ns = (double)this.noticeScale.getCurrentValue();
            float nh = (float)font.getHeight(true, ns);
            int active = this.getActiveNoticeCount();
            float maxNw = 0.0F;

            for(Notice n : this.notices) {
                if (!n.isFinished(now)) {
                    float nw = this.computeNoticeVisualWidth(n.msg, font, ns);
                    if (nw > maxNw) {
                        maxNw = nw;
                    }
                }
            }

            w = maxNw + (this.padding.getCurrentValue() + this.noticeInnerPadX.getCurrentValue()) * 2.0F;
            float spacingY = this.arrayVertical.getCurrentValue();
            float blockH = active > 0 ? nh * (float)active + spacingY * (float)(active - 1) : 0.0F;
            float motionRoom = nh * 1.0F;
            h = this.vPadding.getCurrentValue() * 2.0F + blockH + motionRoom;
        } else {
            if (this.autoWidth.getCurrentValue()) {
                w = Math.max(w, textWidth + this.padding.getCurrentValue() * 2.0F);
            }

            if (this.autoHeight.getCurrentValue()) {
                h = Math.max(h, textHeight + this.vPadding.getCurrentValue() * 2.0F);
            }
        }

        long transDur = Math.max(0L, this.activeTransDurMs);
        long elapsed = Math.max(0L, now - this.transitionStartMs);
        if (elapsed < transDur) {
            float t = Math.min(1.0F, (float)elapsed / (float)transDur);
            float eased;
            if (this.lastOverride) {
                eased = this.easeOutExpo(t) + this.bounce(t);
            } else {
                eased = this.easeInExpo(t) * 0.6F + this.easeCube(t) * 0.4F + this.bounce(t);
            }

            float cw = this.prevTargetW + (w - this.prevTargetW) * eased;
            float ch = this.prevTargetH + (h - this.prevTargetH) * eased;
            w = Math.max(1.0F, cw);
            h = Math.max(1.0F, ch);
        } else if (!effectiveActive) {
            this.animW.target = w;
            this.animH.target = h;
            this.animW.update(true);
            this.animH.update(true);
            w = Math.max(1.0F, this.animW.value);
            h = Math.max(1.0F, this.animH.value);
        }

        this.lastBoxW = w;
        this.lastBoxH = h;
        this.hasLastBox = true;
        return new float[]{w, h};
    }

    private int getActiveNoticeCount() {
        long now = System.currentTimeMillis();
        int count = 0;

        for(Notice n : this.notices) {
            if (!n.isFinished(now)) {
                ++count;
            }
        }

        return count;
    }

    private boolean noticesActive() {
        return this.noticesEnabled.getCurrentValue();
    }

    private void renderNotices(EventRender2D e, CustomTextRenderer font, float left, float top, float w, float h) {
        if (!this.notices.isEmpty()) {
            long now = System.currentTimeMillis();
            double ns = (double)this.noticeScale.getCurrentValue();
            float nh = (float)font.getHeight(true, ns);
            float curY = top + this.vPadding.getCurrentValue() + nh * 0.25F;
            float spacing = this.arrayVertical.getCurrentValue();
            Iterator<Notice> it = this.notices.iterator();
            int idx = 0;

            while(it.hasNext()) {
                Notice n = it.next();
                if (n.isFinished(now)) {
                    it.remove();
                } else {
                    if (!n.positionInitialized) {
                        n.posStartIdx = (float)idx;
                        n.targetIdx = (float)idx;
                        n.posStartMs = now;
                        n.positionInitialized = true;
                    } else if (n.targetIdx != (float)idx) {
                        double curIdxInterp = n.interpIndex(now, 220);
                        n.posStartIdx = (float)curIdxInterp;
                        n.targetIdx = (float)idx;
                        n.posStartMs = now;
                    }

                    double alpha = n.alpha(now);
                    double offset = n.offset(now, nh * 0.9F);
                    double idxInterp = n.interpIndex(now, 220);
                    float rowShift = (float)((idxInterp - (double)idx) * (double)(nh + spacing));
                    float ny = curY + (float)offset + rowShift;
                    String msg = n.msg;
                    String moduleName = null;
                    String status = null;
                    if (msg.endsWith(" Enabled!")) {
                        moduleName = msg.substring(0, msg.length() - " Enabled!".length());
                        status = "Enabled";
                    } else if (msg.endsWith(" Disabled!")) {
                        moduleName = msg.substring(0, msg.length() - " Disabled!".length());
                        status = "Disabled";
                    }

                    float lineWidth = this.computeNoticeVisualWidth(msg, font, ns);
                    float nx = left + (w - lineWidth) / 2.0F;
                    long tLocal = now - n.start;
                    double enter = (double)n.enterMs;
                    double hold = (double)n.holdMs;
                    double exit = (double)n.exitMs;
                    double wText;
                    if ((double)tLocal <= enter) {
                        double p = Math.max(0.0D, Math.min(1.0D, (double)tLocal / enter));
                        wText = Math.max(0.0D, Math.min(1.0D, (double)this.easeOutExpo((float)p)));
                    } else if ((double)tLocal <= enter + hold) {
                        wText = 1.0D;
                    } else {
                        double p = Math.max(0.0D, Math.min(1.0D, ((double)tLocal - enter - hold) / exit));
                        wText = Math.max(0.0D, Math.min(1.0D, (double)this.easeCube((float)(1.0D - p))));
                    }

                    int aText = (int)Math.max(0L, Math.min(255L, Math.round(alpha * wText * 255.0D)));
                    if (status == null) {
                        font.render(e.getStack(), msg, (double)nx, (double)ny, new Color(255, 255, 255, aText), true, ns);
                    } else {
                        float gap = 6.0F;
                        float modW = font.getWidth(moduleName, ns);
                        font.render(e.getStack(), moduleName, (double)nx, (double)ny, new Color(255, 255, 255, aText), true, ns);
                        float pillPadX = 6.0F;
                        float pillTextW = font.getWidth(status, ns);
                        float pillW = pillTextW + pillPadX * 2.0F;
                        float pillX = nx + modW + gap;
                        double delay = 80.0D;
                        double tPill = (double)tLocal - delay;
                        double wPill;
                        if (tPill <= 0.0D) {
                            wPill = 0.0D;
                        } else if (tPill <= enter) {
                            double p = Math.max(0.0D, Math.min(1.0D, tPill / enter));
                            wPill = Math.max(0.0D, Math.min(1.0D, (double)this.easeOutExpo((float)p)));
                        } else if (tPill <= enter + hold) {
                            wPill = 1.0D;
                        } else {
                            double p = Math.max(0.0D, Math.min(1.0D, (tPill - enter - hold) / exit));
                            wPill = Math.max(0.0D, Math.min(1.0D, (double)this.easeCube((float)(1.0D - p))));
                        }

                        int aPill = (int)Math.max(0L, Math.min(255L, Math.round(alpha * wPill * 255.0D)));
                        float scalePill = 0.94F + (float)(0.06D * wPill);
                        int baseR = status.equals("Enabled") ? 76 : 244;
                        int baseG = status.equals("Enabled") ? 175 : 67;
                        int baseB = status.equals("Enabled") ? 80 : 54;
                        if (aPill > 0) {
                            float cx = pillX + pillW / 2.0F;
                            float cy = ny + nh / 2.0F;
                            float pw = pillW * scalePill;
                            float ph = nh * scalePill;
                            float px = cx - pw / 2.0F;
                            float py = cy - ph / 2.0F;
                            Color pillColor = new Color(baseR, baseG, baseB, aPill);
                            RenderUtils.drawRoundedRect(e.getStack(), px, py, pw, ph, ph / 2.0F, pillColor.getRGB());
                            float padScaled = pillPadX * scalePill;
                            font.render(e.getStack(), status, (double)(px + padScaled), (double)(ny + (nh - ph) / 2.0F), new Color(255, 255, 255, aPill), true, ns);
                        }
                    }

                    curY += nh + spacing;
                    ++idx;
                }
            }
        }
    }

    private float computeNoticeVisualWidth(String msg, CustomTextRenderer font, double ns) {
        if (msg == null) {
            return 0.0F;
        } else if (msg.endsWith(" Enabled!")) {
            String moduleName = msg.substring(0, msg.length() - " Enabled!".length());
            return this.widthWithPill(moduleName, "Enabled", font, ns);
        } else if (msg.endsWith(" Disabled!")) {
            String moduleName = msg.substring(0, msg.length() - " Disabled!".length());
            return this.widthWithPill(moduleName, "Disabled", font, ns);
        } else {
            return font.getWidth(msg, ns);
        }
    }

    private float widthWithPill(String moduleName, String status, CustomTextRenderer font, double ns) {
        float gap = 6.0F;
        float modW = font.getWidth(moduleName, ns);
        float pillPadX = 6.0F;
        float pillTextW = font.getWidth(status, ns);
        float pillW = pillTextW + pillPadX * 2.0F;
        return modW + gap + pillW;
    }

    private float tyBase(float top, float h, float noticeH, CustomTextRenderer font) {
        double scale = (double)this.textScale.getCurrentValue();
        float textHeight = (float)font.getHeight(true, scale);
        float titleY = top + (h - textHeight) / 2.0F;
        return titleY + textHeight + this.noticeSpacing.getCurrentValue();
    }

    private static class Notice {
        final String msg;
        final long start;
        final long enterMs;
        final long holdMs;
        final long exitMs;
        boolean positionInitialized = false;
        float posStartIdx = 0.0F;
        float targetIdx = 0.0F;
        long posStartMs = 0L;

        Notice(String msg, long start, long enterMs, long holdMs, long exitMs) {
            this.msg = msg;
            this.start = start;
            this.enterMs = enterMs;
            this.holdMs = holdMs;
            this.exitMs = exitMs;
        }

        boolean isFinished(long now) {
            return now - this.start >= this.enterMs + this.holdMs + this.exitMs;
        }

        double alpha(long now) {
            long t = now - this.start;
            if (t <= this.enterMs) {
                double p = (double)t / (double)this.enterMs;
                double base = (new EaseOutExpo()).apply(p);
                double bounce = bounceSmall(p) * 0.08D;
                double v = base + bounce;
                return Math.max(0.0D, Math.min(1.0D, v));
            } else if (t <= this.enterMs + this.holdMs) {
                return 1.0D;
            } else {
                double p = (double)(t - this.enterMs - this.holdMs) / (double)this.exitMs;
                double base = (new EaseCube()).apply(1.0D - p);
                double bounce = bounceSmall(1.0D - p) * 0.06D;
                double v = base + bounce;
                return Math.max(0.0D, Math.min(1.0D, v));
            }
        }

        double offset(long now, float lineH) {
            long t = now - this.start;
            if (t <= this.enterMs) {
                double p = (double)t / (double)this.enterMs;
                double base = (double)(-lineH) * (1.0D - (new EaseOutExpo()).apply(p));
                double j = (double)lineH * 0.06D * bounceSmall(p);
                return base + j;
            } else if (t <= this.enterMs + this.holdMs) {
                return 0.0D;
            } else {
                double p = (double)(t - this.enterMs - this.holdMs) / (double)this.exitMs;
                double base = (double)(-lineH) * (new EaseCube()).apply(p);
                double j = (double)(-lineH) * 0.04D * bounceSmall(p);
                return base + j;
            }
        }

        double interpIndex(long now, int durMs) {
            if (!this.positionInitialized) {
                return (double)this.targetIdx;
            } else {
                long dt = Math.max(0L, now - this.posStartMs);
                double p = durMs <= 0 ? 1.0D : Math.min(1.0D, (double)dt / (double)durMs);
                double eased = (new EaseOutExpo()).apply(p);
                return (double)this.posStartIdx + (double)(this.targetIdx - this.posStartIdx) * eased;
            }
        }

        private static double bounceSmall(double x) {
            x = Math.max(0.0D, Math.min(1.0D, x));
            return Math.sin(x * Math.PI * 2.0D) * Math.pow(1.0D - x, 2.0D);
        }
    }
}