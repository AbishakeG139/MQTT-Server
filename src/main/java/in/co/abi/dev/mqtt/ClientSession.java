package in.co.abi.dev.mqtt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

public class ClientSession {
    private final Socket socket;
    private final OutputStream out;
    private final String clientId;
    private String username; // Authenticated username (null if anonymous)
    private boolean authenticated; // Authentication status

    public ClientSession(Socket socket, String clientId) throws IOException {
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.clientId = clientId;
        this.username = null;
        this.authenticated = false;
    }

    public OutputStream getOut() {
        return out;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void sendBytes(byte[] data) throws IOException {
        synchronized (out) {
            out.write(data);
            out.flush();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClientSession that = (ClientSession) o;
        return Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
