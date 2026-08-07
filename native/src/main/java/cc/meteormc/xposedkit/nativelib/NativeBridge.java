package cc.meteormc.xposedkit.nativelib;

import java.lang.reflect.Executable;

public class NativeBridge {
    static {
        System.loadLibrary("xposedkit");
    }

    public static native void Init();

    public static native <T> T AllocObject(Class<T> clazz);

    public static native Object CallNonvirtualMethod(Executable method, Object instance, Object... args);

    public static native <T> T[] VisitHeapObjects(Class<T> clazz);
}