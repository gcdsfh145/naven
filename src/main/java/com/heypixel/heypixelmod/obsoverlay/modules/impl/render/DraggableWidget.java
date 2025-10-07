package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public abstract class DraggableWidget {
    protected final static Minecraft mc = Minecraft.getInstance();
    private final static int headerColor = new Color(150, 45, 45, 255).getRGB();
    private final static int bodyColor = new Color(0, 0, 0, 190).getRGB();

    private boolean isDragging = false;
    protected final String name;
    protected SmoothAnimationTimer x = new SmoothAnimationTimer(0, 0.5f), y = new SmoothAnimationTimer(0, 0.5f);
    protected float dragX, dragY;

    public DraggableWidget(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SmoothAnimationTimer getX() {
        return x;
    }

    public SmoothAnimationTimer getY() {
        return y;
    }

    public void renderShader() {
        if (shouldRender()) {
            RenderUtils.drawBoundRoundedRect(0, 0, getWidth(), getHeight(), 5, 0xFFFFFFFF);
        }
    }

    public void renderBackground() {
        RenderUtils.drawRectBound(0, 0, getWidth(), 3, headerColor);
        RenderUtils.drawRectBound(0, 3, getWidth(), getHeight(), bodyColor);
    }

    public void render() {
        if (shouldRender()) {
            this.x.update(true);
            this.y.update(true);

            if (isDragging && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                Window window = mc.getWindow();
                double scaleFactor = window.getGuiScale();
                double mouseX = mc.mouseHandler.xpos() / scaleFactor;
                double mouseY = mc.mouseHandler.ypos() / scaleFactor;

                this.x.target = (float) mouseX - dragX;
                this.y.target = (float) mouseY - dragY;
            } else {
                isDragging = false;
            }
            StencilUtils.write(false);
            RenderUtils.drawBoundRoundedRect(0, 0, getWidth(), getHeight(), 5f, 0xFFFFFFFF);

            StencilUtils.erase(true);
            renderBackground();
            renderBody();
            StencilUtils.dispose();
        }
    }

    public abstract void renderBody();
    public abstract float getWidth();
    public abstract float getHeight();
    public abstract boolean shouldRender();

    public void processDrag(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && shouldRender()) {
            if (RenderUtils.isHoveringBound((int)mouseX, (int)mouseY, (int)x.value, (int)y.value, (int)getWidth(), (int)getHeight())) {
                isDragging = true;
                dragX = mouseX - x.value;
                dragY = mouseY - y.value;
            }
        }
    }
}