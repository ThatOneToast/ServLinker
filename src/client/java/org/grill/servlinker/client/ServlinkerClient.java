package org.grill.servlinker.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServlinkerClient implements ClientModInitializer {

    public static final String MOD_ID = "ServLinker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public static final ServLinker serv = new ServLinker();
    public static InputCapture inputCapture;
    
    private static String currentServerAddress;
    private static int currentServerPort;
    private static boolean isConnected = false;

    @Override
    public void onInitializeClient() {
        // Initialize input capture
        inputCapture = new InputCapture(serv);
        
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            extractServerAddressAndPort(handler);
            // Try to connect with retries
            boolean res = false;
            for (int attempt = 0; attempt < 3 && !res; attempt++) {
                LOGGER.info("Attempting to connect to ServLinker Server (attempt {})", attempt + 1);
                res = serv.connect();
                if (!res && attempt < 2) {
                    try {
                        Thread.sleep(1000); // Wait before retry
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            if (res) {
                LOGGER.info("Connected ServLinker Server!");
                inputCapture.startCapturing();
            } else {
                LOGGER.error("!!Failed to connect to ServLinker Server after multiple attempts!!");
            }
        });



        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear server information when client disconnects
            LOGGER.info("Disconnecting from server, cleaning up resources...");
            isConnected = false;
            currentServerAddress = null;
            currentServerPort = 0;
            if (inputCapture != null) {
                inputCapture.stopCapturing();
                LOGGER.info("Input capture stopped");
            }
            if (serv != null) {
                boolean wasConnected = serv.isConnected();
                serv.disconnect();
                LOGGER.info("ServLinker disconnected (was connected: {})", wasConnected);
            }
            LOGGER.info("Disconnected from server - all cleanup complete");
        });

        // Add periodic connection check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isConnected && serv != null && !serv.isConnected() && currentServerAddress != null) {
                LOGGER.warn("ServLinker connection lost, attempting to reconnect...");
                boolean reconnected = serv.connect();
                if (reconnected) {
                    LOGGER.info("Successfully reconnected to ServLinker server");
                    if (inputCapture != null && inputCapture.isCapturing()) {
                        LOGGER.info("Restarting input capture after reconnection");
                        inputCapture.startCapturing();
                    }
                } else {
                    LOGGER.error("Failed to reconnect to ServLinker server");
                }
            }
        });

        LOGGER.info("ServLinker client mod initialized");
    }

    /**
     * Extract server address and port from the network handler
     * @param handler The client play network handler
     */
    private void extractServerAddressAndPort(ClientPlayNetworkHandler handler) {
        try {
            // Get server address from the connection
            String fullAddress = handler
                .getConnection()
                .getAddress()
                .toString();
            LOGGER.info("Connected to server: {}", fullAddress);

            // Parse the address and remove any characters other than numbers, dots, and colons
            String originalAddress = fullAddress;
            fullAddress = fullAddress.replaceAll("[^0-9.:]", "");
            LOGGER.debug("Parsed address from '{}' to '{}'", originalAddress, fullAddress);

            String[] parts = fullAddress.split(":");
            if (parts.length >= 2) {
                currentServerAddress = parts[0];
                try {
                    currentServerPort = Integer.parseInt(parts[1]);
                    isConnected = true;
                    LOGGER.info(
                        "Extracted server address: {} and port: {}",
                        currentServerAddress,
                        currentServerPort
                    );
                    
                    // Validate the extracted address
                    if (currentServerAddress.isEmpty() || currentServerPort <= 0) {
                        LOGGER.warn("Extracted server details may be invalid: address='{}', port={}",
                            currentServerAddress, currentServerPort);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error(
                        "Failed to parse server port: {}",
                        parts[1],
                        e
                    );
                    isConnected = false;
                }
            } else {
                LOGGER.warn(
                    "Unable to parse server address and port from: {}",
                    fullAddress
                );
                isConnected = false;
            }
        } catch (Exception e) {
            LOGGER.error("Error extracting server address and port", e);
            isConnected = false;
        }
    }

    /**
     * Connect to the current server on a different port
     * @param port The port to connect to
     * @return The socket connection if successful, null otherwise
     */
    public static Socket connectToDifferentPort(int port) {
        if (!isConnected || currentServerAddress == null) {
            LOGGER.warn(
                "Not connected to a server or server address is unknown. Connected={}, Address={}",
                isConnected, currentServerAddress
            );
            return null;
        }

        try {
            LOGGER.info(
                "Attempting to connect to {}:{}",
                currentServerAddress,
                port
            );
            
            // Add timeout settings to avoid hanging
            InetAddress serverAddress = InetAddress.getByName(
                currentServerAddress
            );
            Socket socket = new Socket();
            socket.setSoTimeout(5000); // 5 second read timeout
            socket.connect(new java.net.InetSocketAddress(serverAddress, port), 3000); // 3 second connection timeout
            
            LOGGER.info(
                "Successfully connected to {}:{}, Socket details: isConnected={}, isBound={}, isClosed={}",
                currentServerAddress,
                port,
                socket.isConnected(),
                socket.isBound(),
                socket.isClosed()
            );
            return socket;
        } catch (UnknownHostException e) {
            LOGGER.error("Unknown host: {}", currentServerAddress, e);
        } catch (IOException e) {
            LOGGER.error(
                "Failed to connect to {}:{} - {}",
                currentServerAddress,
                port,
                e.getMessage()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error connecting to {}:{}", currentServerAddress, port, e);
        }

        return null;
    }

    /**
     * Check if the client is currently connected to a server
     * @return true if connected, false otherwise
     */
    public static boolean isConnectedToServer() {
        return isConnected;
    }

    /**
     * Get the current server address
     * @return The server address or null if not connected
     */
    public static String getCurrentServerAddress() {
        return currentServerAddress;
    }

    /**
     * Get the current server port
     * @return The server port or 0 if not connected
     */
    public static int getCurrentServerPort() {
        return currentServerPort;
    }
    
    /**
     * Get the input capture instance
     * @return The input capture instance
    **/
    public static InputCapture getInputCapture() {
        return inputCapture;
    }
}
