package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ModuleInfo(name = "AutoHUB", description = "Automatically sends /hub command when H key is pressed", category = Category.MISC)
public class AutoHUB extends Module {
    private static final Minecraft mc = Minecraft.getInstance();
    private boolean wasHKeyPressed = false;

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (!this.isEnabled() || mc.player == null || mc.level == null) return;

        boolean isHKeyPressed = event.getKey() == 72;

        if (isHKeyPressed && !wasHKeyPressed) {
            mc.getConnection().sendCommand("hub");
        }

        wasHKeyPressed = isHKeyPressed;
    }
}