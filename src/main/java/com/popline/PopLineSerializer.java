package com.popline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class PopLineSerializer {
    public String serialize(PlnValue value) {
        StringBuilder buf = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        writeValue(buf, stack, value, 0);
        return buf.toString();
    }

    private void writeValue(StringBuilder buf, Deque<Character> stack,
                            PlnValue value, int closePop) {
        switch (value.getType()) {
            case OBJECT:
                writeObject(buf, stack, value, closePop);
                break;
            case ARRAY:
                writeArray(buf, stack, value, closePop);
                break;
            case NULL:
                putScalar(buf, stack, "null", closePop);
                break;
            case BOOL:
                putScalar(buf, stack, value.getBool() ? "true" : "false", closePop);
                break;
            case INT:
                putScalar(buf, stack, String.valueOf(value.getInt()), closePop);
                break;
            case FLOAT:
                putScalar(buf, stack, String.valueOf(value.getFloat()), closePop);
                break;
            case STRING:
                putString(buf, stack, value.getString(), closePop);
                break;
        }
    }

    private void writeObject(StringBuilder buf, Deque<Character> stack,
                             PlnValue value, int closePop) {
        startContainer(buf, stack, '{');
        stack.addLast('o');
        int n = value.getObject().entrySet().size();
        int i = 0;
        for (Map.Entry<String, PlnValue> entry : value.getObject().entrySet()) {
            int childPop = 0;
            if (i == n - 1 ) childPop = closePop + 1;
            buf.append(entry.getKey()).append(": ");
            writeValue(buf, stack, entry.getValue(), childPop);
            i++;
        }
        stack.removeLast();
        if (!stack.isEmpty() && stack.getLast() == 'o') {/* needKey = true */}
    }

    private void writeArray(StringBuilder buf, Deque<Character> stack,
                            PlnValue value, int closePop) {
        writeContainerInline(buf, stack, value, true, closePop);
    }

    private void writeContainerInline(StringBuilder buf, Deque<Character> stack,
                                       PlnValue value, boolean first, int closePop) {
        boolean isObj = value.getType() == PlnValue.Type.OBJECT;
        char ch = isObj ? '{' : '[';
        char typ = isObj ? 'o' : 'a';

        if (first && !stack.isEmpty() && stack.getLast() == 'o') {
            // awaitingValue is tracked implicitly — if top is object, we're awaiting value
            buf.append(ch);
        } else if (first) {
            buf.append(ch);
        } else {
            buf.append(ch);
        }

        // Non-inline path for correct closePop propagation
        buf.append('\n');
        stack.addLast(typ);
        if (isObj) {
            int n = value.getObject().entrySet().size();
            int i = 0;
            for (Map.Entry<String, PlnValue> entry : value.getObject().entrySet()) {
                int childPop = 0;
                if (i == n - 1 ) childPop = closePop + 1;
                buf.append(entry.getKey()).append(": ");
                writeValue(buf, stack, entry.getValue(), childPop);
                i++;
            }
        } else {
            int n = value.getArray().size();
            for (int i = 0; i < n; i++) {
                int childPop = 0;
                if (i == n - 1 ) childPop = closePop + 1;
                writeValue(buf, stack, value.getArray().get(i), childPop);
            }
        }
        stack.removeLast();
        if (!stack.isEmpty() && stack.getLast() == 'o') {/* needKey = true */}
    }

    private void startContainer(StringBuilder buf, Deque<Character> stack,
                                char ch) {
        if (!stack.isEmpty() && stack.getLast() == 'o') {
            buf.append(ch);
        } else {
            buf.append(ch);
        }
        buf.append('\n');
    }

    private void putScalar(StringBuilder buf, Deque<Character> stack,
                           String s, int closePop) {
        buf.append(s);
        if (closePop > 0) buf.append(' ').append(closePop);
        buf.append('\n');
        if (!stack.isEmpty() && stack.getLast() == 'o') {/* needKey = true */}
    }

    private void putString(StringBuilder buf, Deque<Character> stack,
                           String s, int closePop) {
        buf.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            buf.append(c);
            if (c == '"') buf.append('"');
        }
        buf.append('"');
        if (closePop > 0) buf.append(' ').append(closePop);
        buf.append('\n');
    }
}
