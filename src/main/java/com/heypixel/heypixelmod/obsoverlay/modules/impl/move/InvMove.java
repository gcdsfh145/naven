package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;

import java.util.Arrays;
import java.util.List;

@ModuleInfo(name = "InvMove", description = "Enables movement while GUI is open", category = Category.MOVEMENT)
public class InvMove extends Module {
    private static final Minecraft minecraft = Minecraft.getInstance();

    BooleanValue noJump  = ValueBuilder.create(this, "No Jump ").setDefaultBooleanValue(true).build().getBooleanValue();

    private boolean wasSprintingBeforeGui = false;
    private boolean isInGui = false;
    private int guiOpenTicks = 0;
    private boolean wasInContainer = false;

    private static final List<KeyMapping> BASE_KEYS = Arrays.asList(
            minecraft.options.keyUp,
            minecraft.options.keyDown,
            minecraft.options.keyLeft,
            minecraft.options.keyRight,
            minecraft.options.keySprint
    );

    private List<KeyMapping> getKeys() {
        if (noJump.getCurrentValue()) {
            return BASE_KEYS;
        } else {
            return Arrays.asList(
                    minecraft.options.keyUp,
                    minecraft.options.keyDown,
                    minecraft.options.keyLeft,
                    minecraft.options.keyRight,
                    minecraft.options.keyJump,
                    minecraft.options.keySprint
            );
        }
    }

    @EventTarget
    public void handleMoveInput(EventMoveInput event) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (minecraft.screen != null) {
            if (minecraft.screen instanceof AbstractContainerScreen) {
                if (!wasInContainer) {
                    wasSprintingBeforeGui = minecraft.player.isSprinting();
                    wasInContainer = true;
                }

                if (minecraft.player.isSprinting()) {
                    minecraft.player.setSprinting(false);
                }

                forceUpdateStates();

                event.setForward(this.calculateForwardMovement());
                event.setStrafe(this.calculateStrafeMovement());
                event.setJump(this.calculateJumpMovement());
                event.setSneak(this.isKeyActive(this.minecraft.options.keyShift));

            } else {
                forceUpdateStates();

                if (wasInContainer) {
                    if (wasSprintingBeforeGui) {
                        minecraft.player.setSprinting(wasSprintingBeforeGui);
                    }
                    wasInContainer = false;
                    wasSprintingBeforeGui = false;
                }

                event.setForward(this.calculateForwardMovement());
                event.setStrafe(this.calculateStrafeMovement());
                event.setJump(this.calculateJumpMovement());
                event.setSneak(this.isKeyActive(this.minecraft.options.keyShift));
            }
        } else {
            if (wasInContainer) {
                if (wasSprintingBeforeGui) {
                    minecraft.player.setSprinting(wasSprintingBeforeGui);
                }
                wasInContainer = false;
                wasSprintingBeforeGui = false;
            }
        }
    }

    @EventTarget
    public void processTick(EventRunTicks event) {
        if (!this.isValidTickEvent(event) || this.minecraft.player == null) {
            return;
        }

        this.guiOpenTicks++;

        this.adjustPlayerRotation();

        if (this.minecraft.player.isSprinting()) {
            this.minecraft.player.setSprinting(false);
        }
    }

    private void forceUpdateStates() {
        if (minecraft.screen != null) {
            for (KeyMapping k : getKeys()) {
                InputConstants.Key key = k.getKey();
                boolean isKeyDown = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());

                KeyMapping.set(key, isKeyDown);

                if (isKeyDown) {
                    KeyMapping.click(key);
                }
            }

            if (noJump.getCurrentValue()) {
                KeyMapping.set(minecraft.options.keyJump.getKey(), false);
            }
        }
    }

    private boolean canContinueSprinting(LocalPlayer player) {
        boolean isMovingForward = player.xxa > 0.0f;
        boolean isInValidState = player.getFoodData().getFoodLevel() > 0
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isCrouching()
                && !player.isSwimming();
        return isMovingForward && isInValidState;
    }

    private boolean isMovementAllowed() {
        Screen currentScreen = this.minecraft.screen;
        return this.minecraft.player != null && currentScreen != null &&
                (this.isContainerScreen(currentScreen) || this.isClickGuiScreen(currentScreen));
    }

    private boolean isContainerScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen;
    }

    private boolean isClickGuiScreen(Screen screen) {
        String className = screen.getClass().getSimpleName();
        return screen instanceof ClickGUI || className.contains("ClickGUI") || className.contains("ClickGui");
    }

    private float calculateForwardMovement() {
        if (this.isKeyActive(this.minecraft.options.keyUp)) {
            return 1.0f;
        }
        if (this.isKeyActive(this.minecraft.options.keyDown)) {
            return -1.0f;
        }
        return 0.0f;
    }

    private float calculateStrafeMovement() {
        if (this.isKeyActive(this.minecraft.options.keyLeft)) {
            return 1.0f;
        }
        if (this.isKeyActive(this.minecraft.options.keyRight)) {
            return -1.0f;
        }
        return 0.0f;
    }

    private boolean calculateJumpMovement() {
        return !noJump.getCurrentValue() && this.isKeyActive(this.minecraft.options.keyJump);
    }

    private boolean isValidTickEvent(EventRunTicks event) {
        return event.getType() == EventType.PRE && this.isMovementAllowed();
    }

    private void adjustPlayerRotation() {
        LocalPlayer player = this.minecraft.player;
        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();
        float rotationSpeed = 3.0f;

        if (this.isKeyActive(265)) {
            player.setXRot(Math.max(currentPitch - rotationSpeed, -90.0f));
        }
        if (this.isKeyActive(264)) {
            player.setXRot(Math.min(currentPitch + rotationSpeed, 90.0f));
        }
        if (this.isKeyActive(263)) {
            player.setYRot(currentYaw - rotationSpeed);
        }
        if (this.isKeyActive(262)) {
            player.setYRot(currentYaw + rotationSpeed);
        }
    }

    private boolean isKeyActive(KeyMapping keyMapping) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyMapping.getKey().getValue());
    }

    private boolean isKeyActive(int keyCode) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode);
    }

    @Override
    public void onEnable() {
        this.wasSprintingBeforeGui = false;
        this.isInGui = false;
        this.guiOpenTicks = 0;
        this.wasInContainer = false;
    }

    @Override
    public void onDisable() {
        if (this.minecraft.player != null) {
            this.resetPlayerInput();
            if (this.wasSprintingBeforeGui && this.canContinueSprinting(this.minecraft.player)) {
                this.minecraft.player.setSprinting(true);
            }
        }
        this.wasSprintingBeforeGui = false;
        this.isInGui = false;
        this.guiOpenTicks = 0;
        this.wasInContainer = false;
    }

    private void resetPlayerInput() {
        LocalPlayer player = this.minecraft.player;
        player.xxa = 0.0f;
        player.zza = 0.0f;
        player.setJumping(false);
        player.setShiftKeyDown(false);
    }
}