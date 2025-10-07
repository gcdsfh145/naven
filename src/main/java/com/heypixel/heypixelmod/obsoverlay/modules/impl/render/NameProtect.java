package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.network.chat.Component;

@ModuleInfo(
        name = "NameProtect",
        description = "You can .setname Protect your name",
        category = Category.RENDER
)
public class NameProtect extends Module {
    public static NameProtect instance;

    // 自定义名字字段，支持带有颜色
    private String customName = "§d少羽§7";  // 默认名字带颜色

    public NameProtect() {
        instance = this;
    }

    // 设置自定义名字，支持颜色
    public void setCustomName(String name) {
        this.customName = name;
    }

    // 获取替换的名字，保持颜色
    public static String getName(String string) {
        if (!instance.isEnabled() || mc.player == null) {
            return string;
        } else {
            // 替换名字时保持颜色
            return string.contains(mc.player.getName().getString()) ?
                    StringUtils.replace(string, mc.player.getName().getString(), "§d"+ instance.customName +"§7") : string;
        }
    }

    @EventTarget
    public void onRenderTab(EventRenderTabOverlay e) {
        // 在Tab中渲染时应用自定义名字
        e.setComponent(Component.literal(getName(e.getComponent().getString())));
    }
}