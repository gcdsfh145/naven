package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import jnic.JNICInclude;

@JNICInclude
@ModuleInfo(
        name = "IQBooster",
        description = "Automatically attacks entities",
        category = Category.MISC)
public class IQBooster extends Module {
    private final FloatValue iqValue;
    private float lastValue;

    public IQBooster() {
        iqValue = ValueBuilder.create(this, "IQBooster")
                .setDefaultFloatValue(1337.0F)
                .setFloatStep(1.0F)
                .setMinFloatValue(1.0F)
                .setMaxFloatValue(1337.0F)
                .build()
                .getFloatValue();
        lastValue = iqValue.getCurrentValue();
    }

    private void updateSuffix() {
        setSuffix(String.valueOf(iqValue.getCurrentValue()));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateSuffix();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        setSuffix("");
    }
}