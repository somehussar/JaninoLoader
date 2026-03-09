package io.github.somehussar.janinoloader;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test harness exercising Janino's direct compilation API ({@link Compiler},
 * {@link ClassBodyEvaluator}) without JaninoLoader's wrapper abstractions.
 */
public class JaninoDirectTest {

    /**
     * Creates an in-memory classloader that can define classes from compiled bytecode.
     * This mirrors the approach used by JaninoLoader's MemoryClassLoader, but is a
     * standalone implementation for testing Janino directly.
     */
    private static ClassLoader createMemoryClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
        return new ClassLoader(parent) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name.replace('.', '/') + ".class");
                if (bytes != null) {
                    return defineClass(name, bytes, 0, bytes.length);
                }
                return super.findClass(name);
            }
        };
    }

    /**
     * Compiles one or more source resources using Janino's Compiler, storing the
     * resulting bytecode into the given map, and using the given classloader to
     * resolve already-compiled classes during compilation.
     */
    private static void compile(Map<String, byte[]> classes, ClassLoader loader, StringResource... sources)
            throws CompileException, IOException {
        Compiler compiler = new Compiler();
        compiler.setIClassLoader(new ClassLoaderIClassLoader(loader));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.compile(sources);
    }

    /**
     * Compiles a single class with a static method from a Java source string,
     * loads it via an in-memory classloader, and invokes the method via reflection.
     *
     * <p>Demonstrates the minimal Janino compilation pipeline:
     * source string → Compiler → bytecode map → classloader → reflection call.</p>
     */
    @Test
    public void basicStringCompilationAndInvocation() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "calc/Calculator.java",
                            "package calc; public class Calculator { " +
                            "    public static int add(int a, int b) { return a + b; } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> calcClass = loader.loadClass("calc.Calculator");
            Method addMethod = calcClass.getDeclaredMethod("add", int.class, int.class);
            Object result = addMethod.invoke(null, 7, 35);

            assertEquals(42, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Compiles two classes in a single compilation unit where one class references
     * the other. This tests Janino's ability to resolve cross-class dependencies
     * when all sources are provided simultaneously.
     *
     * <p>Class {@code Helper} provides a utility method; class {@code Facade} delegates to it.
     * Both are compiled together so Janino resolves the reference at compile time.</p>
     */
    @Test
    public void crossClassCompilation() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "cross/Helper.java",
                            "package cross; public class Helper { " +
                            "    public static int triple(int x) { return x * 3; } " +
                            "}"
                    ),
                    new StringResource(
                            "cross/Facade.java",
                            "package cross; public class Facade { " +
                            "    public static int compute(int x) { return Helper.triple(x) + 1; } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> facadeClass = loader.loadClass("cross.Facade");
            Object result = facadeClass.getDeclaredMethod("compute", int.class).invoke(null, 10);

            assertEquals(31, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Uses Janino's {@link ClassBodyEvaluator} to compile a class body (methods and fields)
     * that extends a given base type. This is the mechanism underlying JaninoLoader's
     * SafeScriptClassBody — it generates a class implementing/extending a specified type
     * from just the method bodies.
     *
     * <p>The evaluator creates a class extending {@link Runnable} with a custom {@code run()}
     * method, then invokes it.</p>
     */
    @Test
    public void classBodyEvaluatorScript() {
        try {
            ClassBodyEvaluator evaluator = new ClassBodyEvaluator();
            evaluator.setImplementedInterfaces(new Class<?>[]{ Runnable.class });
            evaluator.cook(
                    "public void run() { " +
                    "    System.out.println(\"ClassBodyEvaluator script executed\"); " +
                    "}"
            );

            Class<?> generatedClass = evaluator.getClazz();
            assertNotNull(generatedClass);

            Object instance = generatedClass.getDeclaredConstructor().newInstance();
            assertTrue(instance instanceof Runnable, "Generated class must implement Runnable");

            ((Runnable) instance).run();
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Compiles a class with mutable instance fields and verifies that state is properly
     * preserved across multiple method calls on the same instance. Also exercises
     * reflection-based field access and non-static method invocation.
     *
     * <p>The compiled {@code Counter} class has an integer field that increments on each
     * call to {@code next()}, demonstrating per-instance mutable state.</p>
     */
    @Test
    public void instanceStatePreservation() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "state/Counter.java",
                            "package state; public class Counter { " +
                            "    private int value = 0; " +
                            "    public int next() { return value++; } " +
                            "    public int getValue() { return value; } " +
                            "    public void reset() { value = 0; } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> counterClass = loader.loadClass("state.Counter");
            Object counter = counterClass.getDeclaredConstructor().newInstance();

            Method nextMethod = counterClass.getDeclaredMethod("next");
            Method getValueMethod = counterClass.getDeclaredMethod("getValue");
            Method resetMethod = counterClass.getDeclaredMethod("reset");

            assertEquals(0, nextMethod.invoke(counter));
            assertEquals(1, nextMethod.invoke(counter));
            assertEquals(2, nextMethod.invoke(counter));
            assertEquals(3, getValueMethod.invoke(counter));

            resetMethod.invoke(counter);
            assertEquals(0, getValueMethod.invoke(counter));

            Object counter2 = counterClass.getDeclaredConstructor().newInstance();
            assertEquals(0, nextMethod.invoke(counter2));
            assertEquals(1, nextMethod.invoke(counter2));
            assertEquals(0, nextMethod.invoke(counter));
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Verifies that Janino correctly reports compile-time errors for invalid Java source.
     * A {@link CompileException} should be thrown when the source contains a syntax error
     * or references a non-existent type.
     *
     * <p>This validates that the compiler doesn't silently produce broken bytecode.</p>
     */
    @Test
    public void compileErrorDetection() {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

        CompileException thrown = assertThrows(CompileException.class, () ->
                compile(classes, loader,
                        new StringResource(
                                "broken/Bad.java",
                                "package broken; public class Bad { " +
                                "    public static void oops() { NonExistentType x = null; } " +
                                "}"
                        )
                )
        );

        assertNotNull(thrown.getMessage());
        assertTrue(
                thrown.getMessage().contains("NonExistentType"),
                "Error message should reference the unresolved type, got: " + thrown.getMessage()
        );
    }

    /**
     * Ensures that classes compiled by Janino into an in-memory classloader are
     * not visible from the parent (application) classloader. This is the foundation
     * of JaninoLoader's isolation model — dynamically compiled classes must not
     * leak into the application's namespace.
     */
    @Test
    public void classloaderIsolation() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader parentLoader = getClass().getClassLoader();
            ClassLoader memLoader = createMemoryClassLoader(parentLoader, classes);

            compile(classes, memLoader,
                    new StringResource(
                            "isolated/Secret.java",
                            "package isolated; public class Secret { " +
                            "    public static String reveal() { return \"hidden\"; } " +
                            "}"
                    )
            );

            memLoader = createMemoryClassLoader(parentLoader, classes);

            Class<?> secretClass = memLoader.loadClass("isolated.Secret");
            Object result = secretClass.getDeclaredMethod("reveal").invoke(null);
            assertEquals("hidden", result);

            assertThrows(ClassNotFoundException.class, () ->
                    parentLoader.loadClass("isolated.Secret")
            );
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Demonstrates recompilation by compiling a class, verifying its output, then
     * replacing it with new source code and verifying the updated behavior. This
     * mirrors JaninoLoader's hot-reload capability at the raw Janino level.
     *
     * <p>The key insight: since the old classloader still holds the old class definition,
     * a <em>new</em> classloader must be created over the updated bytecode map to pick
     * up the recompiled version.</p>
     */
    @Test
    public void recompilationWithBytecodeReplacement() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader parentLoader = getClass().getClassLoader();

            ClassLoader loader = createMemoryClassLoader(parentLoader, classes);
            compile(classes, loader,
                    new StringResource(
                            "hot/Greeter.java",
                            "package hot; public class Greeter { " +
                            "    public static String greet() { return \"v1\"; } " +
                            "}"
                    )
            );
            loader = createMemoryClassLoader(parentLoader, classes);
            assertEquals("v1", loader.loadClass("hot.Greeter").getDeclaredMethod("greet").invoke(null));

            classes.remove("hot/Greeter.class");
            loader = createMemoryClassLoader(parentLoader, classes);
            compile(classes, loader,
                    new StringResource(
                            "hot/Greeter.java",
                            "package hot; public class Greeter { " +
                            "    public static String greet() { return \"v2\"; } " +
                            "}"
                    )
            );
            loader = createMemoryClassLoader(parentLoader, classes);
            assertEquals("v2", loader.loadClass("hot.Greeter").getDeclaredMethod("greet").invoke(null));
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Uses ClassBodyEvaluator to compile a class body that performs a computation and
     * returns a typed result. Exercises the evaluator with a custom interface rather
     * than a well-known JDK type, demonstrating the script-body pattern where user
     * code implements a contract defined by the host application.
     */
    @Test
    public void classBodyEvaluatorWithTypedResult() {
        try {
            ClassBodyEvaluator evaluator = new ClassBodyEvaluator();
            evaluator.setParentClassLoader(getClass().getClassLoader());
            evaluator.setImplementedInterfaces(new Class<?>[]{ Computation.class });
            evaluator.cook(
                    "public int compute(int x) { " +
                    "    int sum = 0; " +
                    "    for (int i = 1; i <= x; i++) { sum += i; } " +
                    "    return sum; " +
                    "}"
            );

            Class<?> clazz = evaluator.getClazz();
            Computation instance = (Computation) clazz.getDeclaredConstructor().newInstance();

            assertEquals(55, instance.compute(10));
            assertEquals(0, instance.compute(0));
            assertEquals(1, instance.compute(1));
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Public interface for ClassBodyEvaluator tests. Must be public so that the
     * dynamically generated class (loaded in a child classloader) can implement it.
     */
    public interface Computation {
        int compute(int x);
    }
}
