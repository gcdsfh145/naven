package org.mixin.O.accessors;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Entity.class})
public interface EntityAccessor {
    @Accessor(value="onGround")
    public boolean getOnGround();

    @Accessor(value="onGround")
    public void setOnGround(boolean var1);
}