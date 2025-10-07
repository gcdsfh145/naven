package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "AutoDisMode", description = "自动关闭模块", category = Category.MISC)
public class AutoDisMode extends Module {
    private final List<Class<? extends Module>> moduleList = new ArrayList<>();
    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onEnable() {
        moduleList.add(Scaffold.class);
        moduleList.add(Aura.class);
        moduleList.add(ChestStealer.class);
        moduleList.add(InventoryCleaner.class);
        moduleList.add(Stuck.class);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isRemoved() || mc.player.isSpectator()) {
            dis();
        }
    }

    public void onPacket(Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            String message = chatPacket.content().getString();
            if (message.contains("恭喜你获得胜利!") || message.contains("你现在是观察者!")) {
                dis();
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        dis();
    }
    public void dis() {
        com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager moduleManager = Naven.getInstance().getModuleManager();

        try {
            Module chestStealer = moduleManager.getModule(ChestStealer.class);
            if (chestStealer.isEnabled()) {
                chestStealer.setEnabled(false);
            }
        } catch (Exception e) {
        }

        for (Class<? extends Module> moduleClass : moduleList) {
            try {
                Module module = moduleManager.getModule(moduleClass);
                if (module.isEnabled()) {
                    module.setEnabled(false);
                }
            } catch (Exception e) {
            }
        }
    }
}