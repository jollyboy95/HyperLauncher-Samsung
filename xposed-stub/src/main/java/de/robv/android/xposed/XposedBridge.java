package de.robv.android.xposed;
import java.lang.reflect.Member;
import java.util.Set;
public final class XposedBridge {
    private XposedBridge() {}
    public static void log(String text) { throw new UnsupportedOperationException("stub"); }
    public static void log(Throwable t) { throw new UnsupportedOperationException("stub"); }
    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> clazz, XC_MethodHook callback) { throw new UnsupportedOperationException("stub"); }
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String name, XC_MethodHook callback) { throw new UnsupportedOperationException("stub"); }
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) { throw new UnsupportedOperationException("stub"); }
}
