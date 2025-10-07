package com.heypixel.heypixelmod.obsoverlay.Config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;

import java.util.function.Supplier;

public abstract class Setting<T> {
    protected final String name;
    protected Module present;
    protected T value;
    protected Supplier<Boolean> visibilityCondition = () -> true;

    public Setting(String name, Module present, T value) {
        this.name = name;
        this.present = present;
        if (present != null) {
            present.getSettings().add(this);
        }
        this.value = value;
    }

    public boolean shouldRender() {
        return visibilityCondition.get();
    }

    public Setting<T> setVisibility(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void toJson(JsonObject object) {
        if (value != null) {
            if (value instanceof Boolean) {
                object.addProperty(name, (Boolean) value);
            } else if (value instanceof Number) {
                object.addProperty(name, (Number) value);
            } else if (value instanceof String) {
                object.addProperty(name, (String) value);
            } else {
                object.addProperty(name, value.toString());
                System.out.println("Warning: Using toString() for setting " + name + " of type " + value.getClass().getSimpleName());
            }
        }
    }

    public void formJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            try {
                if (value instanceof Boolean) {
                    value = (T) Boolean.valueOf(element.getAsBoolean());
                } else if (value instanceof Integer) {
                    value = (T) Integer.valueOf(element.getAsInt());
                } else if (value instanceof Float) {
                    value = (T) Float.valueOf(element.getAsFloat());
                } else if (value instanceof Double) {
                    value = (T) Double.valueOf(element.getAsDouble());
                } else if (value instanceof String) {
                    value = (T) element.getAsString();
                } else {
                    System.err.println("Unsupported setting type: " + value.getClass().getSimpleName());
                }
            } catch (Exception e) {
                System.err.println("Failed to parse setting " + name + ": " + e.getMessage());
            }
        }
    }
}