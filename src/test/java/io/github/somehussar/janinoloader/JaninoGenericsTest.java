package io.github.somehussar.janinoloader;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite exercising Janino's generic type handling capabilities.
 *
 * <p>Janino parses all generic syntax into AST nodes but compiles using type erasure
 * to the first bound (or Object if unbounded). The {@code IClass.parameterize()} method
 * is commented out in Janino's source, meaning parameterized type checking is disabled.
 * This test suite documents exactly what works, what fails, and how.</p>
 *
 * <p>Each test captures detailed error information for documentation in
 * {@code JANINO_GENERICS_TEST_RESULTS.md}.</p>
 *
 * @see <a href="JANINO_GENERICS.md">Janino Generics Deep Dive</a>
 */
public class JaninoGenericsTest {

    // -----------------------------------------------------------------
    //  Infrastructure (mirrors JaninoDirectTest)
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

    // =================================================================
    //  Category 1: Basic Generic Collections
    // =================================================================

    /**
     * Test 1: List&lt;String&gt; with explicit cast on retrieval.
     *
     * <p>Since Janino erases type arguments, {@code list.get(0)} returns Object.
     * An explicit cast to String is required.</p>
     */
    @Test
    public void testListStringWithExplicitCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ListStringCast.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class ListStringCast { " +
                            "    public static String test() { " +
                            "        List<String> list = new ArrayList<String>(); " +
                            "        list.add(\"hello\"); " +
                            "        return (String) list.get(0); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ListStringCast");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 2: List&lt;String&gt; without explicit cast -- tests whether Janino
     * inserts bridge casts like javac does.
     *
     * <p>Expected behavior based on JANINO_GENERICS.md: This may fail because
     * Janino does not perform parameterized type checking and may not insert
     * the implicit cast that javac would generate.</p>
     */
    @Test
    public void testListStringWithoutCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ListStringNoCast.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class ListStringNoCast { " +
                            "    public static String test() { " +
                            "        List<String> list = new ArrayList<String>(); " +
                            "        list.add(\"world\"); " +
                            "        String s = list.get(0); " +
                            "        return s; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ListStringNoCast");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("world", result);
        } catch (CompileException ce) {
            // Document: Janino cannot assign Object to String without cast
            System.err.println("[testListStringWithoutCast] COMPILE ERROR: " + ce.getMessage());
            // This is expected -- Janino erases generics and list.get(0) returns Object
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testListStringWithoutCast] UNEXPECTED ERROR: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 3: ArrayList&lt;Integer&gt; with autoboxing and explicit cast.
     */
    @Test
    public void testArrayListIntegerWithCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ArrayListInt.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "public class ArrayListInt { " +
                            "    public static int test() { " +
                            "        ArrayList<Integer> list = new ArrayList<Integer>(); " +
                            "        list.add(42); " +
                            "        return (Integer) list.get(0); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ArrayListInt");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 4: HashMap&lt;String, Integer&gt; with explicit casts.
     */
    @Test
    public void testHashMapStringIntegerWithCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/HashMapTest.java",
                            "package gen; " +
                            "import java.util.HashMap; " +
                            "import java.util.Map; " +
                            "public class HashMapTest { " +
                            "    public static int test() { " +
                            "        Map<String, Integer> map = new HashMap<String, Integer>(); " +
                            "        map.put(\"answer\", 42); " +
                            "        return (Integer) map.get(\"answer\"); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.HashMapTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 5: HashMap without cast -- testing if Janino can assign map.get() to Integer directly.
     */
    @Test
    public void testHashMapWithoutCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/HashMapNoCast.java",
                            "package gen; " +
                            "import java.util.HashMap; " +
                            "import java.util.Map; " +
                            "public class HashMapNoCast { " +
                            "    public static int test() { " +
                            "        Map<String, Integer> map = new HashMap<String, Integer>(); " +
                            "        map.put(\"key\", 99); " +
                            "        Integer val = map.get(\"key\"); " +
                            "        return val; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.HashMapNoCast");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(99, result);
        } catch (CompileException ce) {
            System.err.println("[testHashMapWithoutCast] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testHashMapWithoutCast] UNEXPECTED ERROR: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 6: Raw List usage (no generics) -- should always work.
     */
    @Test
    public void testRawListUsage() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/RawList.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class RawList { " +
                            "    public static String test() { " +
                            "        List list = new ArrayList(); " +
                            "        list.add(\"raw\"); " +
                            "        return (String) list.get(0); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.RawList");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("raw", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    // =================================================================
    //  Category 2: Generic Class Declarations
    // =================================================================

    /**
     * Test 7: Simple generic class Box&lt;T&gt; with get/set.
     * Uses explicit cast on retrieval.
     */
    @Test
    public void testGenericClassDeclaration() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Box.java",
                            "package gen; " +
                            "public class Box<T> { " +
                            "    private T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "    public void set(T value) { this.value = value; } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/BoxUser.java",
                            "package gen; " +
                            "public class BoxUser { " +
                            "    public static String test() { " +
                            "        Box<String> box = new Box<String>(\"boxed\"); " +
                            "        return (String) box.get(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.BoxUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("boxed", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 8: Generic class Box&lt;T&gt; -- trying to use get() without cast.
     */
    @Test
    public void testGenericClassWithoutCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen2/Box.java",
                            "package gen2; " +
                            "public class Box<T> { " +
                            "    private T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "}"
                    ),
                    new StringResource(
                            "gen2/BoxUser.java",
                            "package gen2; " +
                            "public class BoxUser { " +
                            "    public static String test() { " +
                            "        Box<String> box = new Box<String>(\"hello\"); " +
                            "        String s = box.get(); " +
                            "        return s; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen2.BoxUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
        } catch (CompileException ce) {
            System.err.println("[testGenericClassWithoutCast] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testGenericClassWithoutCast] UNEXPECTED ERROR: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 9: Generic class with two type parameters -- Pair&lt;A, B&gt;.
     */
    @Test
    public void testGenericClassTwoTypeParams() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Pair.java",
                            "package gen; " +
                            "public class Pair<A, B> { " +
                            "    private A first; " +
                            "    private B second; " +
                            "    public Pair(A first, B second) { this.first = first; this.second = second; } " +
                            "    public A getFirst() { return first; } " +
                            "    public B getSecond() { return second; } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/PairUser.java",
                            "package gen; " +
                            "public class PairUser { " +
                            "    public static String test() { " +
                            "        Pair<String, Integer> pair = new Pair<String, Integer>(\"age\", Integer.valueOf(30)); " +
                            "        return (String) pair.getFirst() + \"=\" + ((Integer) pair.getSecond()).toString(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.PairUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("age=30", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    // =================================================================
    //  Category 3: Generic Method Declarations
    // =================================================================

    /**
     * Test 10: Generic method &lt;T&gt; T identity(T t) with explicit cast.
     */
    @Test
    public void testGenericMethodWithCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/GenericMethod.java",
                            "package gen; " +
                            "public class GenericMethod { " +
                            "    public static <T> T identity(T t) { return t; } " +
                            "    public static String test() { " +
                            "        return (String) identity(\"generic\"); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.GenericMethod");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("generic", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 11: Generic method without cast -- String s = identity("value").
     */
    @Test
    public void testGenericMethodWithoutCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/GenericMethodNoCast.java",
                            "package gen; " +
                            "public class GenericMethodNoCast { " +
                            "    public static <T> T identity(T t) { return t; } " +
                            "    public static String test() { " +
                            "        String s = identity(\"inferred\"); " +
                            "        return s; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.GenericMethodNoCast");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("inferred", result);
        } catch (CompileException ce) {
            System.err.println("[testGenericMethodWithoutCast] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testGenericMethodWithoutCast] UNEXPECTED ERROR: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 12: Generic method with multiple type parameters &lt;K, V&gt;.
     */
    @Test
    public void testGenericMethodMultipleTypeParams() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/MultiGenMethod.java",
                            "package gen; " +
                            "public class MultiGenMethod { " +
                            "    public static <K, V> String combine(K key, V value) { " +
                            "        return key.toString() + \"=\" + value.toString(); " +
                            "    } " +
                            "    public static String test() { " +
                            "        return combine(\"pi\", Double.valueOf(3.14)); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.MultiGenMethod");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("pi=3.14", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    // =================================================================
    //  Category 4: Bounded Type Parameters
    // =================================================================

    /**
     * Test 13: Bounded type parameter &lt;T extends Comparable&lt;T&gt;&gt;.
     * Uses explicit cast. The bound should erase to Comparable.
     */
    @Test
    public void testBoundedTypeParameter() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/BoundedMax.java",
                            "package gen; " +
                            "public class BoundedMax { " +
                            "    public static <T extends Comparable> T max(T a, T b) { " +
                            "        return a.compareTo(b) >= 0 ? a : b; " +
                            "    } " +
                            "    public static String test() { " +
                            "        return (String) max(\"apple\", \"banana\"); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.BoundedMax");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("banana", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 14: Bounded type parameter with parameterized bound: &lt;T extends Comparable&lt;T&gt;&gt;.
     * Tests if Janino handles the recursive type bound.
     */
    @Test
    public void testBoundedTypeParameterParameterizedBound() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/BoundedParam.java",
                            "package gen; " +
                            "public class BoundedParam { " +
                            "    public static <T extends Comparable<T>> T max(T a, T b) { " +
                            "        return a.compareTo(b) >= 0 ? a : b; " +
                            "    } " +
                            "    public static String test() { " +
                            "        return (String) max(\"x\", \"y\"); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.BoundedParam");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("y", result);
        } catch (CompileException ce) {
            System.err.println("[testBoundedTypeParameterParameterizedBound] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testBoundedTypeParameterParameterizedBound] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 15: Multiple bounds &lt;T extends Number &amp; Comparable&gt;.
     * Janino should resolve T to the first bound (Number).
     */
    @Test
    public void testMultipleBounds() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/MultiBound.java",
                            "package gen; " +
                            "public class MultiBound { " +
                            "    public static <T extends Number & Comparable> int test() { " +
                            "        T a = (T) Integer.valueOf(5); " +
                            "        return a.intValue(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.MultiBound");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(5, result);
        } catch (CompileException ce) {
            System.err.println("[testMultipleBounds] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testMultipleBounds] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    // =================================================================
    //  Category 5: Wildcards
    // =================================================================

    /**
     * Test 16: Unbounded wildcard -- List&lt;?&gt; parameter.
     */
    @Test
    public void testUnboundedWildcard() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/WildcardUnbounded.java",
                            "package gen; " +
                            "import java.util.List; " +
                            "import java.util.ArrayList; " +
                            "public class WildcardUnbounded { " +
                            "    public static int size(List<?> list) { " +
                            "        return list.size(); " +
                            "    } " +
                            "    public static int test() { " +
                            "        List<String> list = new ArrayList<String>(); " +
                            "        list.add(\"a\"); " +
                            "        list.add(\"b\"); " +
                            "        return size(list); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.WildcardUnbounded");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(2, result);
        } catch (CompileException ce) {
            System.err.println("[testUnboundedWildcard] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testUnboundedWildcard] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 17: Upper bounded wildcard -- List&lt;? extends Number&gt;.
     */
    @Test
    public void testUpperBoundedWildcard() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/WildcardUpper.java",
                            "package gen; " +
                            "import java.util.List; " +
                            "import java.util.ArrayList; " +
                            "public class WildcardUpper { " +
                            "    public static double sum(List<? extends Number> numbers) { " +
                            "        double total = 0; " +
                            "        for (int i = 0; i < numbers.size(); i++) { " +
                            "            total += ((Number) numbers.get(i)).doubleValue(); " +
                            "        } " +
                            "        return total; " +
                            "    } " +
                            "    public static double test() { " +
                            "        List<Integer> list = new ArrayList<Integer>(); " +
                            "        list.add(Integer.valueOf(10)); " +
                            "        list.add(Integer.valueOf(20)); " +
                            "        return sum(list); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.WildcardUpper");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(30.0, result);
        } catch (CompileException ce) {
            System.err.println("[testUpperBoundedWildcard] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testUpperBoundedWildcard] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 18: Lower bounded wildcard -- List&lt;? super Integer&gt;.
     */
    @Test
    public void testLowerBoundedWildcard() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/WildcardLower.java",
                            "package gen; " +
                            "import java.util.List; " +
                            "import java.util.ArrayList; " +
                            "public class WildcardLower { " +
                            "    public static void addNumbers(List<? super Integer> list) { " +
                            "        list.add(Integer.valueOf(1)); " +
                            "        list.add(Integer.valueOf(2)); " +
                            "    } " +
                            "    public static int test() { " +
                            "        List<Number> list = new ArrayList<Number>(); " +
                            "        addNumbers(list); " +
                            "        return list.size(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.WildcardLower");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(2, result);
        } catch (CompileException ce) {
            System.err.println("[testLowerBoundedWildcard] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testLowerBoundedWildcard] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    // =================================================================
    //  Category 6: Diamond Operator & Type Inference
    // =================================================================

    /**
     * Test 19: Diamond operator -- new ArrayList&lt;&gt;().
     */
    @Test
    public void testDiamondOperator() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Diamond.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class Diamond { " +
                            "    public static String test() { " +
                            "        List<String> list = new ArrayList<>(); " +
                            "        list.add(\"diamond\"); " +
                            "        return (String) list.get(0); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.Diamond");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("diamond", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 20: Diamond operator with HashMap.
     */
    @Test
    public void testDiamondOperatorHashMap() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/DiamondMap.java",
                            "package gen; " +
                            "import java.util.HashMap; " +
                            "import java.util.Map; " +
                            "public class DiamondMap { " +
                            "    public static int test() { " +
                            "        Map<String, Integer> map = new HashMap<>(); " +
                            "        map.put(\"val\", Integer.valueOf(77)); " +
                            "        return ((Integer) map.get(\"val\")).intValue(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.DiamondMap");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(77, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    // =================================================================
    //  Category 7: Nested Generics
    // =================================================================

    /**
     * Test 21: Nested generics -- List&lt;Map&lt;String, Integer&gt;&gt;.
     */
    @Test
    public void testNestedGenerics() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/NestedGen.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.HashMap; " +
                            "import java.util.List; " +
                            "import java.util.Map; " +
                            "public class NestedGen { " +
                            "    public static int test() { " +
                            "        List<Map<String, Integer>> list = new ArrayList<Map<String, Integer>>(); " +
                            "        Map<String, Integer> map = new HashMap<String, Integer>(); " +
                            "        map.put(\"x\", Integer.valueOf(10)); " +
                            "        list.add(map); " +
                            "        Map<String, Integer> retrieved = (Map<String, Integer>) list.get(0); " +
                            "        return ((Integer) retrieved.get(\"x\")).intValue(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.NestedGen");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(10, result);
        } catch (CompileException ce) {
            System.err.println("[testNestedGenerics] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testNestedGenerics] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 22: Map&lt;String, List&lt;String&gt;&gt; -- nested generics with interaction.
     */
    @Test
    public void testMapOfLists() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/MapOfLists.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.HashMap; " +
                            "import java.util.List; " +
                            "import java.util.Map; " +
                            "public class MapOfLists { " +
                            "    public static String test() { " +
                            "        Map<String, List<String>> map = new HashMap<String, List<String>>(); " +
                            "        List<String> fruits = new ArrayList<String>(); " +
                            "        fruits.add(\"apple\"); " +
                            "        fruits.add(\"banana\"); " +
                            "        map.put(\"fruits\", fruits); " +
                            "        List<String> retrieved = (List<String>) map.get(\"fruits\"); " +
                            "        return (String) retrieved.get(1); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.MapOfLists");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("banana", result);
        } catch (CompileException ce) {
            System.err.println("[testMapOfLists] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testMapOfLists] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    // =================================================================
    //  Category 8: Type Erasure Implications
    // =================================================================

    /**
     * Test 23: Erased return type -- generic method returning T erases to Object.
     * Verifies the method signature via reflection.
     */
    @Test
    public void testErasedReturnTypeReflection() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ErasedReturn.java",
                            "package gen; " +
                            "public class ErasedReturn { " +
                            "    public <T> T echo(T value) { return value; } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ErasedReturn");
            Method echoMethod = clazz.getDeclaredMethod("echo", Object.class);
            // After type erasure, T becomes Object
            assertEquals(Object.class, echoMethod.getReturnType());

            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object result = echoMethod.invoke(instance, "test");
            assertEquals("test", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 24: Type erasure -- bounded type parameter erases to first bound.
     * &lt;T extends Number&gt; should erase T to Number in the bytecode.
     */
    @Test
    public void testBoundedErasureToFirstBound() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/BoundedErasure.java",
                            "package gen; " +
                            "public class BoundedErasure { " +
                            "    public static <T extends Number> double toDouble(T num) { " +
                            "        return num.doubleValue(); " +
                            "    } " +
                            "    public static double test() { " +
                            "        return toDouble(Integer.valueOf(42)); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.BoundedErasure");

            // Verify erasure: parameter type should be Number, not Object
            Method toDoubleMethod = clazz.getDeclaredMethod("toDouble", Number.class);
            assertNotNull(toDoubleMethod, "Method should accept Number (erased from T extends Number)");

            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(42.0, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 25: Verify that List&lt;String&gt; and List&lt;Integer&gt; produce the same erased class.
     * Both should be raw List at runtime.
     */
    @Test
    public void testErasureListStringVsListInteger() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ErasureEquality.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class ErasureEquality { " +
                            "    public static boolean test() { " +
                            "        List<String> strings = new ArrayList<String>(); " +
                            "        List<Integer> ints = new ArrayList<Integer>(); " +
                            "        return strings.getClass() == ints.getClass(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ErasureEquality");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(true, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    // =================================================================
    //  Category 9: Advanced Generic Scenarios
    // =================================================================

    /**
     * Test 26: Generic interface implementation.
     * A class implementing Comparable&lt;MyClass&gt;.
     */
    @Test
    public void testGenericInterfaceImplementation() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Priority.java",
                            "package gen; " +
                            "public class Priority implements Comparable<Priority> { " +
                            "    private int level; " +
                            "    public Priority(int level) { this.level = level; } " +
                            "    public int getLevel() { return level; } " +
                            "    public int compareTo(Object other) { " +
                            "        return this.level - ((Priority) other).level; " +
                            "    } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/PriorityTest.java",
                            "package gen; " +
                            "public class PriorityTest { " +
                            "    public static boolean test() { " +
                            "        Priority high = new Priority(10); " +
                            "        Priority low = new Priority(1); " +
                            "        return high.compareTo(low) > 0; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.PriorityTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(true, result);
        } catch (CompileException ce) {
            System.err.println("[testGenericInterfaceImplementation] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testGenericInterfaceImplementation] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 27: Generic class extending a generic class -- SubBox&lt;T&gt; extends Box&lt;T&gt;.
     */
    @Test
    public void testGenericInheritance() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen3/Box.java",
                            "package gen3; " +
                            "public class Box<T> { " +
                            "    protected T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "}"
                    ),
                    new StringResource(
                            "gen3/LabeledBox.java",
                            "package gen3; " +
                            "public class LabeledBox<T> extends Box<T> { " +
                            "    private String label; " +
                            "    public LabeledBox(String label, T value) { super(value); this.label = label; } " +
                            "    public String getLabel() { return label; } " +
                            "}"
                    ),
                    new StringResource(
                            "gen3/BoxTest.java",
                            "package gen3; " +
                            "public class BoxTest { " +
                            "    public static String test() { " +
                            "        LabeledBox<String> box = new LabeledBox<String>(\"name\", \"Janino\"); " +
                            "        return box.getLabel() + \":\" + (String) box.get(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen3.BoxTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("name:Janino", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 28: Using for-each loop with generic collection.
     * Tests iterator protocol with generics.
     */
    @Test
    public void testForEachWithGenericCollection() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ForEachGen.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class ForEachGen { " +
                            "    public static int test() { " +
                            "        List<Integer> numbers = new ArrayList<Integer>(); " +
                            "        numbers.add(Integer.valueOf(1)); " +
                            "        numbers.add(Integer.valueOf(2)); " +
                            "        numbers.add(Integer.valueOf(3)); " +
                            "        int sum = 0; " +
                            "        for (Object n : numbers) { " +
                            "            sum += ((Integer) n).intValue(); " +
                            "        } " +
                            "        return sum; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ForEachGen");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(6, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 29: For-each with typed loop variable (no cast) -- for (String s : list).
     * Tests whether Janino handles the implicit cast from Iterator.next() to String.
     */
    @Test
    public void testForEachTypedLoopVariable() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ForEachTyped.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class ForEachTyped { " +
                            "    public static String test() { " +
                            "        List<String> words = new ArrayList<String>(); " +
                            "        words.add(\"hello\"); " +
                            "        words.add(\" \"); " +
                            "        words.add(\"world\"); " +
                            "        String result = \"\"; " +
                            "        for (String w : words) { " +
                            "            result += w; " +
                            "        } " +
                            "        return result; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ForEachTyped");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("hello world", result);
        } catch (CompileException ce) {
            System.err.println("[testForEachTypedLoopVariable] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testForEachTypedLoopVariable] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 30: Cross-class generic compilation -- one generic class used by another.
     * Compiled in a single compilation unit.
     */
    @Test
    public void testCrossClassGenericCompilation() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Container.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class Container<T> { " +
                            "    private List<T> items = new ArrayList<T>(); " +
                            "    public void add(T item) { items.add(item); } " +
                            "    public T getFirst() { return items.size() > 0 ? (T) items.get(0) : null; } " +
                            "    public int count() { return items.size(); } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/ContainerUser.java",
                            "package gen; " +
                            "public class ContainerUser { " +
                            "    public static String test() { " +
                            "        Container<String> c = new Container<String>(); " +
                            "        c.add(\"first\"); " +
                            "        c.add(\"second\"); " +
                            "        return (String) c.getFirst() + \":\" + c.count(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ContainerUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("first:2", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 31: Generic method in generic class -- combined complexity.
     */
    @Test
    public void testGenericMethodInGenericClass() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Transformer.java",
                            "package gen; " +
                            "public class Transformer<T> { " +
                            "    private T value; " +
                            "    public Transformer(T value) { this.value = value; } " +
                            "    public <R> R transform(Object mapper) { " +
                            "        return (R) mapper; " +
                            "    } " +
                            "    public T getValue() { return value; } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/TransformerTest.java",
                            "package gen; " +
                            "public class TransformerTest { " +
                            "    public static String test() { " +
                            "        Transformer<Integer> t = new Transformer<Integer>(Integer.valueOf(42)); " +
                            "        return ((Integer) t.getValue()).toString(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.TransformerTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("42", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 32: Collections.sort with Comparable -- tests generic interaction with JDK.
     */
    @Test
    public void testCollectionsSortWithGenerics() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/SortTest.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.Collections; " +
                            "import java.util.List; " +
                            "public class SortTest { " +
                            "    public static String test() { " +
                            "        List<String> list = new ArrayList<String>(); " +
                            "        list.add(\"cherry\"); " +
                            "        list.add(\"apple\"); " +
                            "        list.add(\"banana\"); " +
                            "        Collections.sort(list); " +
                            "        return (String) list.get(0); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.SortTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("apple", result);
        } catch (CompileException ce) {
            System.err.println("[testCollectionsSortWithGenerics] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testCollectionsSortWithGenerics] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 33: Iterator pattern with generics -- explicit Iterator&lt;String&gt; usage.
     */
    @Test
    public void testIteratorWithGenerics() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/IteratorTest.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.Iterator; " +
                            "import java.util.List; " +
                            "public class IteratorTest { " +
                            "    public static String test() { " +
                            "        List<String> list = new ArrayList<String>(); " +
                            "        list.add(\"a\"); " +
                            "        list.add(\"b\"); " +
                            "        Iterator it = list.iterator(); " +
                            "        String result = \"\"; " +
                            "        while (it.hasNext()) { " +
                            "            result += (String) it.next(); " +
                            "        } " +
                            "        return result; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.IteratorTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("ab", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 34: Iterator&lt;String&gt; with typed next() -- no explicit cast.
     */
    @Test
    public void testTypedIterator() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/TypedIterator.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.Iterator; " +
                            "import java.util.List; " +
                            "public class TypedIterator { " +
                            "    public static String test() { " +
                            "        List<String> list = new ArrayList<String>(); " +
                            "        list.add(\"x\"); " +
                            "        list.add(\"y\"); " +
                            "        Iterator<String> it = list.iterator(); " +
                            "        String result = \"\"; " +
                            "        while (it.hasNext()) { " +
                            "            String s = it.next(); " +
                            "            result += s; " +
                            "        } " +
                            "        return result; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.TypedIterator");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("xy", result);
        } catch (CompileException ce) {
            System.err.println("[testTypedIterator] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testTypedIterator] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 35: Static generic factory method -- static &lt;T&gt; Box&lt;T&gt; of(T value).
     */
    @Test
    public void testStaticGenericFactoryMethod() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen4/Box.java",
                            "package gen4; " +
                            "public class Box<T> { " +
                            "    private T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "    public static <T> Box<T> of(T value) { return new Box<T>(value); } " +
                            "}"
                    ),
                    new StringResource(
                            "gen4/FactoryTest.java",
                            "package gen4; " +
                            "public class FactoryTest { " +
                            "    public static String test() { " +
                            "        Box<String> box = Box.of(\"factory\"); " +
                            "        return (String) box.get(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen4.FactoryTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("factory", result);
        } catch (CompileException ce) {
            System.err.println("[testStaticGenericFactoryMethod] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testStaticGenericFactoryMethod] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 36: Generic array -- T[] parameter.
     */
    @Test
    public void testGenericArrayParameter() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ArrayGen.java",
                            "package gen; " +
                            "public class ArrayGen { " +
                            "    public static <T> T first(T[] array) { " +
                            "        return array.length > 0 ? array[0] : null; " +
                            "    } " +
                            "    public static String test() { " +
                            "        String[] arr = new String[] { \"first\", \"second\" }; " +
                            "        return (String) first(arr); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ArrayGen");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("first", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 37: Generic varargs -- &lt;T&gt; List&lt;T&gt; asList(T... items).
     */
    @Test
    public void testGenericVarargs() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/VarargsGen.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class VarargsGen { " +
                            "    public static <T> List asList(T[] items) { " +
                            "        List list = new ArrayList(); " +
                            "        for (int i = 0; i < items.length; i++) { " +
                            "            list.add(items[i]); " +
                            "        } " +
                            "        return list; " +
                            "    } " +
                            "    public static int test() { " +
                            "        List list = asList(new String[] { \"a\", \"b\", \"c\" }); " +
                            "        return list.size(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.VarargsGen");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(3, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 38: Generic class with bounded type in field -- class NumberBox&lt;T extends Number&gt;.
     * Tests that Janino erases T to Number in the field type.
     */
    @Test
    public void testGenericClassWithBoundedField() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/NumberBox.java",
                            "package gen; " +
                            "public class NumberBox<T extends Number> { " +
                            "    private T num; " +
                            "    public NumberBox(T num) { this.num = num; } " +
                            "    public double doubleValue() { return num.doubleValue(); } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/NumberBoxTest.java",
                            "package gen; " +
                            "public class NumberBoxTest { " +
                            "    public static double test() { " +
                            "        NumberBox<Integer> box = new NumberBox<Integer>(Integer.valueOf(7)); " +
                            "        return box.doubleValue(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.NumberBoxTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(7.0, result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 39: Enum with generic interface -- tests compilation of enum implementing
     * a generic interface.
     */
    @Test
    public void testEnumWithGenericInterface() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/Color.java",
                            "package gen; " +
                            "public enum Color implements Comparable { " +
                            "    RED, GREEN, BLUE; " +
                            "}"
                    ),
                    new StringResource(
                            "gen/ColorTest.java",
                            "package gen; " +
                            "public class ColorTest { " +
                            "    public static boolean test() { " +
                            "        return Color.RED.compareTo(Color.BLUE) < 0; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ColorTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(true, result);
        } catch (CompileException ce) {
            System.err.println("[testEnumWithGenericInterface] COMPILE ERROR: " + ce.getMessage());
            assertNotNull(ce.getMessage());
        } catch (Throwable t) {
            System.err.println("[testEnumWithGenericInterface] UNEXPECTED: " + t.getClass().getName() + ": " + t.getMessage());
            fail(t);
        }
    }

    /**
     * Test 40: Recursive generic type -- TreeNode&lt;T&gt; with children of same type.
     */
    @Test
    public void testRecursiveGenericType() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/TreeNode.java",
                            "package gen; " +
                            "import java.util.ArrayList; " +
                            "import java.util.List; " +
                            "public class TreeNode<T> { " +
                            "    private T data; " +
                            "    private List children; " +
                            "    public TreeNode(T data) { this.data = data; this.children = new ArrayList(); } " +
                            "    public T getData() { return data; } " +
                            "    public void addChild(TreeNode child) { children.add(child); } " +
                            "    public TreeNode getChild(int index) { return (TreeNode) children.get(index); } " +
                            "}"
                    ),
                    new StringResource(
                            "gen/TreeTest.java",
                            "package gen; " +
                            "public class TreeTest { " +
                            "    public static String test() { " +
                            "        TreeNode<String> root = new TreeNode<String>(\"root\"); " +
                            "        TreeNode<String> child = new TreeNode<String>(\"child1\"); " +
                            "        root.addChild(child); " +
                            "        TreeNode retrieved = root.getChild(0); " +
                            "        return (String) retrieved.getData(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.TreeTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("child1", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Test 41: Class.cast() -- runtime type casting alternative to (T) cast.
     */
    @Test
    public void testClassCastMethod() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/ClassCastTest.java",
                            "package gen; " +
                            "public class ClassCastTest { " +
                            "    public static String test() { " +
                            "        Object obj = \"safe_cast\"; " +
                            "        return String.class.cast(obj); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.ClassCastTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("safe_cast", result);
        } catch (Throwable t) {
            fail("Class.cast() should work with generic return type substitution: " + t.getMessage(), t);
        }
    }

    /**
     * Test 42: Map.Entry iteration with generics.
     */
    @Test
    public void testMapEntryIteration() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "gen/MapEntryTest.java",
                            "package gen; " +
                            "import java.util.HashMap; " +
                            "import java.util.Map; " +
                            "import java.util.Iterator; " +
                            "import java.util.Set; " +
                            "public class MapEntryTest { " +
                            "    public static String test() { " +
                            "        Map<String, Integer> map = new HashMap<String, Integer>(); " +
                            "        map.put(\"a\", Integer.valueOf(1)); " +
                            "        Set entrySet = map.entrySet(); " +
                            "        Iterator it = entrySet.iterator(); " +
                            "        Map.Entry entry = (Map.Entry) it.next(); " +
                            "        return entry.getKey().toString() + \"=\" + entry.getValue().toString(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("gen.MapEntryTest");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("a=1", result);
        } catch (Throwable t) {
            fail(t);
        }
    }

    // =================================================================
    //  Regression Test: Diamond + no-cast on user-defined generic class
    // =================================================================

    /**
     * Test 43: Box&lt;String&gt; box = new Box&lt;&gt;("FAAAH MAMBO"); String str = box.get();
     * Tests diamond operator on user-defined generic class with no explicit cast.
     * This is the regression case: the return type T of get() must resolve to String.
     */
    @Test
    public void testDiamondOnUserDefinedGenericClassNoCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "regr/Box.java",
                            "package regr; " +
                            "public class Box<T> { " +
                            "    private T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "}"
                    ),
                    new StringResource(
                            "regr/BoxUser.java",
                            "package regr; " +
                            "public class BoxUser { " +
                            "    public static String test() { " +
                            "        Box<String> box = new Box<>(\"FAAAH MAMBO\"); " +
                            "        String str = box.get(); " +
                            "        return str; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("regr.BoxUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("FAAAH MAMBO", result);
        } catch (Throwable t) {
            fail("Regression: diamond + no-cast on user-defined generic class should work: " + t.getMessage(), t);
        }
    }

    @Test
    public void testExplicitTypeArgOnUserDefinedGenericClassNoCast() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "regr2/Box.java",
                            "package regr2; " +
                            "public class Box<T> { " +
                            "    private T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "}"
                    ),
                    new StringResource(
                            "regr2/BoxUser.java",
                            "package regr2; " +
                            "public class BoxUser { " +
                            "    public static String test() { " +
                            "        Box<String> box = new Box<String>(\"hello\"); " +
                            "        String s = box.get(); " +
                            "        return s; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("regr2.BoxUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
        } catch (Throwable t) {
            fail("Explicit type arg + no-cast on user-defined generic class should work: " + t.getMessage(), t);
        }
    }

    @Test
    public void testSignatureAttributeGenerated() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "sigtest/Box.java",
                            "package sigtest; " +
                            "public class Box<T> { " +
                            "    private T value; " +
                            "    public Box(T value) { this.value = value; } " +
                            "    public T get() { return value; } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> boxClass = loader.loadClass("sigtest.Box");

            java.lang.reflect.TypeVariable<?>[] typeParams = boxClass.getTypeParameters();
            assertTrue(typeParams.length > 0, "Box should have type parameters via Signature attribute");
            assertEquals("T", typeParams[0].getName());

            Method getMethod = boxClass.getDeclaredMethod("get");
            java.lang.reflect.Type genericReturn = getMethod.getGenericReturnType();
            assertTrue(genericReturn instanceof java.lang.reflect.TypeVariable,
                    "get() should have generic return type T, got: " + genericReturn);
            assertEquals("T", ((java.lang.reflect.TypeVariable<?>) genericReturn).getName());
        } catch (Throwable t) {
            fail("Signature attribute verification failed: " + t.getMessage(), t);
        }
    }
}
