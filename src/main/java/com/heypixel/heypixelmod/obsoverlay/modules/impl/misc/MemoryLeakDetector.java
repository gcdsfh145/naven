package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import jnic.JNICInclude;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleInfo(
        name = "MemoryLeakDetector",
        description = "MemoryLeakDetector",
        category = Category.MISC
)
public class MemoryLeakDetector extends Module {

    public static final String MOD_ID = "memoryleakdetector";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final long MEMORY_THRESHOLD = 85;
    private static final long CHECK_INTERVAL = 600;
    private static final long SHUTDOWN_DELAY = 1200;

    private static MemoryMXBean memoryMXBean;
    private static ScheduledExecutorService scheduler;
    private static boolean shutdownScheduled = false;
    private static long tickCounter = 0;
    private static boolean memoryLeakDetected = false;
    private static long shutdownTick = 0;

    public MemoryLeakDetector() {
        MinecraftForge.EVENT_BUS.register(this);
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        LOGGER.info("内存泄漏检测器已加载");
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        startMemoryMonitoring();
        LOGGER.info("内存监控已启动");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        stopMemoryMonitoring();
        LOGGER.info("内存监控已停止");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter % CHECK_INTERVAL == 0 && !shutdownScheduled) {
            checkMemoryUsage();
        }
        if (memoryLeakDetected) {
            handleShutdownCountdown();
        }
    }

    private static void startMemoryMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        shutdownScheduled = false;
        memoryLeakDetected = false;
        tickCounter = 0;
    }

    private static void stopMemoryMonitoring() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        shutdownScheduled = false;
        memoryLeakDetected = false;
    }

    private static void checkMemoryUsage() {
        if (shutdownScheduled) {
            return;
        }

        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        long used = heapMemoryUsage.getUsed();
        long max = heapMemoryUsage.getMax();

        if (max <= 0) return;

        double usagePercent = (double) used / max * 100;

        LOGGER.info("内存使用情况: {}/{} MB ({}%)",
                used / 1024 / 1024, max / 1024 / 1024,
                String.format("%.2f", usagePercent));

        if (usagePercent >= MEMORY_THRESHOLD) {
            handleMemoryLeakDetected(usagePercent);
        }
    }

    private static void handleMemoryLeakDetected(double usagePercent) {
        LOGGER.warn("检测到可能的内存泄漏! 内存使用率: {}%",
                String.format("%.2f", usagePercent));
        ChatUtils.addChatMessage("内存可能泄露");
        memoryLeakDetected = true;
        shutdownScheduled = true;
        shutdownTick = tickCounter + SHUTDOWN_DELAY;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            String warningMessage = String.format(
                    "§c§l警告: 检测到内存泄漏! 服务器将在60秒后自动关闭以防止崩溃! " +
                            "当前内存使用率: %.2f%%", usagePercent);
            ChatUtils.addChatMessage( "§c§l警告: 检测到内存泄漏! 服务器将在60秒后自动关闭以防止崩溃! " +
                    "当前内存使用率: %.2f%%");
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(warningMessage));
            }
        }
    }

    private static void handleShutdownCountdown() {
        long remainingTicks = shutdownTick - tickCounter;
        long remainingSeconds = remainingTicks / 20;

        if (remainingTicks <= 0) {
            LOGGER.error("由于内存泄漏，服务器正在强制关闭");
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.halt(false);
            }
            memoryLeakDetected = false;
            return;
        }

        if ((remainingSeconds <= 10 && remainingTicks % 20 == 0) ||
                (remainingSeconds > 10 && remainingSeconds % 10 == 0 && remainingTicks % 20 == 0)) {

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                String countdownMsg = String.format("§e服务器将在 %d 秒后关闭...", remainingSeconds);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(countdownMsg));
                }
            }
        }
    }

    public static void forceGarbageCollection() {
        LOGGER.info("强制执行垃圾回收...");
        System.gc();
    }
}