package com.popline;

import java.util.ArrayDeque;
import java.util.Deque;

public class PopLineParser {
    private final Deque<Character> stack = new ArrayDeque<>();
    private boolean inString = false;
    private final StringBuilder strbuf = new StringBuilder();
    private String error;
    private String currentKey = "";

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
                if (result instanceof PlnValue) root = (PlnValue) result;
                else if (result instanceof String) key = (String) result;
            } catch (PlnParseException e) {
                error = e.getMessage();
                return null;
            }

            lineStart = pos + 1;
        }

        return root;
    }

    private Object processLine(String line, Deque<PlnValue> frames, PlnValue ignoredRoot, String key) {
        // handle multi-line strings
        if (inString) {
            handleStringLine(line, frames, null, key);
            return null;
        }

        // skip empty lines (message separators)
        if (line.isEmpty()) return null;

        String rest = line;

        // root level — return the new root to the caller
        if (frames.isEmpty()) {
            // Check top-level inline containers: `[ [` or `[ {`
            if (rest.length() > 1 && rest.charAt(0) == '[') {
                String trimmed = rest.substring(1).stripLeading();
                if (trimmed.length() > 0 && (trimmed.charAt(0) == '[' || trimmed.charAt(0) == '{')) {
                    return parseInlineContainers(rest, frames);
                }
            }
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
            parseObjectLine(rest, frames, top);
        } else {
            parseArrayLine(rest, frames, top);
        }
        return null; // don't overwrite root
    }

    /**
     * Parse consecutive container openers on a single line: {@code [ [}, {@code [ {}, etc.
     * Returns the root PlnValue if at top level, or null if added to existing frames.
     */
    private PlnValue parseInlineContainers(String s, Deque<PlnValue> frames) {
        String part = s.strip();
        boolean wasEmpty = frames.isEmpty();
        while (part.length() > 0) {
            char ch = part.charAt(0);
            if (ch != '{' && ch != '[') {
                throw new PlnParseException("inline containers must be '{' or '['");
            }
            PlnValue v = (ch == '{') ? PlnValue.newObject() : PlnValue.newArray();
            if (frames.isEmpty()) {
                frames.addLast(v);
            } else {
                PlnValue top = frames.getLast();
                if (top.getType() == PlnValue.Type.OBJECT) {
                    top.addToObject(currentKey, v);
                } else {
                    top.addToArray(v);
                }
                frames.addLast(v);
            }
            part = part.substring(1).stripLeading();
        }
        return wasEmpty ? frames.getFirst() : null;
    }

    private void parseObjectLine(String rest, Deque<PlnValue> frames, PlnValue top) {
        int sep = rest.indexOf(": ");
        if (sep < 0) {
            throw new PlnParseException("object line must be 'key: value': " + rest);
        }
        String key = rest.substring(0, sep);
        if (!isKeyValid(key)) {
            throw new PlnParseException("invalid key name: " + key);
        }
        String valPart = rest.substring(sep + 2);

        // Check value inline containers: `key: [ [` or `key: [ {`
        if (valPart.length() > 1 && (valPart.charAt(0) == '[' || valPart.charAt(0) == '{')) {
            String trimmed = valPart.substring(1).stripLeading();
            if (trimmed.length() > 0 && (trimmed.charAt(0) == '[' || trimmed.charAt(0) == '{')) {
                currentKey = key;
                parseInlineContainers(valPart, frames);
                return;
            }
        }

        if (valPart.equals("{")) {
            PlnValue obj = PlnValue.newObject();
            top.addToObject(key, obj);
            frames.addLast(obj);
        } else if (valPart.equals("[")) {
            PlnValue arr = PlnValue.newArray();
            top.addToObject(key, arr);
            frames.addLast(arr);
        } else {
            // Check for suffix pop (only for leaf values)
            StringBuilder sb = new StringBuilder(valPart);
            int suffixPop = fwdTrimPopSuffix(sb);
            String trimmedVal = sb.toString();
            if (trimmedVal.isEmpty()) {
                throw new PlnParseException("empty value");
            }
            PlnValue val = parseScalar(trimmedVal);
            if (val == null) {
                // multi-line string started, save the key
                currentKey = key;
                return;
            }
            top.addToObject(key, val);
            // Apply suffix pop (with root protection)
            for (int p = 0; p < suffixPop; p++) {
                if (frames.isEmpty()) break;
                frames.removeLast();
            }
        }
    }

    private void parseArrayLine(String rest, Deque<PlnValue> frames, PlnValue top) {
        // Check array element inline containers: `[ [`、`[ {`、`{ [`、`{ {`
        if (rest.length() > 1 && (rest.charAt(0) == '[' || rest.charAt(0) == '{')) {
            String trimmed = rest.substring(1).stripLeading();
            if (trimmed.length() > 0 && (trimmed.charAt(0) == '[' || trimmed.charAt(0) == '{')) {
                parseInlineContainers(rest, frames);
                return;
            }
        }
        if (rest.equals("{")) {
            PlnValue obj = PlnValue.newObject();
            top.addToArray(obj);
            frames.addLast(obj);
        } else if (rest.equals("[")) {
            PlnValue arr = PlnValue.newArray();
            top.addToArray(arr);
            frames.addLast(arr);
        } else {
            // Check for suffix pop (only for leaf values)
            StringBuilder sb = new StringBuilder(rest);
            int suffixPop = fwdTrimPopSuffix(sb);
            String trimmedRest = sb.toString();
            if (trimmedRest.isEmpty()) {
                throw new PlnParseException("empty value");
            }
            PlnValue val = parseScalar(trimmedRest);
            if (val == null) {
                // multi-line string started
                return;
            }
            top.addToArray(val);
            // Apply suffix pop (with root protection)
            for (int p = 0; p < suffixPop; p++) {
                if (frames.isEmpty()) break;
                frames.removeLast();
            }
        }
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
                    // line closes the string -- check for suffix pop
                    String after = line.substring(i + 1);
                    int suffixPop = 0;
                    if (!after.strip().isEmpty()) {
                        suffixPop = popSuffixAfter(after);
                        if (suffixPop < 0) {
                            throw new PlnParseException("trailing content after closing quote");
                        }
                    }
                    inString = false;
                    String full = strbuf.toString() + result;
                    PlnValue val = PlnValue.newString(full);
                    strbuf.setLength(0);
                    // add to parent
                    PlnValue top = frames.getLast();
                    if (top.getType() == PlnValue.Type.OBJECT) {
                        top.addToObject(currentKey, val);
                    } else {
                        top.addToArray(val);
                    }
                    // apply suffix pop (with root protection)
                    for (int p = 0; p < suffixPop; p++) {
                        if (frames.isEmpty()) break;
                        frames.removeLast();
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

    /**
     * Checks if the string ends with " N" (space followed by number).
     * If so, removes the suffix from the StringBuilder and returns the pop count.
     * Returns 0 if no pop suffix is present.
     */
    /** Forward-scan for " N" pop suffix: when space found, check if rest is all digits */
    private int fwdTrimPopSuffix(StringBuilder sb) {
        boolean inString = false;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '"') inString = !inString;
            if (!inString && c == ' ') {
                boolean allDigits = true;
                for (int j = i + 1; j < sb.length(); j++) {
                    if (sb.charAt(j) < '0' || sb.charAt(j) > '9') { allDigits = false; break; }
                }
                if (allDigits && i + 1 < sb.length()) {
                    int popCount = Integer.parseInt(sb.substring(i + 1));
                    sb.setLength(i);
                    return popCount;
                }
            }
        }
        return 0;
    }

    /**
     * Validates content after closing quote in multi-line strings.
     * Returns pop count (0 for empty, N for " N" suffix, -1 for invalid).
     */
    private int popSuffixAfter(String s) {
        if (s.isEmpty()) return 0;
        if (s.charAt(0) != ' ') return -1;
        if (s.length() < 2 || !Character.isDigit(s.charAt(1))) return -1;
        int n = 0;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return -1;
            n = n * 10 + (c - '0');
        }
        return n;
    }

    private boolean isKeyValid(String key) {
        if (key.isEmpty()) return false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == ':' || c == '"' || c == '{' ||
                c == '[' || c == '#' ||
                c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                return false;
            }
        }
        return true;
    }

    public String getError() { return error; }
}
