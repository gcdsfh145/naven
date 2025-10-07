package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.collect.Lists;

import java.util.List;

@ModuleInfo(name = "Widget", description = "Widgets", category = Category.RENDER)
public class Widget extends Module {
    BooleanValue players = ValueBuilder.create(this, "Players").setDefaultBooleanValue(true).build().getBooleanValue();

    public List<DraggableWidget> widgets = Lists.newArrayList(
            new PlayerWidget(players)
    );

    public Widget() {
        super();
        for (DraggableWidget widget : widgets) {
             //EventManager.register(widget);
        }
    }

    public void onRender2D(PoseStack poseStack, float partialTicks) {
        for (DraggableWidget widget : widgets) {
            poseStack.pushPose();
            poseStack.translate(widget.getX().value, widget.getY().value, 0);
            widget.render();
            poseStack.popPose();
        }
    }

    public void onShaderRender(PoseStack poseStack, float partialTicks) {
        for (DraggableWidget widget : widgets) {
            poseStack.pushPose();
            poseStack.translate(widget.getX().value, widget.getY().value, 0);
            widget.renderShader();
            poseStack.popPose();
        }
    }

    public void processDrag(float mouseX, float mouseY, int mouseButton) {
        widgets.forEach(widget -> widget.processDrag(mouseX, mouseY, mouseButton));
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}