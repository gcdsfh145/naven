package com.heypixel.heypixelmod.obsoverlay.values;

import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchValueException;
import com.heypixel.heypixelmod.obsoverlay.utils.IntValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import java.util.ArrayList;
import java.util.List;

public class ValueManager {
   private final List<Value> values = new ArrayList<>();

   public void addValue(Value value) {
      this.values.add(value);
   }

   public List<Value> getValuesByHasValue(HasValue key) {
      List<Value> values = new ArrayList<>();

      for (Value value : this.values) {
         if (value.getKey() == key) {
            values.add(value);
         }
      }

      return values;
   }

   public Value getValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name)) {
            return value;
         }
      }

      throw new NoSuchValueException();
   }

   public List<Value> getValues() {
      return this.values;
   }
   public IntValue getIntValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name) && value.getValueType() == ValueType.INT) {
            return value.getIntValue();
         }
      }
      throw new NoSuchValueException();
   }

   public BooleanValue getBooleanValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name) && value.getValueType() == ValueType.BOOLEAN) {
            return value.getBooleanValue();
         }
      }
      throw new NoSuchValueException();
   }

   public FloatValue getFloatValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name) && value.getValueType() == ValueType.FLOAT) {
            return value.getFloatValue();
         }
      }
      throw new NoSuchValueException();
   }

   public ModeValue getModeValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name) && value.getValueType() == ValueType.MODE) {
            return value.getModeValue();
         }
      }
      throw new NoSuchValueException();
   }

   public StringValue getStringValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name) && value.getValueType() == ValueType.STRING) {
            return value.getStringValue();
         }
      }
      throw new NoSuchValueException();
   }
}