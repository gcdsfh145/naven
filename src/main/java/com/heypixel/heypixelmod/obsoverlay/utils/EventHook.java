package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;

import java.lang.reflect.Method;

public class EventHook {
    private final EventInterface eventInterface;
    private final Method method;
    private final boolean ignoreCondition;

    /**
     * 使用指定的监听器、方法和事件目标构造一个 EventHook 实例。
     *
     * @param eventInterface    监听器实例
     * @param method      用于处理事件的方法
     * @param eventTarget 提供事件目标信息的注解
     */
    public EventHook(EventInterface eventInterface, Method method, EventTarget eventTarget) {
        this.eventInterface = eventInterface;
        this.method = method;
        this.ignoreCondition = eventTarget.ignoreCondition();

        if (!method.canAccess(eventInterface)) {
            this.method.setAccessible(true);
        }
    }

    /**
     * 获取与此事件钩子关联的监听器实例。
     *
     * @return 监听器实例
     */
    public EventInterface getEventInterface() {
        return eventInterface;
    }

    /**
     * 获取与此事件钩子关联的方法。
     *
     * @return 要调用的方法
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 检查是否应忽略此事件钩子的条件。
     *
     * @return 如果应忽略条件则返回 true，否则返回 false
     */
    public boolean isIgnoreCondition() {
        return ignoreCondition;
    }
}