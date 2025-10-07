package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraftforge.eventbus.api.Event;

public class MoveInputEvent extends Event {
    private float Forward;
    private float Strafe;

    public MoveInputEvent(float Forward, float Strafe) {
        this.Forward = Forward;
        this.Strafe = Strafe;
    }

    public float getForward() {
        return this.Forward;
    }

    public void setForward(float forward) {
        this.Forward = forward;
    }

    public float getStrafe() {
        return this.Strafe;
    }

    public void setStrafe(float strafe) {
        this.Strafe = strafe;
    }
}

