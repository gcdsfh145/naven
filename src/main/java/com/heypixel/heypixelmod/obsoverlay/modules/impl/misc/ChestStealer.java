package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import org.apache.commons.lang3.RandomUtils;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "ChestStealer",
        description = "Automatically steals items from chests",
        category = Category.MISC
)
public class ChestStealer extends Module {
    private static final MSTimers timer = new MSTimers();

    private final FloatValue MinDelay = ValueBuilder.create(this, "MinDelay")
            .setDefaultFloatValue(200)
            .setFloatStep(10)
            .setMinFloatValue(0)
            .setMaxFloatValue(600)
            .build()
            .getFloatValue();

    private final FloatValue MaxDelay = ValueBuilder.create(this, "MaxDelay")
            .setDefaultFloatValue(200)
            .setFloatStep(10)
            .setMinFloatValue(0)
            .setMaxFloatValue(600)
            .build()
            .getFloatValue();

    private final BooleanValue pickEnderChest = ValueBuilder.create(this, "Ender Chest").setDefaultBooleanValue(false).build().getBooleanValue();
    private Screen lastTickScreen;
    private final CopyOnWriteArrayList<ChestInfo> chests = new CopyOnWriteArrayList<>();
    private float nextClickDelay = 0F;

    public static boolean isWorking() {
        return !timer.delay(3);
    }

    @EventTarget(1)
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            Screen currentScreen = mc.screen;
            if (currentScreen instanceof ContainerScreen container) {
                ChestMenu menu = (ChestMenu)container.getMenu();
                if (currentScreen != this.lastTickScreen) {
                    timer.reset();
                    nextClickDelay = RandomUtils.nextFloat(MinDelay.getCurrentValue(), MaxDelay.getCurrentValue());
                } else {
                    String chestTitle = container.getTitle().getString();
                    String chest = Component.translatable("container.chest").getString();
                    String largeChest = Component.translatable("container.chestDouble").getString();
                    String enderChest = Component.translatable("container.enderchest").getString();
                    if (chestTitle.equals(chest)
                            || chestTitle.equals(largeChest)
                            || chestTitle.equals("Chest")
                            || this.pickEnderChest.getCurrentValue() && chestTitle.equals(enderChest)) {
                        if (this.isChestEmpty(menu) && timer.delay(nextClickDelay)) {
                            mc.player.closeContainer();
                            timer.reset();
                            nextClickDelay = RandomUtils.nextFloat(MinDelay.getCurrentValue(), MaxDelay.getCurrentValue());
                        } else {
                            List<Integer> slots = IntStream.range(0, menu.getRowCount() * 9).boxed().collect(Collectors.toList());
                            Collections.shuffle(slots);

                            for (Integer pSlotId : slots) {
                                ItemStack stack = menu.getSlot(pSlotId).getItem();
                                if (isItemUseful(stack) && this.isBestItemInChest(menu, stack) && timer.delay(nextClickDelay)) {
                                    mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, 0, ClickType.QUICK_MOVE, mc.player);
                                    timer.reset();
                                    nextClickDelay = RandomUtils.nextFloat(MinDelay.getCurrentValue(), MaxDelay.getCurrentValue());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            this.lastTickScreen = currentScreen;
        }
    }

    @EventTarget
    public void onUpdate(EventRender event) {
        this.chests.clear();
        double range = 6.0;
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        BlockPos playerPos = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-6, -6, -6), playerPos.offset(6, 6, 6))) {
            if (!(mc.level.getBlockEntity(pos) instanceof ChestBlockEntity)) continue;
            Vec3 topCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            if (topCenter.distanceTo(cameraPos) > range) continue;
            Vector2f screenPos = ProjectionUtils.project(topCenter.x, topCenter.y, topCenter.z, event.getRenderPartialTicks());
            this.chests.add(new ChestInfo(pos, screenPos));
        }
    }

    @EventTarget
    public void onRender(EventRender2D event) {
        for (ChestInfo chest : this.chests) {
            Vector2f pos = chest.getScreenPos();
            Fonts.harmony.render(event.getStack(), "[Chest]", pos.x, pos.y, Color.YELLOW, true, 0.4f);
        }
    }

