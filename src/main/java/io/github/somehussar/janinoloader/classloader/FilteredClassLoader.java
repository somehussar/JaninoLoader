package io.github.somehussar.janinoloader.classloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;

public class FilteredClassLoader extends ClassLoader {

    private final IDynamicCompiler.LoadClassCondition classFilter;

    public FilteredClassLoader(ClassLoader parent, IDynamicCompiler.LoadClassCondition classFilter) {
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
