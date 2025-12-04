package io.github.somehussar.janinoloader.script;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.janino.ClassBodyEvaluator;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SafeScriptClassBody<DesiredType> implements IScriptClassBody<DesiredType> {

    public static final InstanceDelegate<?> DEFAULT_INSTANCE_DELEGATE = clazz -> {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
    public static final ReloadDelegate<?> DEFAULT_RELOAD_DELEGATE = ((oldInstance, newInstance, internalClassLoader) -> {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(oldInstance);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ReloadingObjectInputStream in = new ReloadingObjectInputStream(bis, internalClassLoader);
            return in.readObject();
        } catch (Throwable i) {
            return newInstance;
        }

    });
    private String rawScript;
    private DesiredType object;

    private boolean needToRecompile = true;

    private IDynamicCompiler compiler;
    private final String[] defaultImports;
    private final Class<?>[] interfaces;
    private final InstanceDelegate<DesiredType> instanceDelegate;
    private final ReloadDelegate<DesiredType> reloadDelegate;

    private ClassLoader internalClassLoader;
    private String compiledClassName;

    private final Class<DesiredType> clazz;
    private Map<String, byte[]> classBytes = new HashMap<>();

    @SuppressWarnings("unchecked")
    SafeScriptClassBody(Class<DesiredType> outputClazz, IDynamicCompiler compiler, String[] defaultImports,
                        Class<?>[] interfaces, String rawScript,
                        InstanceDelegate<DesiredType> instanceDelegate,
                        ReloadDelegate<DesiredType> reloadDelegate) {
        this.clazz = outputClazz;
        this.compiler = compiler;
        compiler.addReloadListener(this);
        this.defaultImports = defaultImports;

        this.instanceDelegate = instanceDelegate != null ? instanceDelegate : (InstanceDelegate<DesiredType>) DEFAULT_INSTANCE_DELEGATE;
        this.reloadDelegate = reloadDelegate != null ? reloadDelegate : (ReloadDelegate<DesiredType>) DEFAULT_RELOAD_DELEGATE;

        if (clazz.isInterface()) {
            interfaces = Arrays.copyOf(interfaces, interfaces.length+1);
            interfaces[interfaces.length-1] = clazz;
        }
        this.interfaces = interfaces;
        this.rawScript = rawScript;
    }


    @Override
    public boolean handleClassLoaderReload(ClassLoader loader) {
        try {
            assertCompiled();
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
        assertCompiled();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void assertCompiled() throws CompileException, IOException, ClassNotFoundException {
        if (needToRecompile) {
            ClassBodyEvaluator se = new ClassBodyEvaluator();
            se.setParentClassLoader(compiler.getClassLoader());
            if (!clazz.isInterface()) {
                se.setExtendedClass(clazz);
            }

            se.setImplementedInterfaces(interfaces);
            if (defaultImports != null) se.setDefaultImports(defaultImports);

            se.cook(new StringReader(rawScript));
            classBytes = se.getBytecodes();
            Class<? extends DesiredType> outputClazz = (Class<? extends DesiredType>) se.getClazz();
            internalClassLoader = outputClazz.getClassLoader();
            compiledClassName = outputClazz.getCanonicalName();

            object = instanceDelegate.apply(outputClazz);
            needToRecompile = false;
        } else {
            internalClassLoader = new ByteArrayClassLoader(classBytes, compiler.getClassLoader());
            Class<? extends DesiredType> outputClazz = (Class<? extends DesiredType>) internalClassLoader.loadClass(compiledClassName);
            try {
                DesiredType newObject = instanceDelegate.apply(outputClazz);
                object = reloadDelegate.apply(object, newObject, internalClassLoader);
            } catch (Throwable e) {
                try {
                    object = instanceDelegate.apply(outputClazz);
                } catch (Throwable failedAgain) {
                    needToRecompile = true;
                    this.assertCompiled();
                }
            }
        }
    }

    @Override
    public void prepareToUnload() {
        if (compiler != null) compiler.removeListener(this);
        compiler = null;
        internalClassLoader = null;
        object = null;
    }
}
