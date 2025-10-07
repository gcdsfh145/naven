package dev.yalan.live.events;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public class EventLiveConnectionStatus implements Event {
    private final boolean isSuccess;
    private final Throwable cause;

    public EventLiveConnectionStatus(boolean isSuccess, Throwable cause) {
        this.isSuccess = isSuccess;
        this.cause = cause;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Throwable getCause() {
        return cause;
    }
}
