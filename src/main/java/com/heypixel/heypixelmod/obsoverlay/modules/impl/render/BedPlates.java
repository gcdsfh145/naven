package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "BedPlates", description = "Renders Defense A Bed Has", category = Category.RENDER)
public class BedPlates extends Module {
    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(10.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(5.0f)
            .setMaxFloatValue(30.0f)
            .build()
            .getFloatValue();
    private static final int[][] OFFSETS = new int[][]{{0, 1, 0}};
    private final List<BlockInfo> obstructingBlocks = new ArrayList<BlockInfo>();

    @EventTarget
    public void onUpdate(EventRender event) {
        this.obstructingBlocks.clear();
        if (mc.level == null || mc.player == null) {
            return;
        }
        BlockPos playerPos = mc.player.blockPosition();
        int far = (int) this.range.getCurrentValue();
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-far, -far, -far), playerPos.offset(far, far, far))) {
            BlockState state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.FOOT) {
                continue;
            }
            for (int[] offset : OFFSETS) {
                BlockPos offsetPos = pos.offset(offset[0], offset[1], offset[2]);
                BlockState offsetState = mc.level.getBlockState(offsetPos);
                if (offsetState.isAir() || offsetState.getBlock() instanceof BedBlock) {
                    continue;
                }
                Vec3 blockCenter = new Vec3(offsetPos.getX() + 0.5, offsetPos.getY() + 0.5, offsetPos.getZ() + 0.5);
                Vector2f screenPos = ProjectionUtils.project(blockCenter.x, blockCenter.y, blockCenter.z, event.getRenderPartialTicks());
                this.obstructingBlocks.add(new BlockInfo(offsetState.getBlock().getName().getString(), screenPos));
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        PoseStack stack = event.getStack();
        for (BlockInfo blockInfo : this.obstructingBlocks) {
            stack.pushPose();
            Fonts.harmony.render(stack, blockInfo.getName(), blockInfo.getScreenPos().x, blockInfo.getScreenPos().y, Color.WHITE, true, 0.25f);
            stack.popPose();
        }
    }

    private static class BlockInfo {
        private final String name;
        private final Vector2f screenPos;

        public BlockInfo(String name, Vector2f screenPos) {
            this.name = name;
            this.screenPos = screenPos;
        }

        public String getName() {
            return this.name;
        }

        public Vector2f getScreenPos() {
            return this.screenPos;
        }
    }
}
