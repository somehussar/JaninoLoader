package io.github.somehussar.janinoloader.script;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;
import io.github.somehussar.janinoloader.classloader.MemoryClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassBodyEvaluator;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SafeScriptClassBody<DesiredType> implements IScriptClassBody<DesiredType> {
    private String rawScript;
    private DesiredType object;

    private boolean needToRecompile = true;

    private IDynamicCompiler compiler;
    private final String[] defaultImports;
    private final Class<?>[] interfaces;
    private final InstanceDelegate<DesiredType> instanceDelegate;

    private ClassLoader internalClassLoader;
    private String compiledClassName;

    private Class<DesiredType> clazz;
    private Map<String, byte[]> classBytes = new HashMap<>();

    SafeScriptClassBody(Class<DesiredType> outputClazz, IDynamicCompiler compiler, String[] defaultImports, String rawScript, InstanceDelegate<DesiredType> instanceDelegate, Class<?>[] interfaces) {
        this.clazz = outputClazz;
        this.compiler = compiler;
        compiler.addReloadListener(this);
        this.defaultImports = defaultImports;
        this.instanceDelegate = instanceDelegate != null ? instanceDelegate : clazz -> {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
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
            se.setParentClassLoader(compiler.getClassLoader());
            if (!clazz.isInterface()) {
                se.setExtendedClass(clazz);
            }

            se.setImplementedInterfaces(interfaces);
            if (defaultImports != null) se.setDefaultImports(defaultImports);

            se.cook(new StringReader(rawScript));
            classBytes = se.getBytecodes();
            internalClassLoader = new MemoryClassLoader(compiler.getClassLoader(), null, classBytes);

            Class<? extends DesiredType> outputClazz = (Class<? extends DesiredType>) se.getClazz();
            compiledClassName = outputClazz.getCanonicalName();
//            byte[] bytes = classBytes.get(compiledClassName);
//            System.out.println(compiledClassName + " bytes: ");
//            int count = 0;
//            for (int i = 0; i < bytes.length; i++) {
//                if (count >= 14) {
//                    count = 0;
//                    System.out.print("\n");
//                }
//                count++;
//                System.out.print(String.format("%02x ",bytes[i]));
//            }
//
//            System.out.println("\n");
            object = instanceDelegate.apply(outputClazz);
            needToRecompile = false;
        } else {
            System.out.println("test");
            internalClassLoader = new MemoryClassLoader(compiler.getClassLoader(), null, classBytes);
//            if (serialized != null) {
//                ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
//
//                ObjectInputStream in = new ReloadingObjectInputStream(
//                        bais,
//                        internalClassLoader
//                );
//
//                object = (DesiredType) in.readObject();
//            } else
            object = instanceDelegate.apply((Class<? extends DesiredType>) internalClassLoader.loadClass(compiledClassName));
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