    private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
        if (!InventoryUtils.isGodItem(stack) && !InventoryUtils.isSharpnessAxe(stack)) {
            for (int i = 0; i < menu.getRowCount() * 9; i++) {
                ItemStack checkStack = menu.getSlot(i).getItem();
                if (stack.getItem() instanceof ArmorItem && checkStack.getItem() instanceof ArmorItem) {
                    ArmorItem item = (ArmorItem)stack.getItem();
                    ArmorItem checkItem = (ArmorItem)checkStack.getItem();
                    if (item.getEquipmentSlot() == checkItem.getEquipmentSlot() && InventoryUtils.getProtection(checkStack) > InventoryUtils.getProtection(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof SwordItem && checkStack.getItem() instanceof SwordItem) {
                    if (InventoryUtils.getSwordDamage(checkStack) > InventoryUtils.getSwordDamage(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof PickaxeItem && checkStack.getItem() instanceof PickaxeItem) {
                    if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof AxeItem && checkStack.getItem() instanceof AxeItem) {
                    if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof ShovelItem
                        && checkStack.getItem() instanceof ShovelItem
                        && InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    private boolean isChestEmpty(ChestMenu menu) {
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            if (!item.isEmpty() && isItemUseful(item) && this.isBestItemInChest(menu, item)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (InventoryUtils.isGodItem(stack) || InventoryUtils.isSharpnessAxe(stack)) {
            return true;
        } else if (stack.getItem() instanceof ArmorItem) {
            ArmorItem item = (ArmorItem)stack.getItem();
            float protection = InventoryUtils.getProtection(stack);
            float bestArmor = InventoryUtils.getBestArmorScore(item.getEquipmentSlot());
            return !(protection <= bestArmor);
        } else if (stack.getItem() instanceof SwordItem) {
            float damage = InventoryUtils.getSwordDamage(stack);
            float bestDamage = InventoryUtils.getBestSwordDamage();
            return !(damage <= bestDamage);
        } else if (stack.getItem() instanceof PickaxeItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestPickaxeScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof AxeItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestAxeScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof ShovelItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestShovelScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof CrossbowItem) {
            float score = InventoryUtils.getCrossbowScore(stack);
            float bestScore = InventoryUtils.getBestCrossbowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
            float score = InventoryUtils.getPunchBowScore(stack);
            float bestScore = InventoryUtils.getBestPunchBowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
            float score = InventoryUtils.getPowerBowScore(stack);
            float bestScore = InventoryUtils.getBestPowerBowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() == Items.COMPASS) {
            return !InventoryUtils.hasItem(stack.getItem());
        } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) >= InventoryCleaner.getWaterBucketCount()) {
            return false;
        } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) >= InventoryCleaner.getLavaBucketCount()) {
            return false;
        } else if (stack.getItem() instanceof BlockItem
                && Scaffold.isValidStack(stack)
                && InventoryUtils.getBlockCountInInventory() + stack.getCount() >= InventoryCleaner.getMaxBlockSize()) {
            return false;
        } else if (stack.getItem() == Items.ARROW && InventoryUtils.getItemCount(Items.ARROW) + stack.getCount() >= InventoryCleaner.getMaxArrowSize()) {
            return false;
        } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) >= 1) {
            return false;
        } else if (stack.getItem() != Items.SNOWBALL && stack.getItem() != Items.EGG
                || InventoryUtils.getItemCount(Items.SNOWBALL) + InventoryUtils.getItemCount(Items.EGG) + stack.getCount() < InventoryCleaner.getMaxProjectileSize()
                && InventoryCleaner.shouldKeepProjectile()) {
            return stack.getItem() instanceof ItemNameBlockItem ? false : InventoryUtils.isCommonItemUseful(stack);
        } else {
            return false;
        }
    }

    private static class ChestInfo {
        private BlockPos blockPos;
        private Vector2f screenPos;

        public ChestInfo(BlockPos blockPos, Vector2f screenPos) {
            this.blockPos = blockPos;
            this.screenPos = screenPos;
        }

        public BlockPos getBlockPos() {
            return blockPos;
        }

        public Vector2f getScreenPos() {
            return screenPos;
        }

        public void setBlockPos(BlockPos blockPos) {
            this.blockPos = blockPos;
        }

        public void setScreenPos(Vector2f screenPos) {
            this.screenPos = screenPos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChestInfo chestInfo = (ChestInfo) o;
            if (blockPos != null ? !blockPos.equals(chestInfo.blockPos) : chestInfo.blockPos != null) return false;
            return screenPos != null ? screenPos.equals(chestInfo.screenPos) : chestInfo.screenPos == null;
        }

        @Override
        public int hashCode() {
            int result = blockPos != null ? blockPos.hashCode() : 0;
            result = 31 * result + (screenPos != null ? screenPos.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ChestInfo{" +
                    "blockPos=" + blockPos +
                    ", screenPos=" + screenPos +
                    '}';
        }
    }
}