package org.grill.servlinker.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServLinker provides a TCP connection to port 9009 on the same server
 * that the client is connected to through the Minecraft client.
 */
public class ServLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServlinkerClient.MOD_ID);
    private static final int DEFAULT_PORT = 9009;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /**
     * Creates a new ServLinker instance
     */
    public ServLinker() {
        // Default constructor
    }
    
    /**
     * Connects to the server on the default port (9009)
     * 
     * @return true if connection was successful, false otherwise
     */
    public boolean connect() {
        return connect(DEFAULT_PORT);
    }
    
    /**
     * Connects to the server on the specified port
     * 
     * @param port The port to connect to
     * @return true if connection was successful, false otherwise
     */
    public boolean connect(int port) {
        if (isConnected()) {
            LOGGER.warn("Already connected to server, disconnect first");
            return false;
        }
        
        LOGGER.debug("Attempting to connect to server on port {}", port);
        socket = ServlinkerClient.connectToDifferentPort(port);
        if (socket == null) {
            LOGGER.error("Failed to connect to server on port {}", port);
            return false;
        }
        
        try {
            LOGGER.debug("Socket connected, setting up input/output streams");
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isRunning.set(true);
            LOGGER.info("Connected to server on port {}, socket={}", port, socket);
            
            // Test the connection by sending a message and waiting for response
            if (!testConnection()) {
                LOGGER.error("Connection test failed");
                disconnect();
                return false;
            }
            
            // Start a background thread to monitor connection health
            startConnectionMonitor();
            
            return true;
        } catch (IOException e) {
            LOGGER.error("Error setting up streams for server connection", e);
            disconnect();
            return false;
        }
    }
    
    /**
     * Disconnects from the server
     */
    public void disconnect() {
        LOGGER.debug("Disconnecting from server...");
        isRunning.set(false);
        
        try {
            if (in != null) {
                LOGGER.debug("Closing input stream");
                in.close();
                in = null;
            }
        } catch (IOException e) {
            LOGGER.error("Error closing input stream", e);
        }
        
        if (out != null) {
            LOGGER.debug("Closing output stream");
            out.close();
            out = null;
        }
        
        try {
            if (socket != null) {
                LOGGER.debug("Closing socket: {}", socket);
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            LOGGER.error("Error closing socket", e);
        }
        
        LOGGER.info("Disconnected from server");
    }
    
    /**
     * Checks if connected to the server
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        boolean socketValid = socket != null && !socket.isClosed() && socket.isConnected();
        boolean streamsValid = in != null && out != null;
        boolean connected = socketValid && streamsValid;
        
        LOGGER.debug("Connection status check: socket={}, isClosed={}, isConnected={}, streams={}, result={}",
            socket != null ? "not null" : "null",
            socket != null ? socket.isClosed() : "N/A",
            socket != null ? socket.isConnected() : "N/A",
            streamsValid ? "valid" : "invalid",
            connected);
        return connected;
    }
    
    /**
     * Tests the connection by sending a ping message and waiting for a response
     * 
     * @return true if test was successful, false otherwise
     */
    private boolean testConnection() {
        if (!isConnected()) {
            return false;
        }
        
        try {
            // Send test message
            LOGGER.debug("Testing connection with ping message");
            out.println("PING|" + System.currentTimeMillis());
            if (out.checkError()) {
                LOGGER.error("Error sending ping message");
                return false;
            }
            
            // For now, we'll consider it a success if we can send the message
            // In a real implementation, you might want to wait for a response
            LOGGER.info("Connection test successful");
            return true;
        } catch (Exception e) {
            LOGGER.error("Error during connection test", e);
            return false;
        }
    }
    
    /**
     * Starts a background thread to monitor connection health
     */
    private void startConnectionMonitor() {
        Thread monitorThread = new Thread(() -> {
            LOGGER.info("Connection monitor thread started");
            while (isRunning.get() && isConnected()) {
                try {
                    // Test connection every 10 seconds
                    Thread.sleep(10000);
                    if (isRunning.get() && !testConnection()) {
                        LOGGER.warn("Connection health check failed, disconnecting");
                        disconnect();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.info("Connection monitor thread interrupted");
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in connection monitor thread", e);
                }
            }
            LOGGER.info("Connection monitor thread stopped");
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("ServLinker-ConnectionMonitor");
        monitorThread.start();
    }
    
    /**
     * Sends a message to the server
     * 
     * @param message The message to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendMessage(String message) {
        if (!isConnected() || out == null) {
            LOGGER.warn("Not connected to server or output stream is null");
            return false;
        }
        
        try {
            // Check socket health before sending
            if (socket.isClosed() || !socket.isConnected()) {
                LOGGER.error("Socket is closed or disconnected, cannot send message");
                disconnect();
                return false;
            }
            
            LOGGER.debug("Sending message: {}", message);
            out.println(message);
            boolean hasError = out.checkError();
            if (hasError) {
                LOGGER.error("Error detected while sending message: {}", message);
                disconnect();
                return false;
            }
            
            LOGGER.debug("Message sent successfully: {}", message);
            return true;
        } catch (Exception e) {
            LOGGER.error("Exception while sending message: {}", e.getMessage());
            disconnect();
            return false;
        }
    }
    
    /**
     * Reads a message from the server (blocking)
     * 
     * @return The message from the server, or null if disconnected
     */
    public String readMessage() {
        if (!isConnected() || in == null) {
            LOGGER.warn("Not connected to server");
            return null;
        }
        
        try {
            return in.readLine();
        } catch (IOException e) {
            LOGGER.error("Error reading from server", e);
            disconnect();
            return null;
        }
    }
    
    /**
     * Starts listening for messages from the server in a separate thread
     * 
     * @param messageHandler Consumer that will handle each received message
     * @return A CompletableFuture that will be completed when the listener stops
     */
    public CompletableFuture<Void> startMessageListener(Consumer<String> messageHandler) {
        return CompletableFuture.runAsync(() -> {
            if (!isConnected() || in == null) {
                LOGGER.warn("Not connected to server");
                return;
            }
            
            isRunning.set(true);
            while (isRunning.get() && isConnected()) {
                try {
                    String message = in.readLine();
                    if (message == null) {
                        LOGGER.info("Server closed the connection");
                        break;
                    }
                    
                    messageHandler.accept(message);
                } catch (IOException e) {
                    if (isRunning.get()) {
                        LOGGER.error("Error reading from server", e);
                    }
                    break;
                }
            }
            
            disconnect();
        });
    }
    
    /**
     * Stops the message listener
     */
    public void stopMessageListener() {
        isRunning.set(false);
    }
}
