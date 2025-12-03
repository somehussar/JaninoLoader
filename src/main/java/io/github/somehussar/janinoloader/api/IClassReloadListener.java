package io.github.somehussar.janinoloader.api;

import io.github.somehussar.janinoloader.JaninoCompiler;
import org.codehaus.commons.compiler.CompileException;

import java.io.IOException;

public interface IClassReloadListener {
    /**
     * Manually have to register a listener to a {@link JaninoCompiler}
     * @param loader The new classloader for Janino-loaded classes
     * @return true if you want to stop being a listener
     */
    boolean handleClassLoaderReload(ClassLoader loader);
}
