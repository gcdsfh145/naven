package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketEvent2;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraftforge.event.TickEvent;

@ModuleInfo(
        name = "Ambience",
        description = "Ambience",
        category = Category.RENDER
)
public class Ambience extends Module {
    public ModeValue timeModeValue = ValueBuilder.create(this, "TimeMode")
            .setDefaultModeIndex(0)
            .setModes("Static", "Cycle")
            .build()
            .getModeValue();
    private final FloatValue rainStrengthValue = ValueBuilder.create(this, "RainStrength")
            .setDefaultFloatValue(0.1F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.01F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue worldTimeValue = ValueBuilder.create(this, "LivingTimeTicks")
            .setDefaultFloatValue(12000)
            .setFloatStep(100.0F)
            .setMinFloatValue(12000)
            .setMaxFloatValue(24000)
            .build()
            .getFloatValue();
    private final FloatValue cycleSpeedValue = ValueBuilder.create(this, "CycleSpeed")
            .setDefaultFloatValue(1)
            .setFloatStep(1.0F)
            .setMinFloatValue(-14)
            .setMaxFloatValue(24)
            .build()
            .getFloatValue();
    public ModeValue weatherModeValue = ValueBuilder.create(this, "WeatherMode")
            .setDefaultModeIndex(0)
            .setModes("Clear", "Rain", "NoModification")
            .build()
            .getModeValue();
    BooleanValue displayTag = ValueBuilder.create(this, "Display-Tag").setDefaultBooleanValue(true).build().getBooleanValue();
    private long timeCycle = 0L;

    public void onEnabled() {
       this.timeCycle = 0L;
    }
    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix((displayTag.getCurrentValue() ? "Time: " + (timeModeValue.getCurrentMode().equalsIgnoreCase("cycle") ? "Cycle" + (cycleSpeedValue.getCurrentValue() > 0 ? ", Reverse" : "") : "Static, " + worldTimeValue.getCurrentValue()) + " | Weather: " + weatherModeValue.getCurrentMode() : null));}

    @EventTarget
    private void onPacket(PacketEvent2 event) {
        if (event.getPacket() instanceof ClientboundSetTimePacket) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    private void onTick(TickEvent event) {
        if (mc.level != null) {
            if (this.timeModeValue.getCurrentMode().equalsIgnoreCase("Static")) {
                mc.level.setDayTime((long) this.worldTimeValue.getCurrentValue());
            } else {
                mc.level.setDayTime(this.timeCycle);
                this.timeCycle = this.timeCycle + (long) this.cycleSpeedValue.getCurrentValue() * 10L;
                if (this.timeCycle >= 24000L) {
                    this.timeCycle = 0L;
                } else if (this.timeCycle < 0L) {
                    this.timeCycle = 24000L;
                }
            }

            if (!this.weatherModeValue.getCurrentMode().equalsIgnoreCase("NoModification")) {
                mc.level.setRainLevel(this.weatherModeValue.getCurrentMode().equalsIgnoreCase("Clear") ? 0.0F : this.rainStrengthValue.getCurrentValue());
            }
        }
    }
}