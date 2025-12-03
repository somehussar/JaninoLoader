package io.github.somehussar.janinoloader.api;

import io.github.somehussar.janinoloader.JaninoCompilerBuilder;
import io.github.somehussar.janinoloader.api.delegates.LoadClassCondition;

public interface IDynamicCompilerBuilder {

    static IDynamicCompilerBuilder createBuilder() {
        return new JaninoCompilerBuilder();
    }

    IDynamicCompilerBuilder setParentClassLoader(ClassLoader classLoader);
    IDynamicCompilerBuilder setClassFilter(LoadClassCondition filter);
    IDynamicCompiler getCompiler();

}
