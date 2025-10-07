package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "MidPearl", description = "Throwing pearls more conveniently", category = Category.MISC)
public class MidPearl extends Module {
    ModeValue key = ValueBuilder.create(this, "Key").setModes("Middle", "Mouse4", "Mouse5").build().getModeValue();
    private boolean pearlThrown = false;
    private boolean Down;
    private TimeHelper timer = new TimeHelper();
    public static MidPearl instance;
    public boolean currentDownState = false;
    private static final Minecraft mc = Minecraft.getInstance();

    public MidPearl() {
        instance = this;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE && mc.player != null) {
            this.handlePearlThrow();
        }
    }

    public void handlePearlThrow() {
        String modeValue = this.key.getCurrentMode();
        switch (modeValue) {
            case "Middle":
                this.currentDownState = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 2) == 1;
                break;
            case "Mouse4":
                this.currentDownState = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 3) == 1;
                break;
            case "Mouse5":
                this.currentDownState = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 4) == 1;
                break;
        }
        if (!this.currentDownState && this.Down && !this.pearlThrown) {
            this.throwPearl();
            this.pearlThrown = true;
        } else if (!this.currentDownState) {
            this.pearlThrown = false;
        }
        this.Down = this.currentDownState;
    }

    private void throwPearl() {
        if (mc.player != null) {
            int pearlSlot = this.findEnderPearlSlot();
            if (pearlSlot != -1) {
                Inventory inventory = mc.player.getInventory();
                int originalSlot = inventory.selected;
                inventory.selected = pearlSlot;
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

                if (this.timer.delay(500.0, true)) {
                    inventory.selected = originalSlot;
                }
            } else {
                Notification notification = new Notification(NotificationLevel.ERROR, "No Ender Pearl found in inventory.", 3000L);
                Naven.getInstance().getNotificationManager().addNotification(notification);
                this.pearlThrown = false;
            }
        } else {
            this.pearlThrown = false;
        }
    }

    private int findEnderPearlSlot() {
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            ItemStack itemstack = inventory.getItem(i);
            if (itemstack.getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }
}