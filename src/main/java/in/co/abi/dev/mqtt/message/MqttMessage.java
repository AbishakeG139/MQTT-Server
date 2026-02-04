package in.co.abi.dev.mqtt.message;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Immutable wrapper for MQTT messages with type-aware access methods.
 * Encapsulates the raw payload, detected type, and provides convenient
 * accessors.
 */
public final class MqttMessage {

    private static final Logger logger = LogManager.getLogger(MqttMessage.class);

    private final String topic;
    private final byte[] rawPayload;
    private final MessageType type;
    private final long timestamp;

    /**
     * Creates a new MqttMessage with auto-detected type.
     *
     * @param topic   The MQTT topic
     * @param payload The raw message payload
     */
    public MqttMessage(String topic, byte[] payload) {
        this.topic = topic;
        this.rawPayload = payload != null ? Arrays.copyOf(payload, payload.length) : new byte[0];
        this.type = MessageTypeDetector.detect(this.rawPayload);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates a new MqttMessage with explicit type.
     *
     * @param topic   The MQTT topic
     * @param payload The raw message payload
     * @param type    The message type
     */
    public MqttMessage(String topic, byte[] payload, MessageType type) {
        this.topic = topic;
        this.rawPayload = payload != null ? Arrays.copyOf(payload, payload.length) : new byte[0];
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates an MqttMessage from a string payload.
     *
     * @param topic   The MQTT topic
     * @param payload The string payload
     * @return New MqttMessage with STRING type
     */
    public static MqttMessage fromString(String topic, String payload) {
        byte[] bytes = payload != null ? payload.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return new MqttMessage(topic, bytes, MessageType.STRING);
    }

    /**
     * Creates an MqttMessage from a JSON object.
     *
     * @param topic The MQTT topic
     * @param obj   The object to serialize as JSON
     * @return New MqttMessage with JSON type
     * @throws IOException if serialization fails
     */
    public static MqttMessage fromJson(String topic, Object obj) throws IOException {
        byte[] bytes = JsonUtil.toJson(obj);
        return new MqttMessage(topic, bytes, MessageType.JSON);
    }

    // ========== Getters ==========

    public String getTopic() {
        return topic;
    }

    public MessageType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a copy of the raw payload bytes.
     */
    public byte[] asBytes() {
        return Arrays.copyOf(rawPayload, rawPayload.length);
    }

    /**
     * Returns the payload as a UTF-8 string.
     * For BYTES type, this may produce garbage - check type first.
     */
    public String asString() {
        return new String(rawPayload, StandardCharsets.UTF_8);
    }

    /**
     * Parses the JSON payload into the specified class.
     *
     * @param clazz The target class
     * @param <T>   The type parameter
     * @return Deserialized object
     * @throws IOException           if parsing fails
     * @throws IllegalStateException if message type is not JSON
     */
    public <T> T asJson(Class<T> clazz) throws IOException {
        if (type != MessageType.JSON) {
            throw new IllegalStateException("Message type is " + type + ", not JSON");
        }
        return JsonUtil.fromJson(rawPayload, clazz);
    }

    /**
     * Parses the JSON payload into a JsonNode for dynamic access.
     *
     * @return JsonNode representation
     * @throws IOException           if parsing fails
     * @throws IllegalStateException if message type is not JSON
     */
    public JsonNode asJsonNode() throws IOException {
        if (type != MessageType.JSON) {
            throw new IllegalStateException("Message type is " + type + ", not JSON");
        }
        return JsonUtil.parseJson(rawPayload);
    }

    /**
     * Returns the payload length in bytes.
     */
    public int getPayloadSize() {
        return rawPayload.length;
    }

    /**
     * Checks if the payload is empty.
     */
    public boolean isEmpty() {
        return rawPayload.length == 0;
    }

    /**
     * Returns a human-readable representation of the payload.
     * For JSON, pretty prints. For STRING, returns as-is. For BYTES, shows hex
     * summary.
     */
    public String getPayloadPreview() {
        return getPayloadPreview(100);
    }

    /**
     * Returns a human-readable representation with max length.
     */
    public String getPayloadPreview(int maxLength) {
        String preview;
        switch (type) {
            case JSON:
                preview = asString();
                break;
            case STRING:
                preview = asString();
                break;
            case BYTES:
            default:
                preview = bytesToHex(rawPayload, Math.min(maxLength / 3, 32));
                break;
        }

        if (preview.length() > maxLength) {
            return preview.substring(0, maxLength - 3) + "...";
        }
        return preview;
    }

    private static String bytesToHex(byte[] bytes, int maxBytes) {
        StringBuilder sb = new StringBuilder("[");
        int limit = Math.min(bytes.length, maxBytes);
        for (int i = 0; i < limit; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(String.format("%02X", bytes[i]));
        }
        if (bytes.length > maxBytes) {
            sb.append("...");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("MqttMessage{topic='%s', type=%s, size=%d}",
                topic, type, rawPayload.length);
    }
}
