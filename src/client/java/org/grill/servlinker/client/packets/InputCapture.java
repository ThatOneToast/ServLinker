package org.grill.servlinker.client.packets;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputCapture {
    public final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private final Set<Integer> pressedKeys = new HashSet<>();
    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;
    private boolean middleMouseDown = false;
    private boolean button4Down = false;
    private boolean button5Down = false;

    private final InputSender sender;

    private static final int[] VALID_KEY_CODES = {
            // Function keys
            GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4,
            GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8,
            GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12,

            // Number keys
            GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
            GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7,
            GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9,

            // Letter keys
            GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_D,
            GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H,
            GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
            GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P,
            GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_T,
            GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_X,
            GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_Z,

            // Special keys
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB,
            GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_INSERT, GLFW.GLFW_KEY_DELETE,
            GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP,
            GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END,
            GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_SCROLL_LOCK, GLFW.GLFW_KEY_NUM_LOCK,
            GLFW.GLFW_KEY_PRINT_SCREEN, GLFW.GLFW_KEY_PAUSE,

            // Modifier keys
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_LEFT_SUPER,
            GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_SUPER,

            // Punctuation and other keys
            GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_LEFT_BRACKET,
            GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE,
            GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH
    };

    // Interface to send string message via packet
    public interface InputSender {
        void send(String message);
    }

    public InputCapture(InputSender sender) {
        this.sender = sender;
        register();
    }

    private void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isCapturing.get()) {
                checkKeyboardState(client);
                checkMouseState(client);
            }
        });
    }

    public void startCapturing() {
        isCapturing.set(true);
    }

    public void stopCapturing() {
        isCapturing.set(false);
        pressedKeys.clear();
        leftMouseDown = rightMouseDown = middleMouseDown = false;
    }

    private void checkKeyboardState(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        for (int keyCode : VALID_KEY_CODES) {
            boolean pressed = InputUtil.isKeyPressed(window, keyCode);
            if (pressed && !pressedKeys.contains(keyCode)) {
                pressedKeys.add(keyCode);
                sendKeyEvent("PRESS", keyCode);
            } else if (!pressed && pressedKeys.contains(keyCode)) {
                pressedKeys.remove(keyCode);
                sendKeyEvent("RELEASE", keyCode);
            }
        }
    }

    private void checkMouseState(MinecraftClient client) {
        long window = client.getWindow().getHandle();

        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean middleDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        boolean button4Now = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS;
        boolean button5Now = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_5) == GLFW.GLFW_PRESS;

        if (leftDown != leftMouseDown) {
            leftMouseDown = leftDown;
            sendMouseEvent("LEFT", leftMouseDown ? "PRESS" : "RELEASE");
        }
        if (rightDown != rightMouseDown) {
            rightMouseDown = rightDown;
            sendMouseEvent("RIGHT", rightMouseDown ? "PRESS" : "RELEASE");
        }
        if (middleDown != middleMouseDown) {
            middleMouseDown = middleDown;
            sendMouseEvent("MIDDLE", middleMouseDown ? "PRESS" : "RELEASE");
        }
        if (button4Now != button4Down) {
            button4Down = button4Now;
            sendMouseEvent("BUTTON_4", button4Down ? "PRESS" : "RELEASE");
        }
        if (button5Now != button5Down) {
            button5Down = button5Now;
            sendMouseEvent("BUTTON_5", button5Down ? "PRESS" : "RELEASE");
        }
    }

    private void sendKeyEvent(String action, int keyCode) {
        String keyName = InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
        String msg = "KEY|" + action + "|" + keyCode + "|" + keyName;
        sender.send(msg);
    }

    private void sendMouseEvent(String button, String action) {
        String msg = "MOUSE_BUTTON|" + button + "|" + action;
        sender.send(msg);
    }
}
