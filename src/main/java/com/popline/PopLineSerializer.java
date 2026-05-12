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
            case OBJECT:
                writeObject(buf, stack, pendingPop, needKey, awaitingValue, value);
                break;
            case ARRAY:
                writeArray(buf, stack, pendingPop, needKey, awaitingValue, value);
                break;
            case NULL:
                putScalar(buf, stack, pendingPop, needKey, awaitingValue, "null");
                break;
            case BOOL:
                putScalar(buf, stack, pendingPop, needKey, awaitingValue, value.getBool() ? "true" : "false");
                break;
            case INT:
                putScalar(buf, stack, pendingPop, needKey, awaitingValue, String.valueOf(value.getInt()));
                break;
            case FLOAT:
                putScalar(buf, stack, pendingPop, needKey, awaitingValue, String.valueOf(value.getFloat()));
                break;
            case STRING:
                putString(buf, stack, pendingPop, needKey, awaitingValue, value.getString());
                break;
        }
    }

    private void writeObject(StringBuilder buf, Deque<Character> stack,
                             int[] pendingPop, boolean[] needKey,
                             boolean[] awaitingValue, PlnValue value) {
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

    private void writeArray(StringBuilder buf, Deque<Character> stack,
                            int[] pendingPop, boolean[] needKey,
                            boolean[] awaitingValue, PlnValue value) {
        writeContainerInline(buf, stack, pendingPop, needKey, awaitingValue, value, true);
    }

    private void writeContainerInline(StringBuilder buf, Deque<Character> stack,
                                       int[] pendingPop, boolean[] needKey,
                                       boolean[] awaitingValue, PlnValue value, boolean first) {
        boolean isObj = value.getType() == PlnValue.Type.OBJECT;
        char ch = isObj ? '{' : '[';
        char typ = isObj ? 'o' : 'a';

        if (first && !stack.isEmpty() && stack.getLast() == 'o' && awaitingValue[0]) {
            buf.append(ch);
            awaitingValue[0] = false;
        } else if (first) {
            flushPop(buf, stack, pendingPop, needKey, awaitingValue);
            buf.append(ch);
        } else {
            buf.append(ch);
        }

        boolean canInline = !isObj && value.getArray().size() > 0 &&
            (value.getArray().get(0).getType() == PlnValue.Type.OBJECT ||
             value.getArray().get(0).getType() == PlnValue.Type.ARRAY);

        if (canInline) {
            stack.addLast('a');
            needKey[0] = false;
            awaitingValue[0] = false;
            writeContainerInline(buf, stack, pendingPop, needKey, awaitingValue,
                value.getArray().get(0), false);
            for (int i = 1; i < value.getArray().size(); i++) {
                writeValue(buf, stack, pendingPop, needKey, awaitingValue, value.getArray().get(i));
            }
            stack.removeLast();
            pendingPop[0]++;
            if (!stack.isEmpty() && stack.getLast() == 'o') needKey[0] = true;
        } else {
            buf.append('\n');
            stack.addLast(typ);
            needKey[0] = (typ == 'o');
            awaitingValue[0] = false;
            if (isObj) {
                for (java.util.Map.Entry<String, PlnValue> entry : value.getObject().entrySet()) {
                    flushPop(buf, stack, pendingPop, needKey, awaitingValue);
                    buf.append(entry.getKey()).append(": ");
                    needKey[0] = false;
                    awaitingValue[0] = true;
                    writeValue(buf, stack, pendingPop, needKey, awaitingValue, entry.getValue());
                }
            } else {
                for (PlnValue item : value.getArray()) {
                    writeValue(buf, stack, pendingPop, needKey, awaitingValue, item);
                }
            }
            stack.removeLast();
            pendingPop[0]++;
            if (!stack.isEmpty() && stack.getLast() == 'o') needKey[0] = true;
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
