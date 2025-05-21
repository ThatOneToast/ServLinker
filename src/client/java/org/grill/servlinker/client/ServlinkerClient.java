package org.grill.servlinker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.grill.servlinker.client.keybinding.KeybindManager;
import org.grill.servlinker.client.utils.InputCapture;
import org.grill.servlinker.client.networking.KeyPressC2SPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServlinkerClient implements ClientModInitializer {
    public static final String MOD_ID = "ServLinker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static InputCapture inputCapture;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(KeyPressC2SPacket.ID, KeyPressC2SPacket.CODEC);
        inputCapture = new InputCapture(message -> {
            if (ClientPlayNetworking.canSend(KeyPressC2SPacket.ID)) {
                ClientPlayNetworking.send(new KeyPressC2SPacket(message));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if(inputCapture.isCapturing.get()) {
                inputCapture.isCapturing.set(false);
                inputCapture.stopCapturing();
            }
        });

        KeybindManager.registerInputKeybind(inputCapture);
        LOGGER.info("ServLinker client mod initialized");
    }
}
