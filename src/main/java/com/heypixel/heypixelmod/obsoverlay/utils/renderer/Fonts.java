package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import java.awt.FontFormatException;
import java.io.IOException;

public class Fonts {
   public static CustomTextRenderer opensans;
   public static CustomTextRenderer harmony;
   public static CustomTextRenderer icons;
   public static CustomTextRenderer naven;
   public static CustomTextRenderer nitro;
   public static CustomTextRenderer vanilla;
   public static CustomTextRenderer southside;
   public static CustomTextRenderer NewNaven;
   public static void loadFonts() throws IOException, FontFormatException {
      opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
      harmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
      icons = new CustomTextRenderer("icon", 32, 59648, 59652, 512);
      nitro = new CustomTextRenderer("nitro", 32, 0, 65535, 16384);
      vanilla = new CustomTextRenderer("Minecraft-Regular", 32, 0, 65535, 16384);
      naven = new CustomTextRenderer("Galaxy", 32, 0, 65535, 16384);
      southside = new CustomTextRenderer("wqy_microhei", 32, 0, 65535, 16384);
      NewNaven = new CustomTextRenderer("regular", 32, 0, 65535, 16384);
   }
}
