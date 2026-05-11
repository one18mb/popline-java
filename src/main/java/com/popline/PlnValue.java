package com.popline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlnValue {
    public enum Type { NULL, BOOL, INT, FLOAT, STRING, OBJECT, ARRAY }

    private Type type;
    private boolean boolVal;
    private long intVal;
    private double floatVal;
    private String stringVal;
    private LinkedHashMap<String, PlnValue> objectVal;
    private ArrayList<PlnValue> arrayVal;

    public PlnValue(Type type) {
        this.type = type;
        switch (type) {
            case OBJECT -> objectVal = new LinkedHashMap<>();
            case ARRAY  -> arrayVal = new ArrayList<>();
        }
    }

    public static PlnValue newObject() { return new PlnValue(Type.OBJECT); }
    public static PlnValue newArray()  { return new PlnValue(Type.ARRAY); }
    public static PlnValue newNull()   { return new PlnValue(Type.NULL); }
    public static PlnValue newBool(boolean v) {
        PlnValue val = new PlnValue(Type.BOOL);
        val.boolVal = v;
        return val;
    }
    public static PlnValue newInt(long v) {
        PlnValue val = new PlnValue(Type.INT);
        val.intVal = v;
        return val;
    }
    public static PlnValue newFloat(double v) {
        PlnValue val = new PlnValue(Type.FLOAT);
        val.floatVal = v;
        return val;
    }
    public static PlnValue newString(String v) {
        PlnValue val = new PlnValue(Type.STRING);
        val.stringVal = v;
        return val;
    }

    public Type getType() { return type; }
    public boolean getBool()   { return boolVal; }
    public long getInt()       { return intVal; }
    public double getFloat()   { return floatVal; }
    public String getString()  { return stringVal; }

    public void addToObject(String key, PlnValue val) {
        if (type != Type.OBJECT) throw new IllegalStateException("not an object");
        objectVal.put(key, val);
    }
    public void addToArray(PlnValue val) {
        if (type != Type.ARRAY) throw new IllegalStateException("not an array");
        arrayVal.add(val);
    }
    public Map<String, PlnValue> getObject() { return objectVal; }
    public List<PlnValue> getArray()         { return arrayVal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlnValue other)) return false;
        if (type != other.type) return false;
        return switch (type) {
            case NULL   -> true;
            case BOOL   -> boolVal == other.boolVal;
            case INT    -> intVal == other.intVal;
            case FLOAT  -> floatVal == other.floatVal;
            case STRING -> stringVal.equals(other.stringVal);
            case OBJECT -> objectVal.equals(other.objectVal);
            case ARRAY  -> arrayVal.equals(other.arrayVal);
        };
    }

    @Override
    public String toString() {
        return switch (type) {
            case NULL   -> "null";
            case BOOL   -> String.valueOf(boolVal);
            case INT    -> String.valueOf(intVal);
            case FLOAT  -> String.valueOf(floatVal);
            case STRING -> "\"" + stringVal.replace("\"", "\"\"") + "\"";
            case OBJECT -> objectVal.toString();
            case ARRAY  -> arrayVal.toString();
        };
    }
}
