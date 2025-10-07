package com.heypixel.heypixelmod.obsoverlay.utils;


import net.minecraftforge.eventbus.api.Event;

import javax.swing.plaf.nimbus.State;

public class MotionEvent extends Event {
    private final Type type;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround, Type type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.type = type;
    }

    public double getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = (double) x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = (double) y;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(float z) {
        this.z = (double) z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public Type getType() {
        return this.type;
    }
    public State state;
    public boolean isPost() {
        return state.equals(State.POST);
    }
    public enum State {
        PRE,
        POST
    }
    public static enum Type {
        Pre,
        Post;
    }
}
