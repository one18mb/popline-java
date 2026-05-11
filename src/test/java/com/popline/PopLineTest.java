package com.popline;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class PopLineTest {

    private PlnValue parse(String text) {
        return Pln.parse(text);
    }

    private String serialize(PlnValue v) {
        return Pln.stringify(v);
    }

    // ═══════════════ Unit Tests ═══════════════

    @Test void testBasicTypes() {
        PlnValue v = parse("{\nname: \"popline\"\n");
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
    }

    @Test void testPop() {
        PlnValue v = parse("{\nouter: {\ninner: \"x\"\n1 mid: \"y\"\n");
        assertTrue(v.getObject().containsKey("mid"));

        v = parse("{\na: {\nb: {\nc: \"deep\"\n2 x: \"top\"\n");
        assertEquals("top", v.getObject().get("x").getString());
    }

    @Test void testStrings() {
        PlnValue v = parse("{\nmsg: \"He said: \"\"Hello\"\"\"\n");
        assertEquals("He said: \"Hello\"", v.getObject().get("msg").getString());

        v = parse("{\nkey: \"你好世界\"\n");
        assertEquals("你好世界", v.getObject().get("key").getString());
    }

    @Test void testErrors() {
        assertNull(parse("42\n"));
        assertNull(parse("\"str\"\n"));
        assertNull(parse("true\n"));
        assertNull(parse("{\nbad:key: 1\n"));
        assertNull(parse("{\n\"key\": 1\n"));
    }

    // ═══════════════ Roundtrip ═══════════════

    @Test void testRoundtrip() {
        String[] cases = {
            "{\na: 1\n",
            "{\na: {\nb: 1\nc: 2\n1 d: 3\n",
            "[\n1\n2\n3\n",
            "{\na: [\n1\n2\n1 b: true\n",
            "{\na: true\nb: false\nc: null\n",
        };
        for (String input : cases) {
            PlnValue v1 = parse(input);
            assertNotNull(v1);
            String s = serialize(v1);
            PlnValue v2 = parse(s);
            assertNotNull(v2);
            assertEquals(v1, v2);
        }
    }

    // ═══════════════ Real Data Consistency ═══════════════

    @Test void testRealDataConsistency() throws Exception {
        File jsonFile = new File("package.json");
        File plnFile = new File("package.pln");
        if (!jsonFile.exists() || !plnFile.exists()) return;

        String jsonText = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()));
        String plnText = new String(java.nio.file.Files.readAllBytes(plnFile.toPath()));

        JSONObject jsonObj = new JSONObject(new JSONTokener(jsonText));
        PlnValue plnVal = parse(plnText);
        assertNotNull(plnVal);

        // Convert PopLine to JSONObject for comparison
        JSONObject plnAsJson = plnToJson(plnVal);
        assertEquals(jsonObj.toString(), plnAsJson.toString(), "PopLine vs JSON mismatch");

        // Roundtrip
        String s = serialize(plnVal);
        PlnValue v2 = parse(s);
        assertEquals(plnVal, v2, "PopLine roundtrip mismatch");

        System.out.printf("  data: JSON=%dB, PopLine=%dB (%.1f%%)%n",
            jsonText.length(), plnText.length(),
            (double)plnText.length() / jsonText.length() * 100);
    }

    private JSONObject plnToJson(PlnValue v) {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, PlnValue> e : v.getObject().entrySet()) {
            obj.put(e.getKey(), plnValueToJson(e.getValue()));
        }
        return obj;
    }

    private Object plnValueToJson(PlnValue v) {
        switch (v.getType()) {
            case NULL:   return JSONObject.NULL;
            case BOOL:   return v.getBool();
            case INT:    return v.getInt();
            case FLOAT:  return v.getFloat();
            case STRING: return v.getString();
            case OBJECT: return plnToJson(v);
            case ARRAY: {
                List<Object> list = new ArrayList<>();
                for (PlnValue item : v.getArray()) list.add(plnValueToJson(item));
                return list;
            }
        }
        return null;
    }

    // ═══════════════ Performance Benchmark ═══════════════

    @Test void testBenchmark() throws Exception {
        File jsonFile = new File("package.json");
        File plnFile = new File("package.pln");
        if (!jsonFile.exists() || !plnFile.exists()) return;

        String jsonText = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()));
        String plnText = new String(java.nio.file.Files.readAllBytes(plnFile.toPath()));

        JSONObject jsonObj = new JSONObject(new JSONTokener(jsonText));
        PlnValue plnVal = parse(plnText);
        assertNotNull(plnVal);

        int N = 5000;
        System.out.println("\n── Performance Benchmark (" + N + " iterations) ──");

        // JSON serialize
        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) jsonObj.toString();
        long t1 = System.nanoTime();
        double jsSer = (t1 - t0) / 1_000_000.0;

        // PopLine serialize
        t0 = System.nanoTime();
        for (int i = 0; i < N; i++) serialize(plnVal);
        t1 = System.nanoTime();
        double plSer = (t1 - t0) / 1_000_000.0;

        System.out.printf("  %-26s %8.0f ms  %8.0f us/op%n", "JSON.toString", jsSer, jsSer / N * 1000);
        System.out.printf("  %-26s %8.0f ms  %8.0f us/op%n", "PopLine.serialize", plSer, plSer / N * 1000);
        System.out.printf("  %-26s %7.2fx%n", "PopLine/JSON", plSer / jsSer);

        // JSON parse
        t0 = System.nanoTime();
        for (int i = 0; i < N; i++) new JSONObject(new JSONTokener(jsonText));
        t1 = System.nanoTime();
        double jsPar = (t1 - t0) / 1_000_000.0;

        // PopLine parse
        t0 = System.nanoTime();
        for (int i = 0; i < N; i++) parse(plnText);
        t1 = System.nanoTime();
        double plPar = (t1 - t0) / 1_000_000.0;

        System.out.printf("  %-26s %8.0f ms  %8.0f us/op%n", "JSONTokener", jsPar, jsPar / N * 1000);
        System.out.printf("  %-26s %8.0f ms  %8.0f us/op%n", "PopLine.parse", plPar, plPar / N * 1000);
        System.out.printf("  %-26s %7.2fx%n", "PopLine/JSON", plPar / jsPar);
    }
}
