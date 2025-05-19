package org.grill.servlinker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.Keyboard;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InputCapture provides functionality to capture keyboard and mouse inputs
 * and send them through the ServLinker connection.
 */
public class InputCapture {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServlinkerClient.MOD_ID);
    private final ServLinker servLinker;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private final Set<Integer> pressedKeys = new HashSet<>();
    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;
    private boolean middleMouseDown = false;
    
    // Toggle key binding for the input capture
    private final KeyBinding toggleCapture;
    
    // No mouse position tracking

    /**
     * Creates a new InputCapture instance
     * 
     * @param servLinker The ServLinker instance to send captured inputs to
     */
    public InputCapture(ServLinker servLinker) {
        this.servLinker = servLinker;
        
        // Register toggle key binding (Ctrl+K by default)
        this.toggleCapture = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.servlinker.togglecapture",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.servlinker.general"
        ));
        
        registerEventHandlers();
    }

    /**
     * Register event handlers for key and mouse inputs
     */
    private void registerEventHandlers() {
        // Register client tick event for checking key bindings and mouse state
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check toggle key binding
            if (toggleCapture.wasPressed()) {
                toggleCapturing();
                String status = isCapturing.get() ? "enabled" : "disabled";
                assert client.player != null;
                client.player.sendMessage(Text.of("Input capture " + status), true);
            }
            
            if (isCapturing.get()) {
                LOGGER.debug("Input capture is active, checking inputs...");
                if (!servLinker.isConnected()) {
                    LOGGER.warn("ServLinker not connected while capture is enabled!");
                }
                checkKeyboardState(client);
                checkMouseState(client);
            }
        });
    }
    
    /**
     * Toggle the capturing state
     */
    public void toggleCapturing() {
        boolean newState = !isCapturing.get();
        isCapturing.set(newState);
        LOGGER.info("Input capture {}", newState ? "enabled" : "disabled");
        
        // Check if ServLinker is connected
        if (servLinker.isConnected()) {
            LOGGER.info("ServLinker is connected, sending toggle state");
        } else {
            LOGGER.error("ServLinker is NOT connected, toggle state won't be sent");
        }
        
        // Send the toggle state to the server
        sendInputEvent("TOGGLE", newState ? "ON" : "OFF");
    }
    
    /**
     * Start capturing inputs
     */
    public void startCapturing() {
        if (!isCapturing.get()) {
            isCapturing.set(true);
            LOGGER.info("Input capture enabled");
            sendInputEvent("TOGGLE", "ON");
        }
    }
    
    /**
     * Stop capturing inputs
     */
    public void stopCapturing() {
        if (isCapturing.get()) {
            isCapturing.set(false);
            LOGGER.info("Input capture disabled");
            sendInputEvent("TOGGLE", "OFF");
        }
    }
    
    // List of common GLFW key codes to check
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
    
    /**
     * Check keyboard state and send key events
     */
    private void checkKeyboardState(MinecraftClient client) {
        long windowHandle = client.getWindow().getHandle();
        
        // Only check valid key codes to avoid GL errors
        for (int keyCode : VALID_KEY_CODES) {
            boolean isPressed = InputUtil.isKeyPressed(windowHandle, keyCode);
            
            if (isPressed && !pressedKeys.contains(keyCode)) {
                // Key was just pressed
                pressedKeys.add(keyCode);
                sendKeyEvent("PRESS", keyCode);
            } else if (!isPressed && pressedKeys.contains(keyCode)) {
                // Key was just released
                pressedKeys.remove(keyCode);
                sendKeyEvent("RELEASE", keyCode);
            }
        }
    }
    
    /**
     * Check mouse state and send mouse events
     */
    private void checkMouseState(MinecraftClient client) {
        Mouse mouse = client.mouse;
        
        // Check mouse buttons
        boolean newLeftMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean newRightMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean newMiddleMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        
        // Handle left mouse button
        if (newLeftMouseDown != leftMouseDown) {
            leftMouseDown = newLeftMouseDown;
            sendMouseButtonEvent("LEFT", leftMouseDown ? "PRESS" : "RELEASE");
        }
        
        // Handle right mouse button
        if (newRightMouseDown != rightMouseDown) {
            rightMouseDown = newRightMouseDown;
            sendMouseButtonEvent("RIGHT", rightMouseDown ? "PRESS" : "RELEASE");
        }
        
        // Handle middle mouse button
        if (newMiddleMouseDown != middleMouseDown) {
            middleMouseDown = newMiddleMouseDown;
            sendMouseButtonEvent("MIDDLE", middleMouseDown ? "PRESS" : "RELEASE");
        }
        
        // Mouse movement tracking disabled
    }
    
    /**
     * Send a key event to the server
     * 
     * @param eventType The type of event (PRESS or RELEASE)
     * @param keyCode The GLFW key code
     */
    private void sendKeyEvent(String eventType, int keyCode) {
        // Convert key code to a name for better readability
        String keyName = InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
        
        // Format: KEY|PRESS/RELEASE|keyCode|keyName
        String message = String.format("KEY|%s|%d|%s", eventType, keyCode, keyName);
        LOGGER.info("Attempting to send key event: {}", message);
        sendInputEvent("KEY", message);
    }
    
    /**
     * Send a mouse button event to the server
     * 
     * @param button The button (LEFT, RIGHT, MIDDLE)
     * @param eventType The type of event (PRESS or RELEASE)
     */
    private void sendMouseButtonEvent(String button, String eventType) {
        // Format: MOUSE_BUTTON|LEFT/RIGHT/MIDDLE|PRESS/RELEASE
        String message = String.format("MOUSE_BUTTON|%s|%s", button, eventType);
        LOGGER.info("Attempting to send mouse button event: {}", message);
        sendInputEvent("MOUSE", message);
    }
    
    /**
     * Mouse movement tracking has been disabled
     */
    
    /**
     * Send an input event to the server
     * 
     * @param type The type of input event
     * @param data The event data
     */
    private void sendInputEvent(String type, String data) {
        LOGGER.debug("Checking connection status before sending event...");
        if (!servLinker.isConnected()) {
            LOGGER.warn("Cannot send input event: not connected to server");
            return;
        }
        
        // Format: INPUT|type|data
        String message = String.format("INPUT|%s|%s", type, data);
        LOGGER.debug("Sending message to server: {}", message);
        boolean sent = servLinker.sendMessage(message);
        
        if (sent) {
            LOGGER.info("Successfully sent message: {}", message);
        } else {
            LOGGER.error("Failed to send input event to server: {}", message);
        }
    }
    
    /**
     * Check if input capture is currently enabled
     * 
     * @return true if capturing, false otherwise
     */
    public boolean isCapturing() {
        return isCapturing.get();
    }
}