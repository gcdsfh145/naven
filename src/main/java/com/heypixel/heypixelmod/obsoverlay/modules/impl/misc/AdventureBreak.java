package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "AdventureBreak",
        description = "Allow breaking blocks in adventure mode",
        category = Category.MISC
)
public class AdventureBreak extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    private final BooleanValue onlyWhenSneaking = ValueBuilder.create(this, "Only When Sneaking")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue requireCorrectTool = ValueBuilder.create(this, "Require Correct Tool")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private int sequenceNumber = 0;
    private BlockPos lastBlockPos = null;
    private long lastBreakTime = 0;

    @Override
    public void onEnable() {
        sequenceNumber = 0;
        lastBlockPos = null;
        lastBreakTime = 0;
    }

    @Override
    public void onDisable() {
        if (lastBlockPos != null && mc.getConnection() != null) {
            sendStopBreakPacket(lastBlockPos);
            lastBlockPos = null;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;

        if (mc.gameMode.getPlayerMode() != GameType.ADVENTURE) {
            if (lastBlockPos != null) {
                sendStopBreakPacket(lastBlockPos);
                lastBlockPos = null;
            }
            return;
        }

        if (onlyWhenSneaking.getCurrentValue() && !mc.player.isShiftKeyDown()) {
            if (lastBlockPos != null) {
                sendStopBreakPacket(lastBlockPos);
                lastBlockPos = null;
            }
            return;
        }

        if (mc.options.keyAttack.isDown()) {
            if (mc.hitResult instanceof BlockHitResult) {
                BlockHitResult hitResult = (BlockHitResult) mc.hitResult;
                BlockPos blockPos = hitResult.getBlockPos();

                if (canBreakBlock(blockPos)) {
                    if (!blockPos.equals(lastBlockPos) || System.currentTimeMillis() - lastBreakTime > 1000) {
                        if (lastBlockPos != null) {
                            sendStopBreakPacket(lastBlockPos);
                        }
                        sendStartBreakPacket(blockPos, hitResult.getDirection());
                        lastBlockPos = blockPos;
                        lastBreakTime = System.currentTimeMillis();
                    }

                    sendBreakProgressPacket(blockPos, hitResult.getDirection());
                } else if (lastBlockPos != null) {
                    sendStopBreakPacket(lastBlockPos);
                    lastBlockPos = null;
                }
            } else if (lastBlockPos != null) {
                sendStopBreakPacket(lastBlockPos);
                lastBlockPos = null;
            }
        } else if (lastBlockPos != null) {
            sendStopBreakPacket(lastBlockPos);
            lastBlockPos = null;
        }
    }

    private boolean canBreakBlock(BlockPos pos) {
        BlockState blockState = mc.level.getBlockState(pos);

        if (requireCorrectTool.getCurrentValue()) {
            return mc.player.hasCorrectToolForDrops(blockState);
        }

        return true;
    }

    private void sendStartBreakPacket(BlockPos pos, Direction direction) {
        if (mc.getConnection() == null) return;

        sequenceNumber++;

        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                direction,
                sequenceNumber
        ));

        mc.particleEngine.destroy(pos, mc.level.getBlockState(pos));
    }

    private void sendBreakProgressPacket(BlockPos pos, Direction direction) {
        if (mc.getConnection() == null) return;

        sequenceNumber++;

        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                pos,
                direction,
                sequenceNumber
        ));
    }

    private void sendStopBreakPacket(BlockPos pos) {
        if (mc.getConnection() == null) return;

        sequenceNumber++;

        // 发送停止破坏包
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                Direction.UP, // 方向不重要
                sequenceNumber
        ));
    }
}