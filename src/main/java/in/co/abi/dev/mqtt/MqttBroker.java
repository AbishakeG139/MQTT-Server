package in.co.abi.dev.mqtt;

import in.co.abi.dev.mqtt.properties.MqttProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttBroker {
    private static final Logger logger = LogManager.getLogger(MqttBroker.class);
    private final int port;
    private final ExecutorService clients = Executors.newCachedThreadPool();

    public MqttBroker(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket server = new ServerSocket(port);
        logger.info("MQTT broker started on port {}", port);
        logger.info("Connect URL: mqtt://localhost:{}", port);
        logger.info("Connect URL: mqtt://127.0.0.1:{}", port);
        while (!server.isClosed()) {
            Socket socket = server.accept();
            logger.info("Accepted connection from {}", socket.getRemoteSocketAddress());
            clients.submit(new ClientHandler(socket));
        }
    }

    public static void main(String[] args) throws Exception {
        String portProp = MqttProperties.getProperty("port");
        int port = 1883;
        if (portProp != null && !portProp.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portProp.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid port in properties, using default 1883");
            }
        }
        new MqttBroker(port).start();
    }
}
