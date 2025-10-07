package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

@ModuleInfo(
        name = "NoSlow",
        description = "NoSlowDown",
        category = Category.MOVEMENT
)
public class NoSlow extends Module {
    private boolean ncpShouldWork = true;

    @EventTarget
    public void onSlow(EventSlowdown eventSlowdown) {
        if (mc.player.getUseItemRemainingTicks() % 2 != 0 && mc.player.getUseItemRemainingTicks() <= 30) {
            eventSlowdown.setSlowdown(false);
            mc.player.setSprinting(true);
        }

        if (mc.player.isUsingItem() && ncpShouldWork) {
            if (mc.player.tickCount % 3 == 0) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0));
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
            ncpShouldWork = false;
        } else {
            ncpShouldWork = true;
        }
    }
}