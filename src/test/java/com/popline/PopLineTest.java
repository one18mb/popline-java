package com.popline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PopLineTest {

    private PlnValue parse(String text) {
        return new PopLineParser().parse(text);
    }

    private String serialize(PlnValue v) {
        return new PopLineSerializer().serialize(v);
    }

    @Test void testBasicTypes() {
        PlnValue v = parse("{\nname: \"popline\"\n");
        assertNotNull(v);
        assertEquals(PlnValue.Type.OBJECT, v.getType());
        assertEquals("popline", v.getObject().get("name").getString());

        v = parse("{\na: 42\n");
        assertEquals(42L, v.getObject().get("a").getInt());

        v = parse("{\na: 3.14\n");
        assertEquals(PlnValue.Type.FLOAT, v.getObject().get("a").getType());

        v = parse("{\na: true\nb: false\nc: null\n");
        assertTrue(v.getObject().get("a").getBool());
        assertFalse(v.getObject().get("b").getBool());
        assertEquals(PlnValue.Type.NULL, v.getObject().get("c").getType());
    }

    @Test void testNesting() {
        PlnValue v = parse("{\nouter: {\ninner: \"value\"\n");
        assertEquals("value", v.getObject().get("outer").getObject().get("inner").getString());

        v = parse("{\ntags: [\n\"web\"\n\"primary\"\n");
        assertEquals("web", v.getObject().get("tags").getArray().get(0).getString());
        assertEquals(2, v.getObject().get("tags").getArray().size());
    }

    @Test void testPop() {
        PlnValue v = parse("{\nouter: {\ninner: \"value\"\n1 mid: \"other\"\n");
        assertTrue(v.getObject().containsKey("outer"));
        assertTrue(v.getObject().containsKey("mid"));

        v = parse("{\na: {\nb: {\nc: \"deep\"\n2 x: \"top\"\n");
        assertEquals("deep", v.getObject().get("a").getObject().get("b").getObject().get("c").getString());
        assertEquals("top", v.getObject().get("x").getString());
    }

    @Test void testStrings() {
        PlnValue v = parse("{\nmsg: \"He said: \"\"Hello\"\"\"\n");
        assertEquals("He said: \"Hello\"", v.getObject().get("msg").getString());

        v = parse("{\nmsg: \"Line1\nLine2\"\n");
        assertEquals("Line1\nLine2", v.getObject().get("msg").getString());

        v = parse("{\nkey: \"你好世界\"\n");
        assertEquals("你好世界", v.getObject().get("key").getString());
    }

    @Test void testKeys() {
        PlnValue v = parse("{\nmy-key: 1\n中文键: 2\na.b.c: 3\n");
        assertEquals(3, v.getObject().size());
    }

    @Test void testErrors() {
        assertNull(parse("42\n"));
        assertNull(parse("\"str\"\n"));
        assertNull(parse("true\n"));
        assertNull(parse("{\nbad:key: 1\n"));
        assertNull(parse("{\n\"key\": 1\n"));
    }

    @Test void testRoundtrip() {
        PlnValue v = new PopLineParser().parse("{\na: 1\nb: 2\n");
        String s = new PopLineSerializer().serialize(v);
        PlnValue v2 = new PopLineParser().parse(s);
        assertEquals(v, v2);
    }

    @Test void testComplexRoundtrip() {
        String input = "{\nname: \"test\"\ncount: 42\nactive: true\ntags: [\n\"a\"\n\"b\"\n1 nested: {\nkey: \"val\"\n1 msg: \"He said: \"\"Hi\"\"\"\n";
        PlnValue v = parse(input);
        assertNotNull(v);
        String output = serialize(v);
        PlnValue v2 = parse(output);
        assertEquals(v, v2);
    }

    @Test void testArrayRoundtrip() {
        String input = "[\n1\n2\n3\n[\n4\n5\n";
        PlnValue v = parse(input);
        assertNotNull(v);
        String output = serialize(v);
        PlnValue v2 = parse(output);
        assertEquals(v, v2);
    }
}
