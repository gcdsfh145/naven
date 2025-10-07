// Decompiled with: CFR 0.152
// Class Version: 17
package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import org.mixin.O.accessors.MultiPlayerGameModeAccessor;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

@ModuleInfo(name="AutoSoup", description="Auto use soup.", category=Category.COMBAT)
public class AutoSoup
        extends Module {
    FloatValue health = ValueBuilder.create(this, "Health").setDefaultFloatValue(15.0f).setFloatStep(1.0f).setMinFloatValue(1.0f).setMaxFloatValue(20.0f).build().getFloatValue();
    FloatValue delay = ValueBuilder.create(this, "Delay").setDefaultFloatValue(300.0f).setFloatStep(1.0f).setMinFloatValue(0.0f).setMaxFloatValue(1000.0f).build().getFloatValue();
    FloatValue switchDelay = ValueBuilder.create(this, "Switch Delay").setDefaultFloatValue(100.0f).setFloatStep(1.0f).setMinFloatValue(0.0f).setMaxFloatValue(1000.0f).build().getFloatValue();
    BooleanValue drop = ValueBuilder.create(this, "Drop").setDefaultBooleanValue(false).build().getBooleanValue();
    private final TimeHelper switchTimer = new TimeHelper();
    private final TimeHelper timer = new TimeHelper();
    private int lastSlot = -1;
    public boolean eating;

    @Override
    public void onDisable() {
        this.lastSlot = -1;
        this.eating = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (this.switchTimer.delay(this.switchDelay.getCurrentValue()) && this.lastSlot != -1) {
            mc.player.getInventory().selected = this.lastSlot;
            ((MultiPlayerGameModeAccessor)((Object)mc.gameMode)).invokeEnsureHasSentCarriedItem();
            this.lastSlot = -1;
            this.timer.reset();
        }
        if (!this.timer.delay(this.delay.getCurrentValue())) {
            return;
        }
        int soupInHotbar = InventoryUtils.findItem(0, 9, Items.MUSHROOM_STEW);
        if (mc.player.getHealth() <= this.health.getCurrentValue() && soupInHotbar != -1) {
            boolean isCurrent;
            boolean bl = isCurrent = mc.player.getInventory().selected == soupInHotbar;
            if (!isCurrent) {
                this.lastSlot = mc.player.getInventory().selected;
                mc.player.getInventory().selected = soupInHotbar;
                ((MultiPlayerGameModeAccessor)((Object)mc.gameMode)).invokeEnsureHasSentCarriedItem();
                this.switchTimer.reset();
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            this.eating = true;
            if (this.drop.getCurrentValue()) {
                mc.player.drop(true);
            }
            this.timer.reset();
        } else {
            this.eating = false;
        }
    }
}