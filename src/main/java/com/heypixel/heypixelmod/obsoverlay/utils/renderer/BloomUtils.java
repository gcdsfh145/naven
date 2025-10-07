package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class BloomUtils {
    private static final TimeHelper timer = new TimeHelper();
    private static Framebuffer fboPing;
    private static Framebuffer fboPong;
    private static Framebuffer fboCapture;
    private static Shader blurShader;
    private static long lastWorldCaptureTick;
    private static boolean captureBegun;
    public static boolean worldCaptureEnabled;

    public static boolean isWorldCaptureEnabled() {
        return worldCaptureEnabled;
    }

    public static boolean beginWorldCapture() {
        if (!worldCaptureEnabled) {
            return false;
        }
        if (blurShader == null) {
            blurShader = new Shader("shadow.vert", "shadow.frag");
            fboPing = new Framebuffer();
            fboPong = new Framebuffer();
            fboCapture = new Framebuffer();
            PostProcessRenderer.init();
            System.out.println("[Bloom] init shader+fbos done (world capture)");
        }
        if (captureBegun) {
            return false;
        }
        fboCapture.resize();
        fboCapture.bind();
        fboCapture.setViewport();
        GlStateManager._clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GlStateManager._clear(16384, false);
        captureBegun = true;
        System.out.println("[Bloom] beginWorldCapture -> tex=" + BloomUtils.fboCapture.texture);
        return true;
    }

    public static void endWorldCapture() {
        if (!captureBegun) {
            return;
        }
        fboCapture.unbind();
        captureBegun = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            lastWorldCaptureTick = mc.level.getGameTime();
        }
        System.out.println("[Bloom] endWorldCapture (tick=" + lastWorldCaptureTick + ")");
    }

    public static void onRenderAfterWorld(EventRender2D e, float fps, int strength) {
        boolean DEBUG_VIS_MASK = false;
        Window window = Minecraft.getInstance().getWindow();

        if (blurShader == null) {
            blurShader = new Shader("shadow.vert", "shadow.frag");
            fboPing = new Framebuffer();
            fboPong = new Framebuffer();
            fboCapture = new Framebuffer();
            PostProcessRenderer.init();
            System.out.println("[Bloom] init shader+fbos done");
        }

        boolean shouldRefresh = false;
        if (timer.delay(1000.0 / (double)fps)) {
            shouldRefresh = true;
            timer.reset();
        }

        if (shouldRefresh) {
            if (strength < 1) {
                strength = 1;
            }
            System.out.println("[Bloom] refresh frame. fps=" + fps + ", strength=" + strength +
                    ", win=" + window.getWidth() + "x" + window.getHeight());

            fboPing.resize();
            fboPong.resize();
            boolean usedWorldCapture = false;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && lastWorldCaptureTick == mc.level.getGameTime()) {
                usedWorldCapture = true;
                System.out.println("[Bloom] using world-captured mask for this frame");

                fboCapture.bind();
                fboCapture.setViewport();
                GL.enableBlend();
                GlStateManager._blendFunc(1, 1);
                GL.disableDepth();

                Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), EventType.SHADOW));

                fboCapture.unbind();
                GL.enableDepth();
                GL.disableBlend();
                System.out.println("[Bloom] appended 2D SHADOW masks onto world-captured buffer");
            } else {
                fboCapture.resize();
                fboCapture.bind();
                fboCapture.setViewport();
                System.out.println("[Bloom] viewport capture=" + BloomUtils.fboCapture.width +
                        "x" + BloomUtils.fboCapture.height);

                GlStateManager._clearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GlStateManager._clear(16384, false);
                GL.disableDepth();
                GL.enableBlend();
                GlStateManager._blendFunc(1, 1);

                Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), EventType.SHADOW));

                int errAfterMask = GL11.glGetError();
                if (errAfterMask != 0) {
                    System.out.println("[Bloom] GL error after mask: " + errAfterMask);
                }

                fboCapture.unbind();
                GL.enableDepth();
                GL.disableBlend();
                System.out.println("[Bloom] mask captured (2D fallback) -> tex=" +
                        BloomUtils.fboCapture.texture + ", size=" +
                        BloomUtils.fboCapture.width + "x" + BloomUtils.fboCapture.height);
            }
        }

        if (DEBUG_VIS_MASK) {
            GL.enableBlend();
            GlStateManager._blendFunc(1, 1);
            GL.bindTexture(BloomUtils.fboCapture.texture);

            RenderSystem.setShader(new Supplier<ShaderInstance>() {
                @Override
                public ShaderInstance get() {
                    return RenderSystem.getShader();
                }
            });

            PostProcessRenderer.beginRender(e.getStack());
            PostProcessRenderer.render(e.getStack());
            PostProcessRenderer.endRender();

            GlStateManager._blendFunc(770, 771);
            GL.disableBlend();
            System.out.println("[Bloom] DEBUG overlay: showing raw capture");
        }

        GL.enableBlend();
        blurShader.bind();
        blurShader.set("u_Size", (double)window.getWidth(), (double)window.getHeight());
        PostProcessRenderer.beginRender(e.getStack());

        if (shouldRefresh) {
            fboPing.bind();
            fboPing.setViewport();
            System.out.println("[Bloom] viewport ping=" + BloomUtils.fboPing.width +
                    "x" + BloomUtils.fboPing.height);

            GL.bindTexture(BloomUtils.fboCapture.texture);
            blurShader.set("u_Direction", 1.0, 0.0);
            PostProcessRenderer.render(e.getStack());

            fboPong.bind();
            fboPong.setViewport();
            System.out.println("[Bloom] viewport pong=" + BloomUtils.fboPong.width +
                    "x" + BloomUtils.fboPong.height);

            GL.bindTexture(BloomUtils.fboPing.texture);
            blurShader.set("u_Direction", 0.0, 1.0);
            PostProcessRenderer.render(e.getStack());

            int extraPairs = Math.max(0, strength - 1);
            System.out.println("[Bloom] extra blur pairs=" + extraPairs);

            for (int i = 0; i < extraPairs; ++i) {
                fboPing.bind();
                fboPing.setViewport();
                GL.bindTexture(BloomUtils.fboPong.texture);
                blurShader.set("u_Direction", 1.0, 0.0);
                PostProcessRenderer.render(e.getStack());

                fboPong.bind();
                fboPong.setViewport();
                GL.bindTexture(BloomUtils.fboPing.texture);
                blurShader.set("u_Direction", 0.0, 1.0);
                PostProcessRenderer.render(e.getStack());
            }

            fboPong.unbind();
            System.out.println("[Bloom] blur finished -> tex=" + BloomUtils.fboPong.texture +
                    ", size=" + BloomUtils.fboPong.width + "x" + BloomUtils.fboPong.height);
        }

        RenderSystem.setShader(new Supplier<ShaderInstance>() {
            @Override
            public ShaderInstance get() {
                return RenderSystem.getShader();
            }
        });

        GL.enableBlend();
        GlStateManager._blendFunc(1, 1);
        GL.bindTexture(BloomUtils.fboPong.texture);
        PostProcessRenderer.render(e.getStack());

        int errAfterComposite = GL11.glGetError();
        if (errAfterComposite != 0) {
            System.out.println("[Bloom] GL error after composite: " + errAfterComposite);
        }

        System.out.println("[Bloom] composite pass done");
        GlStateManager._blendFunc(770, 771);
        GL.disableBlend();
        PostProcessRenderer.endRender();
    }

    static {
        lastWorldCaptureTick = -1L;
        captureBegun = false;
        worldCaptureEnabled = false;
    }
}