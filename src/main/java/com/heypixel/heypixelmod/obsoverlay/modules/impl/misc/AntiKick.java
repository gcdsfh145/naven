package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtilss;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketEvent2;
import com.heypixel.heypixelmod.obsoverlay.utils.WorldEvent;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;


@ModuleInfo(name = "AntiKick", description = "AntiKick", category = Category.MISC)
public class AntiKick extends Module {
    private final BooleanValue kickchat = ValueBuilder.create(this, "KickChat")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue kicknomove = ValueBuilder.create(this, "KickNoMove")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public boolean kick = false;
    public String skickchat = null;

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (this.kick && this.kicknomove.getCurrentValue()) {
            MoveUtilss.stopMove();
        }
    }

    public boolean isKickchatEnabled() {
        return kickchat.getCurrentValue();
    }
    @EventTarget
    private void onPacket(PacketEvent2 event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundPlayerPositionPacket && this.kick) {
            this.kick = false;
        }
    }

    @EventTarget
    private void onWorld(WorldEvent event) {
        this.kick = false;
        this.skickchat = null;
    }

    public void onDisabler() {
        this.kick = false;
        this.skickchat = null;
    }

    public void setKick(boolean kick) {
        this.kick = kick;
    }

    public void setSkickchat(String skickchat) {
        this.skickchat = skickchat;
    }
}
