package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;

@ModuleInfo(name = "AntiBlind", description = "AntiBlind", category = Category.MISC)
public class AntiBlind extends Module {
    @EventTarget
    private void onTick(TickEvent event) {
        if (mc.player != null && mc.level != null) {
            for (MobEffectInstance mobeffectinstance : mc.player.getActiveEffects()) {
                if (mobeffectinstance.getEffect().equals(MobEffects.CONFUSION) || mobeffectinstance.getEffect().equals(MobEffects.BLINDNESS)) {
                    mc.player.removeEffect(mobeffectinstance.getEffect());
                }
            }
        }
    }
}
