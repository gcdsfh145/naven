package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntValue extends Value {
    private int currentValue;
    private final int minValue;
    private final int maxValue;
    private final int step;
    private final Consumer<Value> update;

    public IntValue(HasValue key, String name, int defaultValue, int minValue, int maxValue, int step, Consumer<Value> update, Supplier<Boolean> visibility) {
        super(key, name, visibility);
        this.currentValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.update = update;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.INT;
    }

    @Override
    public IntValue getIntValue() {
        return this;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int value) {
        if (value >= minValue && value <= maxValue) {
            this.currentValue = value;
            if (update != null) {
                update.accept(this);
            }
        }
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getStep() {
        return step;
    }

    public String getValueAsString() {
        return Integer.toString(currentValue);
    }

    public void setValueFromString(String value) {
        try {
            int intValue = Integer.parseInt(value);
            setCurrentValue(intValue);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse IntValue from string: " + value);
        }
    }
}