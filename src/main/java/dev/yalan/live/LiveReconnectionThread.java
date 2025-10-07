package dev.yalan.live;

import net.minecraft.client.Minecraft;

public class LiveReconnectionThread extends Thread {

    @Override
    public void run() {
        final Minecraft mc = Minecraft.getInstance();

        while (mc.isRunning()) {
            if (!LiveClient.INSTANCE.isOpen()) {
                LiveClient.INSTANCE.connect();
            }

            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
