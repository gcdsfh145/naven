package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
        name = "Cape",
        description = "Cape",
        category = Category.RENDER
)
public class Cape extends Module {
    public ModeValue modeValue = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("LiquidBounce","LuoTY","HuaXL")
            .build()
            .getModeValue();
    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }
}