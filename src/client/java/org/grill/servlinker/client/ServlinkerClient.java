package org.grill.servlinker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.grill.servlinker.client.networking.KeyInputToggleC2SPacket;
import org.grill.servlinker.client.utils.DebugLogger;
import org.grill.servlinker.client.networking.AckC2SPacket;
import org.grill.servlinker.client.networking.SyncS2CPacket;
import org.grill.servlinker.client.utils.InputCapture;
import org.grill.servlinker.client.networking.KeyPressC2SPacket;
import org.slf4j.LoggerFactory;

public class ServlinkerClient implements ClientModInitializer {
    public static final String MOD_ID = "ServLinker";
    public static final DebugLogger LOGGER = new DebugLogger(LoggerFactory.getLogger(MOD_ID), true);

    public static InputCapture inputCapture;
    public static boolean serverHasPlugin = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ServLinker client initializing...");
        
        PayloadTypeRegistry.playC2S().register(KeyInputToggleC2SPacket.ID, KeyInputToggleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(KeyPressC2SPacket.ID, KeyPressC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AckC2SPacket.ID, AckC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SyncS2CPacket.ID, SyncS2CPacket.CODEC);
        
        LOGGER.info("Creating InputCapture instance...");
        register();

        LOGGER.info("Registering keybinds with InputCapture: {}", inputCapture != null ? "success" : "failed");
        KeybindManager.registerInputKeybind(inputCapture);
        LOGGER.info("ServLinker client mod initialized");
    }

    private void register() {
        LOGGER.info("Registering InputCapture and networking...");
        inputCapture = new InputCapture();
        LOGGER.info("InputCapture created successfully: {}", inputCapture != null);

        ClientPlayNetworking.registerGlobalReceiver(SyncS2CPacket.ID, (packet, context) -> {
            LOGGER.info("Received sync packet from server - server has plugin");
            serverHasPlugin = true;
            ClientPlayNetworking.send(new AckC2SPacket("ack"));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("Disconnected from server - stopping input capture");
            serverHasPlugin = false;
            if (inputCapture != null) {
                inputCapture.stopCapturing();
            }
        });
    }
}
