package com.heypixel.heypixelmod.obsoverlay.Config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import com.heypixel.heypixelmod.obsoverlay.utils.IntValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class SwitchModuleConfig extends Config {

    public SwitchModuleConfig(File file) {
        super(file);
    }

    @Override
    public void read() throws Throwable {
        System.out.println("Reading config from: " + file.getAbsolutePath());

        if (!file.exists()) {
            throw new Exception("Config file does not exist: " + file.getAbsolutePath());
        }

        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        JsonObject moduleConfig = JsonParser.parseString(content).getAsJsonObject();

        if (Naven.getInstance() == null) {
            throw new Exception("Naven instance is null");
        }

        for (Module module : Naven.getInstance().getModuleManager().getModules()) {
            boolean wasEnabled = module.isEnabled();
            module.setState(false);
            if (wasEnabled) {
                Naven.getInstance().getEventManager().unregister(module);
                module.onDisable();
            }
        }

        for (Module module : Naven.getInstance().getModuleManager().getModules()) {
            String name = module.getName();

            if (moduleConfig.has(name)) {
                JsonObject moduleData = moduleConfig.getAsJsonObject(name);

                if (moduleData.has("State")) {
                    boolean state = moduleData.get("State").getAsBoolean();
                    module.setEnabledWhenConfigChange(state);
                }

                if (moduleData.has("KeyBind")) {
                    int keyBind = moduleData.get("KeyBind").getAsInt();
                    module.setKey(keyBind);
                }

                if (moduleData.has("Values")) {
                    JsonObject valuesData = moduleData.get("Values").getAsJsonObject();
                    List<Value> moduleValues = Naven.getInstance().getValueManager().getValuesByHasValue(module);

                    for (Value value : moduleValues) {
                        String valueName = value.getName();
                        if (valuesData.has(valueName)) {
                            try {
                                loadValueFromJson(value, valuesData.get(valueName));
                            } catch (Exception e) {
                                System.err.println("Failed to load value " + valueName + " for module " + name);
                            }
                        }
                    }
                }
            }
        }

        if (Naven.getInstance().getConfigManager() != null) {
            System.out.println("Updating main configuration after loading custom config...");
            Naven.getInstance().getConfigManager().saveImmediately();
        }
    }

    @Override
    public boolean write() throws Throwable {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exception("Failed to create config directory: " + parentDir.getAbsolutePath());
            }
        }

        JsonObject moduleConfig = new JsonObject();

        for (Module module : Naven.getInstance().getModuleManager().getModules()) {
            JsonObject singleModule = new JsonObject();

            singleModule.addProperty("State", module.isEnabled());
            singleModule.addProperty("KeyBind", module.getKey());

            JsonObject valuesConfig = new JsonObject();
            List<Value> moduleValues = Naven.getInstance().getValueManager().getValuesByHasValue(module);

            for (Value value : moduleValues) {
                try {
                    saveValueToJson(valuesConfig, value);
                } catch (Exception e) {
                    System.err.println("Failed to save value " + value.getName() + " for module " + module.getName());
                }
            }
            singleModule.add("Values", valuesConfig);

            moduleConfig.add(module.getName(), singleModule);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            ConfigManager.GSON.toJson(moduleConfig, writer);
            System.out.println("Custom config saved: " + file.getAbsolutePath());

            if (!file.getAbsolutePath().equals(Naven.getInstance().getConfigManager().getModuleConfigFile().getAbsolutePath())) {
                System.out.println("Also updating main configuration...");
                Naven.getInstance().getConfigManager().saveImmediately();
            }

            return true;
        } catch (Exception e) {
            throw new Exception("Failed to write config file: " + e.getMessage());
        }
    }

    private void saveValueToJson(JsonObject parent, Value value) {
        String name = value.getName();
        try {
            switch (value.getValueType()) {
                case BOOLEAN:
                    BooleanValue booleanValue = value.getBooleanValue();
                    parent.addProperty(name, booleanValue.getCurrentValue());
                    break;
                case FLOAT:
                    FloatValue floatValue = value.getFloatValue();
                    parent.addProperty(name, floatValue.getCurrentValue());
                    break;
                case INT:
                    IntValue intValue = value.getIntValue();
                    parent.addProperty(name, intValue.getCurrentValue());
                    break;
                case MODE:
                    ModeValue modeValue = value.getModeValue();
                    parent.addProperty(name, modeValue.getCurrentValue());
                    break;
                case STRING:
                    StringValue stringValue = value.getStringValue();
                    parent.addProperty(name, stringValue.getCurrentValue());
                    break;
                default:
                    System.err.println("Unknown value type for: " + name);
            }
        } catch (Exception e) {
            System.err.println("Error saving value " + name + ": " + e.getMessage());
        }
    }

    private void loadValueFromJson(Value value, com.google.gson.JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }

        try {
            switch (value.getValueType()) {
                case BOOLEAN:
                    BooleanValue booleanValue = value.getBooleanValue();
                    booleanValue.setCurrentValue(element.getAsBoolean());
                    break;
                case FLOAT:
                    FloatValue floatValue = value.getFloatValue();
                    floatValue.setCurrentValue(element.getAsFloat());
                    break;
                case INT:
                    IntValue intValue = value.getIntValue();
                    intValue.setCurrentValue(element.getAsInt());
                    break;
                case MODE:
                    ModeValue modeValue = value.getModeValue();
                    modeValue.setCurrentValue(element.getAsInt());
                    break;
                case STRING:
                    StringValue stringValue = value.getStringValue();
                    stringValue.setCurrentValue(element.getAsString());
                    break;
                default:
                    System.err.println("Unknown value type for: " + value.getName());
            }
        } catch (Exception e) {
            System.err.println("Failed to parse value " + value.getName() + ": " + e.getMessage());
        }
    }
}