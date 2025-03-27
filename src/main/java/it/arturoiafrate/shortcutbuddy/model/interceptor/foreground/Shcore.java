package it.arturoiafrate.shortcutbuddy.model.interceptor.foreground;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinNT.HRESULT; // Importa HRESULT
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Shcore extends StdCallLibrary {
    Shcore INSTANCE = Native.load("Shcore", Shcore.class, W32APIOptions.DEFAULT_OPTIONS);

    public static interface MONITOR_DPI_TYPE {
        public static final int MDT_EFFECTIVE_DPI = 0;
        public static final int MDT_ANGULAR_DPI = 1;
        public static final int MDT_RAW_DPI = 2;
        public static final int MDT_DEFAULT = MDT_EFFECTIVE_DPI;
    }

    HRESULT GetDpiForMonitor(WinUser.HMONITOR hmonitor,
                             int dpiType,
                             IntByReference dpiX,
                             IntByReference dpiY
    );
}
