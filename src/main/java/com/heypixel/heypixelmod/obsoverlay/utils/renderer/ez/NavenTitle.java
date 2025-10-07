package com.heypixel.heypixelmod.obsoverlay.utils.renderer.ez;

import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;

public class NavenTitle {
    public String getTitle() {
        CustomTextRenderer font = Fonts.harmony;
        final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String Title = "ShaoYu | " + Version.getVersion() + " | ShaoYuNuiBi | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());
        return Title;
    }
}