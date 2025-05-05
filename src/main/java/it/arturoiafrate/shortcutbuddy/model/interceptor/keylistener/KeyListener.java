package it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import it.arturoiafrate.shortcutbuddy.model.type.BidirectionalMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Singleton
public class KeyListener implements NativeKeyListener, NativeMouseListener {
    private final ConcurrentMap<Integer, List<IKeyObserver>> observers = new ConcurrentHashMap<>();
    private final Set<Integer> currentlyPressedKeys = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Timer> timers = new HashMap<>();
    private final ScheduledExecutorService keyHoldScheduler;
    private final ConcurrentMap<Integer, ScheduledFuture<?>> pendingHoldTasks = new ConcurrentHashMap<>();
    private static final long HOLD_DELAY_MS = 1000;
    private static final BidirectionalMap<Integer, String> keyCodeToName = new BidirectionalMap<>();
    public static final int KEY_ALL = -999;

    static {
        keyCodeToName.put(NativeKeyEvent.VC_ESCAPE, "Esc");
        keyCodeToName.put(NativeKeyEvent.VC_F1, "F1");
        keyCodeToName.put(NativeKeyEvent.VC_F2, "F2");
        keyCodeToName.put(NativeKeyEvent.VC_F3, "F3");
        keyCodeToName.put(NativeKeyEvent.VC_F4, "F4");
        keyCodeToName.put(NativeKeyEvent.VC_F5, "F5");
        keyCodeToName.put(NativeKeyEvent.VC_F6, "F6");
        keyCodeToName.put(NativeKeyEvent.VC_F7, "F7");
        keyCodeToName.put(NativeKeyEvent.VC_F8, "F8");
        keyCodeToName.put(NativeKeyEvent.VC_F9, "F9");
        keyCodeToName.put(NativeKeyEvent.VC_F10, "F10");
        keyCodeToName.put(NativeKeyEvent.VC_F11, "F11");
        keyCodeToName.put(NativeKeyEvent.VC_F12, "F12");
        keyCodeToName.put(NativeKeyEvent.VC_F13, "F13");
        keyCodeToName.put(NativeKeyEvent.VC_F14, "F14");
        keyCodeToName.put(NativeKeyEvent.VC_F15, "F15");
        keyCodeToName.put(NativeKeyEvent.VC_F16, "F16");
        keyCodeToName.put(NativeKeyEvent.VC_F17, "F17");
        keyCodeToName.put(NativeKeyEvent.VC_F18, "F18");
        keyCodeToName.put(NativeKeyEvent.VC_F19, "F19");
        keyCodeToName.put(NativeKeyEvent.VC_F20, "F20");
        keyCodeToName.put(NativeKeyEvent.VC_F21, "F21");
        keyCodeToName.put(NativeKeyEvent.VC_F22, "F22");
        keyCodeToName.put(NativeKeyEvent.VC_F23, "F23");
        keyCodeToName.put(NativeKeyEvent.VC_F24, "F24");
        keyCodeToName.put(NativeKeyEvent.VC_ENTER, "Enter");
        keyCodeToName.put(NativeKeyEvent.VC_SPACE, "Space");
        keyCodeToName.put(NativeKeyEvent.VC_TAB, "Tab");
        keyCodeToName.put(NativeKeyEvent.VC_BACKSPACE, "Backspace");
        keyCodeToName.put(NativeKeyEvent.VC_UP, "Up");
        keyCodeToName.put(NativeKeyEvent.VC_DOWN, "Down");
        keyCodeToName.put(NativeKeyEvent.VC_LEFT, "Left");
        keyCodeToName.put(NativeKeyEvent.VC_RIGHT, "Right");
        keyCodeToName.put(NativeKeyEvent.VC_DELETE, "Del");
        keyCodeToName.put(NativeKeyEvent.VC_INSERT, "Ins");
        keyCodeToName.put(NativeKeyEvent.VC_HOME, "Home");
        keyCodeToName.put(NativeKeyEvent.VC_END, "End");
        keyCodeToName.put(NativeKeyEvent.VC_PAGE_UP, "Page Up");
        keyCodeToName.put(NativeKeyEvent.VC_PAGE_DOWN, "Page Down");
        keyCodeToName.put(NativeKeyEvent.VC_CAPS_LOCK, "Caps Lock");
        keyCodeToName.put(NativeKeyEvent.VC_NUM_LOCK, "Num Lock");
        keyCodeToName.put(NativeKeyEvent.VC_SCROLL_LOCK, "Scroll Lock");
        keyCodeToName.put(NativeKeyEvent.VC_PRINTSCREEN, "Print Screen");
        keyCodeToName.put(NativeKeyEvent.VC_CONTEXT_MENU, "Context Menu");
        keyCodeToName.put(NativeKeyEvent.VC_PAUSE, "Pause");
        keyCodeToName.put(NativeKeyEvent.VC_SHIFT, "Shift");
        keyCodeToName.put(NativeKeyEvent.VC_CONTROL, "Ctrl");
        keyCodeToName.put(NativeKeyEvent.VC_ALT, "Alt");
        keyCodeToName.put(NativeKeyEvent.VC_META, "Win");

        keyCodeToName.put(NativeKeyEvent.VC_A, "A");
        keyCodeToName.put(NativeKeyEvent.VC_B, "B");
        keyCodeToName.put(NativeKeyEvent.VC_C, "C");
        keyCodeToName.put(NativeKeyEvent.VC_D, "D");
        keyCodeToName.put(NativeKeyEvent.VC_E, "E");
        keyCodeToName.put(NativeKeyEvent.VC_F, "F");
        keyCodeToName.put(NativeKeyEvent.VC_G, "G");
        keyCodeToName.put(NativeKeyEvent.VC_H, "H");
        keyCodeToName.put(NativeKeyEvent.VC_I, "I");
        keyCodeToName.put(NativeKeyEvent.VC_J, "J");
        keyCodeToName.put(NativeKeyEvent.VC_K, "K");
        keyCodeToName.put(NativeKeyEvent.VC_L, "L");
        keyCodeToName.put(NativeKeyEvent.VC_M, "M");
        keyCodeToName.put(NativeKeyEvent.VC_N, "N");
        keyCodeToName.put(NativeKeyEvent.VC_O, "O");
        keyCodeToName.put(NativeKeyEvent.VC_P, "P");
        keyCodeToName.put(NativeKeyEvent.VC_Q, "Q");
        keyCodeToName.put(NativeKeyEvent.VC_R, "R");
        keyCodeToName.put(NativeKeyEvent.VC_S, "S");
        keyCodeToName.put(NativeKeyEvent.VC_T, "T");
        keyCodeToName.put(NativeKeyEvent.VC_U, "U");
        keyCodeToName.put(NativeKeyEvent.VC_V, "V");
        keyCodeToName.put(NativeKeyEvent.VC_W, "W");
        keyCodeToName.put(NativeKeyEvent.VC_X, "X");
        keyCodeToName.put(NativeKeyEvent.VC_Y, "Y");
        keyCodeToName.put(NativeKeyEvent.VC_Z, "Z");

        keyCodeToName.put(NativeKeyEvent.VC_0, "0");
        keyCodeToName.put(NativeKeyEvent.VC_1, "1");
        keyCodeToName.put(NativeKeyEvent.VC_2, "2");
        keyCodeToName.put(NativeKeyEvent.VC_3, "3");
        keyCodeToName.put(NativeKeyEvent.VC_4, "4");
        keyCodeToName.put(NativeKeyEvent.VC_5, "5");
        keyCodeToName.put(NativeKeyEvent.VC_6, "6");
        keyCodeToName.put(NativeKeyEvent.VC_7, "7");
        keyCodeToName.put(NativeKeyEvent.VC_8, "8");
        keyCodeToName.put(NativeKeyEvent.VC_9, "9");

        keyCodeToName.put(NativeKeyEvent.VC_OPEN_BRACKET, "[");
        keyCodeToName.put(NativeKeyEvent.VC_BACK_SLASH, "\\");
        keyCodeToName.put(NativeKeyEvent.VC_CLOSE_BRACKET, "]");
        keyCodeToName.put(NativeKeyEvent.VC_SEMICOLON, ";");
        keyCodeToName.put(NativeKeyEvent.VC_QUOTE, "\"");
        keyCodeToName.put(NativeKeyEvent.VC_COMMA, ",");
        keyCodeToName.put(NativeKeyEvent.VC_PERIOD, ".");
        keyCodeToName.put(NativeKeyEvent.VC_SLASH, "/");
        keyCodeToName.put(NativeKeyEvent.VC_EQUALS, "=");
        keyCodeToName.put(NativeKeyEvent.VC_MINUS, "-");
    }

