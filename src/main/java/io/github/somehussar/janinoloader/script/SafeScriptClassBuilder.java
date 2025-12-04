package io.github.somehussar.janinoloader.script;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.script.IScriptBodyBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;

import java.util.function.Function;

public class SafeScriptClassBuilder<DesiredType> implements IScriptBodyBuilder<DesiredType> {

    private final Class<DesiredType> clazz;
    private final IDynamicCompiler compiler;
    private String[] importList = new String[0];
    private Class<?>[] implementedClasses = new Class[0];
    private String rawScript;
    private Function<Class<? extends DesiredType>, DesiredType> delegate;
    public SafeScriptClassBuilder(Class<DesiredType> clazz, IDynamicCompiler compiler) {
        assert clazz != null;
        assert compiler != null;
        this.clazz = clazz;
        this.compiler = compiler;
    }

    @Override
    public IScriptBodyBuilder<DesiredType> setDefaultImports(String... imports) {
        if (imports == null) imports = new String[0];
        this.importList = imports;
        return this;
    }

    @Override
    public IScriptBodyBuilder<DesiredType> setImplementedTypes(Class<?>... interfaces) {
        if (interfaces == null) interfaces = new Class[0];
        this.implementedClasses = interfaces;
        return this;
    }

    @Override
    public IScriptBodyBuilder<DesiredType> setScript(String script) {
        assert script != null;
        this.rawScript = script;
        return this;
    }

    @Override
    public IScriptBodyBuilder<DesiredType> setInstanceDelegate(Function<Class<? extends DesiredType>, DesiredType> delegate) {
        this.delegate = delegate;
        return this;
    }

    @Override
    public IScriptClassBody<DesiredType> build() {
        return new SafeScriptClassBody<>(clazz, compiler, importList, rawScript, delegate, implementedClasses);
    }
}
