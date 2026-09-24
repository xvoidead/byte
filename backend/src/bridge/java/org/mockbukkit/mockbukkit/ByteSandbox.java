package org.mockbukkit.mockbukkit;

import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.PropertyPermission;

/**
 * Права, с которыми MockBukkit запускает сервер и загружает плагины в песочнице Byte.
 *
 * <p>Ограниченный doPrivileged даёт только перечисленные права, все остальные проверки по-прежнему доходят
 * до кода ученика. В политике песочницы эти права выданы только jar-файлам MockBukkit и его зависимостей,
 * поэтому ученик не получит их, вызвав MockBukkit из своего кода: кадры его методов выше в стеке тоже проверяются.
 */
final class ByteSandbox {

    static final Permission[] PERMISSIONS = {
            // загрузчик классов плагина (MockBukkitConfiguredPluginClassLoader)
            new RuntimePermission("createClassLoader"),
            new RuntimePermission("getClassLoader"),
            new RuntimePermission("getProtectionDomain"),
            // ByteBuddy строит подкласс плагина, который загружает этот загрузчик
            new RuntimePermission("net.bytebuddy.createJavaDispatcher"),
            new ReflectPermission("newProxyInPackage.net.bytebuddy.*"),
            new PropertyPermission("net.bytebuddy.*", "read"),
    };

    private ByteSandbox() {
    }

    @SuppressWarnings("removal")
    static <T> T privileged(PrivilegedAction<T> action) {
        return AccessController.doPrivileged(action, null, PERMISSIONS);
    }
}
