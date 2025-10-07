package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;

public class StringUtils {
   public static boolean containChinese(String str) {
      if (str == null || str.isEmpty()) {
         return false;
      }
      for (char c : str.toCharArray()) {
         if (c <= '一') continue;
         return true;
      }
      return false;
   }

   public static Component replaceInComponent(Component original, String target, String replacement) {
      ComponentContents componentContents = original.getContents();
      if (componentContents instanceof LiteralContents) {
         LiteralContents literal = (LiteralContents) componentContents;
         String newText = literal.text().replace(target, replacement);
         return Component.literal(newText).setStyle(original.getStyle());
      }
      MutableComponent newComponent = Component.empty().setStyle(original.getStyle());
      for (Component child : original.getSiblings()) {
         newComponent.append(replaceInComponent(child, target, replacement));
      }
      return newComponent;
   }
}