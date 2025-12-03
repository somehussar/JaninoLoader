package io.github.somehussar.janinoloader.classloader;

import io.github.somehussar.janinoloader.JaninoCompiler;

public class FilteredClassLoader extends ClassLoader {

    private final JaninoCompiler.LoadClassCondition classFilter;

    public FilteredClassLoader(ClassLoader parent, JaninoCompiler.LoadClassCondition classFilter) {
        super(parent);
        this.classFilter = classFilter;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (classFilter != null && !classFilter.isValid(name))
            throw new ClassNotFoundException(classFilter.classNotLoadedMessage(name));
        return super.loadClass(name, resolve);
    }
}
