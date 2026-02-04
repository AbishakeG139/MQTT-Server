package in.co.abi.dev.mqtt.message;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for detecting the type of MQTT message payload.
 * Uses content-based analysis to determine if payload is JSON, String, or
 * Bytes.
 */
public final class MessageTypeDetector {

    private MessageTypeDetector() {
        // Utility class - prevent instantiation
    }

    /**
     * Detects the message type from the raw payload bytes.
     * Detection order: JSON -> STRING -> BYTES
     *
     * @param payload The raw message payload
     * @return The detected MessageType
     */
    public static MessageType detect(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return MessageType.BYTES;
        }

        // Try JSON first
        if (isValidJson(payload)) {
            return MessageType.JSON;
        }

        // Try UTF-8 String
        if (isValidUtf8(payload)) {
            return MessageType.STRING;
        }

        // Default to binary
        return MessageType.BYTES;
    }

    /**
     * Quick check if payload looks like JSON (starts with { or [).
     * Validates full JSON structure for accuracy.
     */
    private static boolean isValidJson(byte[] payload) {
        if (payload.length < 2) {
            return false;
        }

        // Skip leading whitespace and check for JSON start characters
        int start = 0;
        while (start < payload.length && isWhitespace(payload[start])) {
            start++;
        }

        if (start >= payload.length) {
            return false;
        }

        byte firstChar = payload[start];
        if (firstChar != '{' && firstChar != '[') {
            return false;
        }

        // Validate using JsonUtil
        return JsonUtil.isValidJson(payload);
    }

    /**
     * Checks if the byte array represents valid UTF-8 encoded text.
     * Also verifies the content contains printable characters.
     */
    private static boolean isValidUtf8(byte[] payload) {
        try {
            String decoded = new String(payload, StandardCharsets.UTF_8);

            // Check if round-trip encoding produces same bytes
            byte[] reEncoded = decoded.getBytes(StandardCharsets.UTF_8);
            if (reEncoded.length != payload.length) {
                return false;
            }

            // Check for high ratio of printable/text characters
            int printableCount = 0;
            for (char c : decoded.toCharArray()) {
                if (isPrintableChar(c)) {
                    printableCount++;
                }
            }

            // Consider it a string if >80% printable
            double ratio = (double) printableCount / decoded.length();
            return ratio > 0.8;

        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWhitespace(byte b) {
        return b == ' ' || b == '\t' || b == '\n' || b == '\r';
    }

    private static boolean isPrintableChar(char c) {
        return !Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t';
    }
}
