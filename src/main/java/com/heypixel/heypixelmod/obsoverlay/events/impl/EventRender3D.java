package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.eventbus.api.Event;

public class EventRender3D extends Event {

    private final float partialTicks;
    private final PoseStack poseStack;
    private boolean isUsingShaders;


    public EventRender3D(float partialTicks, PoseStack poseStack) {
        this.partialTicks = partialTicks;
        this.poseStack = poseStack;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public boolean isUsingShaders() {
        return this.isUsingShaders;
    }

    public void setUsingShaders(boolean usingShaders) {
        this.isUsingShaders = usingShaders;
    }
}