package it.arturoiafrate.shortcutbuddy.model.interceptor.foreground;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.geometry.Rectangle2D;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


@Singleton
public class ForegroundAppInterceptor {

    private static final List<String> UWP_APP_NAMES = List.of(
            "ApplicationFrameHost.exe",
            "RuntimeBroker.exe",
            "svchost.exe"
    );

    @Inject
    public ForegroundAppInterceptor(){

    }

    public static String getForegroundAppTitle() {
        char[] buffer = new char[1024*2];
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
        return Native.toString(buffer);
    }

    public String getForegroundAppName(){
        char[] buffer = new char[1024*2];
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
        User32.INSTANCE.GetClassName(hwnd, buffer, 1024);
        return getProcessName(pid.getValue());
    }

    public Rectangle2D getForegroundAppBounds(){
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        User32.HMONITOR hMonitor = User32.INSTANCE.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST);

        WinUser.MONITORINFO monitorInfo = new WinUser.MONITORINFO();
        monitorInfo.cbSize = monitorInfo.size();
        User32.INSTANCE.GetMonitorInfo(hMonitor, monitorInfo);
        Rectangle2D physicalBounds =  new Rectangle2D(
                monitorInfo.rcWork.left,
                monitorInfo.rcWork.top,
                monitorInfo.rcWork.right - monitorInfo.rcWork.left,
                monitorInfo.rcWork.bottom - monitorInfo.rcWork.top
        );

        IntByReference dpiX = new IntByReference();
        IntByReference dpiY = new IntByReference();
        double scaleFactor = 1.0;
        WinNT.HRESULT result = Shcore.INSTANCE.GetDpiForMonitor(hMonitor, Shcore.MONITOR_DPI_TYPE.MDT_EFFECTIVE_DPI, dpiX, dpiY);
        int dpi = dpiX.getValue();
        if (dpi > 0) {
            scaleFactor = (double) dpi / 96.0;
        }
        return new Rectangle2D(
                physicalBounds.getMinX() / scaleFactor,
                physicalBounds.getMinY() / scaleFactor,
                physicalBounds.getWidth() / scaleFactor,
                physicalBounds.getHeight() / scaleFactor
        );
    }

    public void forceFocus(){
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        int foregroundThreadId = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null);
        int currentThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
        WinDef.DWORD foregroundThreadDWORD = new WinDef.DWORD(foregroundThreadId);
        WinDef.DWORD currentThreadDWORD = new WinDef.DWORD(currentThreadId);
        User32.INSTANCE.AttachThreadInput(foregroundThreadDWORD, currentThreadDWORD, true);
        User32.INSTANCE.SetForegroundWindow(hwnd);
    }

    private static String getProcessName(int processId) {
        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                if (processEntry.th32ProcessID.intValue() == processId) {
                    String processName = Native.toString(processEntry.szExeFile);
                    if (UWP_APP_NAMES.contains(processName)) {
                        String foregroundAppName = getForegroundAppTitle();
                        if(!StringUtils.isEmpty(foregroundAppName) && !foregroundAppName.endsWith(".exe")){
                            foregroundAppName = foregroundAppName + ".exe";
                        }
                        return foregroundAppName;
                    }
                    return processName;
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        return null;
    }
}

