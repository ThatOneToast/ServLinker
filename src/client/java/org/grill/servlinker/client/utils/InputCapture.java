package org.grill.servlinker.client.utils;

import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.grill.servlinker.Servlinker;
import org.grill.servlinker.client.ServlinkerClient;
import org.grill.servlinker.client.networking.KeyPressC2SPacket;
import org.grill.servlinker.client.utils.DebugLogger;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class InputCapture {
    @Getter private boolean capturing = true; // player can change this via keybinds
    private static final DebugLogger LOGGER = ServlinkerClient.LOGGER;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private final java.util.List<Integer> keyPressOrder = new java.util.ArrayList<>(); // Track order of key presses
    private final Set<Set<Integer>> activeSequences = new HashSet<>();
    private final Set<Set<Integer>> brokenSequences = new HashSet<>();
    private final java.util.Map<Set<Integer>, java.util.List<Integer>> sequenceOrderMap = new java.util.HashMap<>(); // Store order for each sequence

    // Event buffering for proper modifier key ordering
    private final Set<Integer> modifierKeys = Set.of(
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
        GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
        GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
        GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER
    );
    private final java.util.List<KeyEvent> eventBuffer = new java.util.ArrayList<>();
    private int bufferDelay = 0;

    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;
    private boolean middleMouseDown = false;
    private boolean button4Down = false;
    private boolean button5Down = false;

    // Helper class for buffering key events
    private static class KeyEvent {
        final String action;
        final int keyCode;
        final boolean isModifier;
        
        KeyEvent(String action, int keyCode, boolean isModifier) {
            this.action = action;
            this.keyCode = keyCode;
            this.isModifier = isModifier;
        }
    }

    private static final int[] VALID_KEY_CODES = {
            // Modifier keys (CHECK THESE FIRST for proper sequence detection)
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_LEFT_SUPER,
            GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_SUPER,

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

            // Punctuation and other keys
            GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_LEFT_BRACKET,
            GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE,
            GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH
    };

    public InputCapture() {
        // Test basic logging immediately
        LOGGER.info("InputCapture constructor called - initializing input capture system");
        register();
    }

    private void register() {
        System.out.println("InputCapture register() - SYSTEM.OUT TEST");
        LOGGER.info("Registering client tick events for InputCapture");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only log every 60 ticks to avoid spam
            if (client.world != null && client.world.getTime() % 60 == 0) {
                System.out.println("Tick event fired - time: " + client.world.getTime());
                LOGGER.info("Tick event fired - client.player: {}, capturing: {}", 
                    client.player != null ? "exists" : "null", capturing);
            }
            
            // Comment out server checks for debugging
            // if(!ServlinkerClient.serverHasPlugin) return;
            // boolean connectedToMultiplayer = client.getNetworkHandler() != null && client.getCurrentServerEntry() != null;
            // if (!capturing || !connectedToMultiplayer) return;
            
            if (client.player == null) {
                LOGGER.debug("Skipping input capture - no player");
                return;
            }
            if (!capturing) {
                LOGGER.debug("Skipping input capture - capturing disabled");
                return;
            }

            if (client.world != null && client.world.getTime() % 60 == 0) {
                LOGGER.info("Running input capture checks");
            }
            checkKeyboardState(client);
            processEventBuffer();
            checkSequenceState(client);
            checkMouseState(client);
        });
    }

    public void startCapturing() {
        capturing = true;
    }
    public void stopCapturing() {
        capturing = false;
        pressedKeys.clear();
        keyPressOrder.clear();
        activeSequences.clear();
        brokenSequences.clear();
        sequenceOrderMap.clear();
        eventBuffer.clear();
        bufferDelay = 0;
        leftMouseDown = rightMouseDown = middleMouseDown = button4Down = button5Down = false;
    }

    private void checkKeyboardState(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        LOGGER.debug("Checking keyboard state - window handle: {}, pressed keys: {}", window, pressedKeys.size());
        
        int checkedKeys = 0;
        for (int keyCode : VALID_KEY_CODES) {
            boolean pressed = InputUtil.isKeyPressed(window, keyCode);
            if (pressed) {
                checkedKeys++;
                LOGGER.debug("Key {} is currently pressed", keyCode);
            }
            
            if (pressed && !pressedKeys.contains(keyCode)) {
                pressedKeys.add(keyCode);
                keyPressOrder.add(keyCode); // Track the order this key was pressed
                bufferKeyEvent("PRESS", keyCode);
            } else if (!pressed && pressedKeys.contains(keyCode)) {
                pressedKeys.remove(keyCode);
                keyPressOrder.remove(Integer.valueOf(keyCode)); // Remove from order tracking
                bufferKeyEvent("RELEASE", keyCode);
            }
        }
        
        if (checkedKeys > 0) {
            LOGGER.debug("Found {} currently pressed keys during check", checkedKeys);
        }
    }

    private void checkSequenceState(MinecraftClient client) {
        LOGGER.debug("Checking sequence state. Pressed keys: {} Active sequences: {} Broken sequences: {}", 
                    pressedKeys.size(), activeSequences.size(), brokenSequences.size());
        
        // Only detect sequences for 2+ keys - single keys should never be sequences
        if (pressedKeys.size() >= 2) {
            Set<Integer> currentCombination = new HashSet<>(pressedKeys);
            LOGGER.debug("SEQUENCE: Checking combination: {} (size={})", currentCombination, currentCombination.size());

            boolean isNewSequence = true;
            for (Set<Integer> activeSeq : activeSequences) {
                if (activeSeq.equals(currentCombination)) {
                    isNewSequence = false;
                    break;
                }
            }
            
            if (isNewSequence) {
                boolean wasRestored = false;
                LOGGER.debug("SEQUENCE: Checking for restoration. Current combination: {} Broken sequences: {}", 
                            currentCombination, brokenSequences);
                for (Set<Integer> brokenSeq : new HashSet<>(brokenSequences)) {
                    LOGGER.debug("SEQUENCE: Comparing with broken sequence: {}", brokenSeq);
                    if (brokenSeq.equals(currentCombination)) {
                        brokenSequences.remove(brokenSeq);
                        Set<Integer> restoredSequence = new HashSet<>(currentCombination);
                        activeSequences.add(restoredSequence);
                        
                        // Restore the sequence order mapping by recreating it from current press order
                        java.util.List<Integer> sequenceOrder = new java.util.ArrayList<>();
                        for (Integer keyCode : keyPressOrder) {
                            if (restoredSequence.contains(keyCode)) {
                                sequenceOrder.add(keyCode);
                            }
                        }
                        sequenceOrderMap.put(restoredSequence, sequenceOrder);
                        
                        LOGGER.debug("SEQUENCE: Restoring sequence: {} with order: {}", currentCombination, sequenceOrder);
                        sendSequenceEvent("RESTORE", currentCombination);
                        wasRestored = true;
                        break;
                    }
                }
                
                if (!wasRestored) {
                    Set<Integer> newSequence = new HashSet<>(currentCombination);
                    activeSequences.add(newSequence);
                    // Store the current press order for this sequence
                    java.util.List<Integer> sequenceOrder = new java.util.ArrayList<>();
                    for (Integer keyCode : keyPressOrder) {
                        if (newSequence.contains(keyCode)) {
                            sequenceOrder.add(keyCode);
                        }
                    }
                    sequenceOrderMap.put(newSequence, sequenceOrder);
                    LOGGER.debug("SEQUENCE: New sequence detected: {} with order: {}", currentCombination, sequenceOrder);
                    sendSequenceEvent("PRESS", currentCombination);
                } else {
                    LOGGER.debug("SEQUENCE: Restoration completed");
                }
            }
        }

        // Check for broken sequences (when some keys from a sequence are released)
        for (Set<Integer> activeSeq : new HashSet<>(activeSequences)) {
            if (!pressedKeys.containsAll(activeSeq)) {
                activeSequences.remove(activeSeq);
                LOGGER.debug("SEQUENCE: Breaking sequence: {} (remaining keys: {})", activeSeq, pressedKeys);
                sendSequenceEvent("RELEASE", activeSeq);
                
                // Only add to broken sequences if it was a multi-key sequence
                if (activeSeq.size() >= 2) {
                    brokenSequences.add(activeSeq);
                    LOGGER.debug("SEQUENCE: Added to broken sequences: {}", activeSeq);
                } else {
                    LOGGER.debug("SEQUENCE: Single key sequence, not adding to broken sequences");
                }
                
                // Clean up the sequence order mapping
                sequenceOrderMap.remove(activeSeq);
            }
        }

        // Clean up broken sequences when no keys from them are still pressed
        for (Set<Integer> brokenSeq : new HashSet<>(brokenSequences)) {
            boolean anyKeyStillPressed = false;
            for (Integer key : brokenSeq) {
                if (pressedKeys.contains(key)) {
                    anyKeyStillPressed = true;
                    break;
                }
            }
            if (!anyKeyStillPressed) {
                LOGGER.debug("SEQUENCE: Cleaning up broken sequence: {}", brokenSeq);
                brokenSequences.remove(brokenSeq);
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

    private void bufferKeyEvent(String action, int keyCode) {
        boolean isModifier = modifierKeys.contains(keyCode);
        String keyName = InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
        
        LOGGER.debug("BUFFER: {} {} ({}) isModifier={} bufferSize={} delay={}", 
                    action, keyCode, keyName.replace("key.keyboard.", ""), 
                    isModifier, eventBuffer.size(), bufferDelay);
        
        eventBuffer.add(new KeyEvent(action, keyCode, isModifier));
        
        // If we detect any modifier key events, delay the buffer processing
        if (isModifier && "PRESS".equals(action)) {
            bufferDelay = 3; // Wait 3 ticks to ensure proper ordering
            LOGGER.debug("BUFFER: Set delay to 3 for modifier key");
        }
    }

    private void processEventBuffer() {
        if (eventBuffer.isEmpty()) {
            return;
        }

        // If we have a buffer delay, count it down
        if (bufferDelay > 0) {
            LOGGER.debug("BUFFER: Waiting, delay={} bufferSize={}", bufferDelay, eventBuffer.size());
            bufferDelay--;
            return;
        }

        LOGGER.debug("BUFFER: Processing {} events", eventBuffer.size());

        // Sort events: modifiers first, then others
        eventBuffer.sort((a, b) -> {
            if (a.isModifier && !b.isModifier) return -1;
            if (!a.isModifier && b.isModifier) return 1;
            return 0;
        });

        // Send all buffered events
        for (KeyEvent event : eventBuffer) {
            String keyName = InputUtil.Type.KEYSYM.createFromCode(event.keyCode).getTranslationKey();
            LOGGER.debug("BUFFER: Sending {} {} ({}) isModifier={}", 
                        event.action, event.keyCode, keyName.replace("key.keyboard.", ""), event.isModifier);
            sendKeyEvent(event.action, event.keyCode);
        }
        
        eventBuffer.clear();
    }

    private void sendKeyEvent(String action, int keyCode) {
        String keyName = InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
        String single_key = "KEY|" + action + "|" + keyCode + "|" + keyName;
        
        // Console debug
        LOGGER.debug("KEY EVENT: {} {} ({})", action, keyCode, keyName.replace("key.keyboard.", ""));
        
        sendPacket(single_key);
    }
    
    private void sendSequenceEvent(String action, Set<Integer> keyCombination) {
        StringBuilder keyCodesBuilder = new StringBuilder();
        StringBuilder keyNamesBuilder = new StringBuilder();
        
        java.util.List<Integer> orderedKeys = new java.util.ArrayList<>();
        
        // Use stored sequence order if available (for releases), otherwise use current press order
        java.util.List<Integer> storedOrder = sequenceOrderMap.get(keyCombination);
        if (storedOrder != null) {
            orderedKeys.addAll(storedOrder);
        } else {
            // Fallback to current press order for new sequences
            for (Integer keyCode : keyPressOrder) {
                if (keyCombination.contains(keyCode)) {
                    orderedKeys.add(keyCode);
                }
            }
        }
        
        for (int i = 0; i < orderedKeys.size(); i++) {
            int keyCode = orderedKeys.get(i);
            String keyName = InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
            
            if (i > 0) {
                keyCodesBuilder.append("+");
                keyNamesBuilder.append("+");
            }
            keyCodesBuilder.append(keyCode);
            keyNamesBuilder.append(keyName);
        }
        
        String sequenceMsg = "SEQ|" + action + "|" + keyCodesBuilder.toString() + "|" + keyNamesBuilder.toString();
        LOGGER.debug("Sending sequence event: {}", sequenceMsg);
        sendPacket(sequenceMsg);
    }

    private void sendMouseEvent(String button, String action) {
        String msg = "MOUSE_BUTTON|" + button + "|" + action;
        sendPacket(msg);
    }

    private static void sendPacket(String message) {
        LOGGER.debug("Sending packet: {}", message);
        ClientPlayNetworking.send(new KeyPressC2SPacket(message));
    }
}
