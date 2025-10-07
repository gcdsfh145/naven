package org.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShutdown;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Glow;
import com.heypixel.heypixelmod.obsoverlay.utils.AnimationUtils;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.Window;
import com.mojang.realmsclient.client.RealmsClient;
import dev.yalan.live.LiveClient;
import dev.yalan.live.gui.LiveAuthenticationScreen;
import dev.yalan.live.netty.LiveProto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MixinMinecraft {
   @Unique
   private int skipTicks;
   @Unique
   private long naven_Modern$lastFrame;
   @Shadow
   @Final
   private Window window;
   @Shadow
   public abstract void setScreen(Screen p_91153_);
   @Unique
   private long totalPlayTimeMillis = 0;
   @Unique
   private long sessionStartTime = -1;
   @Unique
   private boolean isInWorld = false;
   @Unique
   private long lastTitleUpdateTime = 0;
   @Unique
   private static final long TITLE_UPDATE_INTERVAL = 1000;
   @Unique
   private boolean iconSet = false;
   @Unique
   private int iconSetAttempts = 0;
   @Unique
   private static final int MAX_ICON_SET_ATTEMPTS = 10;

   /**
    * @author Yalan
    * @reason Force to authentication
    */
   @Overwrite
   private void setInitialScreen(RealmsClient p_279285_, ReloadInstance p_279164_, GameConfig.QuickPlayData p_279146_) {
      setScreen(new LiveAuthenticationScreen());
   }

   @Inject(
           method = {"<init>"},
           at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo info) {
      Naven.modRegister();
      setWindowIcon();
   }

   @Inject(
           method = {"<init>"},
           at = {@At("RETURN")}
   )
   public void onInit(GameConfig pGameConfig, CallbackInfo ci) {
      System.setProperty("java.awt.headless", "false");
      ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("naven"));
      List<IModFileInfo> fileInfoToRemove = new ArrayList<>();

      for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
         for (IModInfo modInfo : fileInfo.getMods()) {
            if (modInfo.getModId().contains("naven")) {
               fileInfoToRemove.add(fileInfo);
            }
         }
      }

      ModList.get().getModFiles().removeAll(fileInfoToRemove);
   }

   @Inject(
           method = {"close"},
           at = {@At("HEAD")},
           remap = false
   )
   private void shutdown(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventShutdown());
      }
      totalPlayTimeMillis = 0;
      sessionStartTime = -1;
      isInWorld = false;
   }

   @Inject(
           method = {"tick"},
           at = {@At("HEAD")}
   )
   private void tickPre(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
      }
      Minecraft self = (Minecraft) (Object) this;
      boolean currentlyInWorld = self.level != null;

      if (currentlyInWorld != isInWorld) {
         if (currentlyInWorld) {
            sessionStartTime = System.currentTimeMillis();
         } else {
            if (sessionStartTime > 0) {
               totalPlayTimeMillis += System.currentTimeMillis() - sessionStartTime;
               sessionStartTime = -1;
            }
         }
         isInWorld = currentlyInWorld;
      }
   }

   @Inject(
           method = {"tick"},
           at = {@At("TAIL")}
   )
   private void tickPost(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
      }

      if (!iconSet && iconSetAttempts < MAX_ICON_SET_ATTEMPTS) {
         setWindowIcon();
         iconSetAttempts++;
         if (iconSetAttempts >= MAX_ICON_SET_ATTEMPTS) {
            iconSet = true;
         }
      }
   }

   @Inject(
           method = {"shouldEntityAppearGlowing"},
           at = {@At("RETURN")},
           cancellable = true
   )
   private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
      if (Glow.shouldGlow(pEntity)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
           method = {"runTick"},
           at = {@At("HEAD")}
   )
   private void runTick(CallbackInfo ci) {
      long currentTime = System.nanoTime() / 1000000L;
      int deltaTime = (int)(currentTime - this.naven_Modern$lastFrame);
      this.naven_Modern$lastFrame = currentTime;
      AnimationUtils.delta = deltaTime;
      long now = System.currentTimeMillis();
      if (now - lastTitleUpdateTime >= TITLE_UPDATE_INTERVAL) {
         updateTitleRealTime();
         lastTitleUpdateTime = now;
      }
   }

   @ModifyArg(
           method = {"runTick"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"
           )
   )
   private float fixSkipTicks(float g) {
      if (this.skipTicks > 0) {
         g = 0.0F;
      }

      return g;
   }

   @Inject(
           method = {"handleKeybinds"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
                   ordinal = 0,
                   shift = Shift.BEFORE
           )},
           cancellable = true
   )
   private void clickEvent(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         EventClick event = new EventClick();
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
   public void clearLevel(CallbackInfo ci) {
      LiveClient.INSTANCE.sendPacket(LiveProto.createRemoveMinecraftProfile());
      LiveClient.INSTANCE.getLiveUserMap().clear();
      if (sessionStartTime > 0) {
         totalPlayTimeMillis += System.currentTimeMillis() - sessionStartTime;
         sessionStartTime = -1;
         isInWorld = false;
      }
   }

   @Overwrite
   public void updateTitle() {
      updateTitleRealTime();
   }

   @Unique
   private void updateTitleRealTime() {
      long currentPlayTime = totalPlayTimeMillis;
      if (sessionStartTime > 0) {
         currentPlayTime += System.currentTimeMillis() - sessionStartTime;
      }
      long hours = currentPlayTime / 3600000;
      long minutes = (currentPlayTime % 3600000) / 60000;
      long seconds = (currentPlayTime % 60000) / 1000;

      String timeString = String.format("%d小时%d分钟%d秒", hours, minutes, seconds);

      this.window.setTitle("ShaoYuNavenClient" + " " + "B14.4" + " - 游玩时间: " + timeString + " - 如果巅峰留不住 那就重走来时路");
   }

   @Unique
   private void setWindowIcon() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getWindow() != null) {
            try {
               net.minecraft.server.packs.resources.ResourceManager resourceManager = mc.getResourceManager();
               net.minecraft.resources.ResourceLocation iconLocation = new net.minecraft.resources.ResourceLocation("shaoyu", "icon/icon.png");
               java.util.Optional<net.minecraft.server.packs.resources.Resource> resourceOptional = resourceManager.getResource(iconLocation);
               if (resourceOptional.isPresent()) {
                  net.minecraft.server.packs.resources.Resource resource = resourceOptional.get();
                  java.io.InputStream iconStream = resource.open();
                  java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);
                  java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                          icon.getWidth(null),
                          icon.getHeight(null),
                          java.awt.image.BufferedImage.TYPE_INT_ARGB
                  );
                  java.awt.Graphics2D g2d = bufferedImage.createGraphics();
                  g2d.drawImage(icon, 0, 0, null);
                  g2d.dispose();

                  try {
                     java.lang.reflect.Method setIconMethod = mc.getWindow().getClass().getMethod(
                             "setIcon",
                             java.awt.Image.class
                     );
                     setIconMethod.invoke(mc.getWindow(), bufferedImage);
                     iconSet = true;
                     System.out.println("[Naven] 窗口图标设置成功 (反射方法)");
                  } catch (Exception reflectEx) {
                     try {
                        org.lwjgl.glfw.GLFWImage glfwImage = org.lwjgl.glfw.GLFWImage.malloc();
                        org.lwjgl.glfw.GLFWImage.Buffer imageBuffer = org.lwjgl.glfw.GLFWImage.malloc(1);
                        int width = bufferedImage.getWidth();
                        int height = bufferedImage.getHeight();
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * 4);
                        for (int y = 0; y < height; y++) {
                           for (int x = 0; x < width; x++) {
                              int pixel = bufferedImage.getRGB(x, y);
                              buffer.put((byte)((pixel >> 16) & 0xFF));
                              buffer.put((byte)((pixel >> 8) & 0xFF));
                              buffer.put((byte)(pixel & 0xFF));
                              buffer.put((byte)((pixel >> 24) & 0xFF));
                           }
                        }
                        buffer.flip();
                        glfwImage.set(width, height, buffer);
                        imageBuffer.put(0, glfwImage);
                        long windowHandle = mc.getWindow().getWindow();
                        if (windowHandle != 0) {
                           org.lwjgl.glfw.GLFW.glfwSetWindowIcon(windowHandle, imageBuffer);
                           System.out.println("[Naven] 窗口图标设置成功 (GLFW方法)");
                           iconSet = true;
                        } else {
                           System.out.println("[Naven] 窗口句柄为空，无法设置图标");
                        }
                        glfwImage.free();
                        imageBuffer.free();
                     } catch (Exception glfwEx) {
                        System.out.println("[Naven] GLFW设置图标失败: " + glfwEx.getMessage());
                     }
                  }
                  iconStream.close();
               } else {
                  System.out.println("[Naven] 图标资源未找到: shaoyu:icon/icon.png");
               }
            } catch (Exception resourceEx) {
               System.out.println("[Naven] 资源管理器加载失败: " + resourceEx.getMessage());
               java.io.InputStream iconStream = getClass().getResourceAsStream("/assets/shaoyu/icon/icon.png");
               if (iconStream != null) {
                  System.out.println("[Naven] 使用备用方法加载图标成功");
                  java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);

                  java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                          icon.getWidth(null),
                          icon.getHeight(null),
                          java.awt.image.BufferedImage.TYPE_INT_ARGB
                  );
                  java.awt.Graphics2D g2d = bufferedImage.createGraphics();
                  g2d.drawImage(icon, 0, 0, null);
                  g2d.dispose();

                  try {
                     org.lwjgl.glfw.GLFWImage glfwImage = org.lwjgl.glfw.GLFWImage.malloc();
                     org.lwjgl.glfw.GLFWImage.Buffer imageBuffer = org.lwjgl.glfw.GLFWImage.malloc(1);

                     int width = bufferedImage.getWidth();
                     int height = bufferedImage.getHeight();

                     java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * 4);

                     for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                           int pixel = bufferedImage.getRGB(x, y);
                           buffer.put((byte)((pixel >> 16) & 0xFF));
                           buffer.put((byte)((pixel >> 8) & 0xFF));
                           buffer.put((byte)(pixel & 0xFF));
                           buffer.put((byte)((pixel >> 24) & 0xFF));
                        }
                     }
                     buffer.flip();

                     glfwImage.set(width, height, buffer);
                     imageBuffer.put(0, glfwImage);

                     long windowHandle = mc.getWindow().getWindow();
                     if (windowHandle != 0) {
                        org.lwjgl.glfw.GLFW.glfwSetWindowIcon(windowHandle, imageBuffer);
                        System.out.println("[Naven] 窗口图标设置成功 (备用GLFW方法)");
                        iconSet = true;
                     } else {
                        System.out.println("[Naven] 窗口句柄为空，无法设置图标");
                     }

                     glfwImage.free();
                     imageBuffer.free();
                  } catch (Exception glfwEx) {
                     System.out.println("[Naven] 备用GLFW设置图标失败: " + glfwEx.getMessage());
                  }

                  iconStream.close();
               } else {
                  System.out.println("[Naven] 备用方法也找不到图标文件: /assets/shaoyu/icon/icon.png");
               }
            }
         } else {
            System.out.println("[Naven] 窗口对象为空");
         }
      } catch (Exception e) {
         System.out.println("[Naven] 设置窗口图标时发生错误: " + e.getMessage());
      }
   }
}