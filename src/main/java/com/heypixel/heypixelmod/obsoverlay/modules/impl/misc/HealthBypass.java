package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventTick;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

@ModuleInfo(
        name = "HealthBypass",
        description = "HealthBypass",
        category = Category.RENDER
)
public class HealthBypass extends Module {
    @EventTarget
    public void onTick(EventTick eventTick) {
        if (mc.player == null || mc.level == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != mc.player) {
                float health = getHealth(player);

                if (health == 0 && !player.isRemoved()) continue;

                player.setHealth(health);
            }
        }
    }

    public static float getHealth(LivingEntity target) {
        if (target.getHealth() > 0.6) {
            return target.getHealth();
        }

        Scoreboard scoreboard = target.level().getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(Scoreboard.DISPLAY_SLOT_BELOW_NAME);

        return objective != null ? scoreboard.getOrCreatePlayerScore(target.getScoreboardName(), objective).getScore() : 20;
    }
}
