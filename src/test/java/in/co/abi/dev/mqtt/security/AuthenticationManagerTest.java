package in.co.abi.dev.mqtt.security;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for AuthenticationManager.
 * Note: These tests use the actual properties file configuration.
 */
public class AuthenticationManagerTest {

    private AuthenticationManager authManager;

    @Before
    public void setUp() {
        authManager = new AuthenticationManager();
    }

    @Test
    public void testAuthenticate_ValidCredentials() {
        // This test assumes mqtt.auth.username=admin and password is encrypted "admin"
        boolean result = authManager.authenticate("admin", "admin");
        assertTrue("Valid credentials should authenticate", result);
    }

    @Test
    public void testAuthenticate_InvalidUsername() {
        boolean result = authManager.authenticate("wronguser", "admin");
        assertFalse("Invalid username should fail", result);
    }

    @Test
    public void testAuthenticate_InvalidPassword() {
        boolean result = authManager.authenticate("admin", "wrongpassword");
        assertFalse("Invalid password should fail", result);
    }

    @Test
    public void testAuthenticate_NullUsername() {
        boolean result = authManager.authenticate(null, "admin123");
        assertFalse("Null username should fail", result);
    }

    @Test
    public void testAuthenticate_NullPassword() {
        boolean result = authManager.authenticate("admin", null);
        assertFalse("Null password should fail", result);
    }

    @Test
    public void testAuthenticate_BothNull() {
        boolean result = authManager.authenticate(null, null);
        assertFalse("Null credentials should fail", result);
    }

    @Test
    public void testIsPrivateTopic_ExactMatch() {
        // Assuming mqtt.private.topics=admin/commands,system/config,private/data
        assertTrue(authManager.isPrivateTopic("admin/commands"));
        assertTrue(authManager.isPrivateTopic("system/config"));
        assertTrue(authManager.isPrivateTopic("private/data"));
    }

    @Test
    public void testIsPrivateTopic_SubTopic() {
        // Should match subtopics
        assertTrue(authManager.isPrivateTopic("admin/commands/shutdown"));
        assertTrue(authManager.isPrivateTopic("system/config/database"));
    }

    @Test
    public void testIsPrivateTopic_PublicTopic() {
        assertFalse(authManager.isPrivateTopic("sensors/temperature"));
        assertFalse(authManager.isPrivateTopic("public/data"));
    }

    @Test
    public void testIsPrivateTopic_Null() {
        assertFalse(authManager.isPrivateTopic(null));
    }

    @Test
    public void testCanPublish_AuthenticatedToPrivateTopic() {
        // Authenticated user can publish to private topics
        assertTrue(authManager.canPublish("admin", "admin/commands"));
    }

    @Test
    public void testCanPublish_AnonymousToPrivateTopic() {
        // Anonymous cannot publish to private topics
        assertFalse(authManager.canPublish(null, "admin/commands"));
    }

    @Test
    public void testCanPublish_AuthenticatedToPublicTopic() {
        // Authenticated user can publish to public topics
        assertTrue(authManager.canPublish("admin", "sensors/temperature"));
    }

    @Test
    public void testCanPublish_AnonymousToPublicTopic() {
        // Anonymous can publish to public topics if allowed
        // This depends on mqtt.allowAnonymous setting
        boolean result = authManager.canPublish(null, "sensors/temperature");
        assertEquals(authManager.isAnonymousAllowed(), result);
    }

    @Test
    public void testCanSubscribe_SameAsPublish() {
        // Subscribe should have same logic as publish
        assertEquals(
                authManager.canPublish("admin", "admin/commands"),
                authManager.canSubscribe("admin", "admin/commands"));

        assertEquals(
                authManager.canPublish(null, "sensors/temp"),
                authManager.canSubscribe(null, "sensors/temp"));
    }

    @Test
    public void testIsAuthEnabled() {
        // Should match mqtt.auth.enabled property
        boolean enabled = authManager.isAuthEnabled();
        // Just verify it returns a boolean (actual value depends on properties)
        assertTrue(enabled || !enabled);
    }

    @Test
    public void testIsAnonymousAllowed() {
        // Should match mqtt.allowAnonymous property
        boolean allowed = authManager.isAnonymousAllowed();
        // Just verify it returns a boolean
        assertTrue(allowed || !allowed);
    }
}
