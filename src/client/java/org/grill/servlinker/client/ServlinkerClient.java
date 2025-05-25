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
        PayloadTypeRegistry.playC2S().register(KeyInputToggleC2SPacket.ID, KeyInputToggleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(KeyPressC2SPacket.ID, KeyPressC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AckC2SPacket.ID, AckC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SyncS2CPacket.ID, SyncS2CPacket.CODEC);
        register();

        KeybindManager.registerInputKeybind(inputCapture);
        LOGGER.info("ServLinker client mod initialized");
    }

    private void register() {
        inputCapture = new InputCapture();

        ClientPlayNetworking.registerGlobalReceiver(SyncS2CPacket.ID, (packet, context) -> {
            serverHasPlugin = true;
            ClientPlayNetworking.send(new AckC2SPacket("ack"));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverHasPlugin = false;
            inputCapture.stopCapturing();
        });
    }
}
