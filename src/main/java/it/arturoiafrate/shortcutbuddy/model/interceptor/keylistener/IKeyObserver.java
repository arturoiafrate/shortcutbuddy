package it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public interface IKeyObserver {
    void update(int keyCode, KeyOperation mode, NativeKeyEvent nativeKeyEvent);
}
