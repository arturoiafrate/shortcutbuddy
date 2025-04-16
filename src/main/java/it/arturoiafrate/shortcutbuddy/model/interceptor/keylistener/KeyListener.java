package it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Singleton
public class KeyListener implements NativeKeyListener {
    private final ConcurrentMap<Integer, List<IKeyObserver>> observers = new ConcurrentHashMap<>();
    private final Set<Integer> currentlyPressedKeys = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Timer> timers = new HashMap<>();
    private final ScheduledExecutorService keyHoldScheduler;
    private final ConcurrentMap<Integer, ScheduledFuture<?>> pendingHoldTasks = new ConcurrentHashMap<>();
    private static final long HOLD_DELAY_MS = 1000;

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
        } catch (NativeHookException e) {
            log.error("Error registering native hook", e);
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        log.info("Shutdown KeyListener...");
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.removeNativeKeyListener(this);
                GlobalScreen.unregisterNativeHook();
                log.info("JNativeHook listener removed.");
            }
        } catch (Exception e) {
            log.error("Error during JNativeHook deregistration", e);
        }

        if (keyHoldScheduler != null && !keyHoldScheduler.isShutdown()) {
            log.info("Shutdown KeyHoldScheduler...");
            keyHoldScheduler.shutdown();
            try {
                if (!keyHoldScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("KeyHoldScheduler not terminated in time, forcing shutdown...");
                    keyHoldScheduler.shutdownNow();
                } else {
                    log.info("KeyHoldScheduler killed.");
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
        final List<IKeyObserver> observersList = observers.get(keyCode);
        if (observersList == null || observersList.isEmpty()) {
            currentlyPressedKeys.remove(keyCode);
            return;
        }
        cancelHoldTask(keyCode);
        List<IKeyObserver> observersToNotifyPress = new ArrayList<>(observersList);
        Platform.runLater(() -> {
            observersToNotifyPress.forEach(observer -> {
                try {
                    observer.update(keyCode, KeyOperation.KEY_PRESS);
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
                            try { observer.update(keyCode, KeyOperation.KEY_HOLD); }
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
        final List<IKeyObserver> observersList = observers.get(keyCode);

        if (observersList == null || observersList.isEmpty()) {
            return;
        }
        cancelHoldTask(keyCode);

        List<IKeyObserver> observersToNotifyRelease = new ArrayList<>(observersList);
        Platform.runLater(() -> {
            observersToNotifyRelease.forEach(observer -> {
                try {
                    observer.update(keyCode, KeyOperation.KEY_RELEASE);
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

}
