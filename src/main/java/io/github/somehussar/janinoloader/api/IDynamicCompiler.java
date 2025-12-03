package io.github.somehussar.janinoloader.api;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;

import java.io.IOException;

public interface IDynamicCompiler {

    /**
     * Compiles and loads into the classloader. <br>
     * <br>
     * Supplying multiple resources at once allows for circular-dependency in compiled classes. <br>
     * <br>
     * {@link Resource} filename <bold>MUST</bold> be either a file-path format (<code>my/project/VeryCoolClass.java</code>) or fully qualified format (<code>my.project.VeryCoolClass</code>) <br>
     * If filename doesn't follow a specific format, neither {@linkplain IDynamicCompiler#recompileClass(Resource...) class hotswapping} nor {@linkplain #removeClass(String...) class removal} is possible. <br>
     * <br>
     * Suggested to use {@link StringResource} however any other resource is fine as well as long as it follows former specification. <br>
     * @param resources List of resources
     * @throws CompileException Error given during compilation
     */
    void compileClass(Resource... resources) throws CompileException, IOException;

    /**
     * Reloads the classloader with new class contents and calls registered {@linkplain IClassReloadListener listeners} <br>
     * <br>
     * See {@link IDynamicCompiler#compileClass(Resource...)}
     */
    void recompileClass(Resource... resources) throws CompileException, IOException;

    /**
     * Unloads a class from the loader and calls listeners.
     * @param names Fully qualified names of classes (example: <code>io.github.somehussar.MyVeryCoolClass</code>)
     */
    void removeClass(String... names);


    /**
     * Allow given objects listen for when a new classloader is allocated. <br>
     * Will only happen after first compilation. <br
     * <br>
     * An example of when it will be called: 
     * <ul>
     *     <li>A class has been removed from the class loader.</li>
     *     <li>A class has been recompiled.</li>
     * </ul>
     * @param listeners Listener(s) to add
     */
    void addReloadListener(IClassReloadListener... listeners);

    /**
     * Remove listener from list if you plan on letting the garbage collector get rid of it.<br>
     * <bold>Very important</bold> for avoiding memory leakage.
     * @param listeners Listener(s) to remove
     */
    void removeListener(IClassReloadListener... listeners);

    /**
     * It is best not to store this anywhere. Please rely on {@link IDynamicCompiler#addReloadListener(IClassReloadListener...)} <br>
     * <bold>Very important</bold> for avoiding memory leakage.
     * @return Current version of the class loader.
     */
    ClassLoader getClassLoader();


}
