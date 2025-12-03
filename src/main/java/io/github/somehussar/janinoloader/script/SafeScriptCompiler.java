package io.github.somehussar.janinoloader.script;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.script.IScriptCompiler;
import io.github.somehussar.janinoloader.classloader.MemoryClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.Scanner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SafeScriptCompiler<DesiredType> implements IScriptCompiler<DesiredType> {
    private String rawScript;
    private DesiredType object;

    private boolean needToRecompile;

    private IDynamicCompiler compiler;
    private final String[] defaultImports;
    private final Function<Class<? extends DesiredType>, DesiredType> instanceDelegate;

    private ClassLoader internalClassLoader;
    private String compiledClassName;

    private Class<DesiredType> clazz;
    private Map<String, byte[]> classBytes = new HashMap<>();


    public SafeScriptCompiler(Class<DesiredType> extendingClass, IDynamicCompiler compiler, String[] defaultImports, String rawScript, Function<Class<? extends DesiredType>, DesiredType> instanceDelegate) {
        this.clazz = extendingClass;
        this.compiler = compiler;
        this.defaultImports = defaultImports;
        this.rawScript = rawScript;
        this.instanceDelegate = instanceDelegate;
    }


    @Override
    public boolean handleClassLoaderReload(ClassLoader loader) {
        try {
            attemptRecompile();
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public DesiredType get() {
        return object;
    }

    @Override
    public void setScript(String script) throws CompileException, IOException, ClassNotFoundException {
        this.needToRecompile = true;
        this.rawScript = script;
        attemptRecompile();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void attemptRecompile() throws CompileException, IOException, ClassNotFoundException {
        if (needToRecompile) {
            ClassBodyEvaluator se = new ClassBodyEvaluator();
            internalClassLoader = new ClassLoader(compiler.getClassLoader()) {};
            se.setParentClassLoader(internalClassLoader);
            se.setExtendedClass(clazz);
            se.setDefaultImports(defaultImports);
            se.cook(new Scanner(rawScript));
            compiledClassName = se.getClazz().getCanonicalName();
            Class<? extends DesiredType> outputClazz = (Class<? extends DesiredType>) se.getClazz();
            object = instanceDelegate.apply(outputClazz);
        } else {
            internalClassLoader = new MemoryClassLoader(compiler.getClassLoader(), null, classBytes);
            object = instanceDelegate.apply((Class<? extends DesiredType>) internalClassLoader.loadClass(compiledClassName));
        }
    }

    @Override
    public void prepareToUnload() {
        if (compiler != null) compiler.removeListener(this);
        compiler = null;
        internalClassLoader = null;

    }
}
