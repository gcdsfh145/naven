package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "GhostHand",
        description = "Ignore the block and open the container.",
        category = Category.MISC
)
public class GhostHand extends Module {

    private static final double MAX_INTERACTION_DISTANCE = 6.0;

    private final Minecraft mc = Minecraft.getInstance();
    private boolean shouldOpenContainer = false;
    private BlockPos containerPosToOpen = null;

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        shouldOpenContainer = false;
        containerPosToOpen = null;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;

        if (mc.options.keyUse.isDown()) {
            BlockPos containerPos = findContainerInSight();

            if (containerPos != null) {
                double distance = mc.player.getEyePosition().distanceTo(
                        Vec3.atCenterOf(containerPos)
                );

                if (distance <= MAX_INTERACTION_DISTANCE) {
                    shouldOpenContainer = true;
                    containerPosToOpen = containerPos;

                    openContainer(containerPos);

                    shouldOpenContainer = false;
                }
            }
        } else {
            shouldOpenContainer = false;
            containerPosToOpen = null;
        }
    }

    private BlockPos findContainerInSight() {
        if (mc.hitResult == null || !(mc.hitResult instanceof BlockHitResult)) {
            return null;
        }

        BlockHitResult hitResult = (BlockHitResult) mc.hitResult;
        BlockPos lookingAtPos = hitResult.getBlockPos();

        if (isContainer(lookingAtPos)) {
            return lookingAtPos;
        }

        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(MAX_INTERACTION_DISTANCE));

        double step = 0.1;
        double distance = 0;

        while (distance < MAX_INTERACTION_DISTANCE) {
            Vec3 currentPos = start.add(look.scale(distance));
            BlockPos blockPos = BlockPos.containing(
                    currentPos.x,
                    currentPos.y,
                    currentPos.z
            );

            if (!mc.level.isEmptyBlock(blockPos) &&
                    !blockPos.equals(BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ()))) {

                if (isContainer(blockPos)) {
                    return blockPos;
                }
            }

            distance += step;
        }

        return null;
    }

    private boolean isContainer(BlockPos pos) {
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);

        if (blockEntity == null) {
            return false;
        }

        return blockEntity instanceof ChestBlockEntity ||
                blockEntity instanceof ShulkerBoxBlockEntity ||
                blockEntity instanceof BarrelBlockEntity ||
                blockEntity instanceof DispenserBlockEntity ||
                blockEntity instanceof DropperBlockEntity ||
                blockEntity instanceof HopperBlockEntity ||
                blockEntity instanceof FurnaceBlockEntity ||
                blockEntity instanceof BlastFurnaceBlockEntity ||
                blockEntity instanceof SmokerBlockEntity ||
                blockEntity instanceof BrewingStandBlockEntity;
    }

    private void openContainer(BlockPos pos) {
        if (mc.getConnection() == null) return;

        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
        );

        mc.getConnection().send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                hitResult,
                0
        ));
    }
}