    @Inject
    public KeyListener() {
        ThreadFactory daemonThreadFactory = runnable -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setName("KeyListener-HoldDetectorThread");
            t.setDaemon(true);
            return t;
        };
        this.keyHoldScheduler = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory);

        registerHook();
    }

    private void registerHook() throws RuntimeException{
        try {
            if (!GlobalScreen.isNativeHookRegistered()){
                GlobalScreen.registerNativeHook();
            }
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
        } catch (NativeHookException e) {
            log.error("Error registering native hook", e);
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.removeNativeKeyListener(this);
                GlobalScreen.removeNativeMouseListener(this);
                GlobalScreen.unregisterNativeHook();
            }
        } catch (Exception e) {
            log.error("Error during JNativeHook deregistration", e);
        }

        if (keyHoldScheduler != null && !keyHoldScheduler.isShutdown()) {
            keyHoldScheduler.shutdown();
            try {
                if (!keyHoldScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("KeyHoldScheduler not terminated in time, forcing shutdown...");
                    keyHoldScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("KeyHoldScheduler interrupted during shutdown, forcing shutdown now...");
                keyHoldScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void subscribe(int keyCode, IKeyObserver observer) {
        observers.computeIfAbsent(keyCode, k -> new ArrayList<>()).add(observer);
    }

    public void unsubscribe(int keyCode, IKeyObserver observer) {
        List<IKeyObserver> keyObservers = observers.get(keyCode);
        if (keyObservers != null) {
            keyObservers.remove(observer);
            if (keyObservers.isEmpty()) {
                 observers.remove(keyCode);
             }
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent){
        final int keyCode = nativeEvent.getKeyCode();
        if (!currentlyPressedKeys.add(keyCode)) {
            return;
        }

        cancelAllHoldTasks();
        final List<IKeyObserver> observersList = observers.getOrDefault(keyCode, new ArrayList<>());
        var allObserverList = observers.get(KEY_ALL);
        if (allObserverList != null && !allObserverList.isEmpty()) {
            observersList.addAll(allObserverList);
        }
        if (observersList == null || observersList.isEmpty()) {
            currentlyPressedKeys.remove(keyCode);
            return;
        }
        List<IKeyObserver> observersToNotifyPress = new ArrayList<>(observersList);
        Platform.runLater(() -> {
            observersToNotifyPress.forEach(observer -> {
                try {
                    observer.update(keyCode, KeyOperation.KEY_PRESS, nativeEvent);
                }
                catch (Exception e) {
                    log.error("Observer error {} during KEY_PRESS for {}", observer.getClass().getSimpleName(), keyCode, e);
                }
            });
        });
        Runnable holdTask = () -> {
            try {
                if (pendingHoldTasks.containsKey(keyCode)) {
                    List<IKeyObserver> observersToNotifyHold = new ArrayList<>(observersList);
                    Platform.runLater(() -> {
                        observersToNotifyHold.forEach(observer -> {
                            try { observer.update(keyCode, KeyOperation.KEY_HOLD, nativeEvent); }
                            catch (Exception e) { log.error("Error in observer {} during KEY_HOLD for {}", observer.getClass().getSimpleName(), keyCode, e); }
                        });
                    });
                    pendingHoldTasks.remove(keyCode);
                }
            } catch (Exception e) {
                log.error("Error in task KEY_HOLD scheduled for {}", keyCode, e);
            }
        };
        ScheduledFuture<?> future = keyHoldScheduler.schedule(holdTask, HOLD_DELAY_MS, TimeUnit.MILLISECONDS);
        pendingHoldTasks.put(keyCode, future);
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        final int keyCode = nativeEvent.getKeyCode();
        boolean wasPressed = currentlyPressedKeys.remove(keyCode);
        if (!wasPressed) {
            return;
        }
        final List<IKeyObserver> observersList = observers.getOrDefault(keyCode, new ArrayList<>());
        var allObserverList = observers.get(KEY_ALL);
        if (allObserverList != null && !allObserverList.isEmpty()) {
            observersList.addAll(allObserverList);
        }
        if (observersList == null || observersList.isEmpty()) {
            return;
        }
        cancelHoldTask(keyCode);

        List<IKeyObserver> observersToNotifyRelease = new ArrayList<>(observersList);
        Platform.runLater(() -> {
            observersToNotifyRelease.forEach(observer -> {
                try {
                    observer.update(keyCode, KeyOperation.KEY_RELEASE, nativeEvent);
                }
                catch (Exception e) {
                    log.error("Error in observer {} during KEY_RELEASE for {}", observer.getClass().getSimpleName(), keyCode, e);
                }
            });
        });
    }

    private void cancelHoldTask(int keyCode) {
        ScheduledFuture<?> existingTask = pendingHoldTasks.remove(keyCode);
        if (existingTask != null) {
            boolean cancelled = existingTask.cancel(false);
            if (cancelled) log.trace("Cancelled pending task KEY_HOLD for {}", keyCode);
        }
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeEvent){
        cancelAllHoldTasks();
    }

    private void cancelAllHoldTasks() {
        if (pendingHoldTasks.isEmpty()) {
            return;
        }
        for (ScheduledFuture<?> future : pendingHoldTasks.values()) {
            future.cancel(false);
        }
        pendingHoldTasks.clear();
    }

    public static String getKeyName(int keyCode) {
        return keyCodeToName.getForward(keyCode);
    }

    public static int getKeyCode(String keyName) {
        return keyCodeToName.getReverse(keyName);
    }
}
