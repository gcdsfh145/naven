package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;

public class NotificationManagers {
    private final List<Notifications> notifications = new CopyOnWriteArrayList<>();
    public void addNotification(Notifications notification) {
        if (!this.notifications.contains(notification)) {
            this.notifications.add(notification);
        }
    }

    public void addNotification(String title, String message, NotifyType type, int duration) {
        NotificationLevel level = convertNotifyType(type);
        Notifications notifications = new Notifications(level, title, message, duration);
        addNotification(notifications);
    }

    private NotificationLevel convertNotifyType(NotifyType type) {
        switch (type) {
            case SUCCESS:
                return NotificationLevel.SUCCESS;
            case ERROR:
                return NotificationLevel.ERROR;
            case WARNING:
                return NotificationLevel.WARNING;
            case INFO:
                return NotificationLevel.INFO;
            default:
                return NotificationLevel.INFO;
        }
    }


    public void onRenderShadow(EventShader e) {
        for (Notifications notification : this.notifications) {
            SmoothAnimationTimer widthTimer = notification.getWidthTimer();
            SmoothAnimationTimer heightTimer = notification.getHeightTimer();
            Window window = Minecraft.getInstance().getWindow();
            notification.renderShader(
                    e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F, (float)window.getGuiScaledHeight() - heightTimer.value
            );
        }
    }

    public static enum NotifyType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO;
    }

    public void onRender(EventRender2D e) {
        float height = 5.0F;

        for (Notifications notification : this.notifications) {
            e.getStack().pushPose();
            float width = notification.getWidth();
            height += notification.getHeight();
            SmoothAnimationTimer widthTimer = notification.getWidthTimer();
            SmoothAnimationTimer heightTimer = notification.getHeightTimer();
            float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
            if (lifeTime > (float)notification.getMaxAge()) {
                widthTimer.target = 0.0F;
                heightTimer.target = 0.0F;
                if (widthTimer.isAnimationDone(true)) {
                    this.notifications.remove(notification);
                }
            } else {
                widthTimer.target = width;
                heightTimer.target = height;
            }

            widthTimer.update(true);
            heightTimer.update(true);
            Window window = Minecraft.getInstance().getWindow();
            notification.render(e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F, (float)window.getGuiScaledHeight() - heightTimer.value);
            e.getStack().popPose();
        }
    }
}
