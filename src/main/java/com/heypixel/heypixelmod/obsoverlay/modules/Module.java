package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.Config.Setting;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public abstract class Module extends HasValue {
   public static final Minecraft mc = Minecraft.getInstance();
   public static boolean update = true;
   private final SmoothAnimationTimer animation = new SmoothAnimationTimer(100.0F);
   private String name;
   private String prettyName;
   private String description;
   private String suffix;
   private Category category;
   private boolean enabled;
   private int minPermission = 0;
   private int key;

   private final List<Setting<?>> settings = new ArrayList<>();

   public Module(String name, String description, Category category) {
      this.name = name;
      this.description = description;
      this.category = category;
      super.setName(name);
      this.setPrettyName();
   }

   public void setEnabledWhenConfigChange(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            onEnable();
            Naven.getInstance().getEventManager().register(this);
         } else {
            onDisable();
            Naven.getInstance().getEventManager().unregister(this);
         }
         triggerConfigSave();
      }
   }

   public void setKey(int key) {
      if (this.key != key) {
         this.key = key;
         triggerConfigSave();
      }
   }

   public List<Setting<?>> getSettings() {
      return settings;
   }

   public Setting<?> findSetting(String name) {
      for (Setting<?> setting : settings) {
         if (setting.getName().replace(" ", "").equalsIgnoreCase(name)) {
            return setting;
         }
      }
      return null;
   }

   public void setSuffix(String suffix) {
      if (suffix == null) {
         this.suffix = null;
         update = true;
      } else if (!suffix.equals(this.suffix)) {
         this.suffix = suffix;
         update = true;
      }
   }

   private void setPrettyName() {
      StringBuilder builder = new StringBuilder();
      char[] chars = this.name.toCharArray();

      for (int i = 0; i < chars.length - 1; i++) {
         if (Character.isLowerCase(chars[i]) && Character.isUpperCase(chars[i + 1])) {
            builder.append(chars[i]).append(" ");
         } else {
            builder.append(chars[i]);
         }
      }

      builder.append(chars[chars.length - 1]);
      this.prettyName = builder.toString();
   }

   protected void initModule() {
      if (this.getClass().isAnnotationPresent(ModuleInfo.class)) {
         ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
         this.name = moduleInfo.name();
         this.description = moduleInfo.description();
         this.category = moduleInfo.category();
         super.setName(this.name);
         this.setPrettyName();
         Naven.getInstance().getHasValueManager().registerHasValue(this);
      }
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         try {
            Naven naven = Naven.getInstance();
            if (enabled) {
               this.enabled = true;
               naven.getEventManager().register(this);
               this.onEnable();
               if (!(this instanceof ClickGUIModule)) {
                  HUD module = (HUD) Naven.getInstance().getModuleManager().getModule(HUD.class);
                  if (module.moduleToggleSound.getCurrentValue()) {
                     mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.5F, 1.3F);
                  }

                  Notification notification = new Notification(NotificationLevel.SUCCESS, this.name + " Enabled!", 3000L);
                  naven.getNotificationManager().addNotification(notification);
               }
            } else {
               this.enabled = false;
               naven.getEventManager().unregister(this);
               this.onDisable();
               if (!(this instanceof ClickGUIModule)) {
                  HUD module = (HUD) Naven.getInstance().getModuleManager().getModule(HUD.class);
                  if (module.moduleToggleSound.getCurrentValue()) {
                     mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_OFF, 0.5F, 0.8F);
                  }

                  Notification notification = new Notification(NotificationLevel.ERROR, this.name + " Disabled!", 3000L);
                  naven.getNotificationManager().addNotification(notification);
               }
            }
            triggerConfigSave();
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   private void triggerConfigSave() {
      new Thread(() -> {
         try {
            Thread.sleep(1000);
            if (Naven.getInstance() != null && Naven.getInstance().getConfigManager() != null) {
               Naven.getInstance().getConfigManager().save();
               System.out.println("Auto-saved config after module change: " + this.name);
            }
         } catch (Exception e) {
            System.err.println("Failed to auto-save config: " + e.getMessage());
         }
      }).start();
   }

   public SmoothAnimationTimer getAnimation() {
      return this.animation;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public String getPrettyName() {
      return this.prettyName;
   }

   public String getDescription() {
      return this.description;
   }

   public String getSuffix() {
      return this.suffix;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public int getMinPermission() {
      return this.minPermission;
   }

   public int getKey() {
      return this.key;
   }

   public Module() {
   }

   public void setMinPermission(int minPermission) {
      this.minPermission = minPermission;
   }

   public boolean getState() {
      return enabled;
   }

   public void setState(boolean state) {
      this.enabled = state;
   }
}