package io.github.somehussar.janinoloader.classloader;

import io.github.somehussar.janinoloader.api.IClassReloadListener;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JaninoClassLoader {
    private Map<String, byte[]> classes = new HashMap<>();
    private final Set<IClassReloadListener> listenerSet = new HashSet<>();

    private final LoadClassCondition classFilter;
    private final ClassLoader parent;

    private Compiler compiler;
    private ClassLoader secure;

    public JaninoClassLoader(ClassLoader parent) {
        this(parent, null);
    }

    public JaninoClassLoader(ClassLoader parent, LoadClassCondition classFilter) {
        this.classFilter = classFilter;
        this.parent = parent;
    }

    public ClassLoader getClassLoader() {
        return secure;
    }

    public void batchCompile(StringResource[] resources) throws CompileException, IOException {
        if (compiler == null)
            resetClassloader();
        compiler.compile(resources);

    }

    public void addClass(StringResource resource) throws CompileException, IOException {
        if (compiler == null)
            resetClassloader();
        compiler.compile(new Resource[]{resource});
    }

    public void removeClass(String... names) {
        for (String name : names) {
            String key = name.replaceAll("\\.", "/") + (name.endsWith(".class") ? "" : ".class");

            classes.remove(key);
        }
        resetClassloader();
    }

    public void addReloadListener(IClassReloadListener listener) {
        listenerSet.add(listener);
    }

    public void removeListener(IClassReloadListener listener) {
        listenerSet.remove(listener);
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

    public interface LoadClassCondition {
        boolean isValid(String name);
        default String classNotLoadedMessage(String name) {
            return name;
        }
    }
}
