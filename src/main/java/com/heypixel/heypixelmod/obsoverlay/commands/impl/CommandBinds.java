package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

@CommandInfo(
        name = "binds",
        description = "Review the key bindings",
        aliases = {"b", "keys"}
)
public class CommandBinds extends Command {
    @Override
    public void onCommand(String[] args) {
        ChatUtils.addChatMessage("Key Binds:");

        for (Module module : Naven.getInstance().getModuleManager().getModules()) {
            int keyCode = module.getKey();
            if (keyCode != 0 && keyCode != InputConstants.UNKNOWN.getValue()) {
                String keyName = getKeyString(keyCode);
                ChatUtils.addChatMessage(module.getName() + ": " + keyName);
            }
        }
    }

    private String getKeyString(int keyCode) {
        String keyName = GLFW.glfwGetKeyName(keyCode, GLFW.glfwGetKeyScancode(keyCode));
        if (keyName == null) {
            InputConstants.Key key = InputConstants.getKey(keyCode, 0);
            if (key != null && key != InputConstants.UNKNOWN) {
                keyName = key.getDisplayName().getString();
            }
        }
        return Objects.requireNonNullElse(keyName, "Unknown");
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}