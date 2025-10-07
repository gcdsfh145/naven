package org.mixin.O;

import com.mojang.authlib.GameProfile;
import dev.yalan.live.LiveClient;
import dev.yalan.live.netty.LiveProto;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInfo.class)
public class MixinPlayerInfo {
    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(GameProfile profile, boolean p_254409_, CallbackInfo ci) {
        LiveClient.INSTANCE.sendPacket(LiveProto.createQueryMinecraftProfile(profile));
    }
}
