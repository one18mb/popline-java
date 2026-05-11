package com.popline;

/**
 * PopLine facade — unified API following JSON conventions.
 *
 * Usage:
 *   PlnValue v = Pln.parse("{\nkey: \"value\"\n");
 *   String s = Pln.stringify(v);
 */
public class Pln {
    public static PlnValue parse(String text) {
        return new PopLineParser().parse(text);
    }

    public static String stringify(PlnValue value) {
        return new PopLineSerializer().serialize(value);
    }
}
