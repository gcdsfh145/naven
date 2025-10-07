package org.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventServerSetPosition;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.AntiKick;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StringUtils;
import dev.yalan.live.LiveClient;
import dev.yalan.live.netty.LiveProto;
import java.lang.invoke.CallSite;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({ClientPacketListener.class})
public class MixinClientPacketListener {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Redirect(
           method = {"handleMovePlayer"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V",
                   ordinal = 1
           )
   )
   public void onSendPacket(Connection instance, Packet<?> pPacket) {
      EventServerSetPosition event = new EventServerSetPosition(pPacket);
      Naven.getInstance().getEventManager().call(event);
      instance.send(event.getPacket());
   }

   @Inject(method = "handleLogin", at = @At(value = "TAIL"))
   private void onLogin(ClientboundLoginPacket p_105030_, CallbackInfo ci) {
      if (this.minecraft.player != null) {
         LiveClient.INSTANCE.sendPacket(LiveProto.createUpdateMinecraftProfile(
                 this.minecraft.player.getUUID(),
                 this.minecraft.player.getName().getString()
         ));
      }
   }

   @Inject(method = "handleRespawn", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
   private void onRespawn(
           ClientboundRespawnPacket p_105066_,
           CallbackInfo ci,
           ResourceKey<Level> resourcekey,
           Holder<DimensionType> holder,
           LocalPlayer localPlayer
   ) {
      LiveClient.INSTANCE.sendPacket(LiveProto.createUpdateMinecraftProfile(
              localPlayer.getUUID(),
              localPlayer.getName().getString()
      ));
   }

   @Inject(
           method = {"onDisconnect"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onDisconnect(Component reason, CallbackInfo ci) {
      AntiKick antikick = (AntiKick) Naven.moduleManager.getModule(AntiKick.class);
      if (antikick != null) {
         antikick.setSkickchat(reason.getString());
         if (this.minecraft.level != null && this.minecraft.player != null && antikick.getState()) {
            antikick.setKick(true);
            boolean kickchatValue = false;
            try {
               java.lang.reflect.Field kickchatField = AntiKick.class.getDeclaredField("kickchat");
               kickchatField.setAccessible(true);
               Object kickchatObj = kickchatField.get(antikick);
               if (kickchatObj instanceof com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue) {
                  kickchatValue = ((com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue) kickchatObj).getCurrentValue();
               }
            } catch (Exception e) {
               e.printStackTrace();
            }

            if (kickchatValue) {
               ChatUtils.addChatMessage("[AntiKick] 你已被此服务器踢出");
               ChatUtils.addChatMessage("[AntiKick]");
               ChatUtils.addChatMessage(reason.getString());
            }

            if (!(this.minecraft.screen instanceof LevelLoadingScreen)) {
               ci.cancel();
            } else {
               this.minecraft.setScreen(null);
            }
         }
      }
   }
}