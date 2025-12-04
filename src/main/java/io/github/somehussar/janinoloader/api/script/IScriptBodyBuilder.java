package io.github.somehussar.janinoloader.api.script;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.script.SafeScriptClassBuilder;

public interface IScriptBodyBuilder<DesiredType> {

    static <T> IScriptBodyBuilder<T> getBuilder(Class<T> targetClass, IDynamicCompiler compiler) {
        return new SafeScriptClassBuilder<>(targetClass, compiler);
    }

    IScriptBodyBuilder<DesiredType> setDefaultImports(String... imports);
    IScriptBodyBuilder<DesiredType> setImplementedTypes(Class<?>... interfaces);
    IScriptBodyBuilder<DesiredType> setScript(String script);
    IScriptBodyBuilder<DesiredType> setInstanceDelegate(IScriptClassBody.InstanceDelegate<DesiredType> delegate);

    IScriptClassBody<DesiredType> build();
}
