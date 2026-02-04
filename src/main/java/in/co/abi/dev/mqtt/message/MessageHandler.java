package in.co.abi.dev.mqtt.message;

import in.co.abi.dev.mqtt.ClientSession;

/**
 * Interface for processing MQTT messages based on their type.
 * Implement this interface to provide custom handling logic for different
 * message types.
 */
public interface MessageHandler {

    /**
     * Handles a raw bytes message.
     *
     * @param topic   The MQTT topic
     * @param payload The raw byte payload
     * @param session The client session (can be null for internal processing)
     */
    void handleBytes(String topic, byte[] payload, ClientSession session);

    /**
     * Handles a string message.
     *
     * @param topic   The MQTT topic
     * @param payload The string payload
     * @param session The client session (can be null for internal processing)
     */
    void handleString(String topic, String payload, ClientSession session);

    /**
     * Handles a JSON message.
     *
     * @param topic   The MQTT topic
     * @param message The parsed MqttMessage containing JSON
     * @param session The client session (can be null for internal processing)
     */
    void handleJson(String topic, MqttMessage message, ClientSession session);

    /**
     * Dispatches the message to the appropriate handler method based on type.
     *
     * @param message The MQTT message
     * @param session The client session
     */
    default void handle(MqttMessage message, ClientSession session) {
        switch (message.getType()) {
            case BYTES:
                handleBytes(message.getTopic(), message.asBytes(), session);
                break;
            case STRING:
                handleString(message.getTopic(), message.asString(), session);
                break;
            case JSON:
                handleJson(message.getTopic(), message, session);
                break;
        }
    }
}
