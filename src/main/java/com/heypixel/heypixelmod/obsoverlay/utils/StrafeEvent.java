package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraftforge.eventbus.api.Event;

public class StrafeEvent extends Event {
    private float yaw;

    public StrafeEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
