package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;

@ModuleInfo(name="LowFire", description="Show the fire lower.", category=Category.RENDER)
public class LowFire
        extends Module {
    public static LowFire instance;

    public LowFire() {
        instance = this;
    }
}
