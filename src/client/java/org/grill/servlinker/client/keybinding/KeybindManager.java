package org.grill.servlinker.client.keybinding;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.grill.servlinker.client.ServlinkerClient;
import org.grill.servlinker.client.utils.InputCapture;
import org.lwjgl.glfw.GLFW;

public class KeybindManager {
    private static KeyBinding toggleInputKey;

    public static void registerInputKeybind(InputCapture inputCapture) {
        toggleInputKey = new KeyBinding(
                "key.servlinker.toggle_input",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.servlinker"
        );
        KeyBindingHelper.registerKeyBinding(toggleInputKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleInputKey.wasPressed()) {
                boolean newState = !inputCapture.isCapturing.get();
                inputCapture.isCapturing.set(newState);
                if (newState) {
                    inputCapture.startCapturing();
                    ServlinkerClient.LOGGER.info("Input capture ENABLED");
                } else {
                    inputCapture.stopCapturing();
                    ServlinkerClient.LOGGER.info("Input capture DISABLED");
                }
            }
        });
    }
}
