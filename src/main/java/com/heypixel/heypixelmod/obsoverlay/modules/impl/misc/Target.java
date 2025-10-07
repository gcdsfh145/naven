package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;

@ModuleInfo(name = "Target", description = "Target", category = Category.MISC)
public class Target extends Module {
    public final BooleanValue targetDead = ValueBuilder.create(this, "TargetDead")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public final BooleanValue targetAnimals = ValueBuilder.create(this, "TargetAnimals")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public final BooleanValue targetMobs = ValueBuilder.create(this, "TargetMobs")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public final BooleanValue targetPlayer = ValueBuilder.create(this, "TargetPlayer")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public final BooleanValue targetInvisible = ValueBuilder.create(this, "TargetInvisible")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public void onEnabled() {
        this.setState(false);
    }
}
