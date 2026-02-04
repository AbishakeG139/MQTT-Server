package in.co.abi.dev.mqtt.message;

import in.co.abi.dev.mqtt.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of MessageHandler that logs message processing.
 * Extend this class to add custom processing logic.
 */
public class DefaultMessageHandler implements MessageHandler {

    private static final Logger logger = LogManager.getLogger(DefaultMessageHandler.class);

    @Override
    public void handleBytes(String topic, byte[] payload, ClientSession session) {
        String clientId = session != null ? session.getClientId() : "internal";
        logger.info("[BYTES] topic='{}' size={} bytes from={}",
                topic, payload.length, clientId);

        if (logger.isDebugEnabled() && payload.length > 0) {
            logger.debug("[BYTES] First 32 bytes: {}", bytesToHex(payload, 32));
        }
    }

    @Override
    public void handleString(String topic, String payload, ClientSession session) {
        String clientId = session != null ? session.getClientId() : "internal";

        // Truncate long strings for logging
        String preview = payload.length() > 200
                ? payload.substring(0, 200) + "..."
                : payload;

        logger.info("[STRING] topic='{}' payload='{}' from={}",
                topic, preview, clientId);
    }

    @Override
    public void handleJson(String topic, MqttMessage message, ClientSession session) {
        String clientId = session != null ? session.getClientId() : "internal";

        String jsonPreview = message.getPayloadPreview(200);
        logger.info("[JSON] topic='{}' payload={} from={}",
                topic, jsonPreview, clientId);

        if (logger.isDebugEnabled()) {
            try {
                String pretty = JsonUtil.prettyPrint(message.asBytes());
                logger.debug("[JSON] Pretty printed:\n{}", pretty);
            } catch (Exception e) {
                logger.debug("[JSON] Failed to pretty print: {}", e.getMessage());
            }
        }
    }

    private static String bytesToHex(byte[] bytes, int maxBytes) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(bytes.length, maxBytes);
        for (int i = 0; i < limit; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(String.format("%02X", bytes[i]));
        }
        if (bytes.length > maxBytes) {
            sb.append("...");
        }
        return sb.toString();
    }
}
