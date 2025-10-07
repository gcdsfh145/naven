package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

@ModuleInfo(
        name = "Teams",
        description = "Prevent attack teammates",
        category = Category.MISC
)
public class Teams extends Module {
   public static Teams instance;
   private static final Minecraft mc = Minecraft.getInstance();

   public ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Scoreboard", "Color", "HeypixelBW", "HeypixelSW")
           .build()
           .getModeValue();

   public Teams() {
      instance = this;
   }

   public static boolean isSameTeam (Entity entity) {
      if (!Naven.getInstance().getModuleManager().getModule(Teams.class).isEnabled()) {
         return false;
      } else if (!(entity instanceof Player)) {
         return false;
      }

      switch (instance.mode.getCurrentMode()) {
         case "Scoreboard":
            String playerTeam = getTeam(entity);
            String targetTeam = getTeam(mc.player);
            return Objects.equals(playerTeam, targetTeam);

         case "Color":
            Integer c1 = entity.getTeam() != null ? entity.getTeam().getColor().getColor() : null;
            Integer c2 = mc.player.getTeam() != null ? mc.player.getTeam().getColor().getColor() : null;
            return Objects.equals(c1, c2);

         case "HeypixelBW":
            if (mc.player == null) return false;
            String[] astring = entity.getDisplayName().getString().split(" ");
            String[] astring1 = mc.player.getDisplayName().getString().split(" ");
            return astring.length > 1 && astring1.length > 1 && astring[0].equals(astring1[0]);

         case "HeypixelSW":
            if (mc.player == null) return false;
            return entity.getDisplayName().getStyle().getColor() != null &&
                    entity.getDisplayName().getStyle().getColor().equals(mc.player.getDisplayName().getStyle().getColor());

         default:
            return false;
      }
   }

   @EventTarget
   public void onEnable() {
      super.onEnable();
   }

   public static String getTeam(Entity entity) {
      if (mc.getConnection() == null) return null;

      PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entity.getUUID());
      if (playerInfo == null) {
         return null;
      } else {
         return playerInfo.getTeam() != null ? playerInfo.getTeam().getName() : null;
      }
   }
}