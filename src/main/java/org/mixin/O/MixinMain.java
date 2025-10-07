package org.mixin.O;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Main.class})
public class MixinMain {
    @Inject(method={"<clinit>"}, at={@At(value="INVOKE", target="Ljava/lang/System;setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")}, cancellable=true)
    private static void fuckOffHeadlessMode(CallbackInfo ci) {
        ci.cancel();
    }
}
