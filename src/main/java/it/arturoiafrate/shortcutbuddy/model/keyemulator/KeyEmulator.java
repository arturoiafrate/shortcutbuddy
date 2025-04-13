package it.arturoiafrate.shortcutbuddy.model.keyemulator;

import lombok.extern.slf4j.Slf4j;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class KeyEmulator {

    private static final Map<String, Integer> KEY_MAP = createKeyMap();

    public void emulateKeysAsync(List<String> keysToPress, long delayMillis) {
        if (keysToPress == null || keysToPress.isEmpty()) {
            log.warn("Empty shortcut!.");
            return;
        }

        Thread emulationThread = new Thread(() -> {
            try {
                Thread.sleep(delayMillis);

                Robot robot = new Robot();
                robot.setAutoDelay(10);
                robot.setAutoWaitForIdle(true);

                if (keysToPress.size() == 2) {
                    String keyName1 = keysToPress.get(0);
                    String keyName2 = keysToPress.get(1);

                    if (keyName1 != null && keyName1.equalsIgnoreCase(keyName2)) {
                        Integer vkCode = KEY_MAP.get(keyName1.toUpperCase());
                        if (vkCode != null && isModifier(vkCode)) {
                            robot.keyPress(vkCode);
                            robot.keyRelease(vkCode);
                            robot.delay(50);
                            robot.keyPress(vkCode);
                            robot.keyRelease(vkCode);
                            return;
                        }
                    }
                }

                List<Integer> modifierKeys = new ArrayList<>();
                List<Integer> normalKeys = new ArrayList<>();

                for (String keyName : keysToPress) {
                    Integer vkCode = KEY_MAP.get(keyName.toUpperCase());
                    if (vkCode != null) {
                        if (isModifier(vkCode)) {
                            modifierKeys.add(vkCode);
                        } else {
                            normalKeys.add(vkCode);
                        }
                    } else {
                        return;
                    }
                }

                for (int modKey : modifierKeys) {
                    robot.keyPress(modKey);
                }

                for (int normalKey : normalKeys) {
                    robot.keyPress(normalKey);
                    robot.keyRelease(normalKey);
                }

                Collections.reverse(modifierKeys);
                for (int modKey : modifierKeys) {
                    robot.keyRelease(modKey);
                }

            } catch (Exception e) {
                log.error("Key emulation error!", e);
            }
        });

        emulationThread.setName("KeyEmulationThread-" + String.join("+", keysToPress));
        emulationThread.setDaemon(true);
        emulationThread.start();
    }




    private static Map<String, Integer> createKeyMap() {
        Map<String, Integer> map = new HashMap<>();

        map.put("CTRL", KeyEvent.VK_CONTROL);
        map.put("ALT", KeyEvent.VK_ALT);
        map.put("ALTGR", KeyEvent.VK_ALT_GRAPH);
        map.put("SHIFT", KeyEvent.VK_SHIFT);
        map.put("WIN", KeyEvent.VK_WINDOWS);
        map.put("META", KeyEvent.VK_META);


        for (char c = 'A'; c <= 'Z'; c++) map.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        for (char c = '0'; c <= '9'; c++) map.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        for (int i = 1; i <= 12; i++) map.put("F" + i, KeyEvent.VK_F1 + (i - 1));

        map.put("SPACE", KeyEvent.VK_SPACE);
        map.put("ENTER", KeyEvent.VK_ENTER);
        map.put("TAB", KeyEvent.VK_TAB);
        map.put("ESC", KeyEvent.VK_ESCAPE);
        map.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        map.put("DELETE", KeyEvent.VK_DELETE);
        map.put("INSERT", KeyEvent.VK_INSERT);
        map.put("HOME", KeyEvent.VK_HOME);
        map.put("END", KeyEvent.VK_END);
        map.put("PAGEUP", KeyEvent.VK_PAGE_UP);
        map.put("PAGEDOWN", KeyEvent.VK_PAGE_DOWN);
        map.put("CAPSLOCK", KeyEvent.VK_CAPS_LOCK);
        map.put("SCROLLLOCK", KeyEvent.VK_SCROLL_LOCK);
        map.put("PAUSE", KeyEvent.VK_PAUSE);
        map.put("PRINTSCREEN", KeyEvent.VK_PRINTSCREEN);
        map.put("CONTEXTMENU", KeyEvent.VK_CONTEXT_MENU);
        map.put("UP", KeyEvent.VK_UP);
        map.put("DOWN", KeyEvent.VK_DOWN);
        map.put("LEFT", KeyEvent.VK_LEFT);
        map.put("RIGHT", KeyEvent.VK_RIGHT);
        map.put("`", KeyEvent.VK_BACK_QUOTE);
        map.put("BACKQUOTE", KeyEvent.VK_BACK_QUOTE);
        map.put("-", KeyEvent.VK_MINUS);
        map.put("MINUS", KeyEvent.VK_MINUS);
        map.put("=", KeyEvent.VK_EQUALS);
        map.put("EQUALS", KeyEvent.VK_EQUALS);
        map.put("[", KeyEvent.VK_OPEN_BRACKET);
        map.put("OPENBRACKET", KeyEvent.VK_OPEN_BRACKET);
        map.put("]", KeyEvent.VK_CLOSE_BRACKET);
        map.put("CLOSEBRACKET", KeyEvent.VK_CLOSE_BRACKET);
        map.put("\\", KeyEvent.VK_BACK_SLASH);
        map.put("BACKSLASH", KeyEvent.VK_BACK_SLASH);
        map.put(";", KeyEvent.VK_SEMICOLON);
        map.put("SEMICOLON", KeyEvent.VK_SEMICOLON);
        map.put("'", KeyEvent.VK_QUOTE);
        map.put("QUOTE", KeyEvent.VK_QUOTE);
        map.put(",", KeyEvent.VK_COMMA);
        map.put("COMMA", KeyEvent.VK_COMMA);
        map.put(".", KeyEvent.VK_PERIOD);
        map.put("PERIOD", KeyEvent.VK_PERIOD);
        map.put("/", KeyEvent.VK_SLASH);
        map.put("SLASH", KeyEvent.VK_SLASH);
        map.put("+", KeyEvent.VK_PLUS);
        map.put("*", KeyEvent.VK_ASTERISK);
        map.put("NUMPAD0", KeyEvent.VK_NUMPAD0);
        map.put("NUMPAD1", KeyEvent.VK_NUMPAD1);
        map.put("NUMPAD2", KeyEvent.VK_NUMPAD2);
        map.put("NUMPAD3", KeyEvent.VK_NUMPAD3);
        map.put("NUMPAD4", KeyEvent.VK_NUMPAD4);
        map.put("NUMPAD5", KeyEvent.VK_NUMPAD5);
        map.put("NUMPAD6", KeyEvent.VK_NUMPAD6);
        map.put("NUMPAD7", KeyEvent.VK_NUMPAD7);
        map.put("NUMPAD8", KeyEvent.VK_NUMPAD8);
        map.put("NUMPAD9", KeyEvent.VK_NUMPAD9);
        map.put("NUM_LOCK", KeyEvent.VK_NUM_LOCK);
        map.put("NUMLOCK", KeyEvent.VK_NUM_LOCK);
        map.put("NUM_SEPARATOR", KeyEvent.VK_SEPARATOR);
        map.put("SEPARATOR", KeyEvent.VK_SEPARATOR);
        map.put("NUM_DECIMAL", KeyEvent.VK_DECIMAL);
        map.put("DECIMAL", KeyEvent.VK_DECIMAL);
        map.put("NUM_ADD", KeyEvent.VK_ADD);
        map.put("ADD", KeyEvent.VK_ADD);
        map.put("NUM_SUBTRACT", KeyEvent.VK_SUBTRACT);
        map.put("SUBTRACT", KeyEvent.VK_SUBTRACT);
        map.put("NUM_MULTIPLY", KeyEvent.VK_MULTIPLY);
        map.put("MULTIPLY", KeyEvent.VK_MULTIPLY);
        map.put("NUM_DIVIDE", KeyEvent.VK_DIVIDE);
        map.put("DIVIDE", KeyEvent.VK_DIVIDE);

        return Collections.unmodifiableMap(map);
    }

    private static boolean isModifier(int vkCode) {
        return vkCode == KeyEvent.VK_CONTROL || vkCode == KeyEvent.VK_ALT ||
                vkCode == KeyEvent.VK_SHIFT || vkCode == KeyEvent.VK_WINDOWS ||
                vkCode == KeyEvent.VK_META || vkCode == KeyEvent.VK_ALT_GRAPH;
    }
}
