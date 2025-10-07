package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.Event;

public class ClickBlockEvent extends Event {
    private final BlockPos pos;

    public ClickBlockEvent(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
