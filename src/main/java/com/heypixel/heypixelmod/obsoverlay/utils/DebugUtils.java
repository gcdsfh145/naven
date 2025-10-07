package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

public class DebugUtils {
   private static final Minecraft MC = Minecraft.getInstance();
   private static final String PREFIX = "§7[§b" + Naven.CLIENT_NAME + "§7] ";
   private static final String MSG_COLOR = "§f";

   private static final String WARNING_COLOR = "§e";  // 黄色
   private static final String ERROR_COLOR = "§c";    // 红色
   private static final String DEBUG_COLOR = "§a";    // 绿色
   private static final String MESSAGE_COLOR = "§b";  // 青色

   public static void component(Component component) {
      ChatComponent chat = MC.gui.getChat();
      chat.addMessage(component);
   }

   public static void addChatMessage(String message) {
      component(Component.literal(message));
   }

   private static String getModuleName(Module module) {
      return (module != null) ? module.getName() : "";
   }

   public static void Warning(Module module, String message) {
      addChatMessage(PREFIX + WARNING_COLOR + "[" + getModuleName(module) + "]" + "[Warning] " + MSG_COLOR + message);
   }

   public static void ERROR(Module module, String message) {
      addChatMessage(PREFIX + ERROR_COLOR + "[" + getModuleName(module) + "[ERROR] " + MSG_COLOR + message);
   }

   public static void Debug(Module module, String message) {
      addChatMessage(PREFIX + DEBUG_COLOR + "[" + getModuleName(module) + "]" + "[Debug] " + MSG_COLOR + message);
   }

   public static void Message(Module module, String message) {
      addChatMessage(PREFIX + MESSAGE_COLOR + "[" + getModuleName(module) + "]" + "[Message] " + MSG_COLOR + message);
   }

   public static void Msg(String message, String Module) {
      addChatMessage(PREFIX + "[" + Module + "]"  + MSG_COLOR + message);
   }
}