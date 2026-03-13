package io.github.somehussar.janinoloader;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.codehaus.janino.SimpleCompiler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduces Janino bug: assigning null to a field of a parameterized type
 * like {@code Consumer<String>} fails with:
 * "Assignment conversion not possible from type (null type) to type
 *  java.util.function.Consumer<[java.lang.String]>"
 *
 * <p>Null should be assignable to any reference type, including parameterized types.</p>
 */
public class NullGenericAssignmentTest {

    // -----------------------------------------------------------------
    //  Infrastructure
    // -----------------------------------------------------------------

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

    private static void compile(Map<String, byte[]> classes, ClassLoader loader, StringResource... sources)
            throws CompileException, IOException {
        Compiler compiler = new Compiler();
        compiler.setIClassLoader(new ClassLoaderIClassLoader(loader));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.compile(sources);
    }

    // -----------------------------------------------------------------
    //  Test cases
    // -----------------------------------------------------------------

    /**
     * Core failing case: {@code public Consumer<String> configurator = null;}
     */
    @Test
    public void testNullAssignedToConsumerStringField() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

        compile(classes, loader,
                new StringResource(
                        "nullbug/NullConsumerField.java",
                        "package nullbug; " +
                        "import java.util.function.Consumer; " +
                        "public class NullConsumerField { " +
                        "    public Consumer<String> configurator = null; " +
                        "}"
                )
        );

        loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
        Class<?> clazz = loader.loadClass("nullbug.NullConsumerField");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Field field = clazz.getField("configurator");
        assertNull(field.get(instance));
    }

    /**
     * Variant: {@code List<String> list = null;}
     */
    @Test
    public void testNullAssignedToListStringField() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

        compile(classes, loader,
                new StringResource(
                        "nullbug/NullListField.java",
                        "package nullbug; " +
                        "import java.util.List; " +
                        "public class NullListField { " +
                        "    public List<String> items = null; " +
                        "}"
                )
        );

        loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
        Class<?> clazz = loader.loadClass("nullbug.NullListField");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Field field = clazz.getField("items");
        assertNull(field.get(instance));
    }

    /**
     * Variant: local variable {@code Consumer<String> c = null;}
     */
    @Test
    public void testNullAssignedToConsumerStringLocalVar() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

        compile(classes, loader,
                new StringResource(
                        "nullbug/NullConsumerLocal.java",
                        "package nullbug; " +
                        "import java.util.function.Consumer; " +
                        "public class NullConsumerLocal { " +
                        "    public static boolean test() { " +
                        "        Consumer<String> c = null; " +
                        "        return c == null; " +
                        "    } " +
                        "}"
                )
        );

        loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
        Class<?> clazz = loader.loadClass("nullbug.NullConsumerLocal");
        Object result = clazz.getDeclaredMethod("test").invoke(null);
        assertEquals(true, result);
    }

    /**
     * Variant: method parameter assignment {@code Consumer<String> c; c = null;}
     */
    @Test
    public void testNullReassignedToConsumerStringVar() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

        compile(classes, loader,
                new StringResource(
                        "nullbug/NullConsumerReassign.java",
                        "package nullbug; " +
                        "import java.util.function.Consumer; " +
                        "public class NullConsumerReassign { " +
                        "    public static boolean test() { " +
                        "        Consumer<String> c = new Consumer<String>() { " +
                        "            public void accept(Object s) { } " +
                        "        }; " +
                        "        c = null; " +
                        "        return c == null; " +
                        "    } " +
                        "}"
                )
        );

        loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
        Class<?> clazz = loader.loadClass("nullbug.NullConsumerReassign");
        Object result = clazz.getDeclaredMethod("test").invoke(null);
        assertEquals(true, result);
    }

    /**
     * Control: non-generic field with null works fine.
     */
    @Test
    public void testNullAssignedToNonGenericField_control() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

        compile(classes, loader,
                new StringResource(
                        "nullbug/NullStringField.java",
                        "package nullbug; " +
                        "public class NullStringField { " +
                        "    public String name = null; " +
                        "}"
                )
        );

        loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
        Class<?> clazz = loader.loadClass("nullbug.NullStringField");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Field field = clazz.getField("name");
        assertNull(field.get(instance));
    }

    /**
     * Variant using SimpleCompiler for simpler reproduction.
     */
    @Test
    public void testNullAssignedToConsumerStringField_simpleCompiler() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook(
            "import java.util.function.Consumer;\n" +
            "public class NullConsumer {\n" +
            "    public Consumer<String> handler = null;\n" +
            "}\n"
        );

        Class<?> clazz = sc.getClassLoader().loadClass("NullConsumer");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Field field = clazz.getField("handler");
        assertNull(field.get(instance));
    }

    /**
     * Map&lt;String, Integer&gt; field = null
     */
    @Test
    public void testNullAssignedToMapField() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook(
            "import java.util.Map;\n" +
            "public class NullMap {\n" +
            "    public Map<String, Integer> data = null;\n" +
            "}\n"
        );

        Class<?> clazz = sc.getClassLoader().loadClass("NullMap");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Field field = clazz.getField("data");
        assertNull(field.get(instance));
    }
}
