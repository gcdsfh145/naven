package com.heypixel.heypixelmod.obsoverlay.files;

import com.heypixel.heypixelmod.obsoverlay.files.impl.CGuiFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.FriendFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.KillSaysFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ModuleFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ProxyFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.SpammerFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ValueFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileManager {
   public static final Logger logger = LogManager.getLogger(FileManager.class);
   public static File clientFolder;
   public static Object trash = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
   private final List<ClientFile> files = new ArrayList<>();

   public FileManager() {
      initializeClientFolder();

      if (!clientFolder.exists() && clientFolder.mkdir()) {
         logger.info("Created client folder!");
      }

      this.files.add(new KillSaysFile());
      this.files.add(new SpammerFile());
      //this.files.add(new ModuleFile());
      this.files.add(new ValueFile());
      this.files.add(new CGuiFile());
      this.files.add(new ProxyFile());
      this.files.add(new FriendFile());
   }

   private void initializeClientFolder() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            File gameDirectory = mc.gameDirectory;
            logger.info("Game directory: " + gameDirectory.getAbsolutePath());
            File versionsFolder = findVersionsFolder(gameDirectory);

            if (versionsFolder != null && versionsFolder.exists()) {
               clientFolder = new File(versionsFolder, "ShaoYuNaven");
            } else {
               clientFolder = new File(gameDirectory, "ShaoYuNaven");
            }

            logger.info("Client folder set to: " + clientFolder.getAbsolutePath());
         } else {
            throw new RuntimeException("Minecraft instance is null");
         }
      } catch (Exception e) {
         logger.warn("Failed to get Minecraft instance, using fallback path: " + e.getMessage());
         clientFolder = new File(System.getProperty("user.home"), "ShaoYuNaven");
      }
   }

   private File findVersionsFolder(File gameDir) {
      if (isVersionFolder(gameDir)) {
         return gameDir;
      }

      File parentDir = gameDir.getParentFile();
      if (parentDir != null) {
         File versionsDir = new File(parentDir, "versions");
         if (versionsDir.exists() && versionsDir.isDirectory()) {
            return findCurrentVersionFolder(versionsDir, gameDir.getName());
         }
      }

      return null;
   }

   public File getConfigDirectory() {
      return clientFolder;
   }
   private boolean isVersionFolder(File dir) {
      File modsDir = new File(dir, "mods");
      File configDir = new File(dir, "config");
      return modsDir.exists() || configDir.exists();
   }

   private File findCurrentVersionFolder(File versionsDir, String currentDirName) {
      File versionFolder = new File(versionsDir, currentDirName);
      if (versionFolder.exists() && versionFolder.isDirectory()) {
         return versionFolder;
      }

      File[] subDirs = versionsDir.listFiles(File::isDirectory);
      if (subDirs != null && subDirs.length > 0) {
         for (File subDir : subDirs) {
            if (isVersionFolder(subDir)) {
               return subDir;
            }
         }
         return subDirs[0];
      }

      return null;
   }

   public void load() {
      for (ClientFile clientFile : this.files) {
         File file = clientFile.getFile();

         try {
            if (!file.exists() && file.createNewFile()) {
               logger.info("Created file " + file.getName() + "!");
               this.saveFile(clientFile);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
            clientFile.read(reader);
            reader.close();
         } catch (IOException var5) {
            logger.error("Failed to load file " + file.getName() + "!", var5);
            this.saveFile(clientFile);
         }
      }
   }

   public void save() {
      for (ClientFile clientFile : this.files) {
         this.saveFile(clientFile);
      }

      logger.info("Saved all files!");
   }

   private void saveFile(ClientFile clientFile) {
      File file = clientFile.getFile();

      try {
         if (!file.exists() && file.createNewFile()) {
            logger.info("Created file " + file.getName() + "!");
         }

         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
         clientFile.save(writer);
         writer.flush();
         writer.close();
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }
}