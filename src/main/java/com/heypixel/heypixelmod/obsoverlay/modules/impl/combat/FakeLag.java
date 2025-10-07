package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraftforge.event.TickEvent;

import java.util.concurrent.LinkedBlockingQueue;

@ModuleInfo(name = "FakeLag", description = "FakeLag", category = Category.COMBAT)
public class FakeLag extends Module {
    BooleanValue NoC0F = ValueBuilder.create(this, "NoC0F").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue NodeLay = ValueBuilder.create(this, "NodeLay").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue TickLowMove = ValueBuilder.create(this, "TickLowMove").setDefaultBooleanValue(true).build().getBooleanValue();

    IntValue Delay = ValueBuilder.create(this, "Delay")
            .setDefaultIntValue(2)
            .setMinIntValue(0)
            .setMaxIntValue(10)
            .build()
            .getIntValue();

    IntValue maxPacketValue = ValueBuilder.create(this, "MaxPacket")
            .setDefaultIntValue(15)
            .setMinIntValue(0)
            .setMaxIntValue(50)
            .build()
            .getIntValue();

    private final MSTimer pulseTimer = new MSTimer();
    private float delay = 0.0F;
    private final LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue<>();

    public void onEnabled() {
        if (mc.player != null) {
            this.pulseTimer.reset();
        }
    }

    public void onDisabler() {
        if (mc.player != null) {
            while (!this.packets.isEmpty()) {
                PacketUtil.sendPacket(this.packets.poll());
            }
        }
    }

    @EventTarget
    private void onPacket(PacketEvent2 event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket) {
            event.setCancelled(true);
        }

        if (packet instanceof ServerboundMovePlayerPacket.Pos
                || packet instanceof ServerboundMovePlayerPacket.PosRot
                || packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundSwingPacket
                || packet instanceof ServerboundPlayerCommandPacket
                || packet instanceof ServerboundPlayerActionPacket
                || packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundSetCarriedItemPacket
                || packet instanceof ServerboundContainerClickPacket
                || packet instanceof ServerboundPongPacket && this.NoC0F.getCurrentValue()) {
            event.setCancelled(true);
            this.packets.add(packet);
        }

        if (packet instanceof ClientboundPlayerPositionPacket) {
            this.setState(false);
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        this.delay++;
    }

    @EventTarget
    private void onTick(TickEvent event) {
        if (this.NodeLay.getCurrentValue() && this.packets.size() >= this.maxPacketValue.getCurrentValue() && !this.packets.isEmpty()) {
            PacketUtil.sendPacket(this.packets.poll());
        }

        if (this.TickLowMove.getCurrentValue() && !this.packets.isEmpty() && this.delay >= (float) this.Delay.getCurrentValue()) {
            PacketUtil.sendPacket(this.packets.poll());
            this.delay--;
        }
    }

    public String getTag() {
        return String.valueOf(this.packets.size());
    }

    @EventTarget
    private void onWorld(WorldEvent event) {
        this.setState(false);
    }
}