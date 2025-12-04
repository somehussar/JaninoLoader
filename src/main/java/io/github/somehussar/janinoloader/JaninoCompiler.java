package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IClassReloadListener;
import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.delegates.LoadClassCondition;
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

    private final LoadClassCondition classFilter;
    private final ClassLoader parent;

    private Compiler compiler;
    private ClassLoader secure;

    private boolean notify = false;

    JaninoCompiler(ClassLoader parent, LoadClassCondition classFilter) {
        this.classFilter = classFilter;
        this.parent = parent;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (compiler == null) resetClassloader();
        return secure;
    }

    @Override
    public void compileClass(Resource... resources) throws CompileException, IOException {
        if (compiler == null)
            resetClassloader();

        compiler.compile(resources);
        if (notify) { notifyListeners(); notify = false; }
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
        notify = secure != null;
        secure = new MemoryClassLoader(parent, classFilter, classes);

        compiler = new Compiler();
        compiler.setIClassLoader(new ClassLoaderIClassLoader(secure));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
    }

}
