package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Config.SwitchModuleConfig;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

import java.io.File;
import java.util.Arrays;

@CommandInfo(
        name = "config",
        description = "Manage client configurations.",
        aliases = {"cfg"}
)
public class CommandConfig extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 0) {
         ChatUtils.addChatMessage("Usage: .config/cfg <create/list/load/save/delete> <name>");
         return;
      }

      String operation = args[0].toLowerCase();

      switch (operation) {
         case "list":
            listConfigs();
            break;
         case "load":
         case "save":
         case "create":
         case "delete":
            if (args.length < 2) {
               ChatUtils.addChatMessage("Usage: .config/cfg " + operation + " <name>");
               return;
            }
            handleConfigOperation(operation, args[1]);
            break;
         default:
            ChatUtils.addChatMessage("Usage: .config/cfg <create/list/load/save/delete> <name>");
      }
   }

   private void listConfigs() {
      try {
         File configDir = Naven.getInstance().getConfigManager().getConfigFile();
         if (configDir == null) {
            ChatUtils.addChatMessage("Config directory is null!");
            return;
         }

         if (!configDir.exists()) {
            ChatUtils.addChatMessage("Config directory does not exist: " + configDir.getAbsolutePath());
            return;
         }

         File[] files = configDir.listFiles((dir, name) -> name.endsWith(".ini"));

         if (files == null) {
            ChatUtils.addChatMessage("Failed to list config files");
            return;
         }

         if (files.length == 0) {
            ChatUtils.addChatMessage("No configs found in: " + configDir.getAbsolutePath());
            return;
         }

         StringBuilder sb = new StringBuilder("Configs (" + files.length + "): ");
         for (int i = 0; i < files.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(files[i].getName().replace(".ini", ""));
         }
         ChatUtils.addChatMessage(sb.toString());
      } catch (Exception e) {
         ChatUtils.addChatMessage("Error listing configs: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private void handleConfigOperation(String operation, String configName) {
      try {
         File configDir = Naven.getInstance().getConfigManager().getConfigFile();
         if (configDir == null) {
            ChatUtils.addChatMessage("Config directory is null!");
            return;
         }

         if (!configDir.exists()) {
            ChatUtils.addChatMessage("Config directory does not exist: " + configDir.getAbsolutePath());
            return;
         }

         File configFile = new File(configDir, configName + ".ini");
         System.out.println("Config operation '" + operation + "' on file: " + configFile.getAbsolutePath());

         SwitchModuleConfig switchModuleConfig = new SwitchModuleConfig(configFile);

         switch (operation) {
            case "load":
               if (!configFile.exists()) {
                  ChatUtils.addChatMessage("Config '" + configName + "' does not exist at: " + configFile.getAbsolutePath());
                  return;
               }
               switchModuleConfig.read();
               ChatUtils.addChatMessage("Loaded config '" + configName + "' successfully and updated main configuration.");
               break;

            case "save":
            case "create":
               if (switchModuleConfig.write()) {
                  ChatUtils.addChatMessage(operation + "d config '" + configName + "' successfully and updated main configuration.");
               } else {
                  ChatUtils.addChatMessage("Failed to " + operation + " config '" + configName + "'.");
               }
               break;

            case "delete":
               if (!configFile.exists()) {
                  ChatUtils.addChatMessage("Config '" + configName + "' does not exist!");
                  return;
               }
               if (configFile.delete()) {
                  ChatUtils.addChatMessage("Deleted config '" + configName + "' successfully.");
               } else {
                  ChatUtils.addChatMessage("Failed to delete config '" + configName + "'.");
               }
               break;
         }
      } catch (Throwable ex) {
         System.err.println("Error in config operation '" + operation + "':");
         ex.printStackTrace();
         ChatUtils.addChatMessage("Error: failed to " + operation + " config '" + configName + "'. Check console for details.");
      }
   }

   @Override
   public String[] onTab(String[] args) {
      if (args.length == 1) {
         return new String[]{"create", "list", "load", "save", "delete"};
      } else if (args.length == 2 && !args[0].equals("create")) {
         try {
            File configDir = Naven.getInstance().getConfigManager().getConfigFile();
            if (configDir != null && configDir.exists()) {
               File[] files = configDir.listFiles((dir, name) -> name.endsWith(".ini"));
               if (files != null) {
                  return Arrays.stream(files)
                          .map(file -> file.getName().replace(".ini", ""))
                          .toArray(String[]::new);
               }
            }
         } catch (Exception e) {
            System.err.println("Error in tab completion: " + e.getMessage());
         }
      }
      return new String[0];
   }
}