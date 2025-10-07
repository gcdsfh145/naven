package com.heypixel.heypixelmod.obsoverlay.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.Minecraft;

public class ConfigManager {
    public final File mainFile;
    public final File configFile;
    public final File moduleConfigFile;
    private final AtomicBoolean isSaving = new AtomicBoolean(false);

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ConfigManager() {
        this.mainFile = initializeMainFile();
        this.configFile = new File(mainFile, "Configs");
        this.moduleConfigFile = new File(mainFile, "module.ini");
    }

    private File initializeMainFile() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                File gameDirectory = mc.gameDirectory;
                File mainDir = new File(gameDirectory, "ShaoYuNaven");
                System.out.println("Config folder set to: " + mainDir.getAbsolutePath());
                return mainDir;
            } else {
                throw new RuntimeException("Minecraft instance is null");
            }
        } catch (Exception e) {
            System.err.println("Failed to get Minecraft instance, using fallback path: " + e.getMessage());
            return new File(System.getProperty("user.home"), "ShaoYuNaven");
        }
    }

    public void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || Naven.getInstance() == null) return;

        try {
            if (!mainFile.exists()) {
                if (!mainFile.mkdirs()) {
                    System.err.println("Failed to create main directory: " + mainFile.getAbsolutePath());
                } else {
                    System.out.println("Main directory created: " + mainFile.getAbsolutePath());
                }
            }

            if (!configFile.exists()) {
                if (!configFile.mkdirs()) {
                    System.err.println("Failed to create config directory: " + configFile.getAbsolutePath());
                } else {
                    System.out.println("Config directory created: " + configFile.getAbsolutePath());
                }
            }

            if (Naven.getInstance().getModuleManager().getModules().isEmpty()) {
                System.err.println("Warning: No modules found when initializing ConfigManager!");
                return;
            }

            if (this.moduleConfigFile.exists()) {
                System.out.println("Loading module config from: " + moduleConfigFile.getAbsolutePath());
                final JsonObject moduleConfig = (JsonObject) JsonParser.parseString(Files.readString(moduleConfigFile.toPath()));

                int loadedModules = 0;
                int loadedValues = 0;

                for (Module module : Naven.getInstance().getModuleManager().getModules()) {
                    final String name = module.getName();

                    if (moduleConfig.has(name)) {
                        final JsonObject singleModule = moduleConfig.getAsJsonObject(name);

                        if (singleModule.has("State")) {
                            boolean state = singleModule.get("State").getAsBoolean();
                            module.setEnabledWhenConfigChange(state);
                            System.out.println("Loaded module state: " + name + " = " + state);
                            loadedModules++;
                        }

                        if (singleModule.has("KeyBind")) {
                            int keyBind = singleModule.get("KeyBind").getAsInt();
                            module.setKey(keyBind);
                            System.out.println("Loaded module keybind: " + name + " = " + keyBind);
                        }

                        if (singleModule.has("Values")) {
                            final JsonObject valuesConfig = singleModule.get("Values").getAsJsonObject();
                            List<Value> moduleValues = Naven.getInstance().getValueManager().getValuesByHasValue(module);

                            for (Value value : moduleValues) {
                                String valueName = value.getName();
                                if (valuesConfig.has(valueName)) {
                                    try {
                                        loadValueFromJson(value, valuesConfig.get(valueName));
                                        System.out.println("Loaded value: " + name + "." + valueName + " = " + getValueAsString(value));
                                        loadedValues++;
                                    } catch (Exception e) {
                                        System.err.println("Failed to load value " + valueName + " for module " + name + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }

                System.out.println("Config loading complete: " + loadedModules + " modules, " + loadedValues + " values loaded");
            } else {
                System.out.println("No existing config found, creating new one");
                save();
            }
        } catch (Throwable e) {
            System.err.println("Error during config initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void save() {
        if (isSaving.get()) {
            System.out.println("Save operation already in progress, skipping...");
            return;
        }

        isSaving.set(true);
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || Naven.getInstance() == null) {
                System.err.println("Cannot save config: Minecraft or Naven instance is null");
                return;
            }

            if (Naven.getInstance().getModuleManager().getModules().isEmpty()) {
                System.err.println("Warning: No modules found when saving config!");
                return;
            }

            final JsonObject moduleConfig = new JsonObject();

            int savedModules = 0;
            int savedValues = 0;

            for (Module module : Naven.getInstance().getModuleManager().getModules()) {
                final JsonObject singleModule = new JsonObject();

                // 保存模块的当前状态
                singleModule.addProperty("State", module.isEnabled());
                singleModule.addProperty("KeyBind", module.getKey());

                final JsonObject valuesConfig = new JsonObject();
                List<Value> moduleValues = Naven.getInstance().getValueManager().getValuesByHasValue(module);

                for (Value value : moduleValues) {
                    try {
                        saveValueToJson(valuesConfig, value);
                        savedValues++;
                    } catch (Exception e) {
                        System.err.println("Failed to save value " + value.getName() + " for module " + module.getName() + ": " + e.getMessage());
                    }
                }
                singleModule.add("Values", valuesConfig);

                moduleConfig.add(module.getName(), singleModule);
                savedModules++;
            }

            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(moduleConfigFile, StandardCharsets.UTF_8))) {
                ConfigManager.GSON.toJson(moduleConfig, bufferedWriter);
                System.out.println("Config saved: " + savedModules + " modules, " + savedValues + " values to " + moduleConfigFile.getAbsolutePath());
            }
        } catch (Throwable e) {
            System.err.println("Error saving config: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isSaving.set(false);
        }
    }

    public void saveImmediately() {
        new Thread(() -> {
            try {
                save();
            } catch (Exception e) {
                System.err.println("Failed to save config immediately: " + e.getMessage());
            }
        }).start();
    }

    private void saveValueToJson(JsonObject parent, Value value) {
        String name = value.getName();
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
    }

    private void loadValueFromJson(Value value, com.google.gson.JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return;

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

    private String getValueAsString(Value value) {
        switch (value.getValueType()) {
            case BOOLEAN:
                return String.valueOf(value.getBooleanValue().getCurrentValue());
            case FLOAT:
                return String.valueOf(value.getFloatValue().getCurrentValue());
            case INT:
                return String.valueOf(value.getIntValue().getCurrentValue());
            case MODE:
                ModeValue modeValue = value.getModeValue();
                return modeValue.getCurrentMode() + " (" + modeValue.getCurrentValue() + ")";
            case STRING:
                return value.getStringValue().getCurrentValue();
            default:
                return "Unknown";
        }
    }

    public File getMainFile() {
        return mainFile;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getModuleConfigFile() {
        return moduleConfigFile;
    }
}