// Decompiled with: CFR 0.152
// Class Version: 17
package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.AimAssist;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.util.Random;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
        name = "AutoRod",
        description = "AutoRod",
        category = Category.COMBAT
)
public class AutoRod extends Module {
    private static final Random random = new Random();
    private final Minecraft mc = Minecraft.getInstance();
    private final FloatValue prepareTimeMin = ValueBuilder.create(this, "PrepareTimeMin(ms)")
            .setDefaultFloatValue(300.0f)
            .setFloatStep(50.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(1000.0f)
            .build()
            .getFloatValue();
    private final FloatValue prepareTimeMax = ValueBuilder.create(this, "PrepareTimeMax(ms)")
            .setDefaultFloatValue(700.0f)
            .setFloatStep(50.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(1000.0f)
            .build()
            .getFloatValue();
    private final FloatValue rodWaitTimeMin = ValueBuilder.create(this, "SwingWaitTimeMin(ms)")
            .setDefaultFloatValue(100.0f)
            .setFloatStep(20.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(500.0f)
            .build()
            .getFloatValue();
    private final FloatValue rodWaitTimeMax = ValueBuilder.create(this, "SwingWaitTimeMax(ms)")
            .setDefaultFloatValue(300.0f)
            .setFloatStep(20.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(500.0f)
            .build()
            .getFloatValue();
    private final FloatValue switchBackTimeMin = ValueBuilder.create(this, "SwitchBackTimeMin(ms)")
            .setDefaultFloatValue(100.0f)
            .setFloatStep(20.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(500.0f)
            .build()
            .getFloatValue();
    private final FloatValue switchBackTimeMax = ValueBuilder.create(this, "SwitchBackTimeMax(ms)")
            .setDefaultFloatValue(300.0f)
            .setFloatStep(20.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(500.0f)
            .build()
            .getFloatValue();
    private final FloatValue cooldownTimeMin = ValueBuilder.create(this, "CooldownTimeMin(ms)")
            .setDefaultFloatValue(1500.0f)
            .setFloatStep(100.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(5000.0f)
            .build()
            .getFloatValue();
    private final FloatValue cooldownTimeMax = ValueBuilder.create(this, "CooldownTimeMax(ms)")
            .setDefaultFloatValue(2500.0f)
            .setFloatStep(100.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(5000.0f)
            .build()
            .getFloatValue();
    private final BooleanValue legitMode = ValueBuilder.create(this, "Legit")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final TimeHelper timer = new TimeHelper();
    private State currentState = State.IDLE;
    private int originalSlot = -1;
    private int rodSlot = -1;
    private long currentStageTime;

    @Override
    public void onEnable() {
        this.currentState = State.IDLE;
        this.originalSlot = -1;
        this.rodSlot = -1;
        this.timer.reset();
        this.currentStageTime = 0L;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        LocalPlayer player = this.mc.player;
        if (event.getType() != EventType.PRE || player == null || this.mc.screen != null) {
            return;
        }
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        AimAssist aimAssist = (AimAssist) Naven.getInstance().getModuleManager().getModule(AimAssist.class);
        boolean shouldActivate = this.checkCombatModulesActive(aura, aimAssist) && this.hasValidTarget(aura, aimAssist);
        if (!shouldActivate) {
            this.currentState = State.IDLE;
            return;
        }
        this.handleStateMachine(player);
    }

    private void handleStateMachine(LocalPlayer player) {
        switch (this.currentState) {
            case IDLE:
                this.rodSlot = this.findRodSlot();
                if (this.rodSlot == -1) {
                    break;
                }
                this.originalSlot = player.getInventory().selected;
                if (this.originalSlot == this.rodSlot) {
                    break;
                }
                this.currentStageTime = this.getRandomTime(this.prepareTimeMin.getCurrentValue(), this.prepareTimeMax.getCurrentValue());
                this.timer.reset();
                this.currentState = State.PREPARING;
                break;

            case PREPARING:
                if (!this.timer.delay(this.currentStageTime)) {
                    break;
                }
                this.currentState = State.SWITCHING_ROD;
                this.switchToRod(player);
                this.timer.reset();
                this.currentStageTime = 50L;
                break;

            case SWITCHING_ROD:
                if (!this.timer.delay(this.currentStageTime)) {
                    break;
                }
                this.currentState = State.CHECKING_ROD;
                break;

            case CHECKING_ROD:
                if (this.isHoldingRod(player)) {
                    this.currentStageTime = this.getRandomTime(this.rodWaitTimeMin.getCurrentValue(), this.rodWaitTimeMax.getCurrentValue());
                    this.timer.reset();
                    this.currentState = State.SWITCHED_ROD;
                } else {
                    this.switchToRod(player);
                    this.timer.reset();
                }
                break;

            case SWITCHED_ROD:
                if (!this.timer.delay(this.currentStageTime)) {
                    break;
                }
                if (!this.checkRodEquipped()) {
                    if (this.legitMode.getCurrentValue()) {
                        this.simulateKeyPress(this.getSlotKey(this.rodSlot));
                    } else {
                        player.getInventory().selected = this.rodSlot;
                    }
                    this.timer.reset();
                    break;
                }
                this.useRod(player);
                this.currentStageTime = this.getRandomTime(this.switchBackTimeMin.getCurrentValue(), this.switchBackTimeMax.getCurrentValue());
                this.timer.reset();
                this.currentState = State.USING_ROD;
                break;

            case USING_ROD:
                if (!this.timer.delay(this.currentStageTime)) {
                    break;
                }
                if (this.originalSlot != -1) {
                    this.switchToOriginal(player);
                    this.currentState = State.SWITCHING_BACK;
                    this.timer.reset();
                    this.currentStageTime = 50L;
                } else {
                    this.currentState = State.COOLDOWN;
                    this.setCooldown();
                }
                break;

            case SWITCHING_BACK:
                if (!this.timer.delay(this.currentStageTime)) {
                    break;
                }
                this.currentState = State.CHECKING_BACK;
                break;

            case CHECKING_BACK:
                if (player.getInventory().selected == this.originalSlot) {
                    this.currentState = State.COOLDOWN;
                    this.setCooldown();
                } else {
                    this.switchToOriginal(player);
                    this.timer.reset();
                }
                break;

            case COOLDOWN:
                if (!this.timer.delay(this.currentStageTime)) {
                    break;
                }
                this.currentState = State.IDLE;
                break;
        }
    }

    private void switchToRod(LocalPlayer player) {
        if (this.legitMode.getCurrentValue()) {
            this.simulateKeyPress(this.getSlotKey(this.rodSlot));
        } else {
            player.getInventory().selected = this.rodSlot;
        }
    }

    private void switchToOriginal(LocalPlayer player) {
        if (this.legitMode.getCurrentValue()) {
            this.simulateKeyPress(this.getSlotKey(this.originalSlot));
        } else {
            player.getInventory().selected = this.originalSlot;
        }
    }

    private void useRod(LocalPlayer player) {
        if (this.legitMode.getCurrentValue()) {
            this.simulateRightClick();
        } else if (this.mc.gameMode != null) {
            this.mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private void setCooldown() {
        this.currentStageTime = this.getRandomTime(this.cooldownTimeMin.getCurrentValue(), this.cooldownTimeMax.getCurrentValue());
        this.timer.reset();
    }

    private boolean isHoldingRod(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        return mainHand.getItem() == Items.FISHING_ROD;
    }

    private int findRodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.FISHING_ROD) {
                return i;
            }
        }
        return -1;
    }

    private long getRandomTime(float min, float max) {
        return min >= max ? (long) min : (long) (min + random.nextFloat() * (max - min));
    }

    private int getSlotKey(int slot) {
        return 49 + slot;
    }

    private void simulateKeyPress(int keyCode) {
        KeyMapping key = this.mc.options.keyHotbarSlots[keyCode - 49];
        if (key == null) {
            return;
        }
        int pressDelay = random.nextInt(20) + 10;
        this.mc.execute(() -> {
            try {
                KeyMapping.set(key.getKey(), true);
                KeyMapping.click(key.getKey());
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < (long) pressDelay) {
                    Thread.yield();
                }
                KeyMapping.set(key.getKey(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void simulateRightClick() {
        KeyMapping useKey = this.mc.options.keyUse;
        if (useKey == null) {
            return;
        }
        int clickDelay = random.nextInt(30) + 20;
        this.mc.execute(() -> {
            try {
                KeyMapping.set(useKey.getKey(), true);
                KeyMapping.click(useKey.getKey());
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < (long) clickDelay) {
                    Thread.yield();
                }
                KeyMapping.set(useKey.getKey(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean checkRodEquipped() {
        return this.mc.player != null && this.mc.player.getMainHandItem().getItem() == Items.FISHING_ROD;
    }

    private boolean checkCombatModulesActive(Aura aura, AimAssist aimAssist) {
        return (aura != null && aura.isEnabled()) || (aimAssist != null && aimAssist.isEnabled());
    }

    private boolean hasValidTarget(Aura aura, AimAssist aimAssist) {
        boolean hasAuraTarget = false;
        if (aura != null && Aura.target != null && Aura.target.isAlive()) {
            hasAuraTarget = true;
        }
        boolean hasAimAssistTarget = aimAssist != null && aimAssist.targetRotation != null && aimAssist.working;
        return hasAuraTarget || hasAimAssistTarget;
    }

    private enum State {
        IDLE,
        PREPARING,
        SWITCHING_ROD,
        CHECKING_ROD,
        SWITCHED_ROD,
        USING_ROD,
        SWITCHING_BACK,
        CHECKING_BACK,
        COOLDOWN
    }
}