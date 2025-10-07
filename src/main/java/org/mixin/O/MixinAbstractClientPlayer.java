package org.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateFoV;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Cape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class MixinAbstractClientPlayer {
   @Inject(
           method = {"getFieldOfViewModifier"},
           at = {@At("RETURN")},
           cancellable = true
   )
   private void hookFoV(CallbackInfoReturnable<Float> cir) {
      Float returnValue = (Float)cir.getReturnValue();
      EventUpdateFoV event = new EventUpdateFoV(returnValue);
      Naven.getInstance().getEventManager().call(event);
      cir.setReturnValue(event.getFov());
   }

   @Inject(
           method = {"getCloakTextureLocation"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onGetCloakTexture(CallbackInfoReturnable<ResourceLocation> cir) {
      Minecraft mc = Minecraft.getInstance();
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;

      if (mc.player != null && player.getUUID().equals(mc.player.getUUID())) {
         Cape capeModule = (Cape) Naven.getInstance().getModuleManager().getModule(Cape.class);

         if (capeModule != null && capeModule.isEnabled()) {
            String mode = capeModule.modeValue.getCurrentMode();
            ResourceLocation capeTexture;

            switch (mode) {
               case "LuoTY":
                  capeTexture = new ResourceLocation("heypixel", "textures/cape/123.png");
                  break;
               case "LiquidBounce":
               default:
                  capeTexture = new ResourceLocation("heypixel", "textures/cape/liquidbounce.png");
                  break;
               case "HuaXL":
                  capeTexture = new ResourceLocation("heypixel", "textures/cape/1234.png");
                  break;
            }

            cir.setReturnValue(capeTexture);
         }
      }
   }
}