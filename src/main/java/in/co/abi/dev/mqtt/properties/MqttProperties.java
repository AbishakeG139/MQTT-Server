package in.co.abi.dev.mqtt.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MqttProperties {

	private MqttProperties() {
	}

	private static final Logger logger = LogManager.getLogger(MqttProperties.class);
	private static final Properties properties = new Properties();

	static {
		try (InputStream input = MqttProperties.class.getClassLoader().getResourceAsStream("mqtt-server.properties")) {
			if (input == null) {
				throw new RuntimeException("Unable to find config.properties");
			}
			properties.load(input);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load properties file", e);
		}
	}

	public static String getProperty(String key) {
		logger.info("");
		return properties.getProperty(key);
	}

	public static String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

}
