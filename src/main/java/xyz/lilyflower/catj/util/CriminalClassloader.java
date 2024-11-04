package xyz.lilyflower.catj.util;

import java.util.HashMap;
import xyz.lilyflower.catj.CrimesAgainstTheJVM;

public class CriminalClassloader extends ClassLoader {
    private static final HashMap<String, Class<?>> LOADED = new HashMap<>();
    public static final CriminalClassloader INSTANCE = new CriminalClassloader(CrimesAgainstTheJVM.class.getClassLoader());

    private CriminalClassloader(ClassLoader parent) {
        super(parent);
    }

    Class<?> load(String name, byte[] bytes) {
        if (LOADED.containsKey(name)) {
            return LOADED.get(name);
        }

        Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
        this.resolveClass(clazz);
        LOADED.put(name, clazz);
        return clazz;
    }
}
