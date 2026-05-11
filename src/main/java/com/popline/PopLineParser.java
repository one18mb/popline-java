package com.popline;

import java.util.ArrayDeque;
import java.util.Deque;

public class PopLineParser {
    private final Deque<Character> stack = new ArrayDeque<>();
    private boolean inString = false;
    private final StringBuilder strbuf = new StringBuilder();
    private String error;

    public PlnValue parse(String text) {
        stack.clear();
        inString = false;
        strbuf.setLength(0);
        error = null;

        PlnValue root = null;
        Deque<PlnValue> frames = new ArrayDeque<>();
        String key = null;

        int len = text.length();
        int lineStart = 0;

        for (int pos = 0; pos <= len; pos++) {
            if (pos < len && text.charAt(pos) != '\n') continue;

            String line = text.substring(lineStart, pos);
            if (!line.isEmpty() && line.charAt(line.length()-1) == '\r')
                line = line.substring(0, line.length()-1);

            try {
                Object result = processLine(line, frames, root, key);
                if (result instanceof PlnValue r) root = r;
                else if (result instanceof String k) key = k;
            } catch (PlnParseException e) {
                error = e.getMessage();
                return null;
            }

            lineStart = pos + 1;
        }

        return root;
    }

    private Object processLine(String line, Deque<PlnValue> frames, PlnValue root, String key) {
        // handle multi-line strings
        if (inString) {
            handleStringLine(line, frames, root, key);
            return root;
        }

        // skip empty lines (message separators)
        if (line.isEmpty()) return root;

        // parse pop prefix
        int popCount = 0;
        int valueStart = 0;
        int i = 0;
        while (i < line.length() && Character.isDigit(line.charAt(i))) i++;
        if (i > 0 && i < line.length() && line.charAt(i) == ' ') {
            popCount = Integer.parseInt(line.substring(0, i));
            valueStart = i + 1;
        }

        // pop layers
        for (int p = 0; p < popCount; p++) {
            if (frames.isEmpty()) {
                throw new PlnParseException("pop exceeds nesting depth");
            }
            frames.removeLast();
        }

        String rest = line.substring(valueStart);
        if (rest.isEmpty()) {
            throw new PlnParseException("bare pop line not allowed");
        }

        // root level
        if (frames.isEmpty()) {
            if (rest.equals("{")) {
                PlnValue obj = PlnValue.newObject();
                frames.addLast(obj);
                return obj;
            } else if (rest.equals("[")) {
                PlnValue arr = PlnValue.newArray();
                frames.addLast(arr);
                return arr;
            }
            throw new PlnParseException("top level must be object or array");
        }

        PlnValue top = frames.getLast();
        if (top.getType() == PlnValue.Type.OBJECT) {
            return parseObjectLine(rest, frames, top);
        } else {
            return parseArrayLine(rest, frames, top);
        }
    }

    private Object parseObjectLine(String rest, Deque<PlnValue> frames, PlnValue top) {
        int sep = rest.indexOf(": ");
        if (sep < 0) {
            throw new PlnParseException("object line must be 'key: value': " + rest);
        }
        String key = rest.substring(0, sep);
        if (!isKeyValid(key)) {
            throw new PlnParseException("invalid key name: " + key);
        }
        String valPart = rest.substring(sep + 2);

        if (valPart.equals("{")) {
            PlnValue obj = PlnValue.newObject();
            top.addToObject(key, obj);
            frames.addLast(obj);
        } else if (valPart.equals("[")) {
            PlnValue arr = PlnValue.newArray();
            top.addToObject(key, arr);
            frames.addLast(arr);
        } else {
            PlnValue val = parseScalar(valPart);
            top.addToObject(key, val);
        }
        return top;
    }

    private Object parseArrayLine(String rest, Deque<PlnValue> frames, PlnValue top) {
        if (rest.equals("{")) {
            PlnValue obj = PlnValue.newObject();
            top.addToArray(obj);
            frames.addLast(obj);
        } else if (rest.equals("[")) {
            PlnValue arr = PlnValue.newArray();
            top.addToArray(arr);
            frames.addLast(arr);
        } else {
            PlnValue val = parseScalar(rest);
            top.addToArray(val);
        }
        return top;
    }

    private PlnValue parseScalar(String s) {
        if (s.isEmpty()) throw new PlnParseException("empty value");

        if (s.charAt(0) == '"') {
            return parseQuotedString(s.substring(1));
        }

        // keywords
        if (s.equals("true"))  return PlnValue.newBool(true);
        if (s.equals("false")) return PlnValue.newBool(false);
        if (s.equals("null"))  return PlnValue.newNull();

        // number
        if (s.charAt(0) == '-' || Character.isDigit(s.charAt(0))) {
            try {
                if (s.contains(".") || s.contains("e") || s.contains("E")) {
                    return PlnValue.newFloat(Double.parseDouble(s));
                } else {
                    return PlnValue.newInt(Long.parseLong(s));
                }
            } catch (NumberFormatException e) {
                throw new PlnParseException("bare string must be quoted: " + s);
            }
        }

        throw new PlnParseException("bare string must be quoted: " + s);
    }

    private PlnValue parseQuotedString(String content) {
        int i = 0;
        StringBuilder result = new StringBuilder();
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '"') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    result.append('"');
                    i += 2;
                } else {
                    // check trailing content
                    String after = content.substring(i + 1);
                    if (!after.strip().isEmpty()) {
                        throw new PlnParseException("trailing content after closing quote");
                    }
                    return PlnValue.newString(result.toString());
                }
            } else {
                result.append(c);
                i++;
            }
        }
        // unclosed quote → multi-line string
        inString = true;
        strbuf.setLength(0);
        strbuf.append(content).append('\n');
        return null;
    }

    private void handleStringLine(String line, Deque<PlnValue> frames, PlnValue root, String key) {
        int i = 0;
        StringBuilder result = new StringBuilder();
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    result.append('"');
                    i += 2;
                } else {
                    // line closes the string
                    String after = line.substring(i + 1);
                    if (!after.strip().isEmpty()) {
                        throw new PlnParseException("trailing content after closing quote");
                    }
                    inString = false;
                    String full = strbuf.toString() + result;
                    PlnValue val = PlnValue.newString(full);
                    strbuf.setLength(0);
                    // add to parent
                    PlnValue top = frames.getLast();
                    if (top.getType() == PlnValue.Type.OBJECT) {
                        top.addToObject(key, val);
                    } else {
                        top.addToArray(val);
                    }
                    return;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        // still not closed
        strbuf.append(result).append('\n');
    }

    private boolean isKeyValid(String key) {
        if (key.isEmpty()) return false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == ':' || c == '"' || c == '{' || c == '}' ||
                c == '[' || c == ']' || c == '#' ||
                c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                return false;
            }
        }
        return true;
    }

    public String getError() { return error; }
}
