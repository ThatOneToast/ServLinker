package org.grill.servlinker.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServlinkerClient implements ClientModInitializer {

    public static final String MOD_ID = "ServLinker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static String currentServerAddress;
    private static int currentServerPort;
    private static boolean isConnected = false;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            extractServerAddressAndPort(handler);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear server information when client disconnects
            isConnected = false;
            currentServerAddress = null;
            currentServerPort = 0;
            LOGGER.info("Disconnected from server");
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
            fullAddress = fullAddress.replaceAll("[^0-9.:]", "");

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
                } catch (NumberFormatException e) {
                    LOGGER.error(
                        "Failed to parse server port: {}",
                        parts[1],
                        e
                    );
                }
            } else {
                LOGGER.warn(
                    "Unable to parse server address and port from: {}",
                    fullAddress
                );
            }
        } catch (Exception e) {
            LOGGER.error("Error extracting server address and port", e);
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
                "Not connected to a server or server address is unknown"
            );
            return null;
        }

        try {
            LOGGER.info(
                "Attempting to connect to {}:{}",
                currentServerAddress,
                port
            );
            InetAddress serverAddress = InetAddress.getByName(
                currentServerAddress
            );
            Socket socket = new Socket(serverAddress, port);
            LOGGER.info(
                "Successfully connected to {}:{}",
                currentServerAddress,
                port
            );
            return socket;
        } catch (UnknownHostException e) {
            LOGGER.error("Unknown host: {}", currentServerAddress, e);
        } catch (IOException e) {
            LOGGER.error(
                "Failed to connect to {}:{}",
                currentServerAddress,
                port,
                e
            );
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
}
