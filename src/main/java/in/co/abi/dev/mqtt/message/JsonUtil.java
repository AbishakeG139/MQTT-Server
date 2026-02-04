package in.co.abi.dev.mqtt.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for JSON serialization and deserialization.
 * Thread-safe singleton ObjectMapper for performance.
 */
public final class JsonUtil {

    private static final Logger logger = LogManager.getLogger(JsonUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Serializes an object to JSON bytes.
     *
     * @param obj The object to serialize
     * @return JSON bytes
     * @throws JsonProcessingException if serialization fails
     */
    public static byte[] toJson(Object obj) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(obj);
    }

    /**
     * Serializes an object to JSON string.
     *
     * @param obj The object to serialize
     * @return JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJsonString(Object obj) throws JsonProcessingException {
        return MAPPER.writeValueAsString(obj);
    }

    /**
     * Deserializes JSON bytes to an object of the specified class.
     *
     * @param bytes The JSON bytes
     * @param clazz The target class
     * @param <T>   The type parameter
     * @return The deserialized object
     * @throws IOException if deserialization fails
     */
    public static <T> T fromJson(byte[] bytes, Class<T> clazz) throws IOException {
        return MAPPER.readValue(bytes, clazz);
    }

    /**
     * Deserializes a JSON string to an object of the specified class.
     *
     * @param json  The JSON string
     * @param clazz The target class
     * @param <T>   The type parameter
     * @return The deserialized object
     * @throws IOException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }

    /**
     * Parses JSON bytes into a JsonNode for dynamic access.
     *
     * @param bytes The JSON bytes
     * @return JsonNode representation
     * @throws IOException if parsing fails
     */
    public static JsonNode parseJson(byte[] bytes) throws IOException {
        return MAPPER.readTree(bytes);
    }

    /**
     * Parses JSON string into a JsonNode for dynamic access.
     *
     * @param json The JSON string
     * @return JsonNode representation
     * @throws IOException if parsing fails
     */
    public static JsonNode parseJson(String json) throws IOException {
        return MAPPER.readTree(json);
    }

    /**
     * Validates if the byte array contains valid JSON.
     *
     * @param bytes The bytes to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        try {
            MAPPER.readTree(bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Validates if the string contains valid JSON.
     *
     * @param json The string to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Pretty prints JSON bytes.
     *
     * @param bytes The JSON bytes
     * @return Pretty-printed JSON string, or error message if invalid
     */
    public static String prettyPrint(byte[] bytes) {
        try {
            JsonNode node = MAPPER.readTree(bytes);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException e) {
            logger.warn("Failed to pretty print JSON: {}", e.getMessage());
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets the shared ObjectMapper instance.
     * Use with caution - prefer the utility methods for thread safety.
     *
     * @return The ObjectMapper instance
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
