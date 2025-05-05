package it.arturoiafrate.shortcutbuddy.model.manager.hotkey;

import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.IKeyObserver;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyListener;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyOperation;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalHotkeyManager allows other classes to subscribe to specific key combinations
 * and notifies them when these combinations are detected.
 */
@Singleton
@Slf4j
public class GlobalHotkeyManager implements IKeyObserver {

    private final KeyListener keyListener;

    private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();

    private final Map<Set<Integer>, List<IKeyObserver>> shortcutSubscribers = new ConcurrentHashMap<>();
    private final Map<Integer, List<IKeyObserver>> keyHoldSubscribers = new ConcurrentHashMap<>();
    private final Map<Integer, List<IKeyObserver>> keyPressSubscribers = new ConcurrentHashMap<>();

    @Inject
    public GlobalHotkeyManager(KeyListener keyListener) {
        this.keyListener = keyListener;
        this.keyListener.subscribe(KeyListener.KEY_ALL, this);
    }

    /**
     * Subscribe to a specific key combination.
     * @param keyCodes Set of key codes that form the shortcut
     * @param observer Observer to be notified when the shortcut is detected
     */
    public void subscribeShortcut(Set<Integer> keyCodes, IKeyObserver observer) {
        if (keyCodes == null || keyCodes.isEmpty() || observer == null) {
            return;
        }

        shortcutSubscribers.computeIfAbsent(keyCodes, k -> new ArrayList<>()).add(observer);
        log.debug("Observer {} subscribed to shortcut {}", observer.getClass().getSimpleName(), keyCodes);
    }

    /**
     * Subscribe to a specific key hold event.
     * @param keyCode Key code of the key to be held
     * @param observer Observer to be notified when the key is held
     */
    public void subscribeKeyHold(int keyCode, IKeyObserver observer) {
        if (keyCode == KeyListener.KEY_ALL || observer == null) {
            return;
        }

        keyHoldSubscribers.computeIfAbsent(keyCode, k -> new ArrayList<>()).add(observer);
        log.debug("Observer {} subscribed to key hold {}", observer.getClass().getSimpleName(), keyCode);
    }


    public void subscribeKeyEvent(int keyCode, IKeyObserver observer) {
        if (keyCode == KeyListener.KEY_ALL || observer == null) {
            return;
        }

        keyPressSubscribers.computeIfAbsent(keyCode, k -> new ArrayList<>()).add(observer);
        log.debug("Observer {} subscribed to key press {}", observer.getClass().getSimpleName(), keyCode);
    }

    /**
     * Unsubscribe from a specific key combination.
     * @param keyCodes Set of key codes that form the shortcut
     * @param observer Observer to be removed from the notification list
     */
    public void unsubscribeShortcut(Set<Integer> keyCodes, IKeyObserver observer) {
        if (keyCodes == null || keyCodes.isEmpty() || observer == null) {
            return;
        }

        List<IKeyObserver> observers = shortcutSubscribers.get(keyCodes);
        if (observers != null) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                shortcutSubscribers.remove(keyCodes);
            }
            log.debug("Observer {} unsubscribed from shortcut {}", observer.getClass().getSimpleName(), keyCodes);
        }
    }

    public void unsubscribeKeyPress(int keyCode, IKeyObserver observer) {
        if (keyCode == KeyListener.KEY_ALL || observer == null) {
            return;
        }

        List<IKeyObserver> observers = keyPressSubscribers.get(keyCode);
        if (observers != null) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                keyPressSubscribers.remove(keyCode);
            }
            log.debug("Observer {} unsubscribed from key press {}", observer.getClass().getSimpleName(), keyCode);
        }
    }

    public void unsubscribeKeyHold(int keyCode, IKeyObserver observer) {
        if (keyCode == KeyListener.KEY_ALL || observer == null) {
            return;
        }

        List<IKeyObserver> observers = keyHoldSubscribers.get(keyCode);
        if (observers != null) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                keyHoldSubscribers.remove(keyCode);
            }
            log.debug("Observer {} unsubscribed from key hold {}", observer.getClass().getSimpleName(), keyCode);
        }
    }

    @Override
    public void update(int keyCode, KeyOperation mode, NativeKeyEvent nativeKeyEvent) {
        // Update the state of keys based on the operation
        if (mode == KeyOperation.KEY_PRESS || mode == KeyOperation.KEY_HOLD) {
            pressedKeys.add(keyCode);
        } else if (mode == KeyOperation.KEY_RELEASE) {
            pressedKeys.remove(keyCode);
        }
        if(mode == KeyOperation.KEY_PRESS || mode == KeyOperation.KEY_RELEASE) {
            // Notify all observers interested in key press
            List<IKeyObserver> observers = keyPressSubscribers.get(keyCode);
            if (observers != null) {
                for (IKeyObserver observer : observers) {
                    try {
                        // Pass the key code that triggered this update and the operation mode
                        observer.update(keyCode, mode, nativeKeyEvent);
                    } catch (Exception e) {
                        log.error("Error notifying observer {} about key press {}",
                                observer.getClass().getSimpleName(), keyCode, e);
                    }
                }
            }
        } else if(mode == KeyOperation.KEY_HOLD) {
            // Notify all observers interested in key hold
            List<IKeyObserver> observers = keyHoldSubscribers.get(keyCode);
            if (observers != null) {
                for (IKeyObserver observer : observers) {
                    try {
                        // Pass the key code that triggered this update and the operation mode
                        observer.update(keyCode, mode, nativeKeyEvent);
                    } catch (Exception e) {
                        log.error("Error notifying observer {} about key hold {}",
                                observer.getClass().getSimpleName(), keyCode, e);
                    }
                }
            }
        }

        // Check if any registered shortcut is active
        for (Map.Entry<Set<Integer>, List<IKeyObserver>> entry : shortcutSubscribers.entrySet()) {
            Set<Integer> shortcut = entry.getKey();

            // Check if all keys in the shortcut are currently pressed
            if (pressedKeys.containsAll(shortcut)) {
                // Notify all observers interested in this shortcut
                for (IKeyObserver observer : entry.getValue()) {
                    try {
                        // Pass the key code that triggered this update and the operation mode
                        observer.update(keyCode, mode, nativeKeyEvent);
                    } catch (Exception e) {
                        log.error("Error notifying observer {} about shortcut {}",
                                observer.getClass().getSimpleName(), shortcut, e);
                    }
                }
            }
        }
    }

    public void shutdown() {
        keyListener.unsubscribe(KeyListener.KEY_ALL, this);
        shortcutSubscribers.clear();
        keyHoldSubscribers.clear();
        keyPressSubscribers.clear();
        pressedKeys.clear();
        keyListener.shutdown();
    }

}
