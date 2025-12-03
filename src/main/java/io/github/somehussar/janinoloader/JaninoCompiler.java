package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IClassReloadListener;
import io.github.somehussar.janinoloader.classloader.MemoryClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;

import java.io.IOException;
import java.util.*;

public class JaninoCompiler implements IDynamicCompiler {
    private final Map<String, byte[]> classes = new HashMap<>();
    private final Set<IClassReloadListener> listenerSet = new HashSet<>();

    private final IDynamicCompiler.LoadClassCondition classFilter;
    private final ClassLoader parent;

    private Compiler compiler;
    private ClassLoader secure;

    public JaninoCompiler(ClassLoader parent) {
        this(parent, null);
    }

    public JaninoCompiler(ClassLoader parent, IDynamicCompiler.LoadClassCondition classFilter) {
        this.classFilter = classFilter;
        this.parent = parent;
    }

    @Override
    public ClassLoader getClassLoader() {
        return secure;
    }

    @Override
    public void compileClass(Resource... resources) throws CompileException, IOException {
        if (compiler == null)
            resetClassloader();

        compiler.compile(resources);

    }

    @Override
    public void recompileClass(Resource... resources) throws CompileException, IOException {
        for (Resource resource : resources) {
            removeClass(resource.getFileName().replaceFirst("\\.java$", ""));
        }
        resetClassloader();
        compileClass(resources);
    }

    @Override
    public void removeClass(String... names) {
        for (String name : names) {
            String key = name.replaceAll("\\.", "/") + (name.endsWith(".class") ? "" : ".class");

            classes.remove(key);
        }
        resetClassloader();
    }

    @Override
    public void addReloadListener(IClassReloadListener... listeners) {
        listenerSet.addAll(Arrays.asList(listeners));
    }

    @Override
    public void removeListener(IClassReloadListener... listeners) {
        Arrays.asList(listeners).forEach(listenerSet::remove);
    }

    private void notifyListeners() {
        listenerSet.removeIf( listener -> listener.handleClassLoaderReload(secure));
    }

    protected void resetClassloader() {
        boolean notify = secure != null;
        secure = new MemoryClassLoader(parent, classFilter, classes);

        if (notify) notifyListeners();

        compiler = new Compiler();
        compiler.setIClassLoader(new ClassLoaderIClassLoader(secure));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
    }

}
