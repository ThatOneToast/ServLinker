package org.grill.servlinker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.grill.servlinker.client.networking.KeyInputToggleC2SPacket;
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
                if (inputCapture.isCapturing()) {
                    inputCapture.stopCapturing();
                    if(ServlinkerClient.serverHasPlugin) {
                        ClientPlayNetworking.send(new KeyInputToggleC2SPacket("off"));
                    }
                    ServlinkerClient.LOGGER.info("Input capture DISABLED");
                    if (client.player != null) client.player.sendMessage(Text.literal("§cInput capture DISABLED"), false);
                } else {
                    inputCapture.startCapturing();
                    if(ServlinkerClient.serverHasPlugin) {
                        ClientPlayNetworking.send(new KeyInputToggleC2SPacket("on"));
                    }
                    ServlinkerClient.LOGGER.info("Input capture ENABLED");
                    if (client.player != null) client.player.sendMessage(Text.literal("§aInput capture ENABLED"), false);
                }
            }
        });
    }
}
