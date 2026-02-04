package in.co.abi.dev.mqtt.message;

import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for JsonUtil.
 */
public class JsonUtilTest {

    @Test
    public void testToJson_SimpleObject() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);

        byte[] json = JsonUtil.toJson(data);
        String jsonStr = new String(json, StandardCharsets.UTF_8);

        assertTrue(jsonStr.contains("\"name\""));
        assertTrue(jsonStr.contains("\"test\""));
        assertTrue(jsonStr.contains("\"value\""));
        assertTrue(jsonStr.contains("123"));
    }

    @Test
    public void testFromJson_ToMap() throws Exception {
        String json = "{\"name\":\"test\",\"count\":42}";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = JsonUtil.fromJson(json, Map.class);

        assertEquals("test", result.get("name"));
        assertEquals(42, result.get("count"));
    }

    @Test
    public void testFromJson_Bytes() throws Exception {
        byte[] json = "{\"active\":true}".getBytes(StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = JsonUtil.fromJson(json, Map.class);

        assertEquals(true, result.get("active"));
    }

    @Test
    public void testIsValidJson_ValidObject() {
        assertTrue(JsonUtil.isValidJson("{\"key\":\"value\"}"));
        assertTrue(JsonUtil.isValidJson("{\"key\":\"value\"}".getBytes()));
    }

    @Test
    public void testIsValidJson_ValidArray() {
        assertTrue(JsonUtil.isValidJson("[1, 2, 3]"));
        assertTrue(JsonUtil.isValidJson("[1, 2, 3]".getBytes()));
    }

    @Test
    public void testIsValidJson_NestedStructure() {
        String json = "{\"user\":{\"id\":1},\"tags\":[\"a\",\"b\"]}";
        assertTrue(JsonUtil.isValidJson(json));
    }

    @Test
    public void testIsValidJson_Invalid() {
        assertFalse(JsonUtil.isValidJson("{invalid}"));
        assertFalse(JsonUtil.isValidJson("not json at all"));
        assertFalse(JsonUtil.isValidJson("{\"unclosed\":"));
    }

    @Test
    public void testIsValidJson_NullAndEmpty() {
        assertFalse(JsonUtil.isValidJson((String) null));
        assertFalse(JsonUtil.isValidJson(""));
        assertFalse(JsonUtil.isValidJson((byte[]) null));
        assertFalse(JsonUtil.isValidJson(new byte[0]));
    }

    @Test
    public void testParseJson_DynamicAccess() throws Exception {
        String json = "{\"name\":\"test\",\"nested\":{\"id\":42}}";

        com.fasterxml.jackson.databind.JsonNode node = JsonUtil.parseJson(json);

        assertEquals("test", node.get("name").asText());
        assertEquals(42, node.get("nested").get("id").asInt());
    }

    @Test
    public void testToJsonString() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("message", "hello");

        String json = JsonUtil.toJsonString(data);

        assertTrue(json.contains("\"message\""));
        assertTrue(json.contains("\"hello\""));
    }

    @Test
    public void testPrettyPrint() {
        byte[] json = "{\"name\":\"test\"}".getBytes();

        String pretty = JsonUtil.prettyPrint(json);

        assertTrue(pretty.contains("name"));
        assertTrue(pretty.contains("test"));
        // Pretty print should have newlines/indentation
        assertTrue(pretty.contains("\n") || pretty.length() > "{\"name\":\"test\"}".length());
    }

    @Test(expected = IOException.class)
    public void testFromJson_InvalidThrows() throws Exception {
        JsonUtil.fromJson("not valid json", Map.class);
    }

    @Test
    public void testRoundTrip() throws Exception {
        Map<String, Object> original = new HashMap<>();
        original.put("string", "hello");
        original.put("number", 42);
        original.put("boolean", true);

        byte[] json = JsonUtil.toJson(original);

        @SuppressWarnings("unchecked")
        Map<String, Object> restored = JsonUtil.fromJson(json, Map.class);

        assertEquals(original.get("string"), restored.get("string"));
        assertEquals(original.get("number"), restored.get("number"));
        assertEquals(original.get("boolean"), restored.get("boolean"));
    }
}
