package com.popline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class PopLineSerializer {
    public String serialize(PlnValue value) {
        StringBuilder buf = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        int[] pendingPop = {0};
        boolean[] needKey = {false};
        boolean[] awaitingValue = {false};

        writeValue(buf, stack, pendingPop, needKey, awaitingValue, value);
        return buf.toString();
    }

    private void writeValue(StringBuilder buf, Deque<Character> stack,
                            int[] pendingPop, boolean[] needKey,
                            boolean[] awaitingValue, PlnValue value) {
        switch (value.getType()) {
            case OBJECT -> {
                startContainer(buf, stack, pendingPop, needKey, awaitingValue, '{', 'o');
                for (Map.Entry<String, PlnValue> entry : value.getObject().entrySet()) {
                    flushPop(buf, stack, pendingPop, needKey, awaitingValue);
                    buf.append(entry.getKey()).append(": ");
                    needKey[0] = false;
                    awaitingValue[0] = true;
                    writeValue(buf, stack, pendingPop, needKey, awaitingValue, entry.getValue());
                }
                stack.removeLast();
                pendingPop[0]++;
                if (!stack.isEmpty() && stack.getLast() == 'o') needKey[0] = true;
            }
            case ARRAY -> {
                startContainer(buf, stack, pendingPop, needKey, awaitingValue, '[', 'a');
                for (PlnValue item : value.getArray()) {
                    writeValue(buf, stack, pendingPop, needKey, awaitingValue, item);
                }
                stack.removeLast();
                pendingPop[0]++;
                if (!stack.isEmpty() && stack.getLast() == 'o') needKey[0] = true;
            }
            case NULL   -> putScalar(buf, stack, pendingPop, needKey, awaitingValue, "null");
            case BOOL   -> putScalar(buf, stack, pendingPop, needKey, awaitingValue, value.getBool() ? "true" : "false");
            case INT    -> putScalar(buf, stack, pendingPop, needKey, awaitingValue, String.valueOf(value.getInt()));
            case FLOAT  -> putScalar(buf, stack, pendingPop, needKey, awaitingValue, String.valueOf(value.getFloat()));
            case STRING -> putString(buf, stack, pendingPop, needKey, awaitingValue, value.getString());
        }
    }

    private void startContainer(StringBuilder buf, Deque<Character> stack,
                                int[] pendingPop, boolean[] needKey,
                                boolean[] awaitingValue, char ch, char typ) {
        if (!stack.isEmpty() && stack.getLast() == 'o' && awaitingValue[0]) {
            buf.append(ch);
            awaitingValue[0] = false;
        } else {
            flushPop(buf, stack, pendingPop, needKey, awaitingValue);
            buf.append(ch);
        }
        buf.append('\n');
        stack.addLast(typ);
        needKey[0] = (typ == 'o');
        awaitingValue[0] = false;
    }

    private void putScalar(StringBuilder buf, Deque<Character> stack,
                           int[] pendingPop, boolean[] needKey,
                           boolean[] awaitingValue, String s) {
        if (!stack.isEmpty() && stack.getLast() == 'o') {
            awaitingValue[0] = false;
            buf.append(s).append('\n');
            needKey[0] = true;
        } else {
            flushPop(buf, stack, pendingPop, needKey, awaitingValue);
            buf.append(s).append('\n');
        }
    }

    private void putString(StringBuilder buf, Deque<Character> stack,
                           int[] pendingPop, boolean[] needKey,
                           boolean[] awaitingValue, String s) {
        if (!stack.isEmpty() && stack.getLast() == 'o') {
            awaitingValue[0] = false;
            needKey[0] = true;
        } else {
            flushPop(buf, stack, pendingPop, needKey, awaitingValue);
        }
        buf.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            buf.append(c);
            if (c == '"') buf.append('"');
        }
        buf.append('"').append('\n');
    }

    private void flushPop(StringBuilder buf, Deque<Character> stack,
                          int[] pendingPop, boolean[] needKey,
                          boolean[] awaitingValue) {
        if (pendingPop[0] > 0) {
            buf.append(pendingPop[0]).append(' ');
            pendingPop[0] = 0;
        }
    }
}
