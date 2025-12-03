package io.github.somehussar.janinoloader.classloader;


import io.github.somehussar.janinoloader.JaninoCompiler;

import java.util.Map;

public class MemoryClassLoader extends FilteredClassLoader {
    private final Map<String, byte[]> classes;

    public MemoryClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
        this(parent, null, classes);
    }
    public MemoryClassLoader(ClassLoader parent, JaninoCompiler.LoadClassCondition classFilter, Map<String,byte[]> classes) {
        super(parent, classFilter);
        this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name.replaceAll("\\.", "/") + ".class");
        if (bytes != null) {
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }

}
