package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStrafe;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import jnic.JNICInclude;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@JNICInclude
@ModuleInfo(name="NoFall", category=Category.MOVEMENT, description="Prevent fall damage")
public class NoFall
        extends Module {
    FloatValue distance = ValueBuilder.create(this, "Fall Distance").setDefaultFloatValue(3.0f).setFloatStep(0.1f).setMinFloatValue(3.0f).setMaxFloatValue(15.0f).build().getFloatValue();
    public double preFallDistance;
    private boolean lagged = false;
    public static boolean doSex = false;
    private boolean sendLag = false;
    public static boolean jump = false;

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    private void reset() {
        this.lagged = false;
        doSex = false;
        this.sendLag = false;
        jump = false;
    }

    private boolean shouldBlockJump() {
        return doSex || jump;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() == EventType.POST || mc.player == null) {
            return;
        }
        if (this.shouldBlockJump()) {
            mc.options.keyJump.setDown(false);
        }
        this.preFallDistance = mc.player.onGround() ? 0.0 : (double)mc.player.fallDistance;
        if (this.lagged && doSex) {
            jump = true;
            doSex = false;
            this.lagged = false;
        }
    }

    @EventTarget
    public void onLivingUpdate(EventUpdate event) {
        if (this.shouldBlockJump() && mc.options != null) {
            mc.options.keyJump.setDown(false);
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (mc.player.onGround() && jump) {
            mc.player.jumpFromGround();
            jump = false;
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (this.shouldBlockJump()) {
            event.setJump(false);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.POST) {
            return;
        }
        if (!doSex && mc.player.fallDistance > this.distance.getCurrentValue() && !event.isOnGround()) {
            doSex = true;
            this.lagged = false;
            this.sendLag = false;
        }
        if (doSex && mc.player.fallDistance < 3.0f) {
            event.setOnGround(false);
            if (!this.sendLag) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(event.getX() - 1000.0, event.getY(), event.getZ(), false));
                this.sendLag = true;
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND) {
            if (doSex && this.sendLag && !this.lagged && event.getPacket() instanceof ServerboundMovePlayerPacket) {
                event.setCancelled(true);
            }
        } else if (doSex && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.lagged = true;
        }
    }
}