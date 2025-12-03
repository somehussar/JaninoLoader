package io.github.somehussar.janinoloader.api;

public interface IClassReloadListener {
    /**
     * Manually have to register a listener to a {@link io.github.somehussar.janinoloader.classloader.JaninoClassLoader}
     * @param loader The new classloader for Janino-loaded classes
     * @return true if you want to stop being a listener
     */
    boolean handleClassLoaderReload(ClassLoader loader);
}
