package in.co.abi.dev.mqtt.message;

/**
 * Defines the types of messages that can be processed by the MQTT broker.
 */
public enum MessageType {
    /**
     * Raw binary data - non-textual content
     */
    BYTES,

    /**
     * UTF-8 encoded text messages
     */
    STRING,

    /**
     * JSON structured data
     */
    JSON
}
