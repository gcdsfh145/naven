package org.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventGlobalPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.AntiKick;
import com.heypixel.heypixelmod.obsoverlay.utils.HttpUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Connection.class})
public abstract class MixinConnection extends SimpleChannelInboundHandler<Packet<?>> {
   @Shadow
   @Final
   private static Logger LOGGER;

   @Shadow
   private Channel channel;

   @Shadow
   private boolean disconnectionHandled;

   @Shadow
   private PacketListener packetListener;

   @Shadow
   private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
   }

   @Shadow
   protected abstract void sendPacket(Packet<?> var1, @Nullable PacketSendListener var2);

   @Shadow
   @Nullable
   public abstract Component getDisconnectedReason();

   @Shadow
   public abstract void disconnect(Component var1);

   @Inject(
           method = {"connectToServer"},
           at = {@At("HEAD")}
   )
   private static void injectHook(InetSocketAddress p_178301_, boolean p_178302_, CallbackInfoReturnable<Connection> cir) {
      try {
         HttpUtils.get("http://127.0.0.1:23233/api/setHook?hook=1");
      } catch (IOException var4) {
      }
   }

   @Inject(
           method = {"connect"},
           at = {@At("HEAD")}
   )
   private static void injectHook2(InetSocketAddress inetSocketAddress, boolean bl, Connection arg, CallbackInfoReturnable<ChannelFuture> cir) {
      try {
         HttpUtils.get("http://127.0.0.1:23233/api/setHook?hook=1");
      } catch (IOException var5) {
      }
   }

   @Redirect(
           method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
           )
   )
   private void onGenericsFtw(Packet<?> pPacket, PacketListener pListener) {
      EventGlobalPacket event = new EventGlobalPacket(EventType.RECEIVE, pPacket);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         genericsFtw(event.getPacket(), pListener);
      }
   }

   @Redirect(
           method = {"send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"
           )
   )
   private void onSend(Connection instance, Packet<?> pInPacket, PacketSendListener pFutureListeners) {
      if (NetworkUtils.passthroughsPackets.contains(pInPacket)) {
         NetworkUtils.passthroughsPackets.remove(pInPacket);
         this.sendPacket(pInPacket, pFutureListeners);
      } else {
         EventGlobalPacket event = new EventGlobalPacket(EventType.SEND, pInPacket);
         Naven.getInstance().getEventManager().call(event);
         if (!event.isCancelled()) {
            this.sendPacket(event.getPacket(), pFutureListeners);
         }
      }
   }

   @Overwrite
   public void handleDisconnection() {
      if (this.channel != null && !this.channel.isOpen()) {
         if (this.disconnectionHandled) {
            Module module = Naven.moduleManager.getModule(AntiKick.class);
            if (module instanceof AntiKick) {
               AntiKick antiKick = (AntiKick) module;
               if (!antiKick.getState()) {
                  LOGGER.warn("handleDisconnection() called twice");
               }
            } else {
               LOGGER.warn("handleDisconnection() called twice");
            }
         } else {
            this.disconnectionHandled = true;
            PacketListener packetlistener = this.packetListener;
            Component component = this.getDisconnectedReason();
            if (packetlistener != null) {
               Component disconnectReason = component != null ? component : Component.translatable("multiplayer.disconnect.generic");
               try {
                  packetlistener.onDisconnect(disconnectReason);
               } catch (Exception e) {
                  LOGGER.error("Error in onDisconnect", e);
               }
            }
         }
      }
   }

   private boolean isAntiKickEnabled() {
      Module module = Naven.getInstance().getModuleManager().getModule(AntiKick.class);
      return module instanceof AntiKick && ((AntiKick) module).isEnabled();
   }
}