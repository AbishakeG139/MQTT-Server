package in.co.abi.dev.mqtt.security;

import in.co.abi.dev.mqtt.properties.MqttProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages authentication and authorization for MQTT connections.
 * Loads credentials from properties and validates access to private topics.
 */
public class AuthenticationManager {

    private static final Logger logger = LogManager.getLogger(AuthenticationManager.class);

    private final boolean authEnabled;
    private final String configuredUsername;
    private final String encryptedPassword;
    private final Set<String> privateTopics;
    private final boolean allowAnonymous;

    public AuthenticationManager() {
        // Load configuration
        this.authEnabled = Boolean.parseBoolean(
                MqttProperties.getProperty("mqtt.auth.enabled", "false"));
        this.configuredUsername = MqttProperties.getProperty("mqtt.auth.username", "admin");
        this.encryptedPassword = MqttProperties.getProperty("mqtt.auth.password", "");
        this.allowAnonymous = Boolean.parseBoolean(
                MqttProperties.getProperty("mqtt.allowAnonymous", "true"));

        // Parse private topics (comma-separated)
        String topicsStr = MqttProperties.getProperty("mqtt.private.topics", "");
        this.privateTopics = new HashSet<>();
        if (!topicsStr.trim().isEmpty()) {
            String[] topics = topicsStr.split(",");
            for (String topic : topics) {
                String trimmed = topic.trim();
                if (!trimmed.isEmpty()) {
                    privateTopics.add(trimmed);
                }
            }
        }

        logger.info("Authentication enabled: {}", authEnabled);
        logger.info("Allow anonymous: {}", allowAnonymous);
        logger.info("Private topics: {}", privateTopics);
    }

    /**
     * Authenticates a user with username and password.
     *
     * @param username The username
     * @param password The plaintext password
     * @return true if authentication succeeds, false otherwise
     */
    public boolean authenticate(String username, String password) {
        if (!authEnabled) {
            logger.debug("Authentication disabled, allowing access");
            return true;
        }

        if (username == null || password == null) {
            logger.warn("Authentication failed: null credentials");
            return false;
        }

        // Check username
        if (!configuredUsername.equals(username)) {
            logger.warn("Authentication failed: invalid username '{}'", username);
            return false;
        }

        // Decrypt and verify password
        try {
            String decryptedPassword = CryptoUtil.decrypt(encryptedPassword);
            boolean valid = decryptedPassword.equals(password);

            if (valid) {
                logger.info("Authentication successful for user '{}'", username);
            } else {
                logger.warn("Authentication failed: invalid password for user '{}'", username);
            }

            return valid;

        } catch (Exception e) {
            logger.error("Authentication failed: error decrypting password", e);
            return false;
        }
    }

    /**
     * Checks if a topic is private (requires authentication).
     *
     * @param topic The topic to check
     * @return true if topic is private, false otherwise
     */
    public boolean isPrivateTopic(String topic) {
        if (topic == null) {
            return false;
        }

        // Exact match or wildcard match
        for (String privateTopic : privateTopics) {
            if (topicMatches(topic, privateTopic)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a user can publish to a topic.
     * Private topics ALWAYS require authentication, regardless of mqtt.auth.enabled
     * setting.
     *
     * @param username The authenticated username (null if anonymous)
     * @param topic    The topic to publish to
     * @return true if authorized, false otherwise
     */
    public boolean canPublish(String username, String topic) {
        // Private topics ALWAYS require authentication
        if (isPrivateTopic(topic)) {
            if (username == null) {
                logger.warn("Publish denied: private topic '{}' requires authentication", topic);
                return false;
            }
            logger.debug("Publish allowed: user '{}' to private topic '{}'", username, topic);
            return true;
        }

        // Public topics - check global auth settings
        if (allowAnonymous || username != null) {
            return true;
        }

        logger.warn("Publish denied: anonymous access disabled for topic '{}'", topic);
        return false;
    }

    /**
     * Checks if a user can subscribe to a topic.
     * Private topics ALWAYS require authentication, regardless of mqtt.auth.enabled
     * setting.
     *
     * @param username The authenticated username (null if anonymous)
     * @param topic    The topic to subscribe to
     * @return true if authorized, false otherwise
     */
    public boolean canSubscribe(String username, String topic) {
        // Private topics ALWAYS require authentication
        if (isPrivateTopic(topic)) {
            if (username == null) {
                logger.warn("Subscribe denied: private topic '{}' requires authentication", topic);
                return false;
            }
            logger.debug("Subscribe allowed: user '{}' to private topic '{}'", username, topic);
            return true;
        }

        // Public topics - check global auth settings
        if (allowAnonymous || username != null) {
            return true;
        }

        logger.warn("Subscribe denied: anonymous access disabled for topic '{}'", topic);
        return false;
    }

    /**
     * Checks if authentication is enabled.
     */
    public boolean isAuthEnabled() {
        return authEnabled;
    }

    /**
     * Checks if anonymous access is allowed.
     */
    public boolean isAnonymousAllowed() {
        return allowAnonymous;
    }

    /**
     * Simple topic matching (exact match for now, can be extended for wildcards).
     */
    private boolean topicMatches(String topic, String pattern) {
        // For now, exact match
        // TODO: Add MQTT wildcard support (+ and #)
        return topic.equals(pattern) || topic.startsWith(pattern + "/");
    }
}
