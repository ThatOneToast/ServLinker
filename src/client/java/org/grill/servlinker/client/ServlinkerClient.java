package org.grill.servlinker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.grill.servlinker.client.packets.InputCapture;
import org.grill.servlinker.client.packets.KeyPressC2SPacket;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServlinkerClient implements ClientModInitializer {
    public static final String MOD_ID = "ServLinker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static InputCapture inputCapture;
    public static KeyBinding toggleInputKey;

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleInputKey.wasPressed()) {
                boolean newState = !inputCapture.isCapturing.get();
                inputCapture.isCapturing.set(newState);
                if (newState) {
                    inputCapture.startCapturing();
                    LOGGER.info("Input capture ENABLED");
                } else {
                    inputCapture.stopCapturing();
                    LOGGER.info("Input capture DISABLED");
                }
            }
        });

        toggleInputKey = new KeyBinding(
                "key.servlinker.toggle_input",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.servlinker"
        );
        KeyBindingHelper.registerKeyBinding(toggleInputKey);


        LOGGER.info("ServLinker client mod initialized");
    }
}
