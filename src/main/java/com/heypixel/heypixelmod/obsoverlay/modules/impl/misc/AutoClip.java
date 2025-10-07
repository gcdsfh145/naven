package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;

@ModuleInfo(name = "AutoClip", description = "AutoClip", category = Category.MISC)
public class AutoClip extends Module {
    BooleanValue AutoDisablerFly = ValueBuilder.create(this, "AutoDisablerFly").setDefaultBooleanValue(true).build().getBooleanValue();
    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player.getAbilities().flying) {
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 4.0, mc.player.getZ());
            if (this.AutoDisablerFly.getCurrentValue()) {
                mc.player.getAbilities().flying = false;
            }
        }
    }
}
