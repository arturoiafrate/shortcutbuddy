package it.arturoiafrate.shortcutbuddy.model.interceptor.clipboard;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

public interface User32Ext extends User32 {
    User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

    int WM_CLIPBOARDUPDATE = 0x031D;

    boolean AddClipboardFormatListener(WinDef.HWND hwnd);
    boolean RemoveClipboardFormatListener(WinDef.HWND hwnd);

    void PostMessage(HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);

    /**
     * Structure to hold GUI thread information, including caret position
     */
    @Structure.FieldOrder({"cbSize", "flags", "hwndActive", "hwndFocus", "hwndCapture", 
                          "hwndMenuOwner", "hwndMoveSize", "hwndCaret", "rcCaret"})
    class GUITHREADINFO extends Structure {
        public int cbSize;
        public int flags;
        public HWND hwndActive;
        public HWND hwndFocus;
        public HWND hwndCapture;
        public HWND hwndMenuOwner;
        public HWND hwndMoveSize;
        public HWND hwndCaret;
        public RECT rcCaret;

        public GUITHREADINFO() {
            cbSize = size();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("cbSize", "flags", "hwndActive", "hwndFocus", "hwndCapture", 
                                "hwndMenuOwner", "hwndMoveSize", "hwndCaret", "rcCaret");
        }
    }

    /**
     * Gets information about the active GUI thread
     * @param idThread Thread ID or 0 for the current thread
     * @param lpgui Pointer to a GUITHREADINFO structure
     * @return true if successful, false otherwise
     */
    boolean GetGUIThreadInfo(int idThread, GUITHREADINFO lpgui);

    /**
     * Converts client coordinates to screen coordinates
     * @param hWnd Handle to the window whose client area is used for the conversion
     * @param lpPoint Pointer to a POINT structure that contains the client coordinates to be converted
     * @return true if successful, false otherwise
     */
    boolean ClientToScreen(HWND hWnd, WinDef.POINT lpPoint);
}
