package it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class KeyListener implements NativeKeyListener {


    private final Map<Integer, List<IKeyObserver>> observers = new HashMap<>();
    private final Map<Integer, Timer> timers = new HashMap<>();

    public KeyListener() {
        registerHook();
    }

    private void registerHook() throws RuntimeException{
        try {
            GlobalScreen.unregisterNativeHook();
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            log.error("Error registering native hook", e);
            throw new RuntimeException(e);
        }
    }

    public void subscribe(int keyCode, IKeyObserver observer) {
        observers.computeIfAbsent(keyCode, k -> new ArrayList<>()).add(observer);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent){
        List<IKeyObserver> observersList = observers.get(nativeEvent.getKeyCode());
        if (observersList == null) {
            return;
        }
        timers.put(nativeEvent.getKeyCode(), new Timer());
        timers.get(nativeEvent.getKeyCode())
                .schedule(new TimerTask() {
                    @Override
                    public void run() {
                        observersList.forEach( observer -> observer.update(nativeEvent.getKeyCode(), KeyOperation.KEY_HOLD));
                        timers.remove(nativeEvent.getKeyCode());
                    }
                }, 1000);
        observersList.forEach( observer -> observer.update(nativeEvent.getKeyCode(), KeyOperation.KEY_PRESS));
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        List<IKeyObserver> observersList = observers.get(nativeEvent.getKeyCode());
        if (observersList == null) {
            return;
        }
        if (timers.get(nativeEvent.getKeyCode()) != null) {
            timers.get(nativeEvent.getKeyCode()).cancel();
            timers.get(nativeEvent.getKeyCode()).purge();
            timers.remove(nativeEvent.getKeyCode());
        }
        observersList.forEach( observer -> observer.update(nativeEvent.getKeyCode(), KeyOperation.KEY_RELEASE));
    }

}
