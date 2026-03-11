package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptBodyBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for two specific Janino limitations:
 *
 * <h2>Limitation 1: Map.Entry.getValue() Type Erasure</h2>
 * <p>When iterating over Map.Entry&lt;String, Integer&gt;, calling getValue() returns
 * erased Object instead of Integer. This requires an explicit cast that javac would
 * insert automatically via bridge methods/type checking.</p>
 *
 * <h2>Limitation 2: Bounded Type Parameters in Local Classes</h2>
 * <p>Declaring a local class with &lt;T extends Comparable&lt;T&gt;&gt; causes an
 * AssertionError inside Janino's IClass.parameterize() because local classes don't
 * properly handle parameterized type bounds.</p>
 */
public class JaninoLimitationsTest {

    // =====================================================================
    //  Helper interfaces for ScriptBody-based tests
    // =====================================================================

    public interface StringResult {
        String run();
    }

    public interface IntResult {
        int run();
    }

    public interface ObjectResult {
        Object run();
    }

    // =====================================================================
    //  Infrastructure for direct Janino compilation tests
    // =====================================================================

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

    private IDynamicCompiler newCompiler() {
        return IDynamicCompilerBuilder.createBuilder().getCompiler();
    }

    // =====================================================================
    //  LIMITATION 1: Map.Entry.getValue() Type Erasure
    // =====================================================================

    /**
     * Test 1a: Map.Entry getValue() WITHOUT cast via ScriptBody.
     * <p>
     * This is the core failing case: entry.getValue() on a Map.Entry&lt;String, Integer&gt;
     * should return Integer, but Janino erases it to Object.
     * </p>
     */
    @Disabled("Janino limitation: Map.Entry.getValue() generic return type erased to Object")
    @Test
    public void test_L1_mapEntryGetValueWithoutCast_scriptBody() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "\n" +
                "public int run() {\n" +
                "    Map<String, Integer> scores = new HashMap<>();\n" +
                "    scores.put(\"Alice\", 95);\n" +
                "    int total = 0;\n" +
                "    for (Map.Entry<String, Integer> entry : scores.entrySet()) {\n" +
                "        Integer value = entry.getValue();\n" +   // No cast - should work with proper generics
                "        total += value;\n" +
                "    }\n" +
                "    return total;\n" +
                "}\n";

        IScriptClassBody<IntResult> body = IScriptBodyBuilder.getBuilder(IntResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            IntResult result = body.get();
            assertNotNull(result);
            assertEquals(95, result.run());
            System.out.println("[PASS] test_L1_mapEntryGetValueWithoutCast_scriptBody: getValue() returns Integer without cast");
        } catch (CompileException e) {
            System.out.println("[FAIL] test_L1_mapEntryGetValueWithoutCast_scriptBody: " + e.getMessage());
            fail("Map.Entry<String, Integer>.getValue() should return Integer without cast. Error: " + e.getMessage(), e);
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L1_mapEntryGetValueWithoutCast_scriptBody: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    /**
     * Test 1b: Map.Entry getValue() WITHOUT cast via direct Janino compilation.
     * <p>
     * Same test but using direct Compiler API to isolate from ScriptBody wrapper.
     * </p>
     */
    @Disabled("Janino limitation: Map.Entry.getValue() generic return type erased to Object (direct API)")
    @Test
    public void test_L1_mapEntryGetValueWithoutCast_direct() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "lim1/MapEntryNoCast.java",
                            "package lim1; " +
                            "import java.util.HashMap; " +
                            "import java.util.Map; " +
                            "public class MapEntryNoCast { " +
                            "    public static int test() { " +
                            "        Map<String, Integer> scores = new HashMap<>(); " +
                            "        scores.put(\"Alice\", 95); " +
                            "        int total = 0; " +
                            "        for (Map.Entry<String, Integer> entry : scores.entrySet()) { " +
                            "            Integer value = entry.getValue(); " +  // No cast
                            "            total += value; " +
                            "        } " +
                            "        return total; " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("lim1.MapEntryNoCast");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals(95, result);
            System.out.println("[PASS] test_L1_mapEntryGetValueWithoutCast_direct: Map.Entry getValue() works without cast");
        } catch (CompileException ce) {
            System.out.println("[FAIL] test_L1_mapEntryGetValueWithoutCast_direct: COMPILE ERROR: " + ce.getMessage());
            fail("Map.Entry<String, Integer>.getValue() should return Integer without cast. Error: " + ce.getMessage(), ce);
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L1_mapEntryGetValueWithoutCast_direct: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    /**
     * Test 1c: Map.Entry getValue() WITH explicit cast (workaround that currently works).
     */
    @Test
    public void test_L1_mapEntryGetValueWithCast_control() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "\n" +
                "public int run() {\n" +
                "    Map<String, Integer> scores = new HashMap<>();\n" +
                "    scores.put(\"Alice\", 95);\n" +
                "    int total = 0;\n" +
                "    for (Map.Entry<String, Integer> entry : scores.entrySet()) {\n" +
                "        Integer value = (Integer) entry.getValue();\n" +   // Explicit cast - workaround
                "        total += value;\n" +
                "    }\n" +
                "    return total;\n" +
                "}\n";

        IScriptClassBody<IntResult> body = IScriptBodyBuilder.getBuilder(IntResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            IntResult result = body.get();
            assertNotNull(result);
            assertEquals(95, result.run());
            System.out.println("[PASS] test_L1_mapEntryGetValueWithCast_control: explicit cast workaround works");
        } catch (CompileException e) {
            fail("Explicit cast workaround should always work. Error: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    /**
     * Test 1d: Map.Entry getKey() without cast - same issue for keys.
     */
    @Disabled("Janino limitation: Map.Entry.getKey() generic return type erased to Object")
    @Test
    public void test_L1_mapEntryGetKeyWithoutCast_scriptBody() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "\n" +
                "public String run() {\n" +
                "    Map<String, Integer> scores = new HashMap<>();\n" +
                "    scores.put(\"Alice\", 95);\n" +
                "    for (Map.Entry<String, Integer> entry : scores.entrySet()) {\n" +
                "        String key = entry.getKey();\n" +   // No cast - should work
                "        return key;\n" +
                "    }\n" +
                "    return \"empty\";\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("Alice", result.run());
            System.out.println("[PASS] test_L1_mapEntryGetKeyWithoutCast_scriptBody: getKey() returns String without cast");
        } catch (CompileException e) {
            System.out.println("[FAIL] test_L1_mapEntryGetKeyWithoutCast_scriptBody: " + e.getMessage());
            fail("Map.Entry<String, Integer>.getKey() should return String without cast. Error: " + e.getMessage(), e);
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L1_mapEntryGetKeyWithoutCast_scriptBody: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    // =====================================================================
    //  LIMITATION 2: Bounded Type Parameters in Local Classes
    // =====================================================================

    /**
     * Test 2a: Local class with &lt;T extends Comparable&lt;T&gt;&gt; via ScriptBody.
     * <p>
     * This is the core failing case: Janino throws AssertionError when trying to
     * compile a local class with a bounded type parameter that has a parameterized
     * bound (Comparable&lt;T&gt;).
     * </p>
     */
    @Disabled("Janino bug: IClass.parameterize() fails on <T extends Comparable<T>> bounded type parameters")
    @Test
    public void test_L2_boundedTypeParamLocalClass_scriptBody() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "public Object run() {\n" +
                "    class Pair<T extends Comparable<T>> {\n" +
                "        T a, b;\n" +
                "        Pair(T a, T b) { this.a = a; this.b = b; }\n" +
                "        T max() { return a.compareTo(b) >= 0 ? a : b; }\n" +
                "    }\n" +
                "    Pair<String> p = new Pair<>(\"apple\", \"banana\");\n" +
                "    return (String) p.max();\n" +
                "}\n";

        IScriptClassBody<ObjectResult> body = IScriptBodyBuilder.getBuilder(ObjectResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            ObjectResult result = body.get();
            assertNotNull(result);
            assertEquals("banana", result.run());
            System.out.println("[PASS] test_L2_boundedTypeParamLocalClass_scriptBody: <T extends Comparable<T>> on local class works");
        } catch (CompileException e) {
            System.out.println("[FAIL] test_L2_boundedTypeParamLocalClass_scriptBody (CompileException): " + e.getMessage());
            fail("Local class with <T extends Comparable<T>> should compile. Error: " + e.getMessage(), e);
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L2_boundedTypeParamLocalClass_scriptBody (" + t.getClass().getSimpleName() + "): " + t.getMessage());
            fail("Local class with <T extends Comparable<T>> should not throw " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        }
    }

    /**
     * Test 2b: Local class with &lt;T extends Comparable&lt;T&gt;&gt; via direct compilation.
     */
    @Disabled("Janino bug: IClass.parameterize() fails on <T extends Comparable<T>> bounded type parameters")
    @Test
    public void test_L2_boundedTypeParamLocalClass_direct() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "lim2/BoundedLocal.java",
                            "package lim2; " +
                            "public class BoundedLocal { " +
                            "    public static String test() { " +
                            "        class Pair<T extends Comparable<T>> { " +
                            "            T a, b; " +
                            "            Pair(T a, T b) { this.a = a; this.b = b; } " +
                            "            T max() { return a.compareTo(b) >= 0 ? a : b; } " +
                            "        } " +
                            "        Pair<String> p = new Pair<>(\"apple\", \"banana\"); " +
                            "        return (String) p.max(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("lim2.BoundedLocal");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("banana", result);
            System.out.println("[PASS] test_L2_boundedTypeParamLocalClass_direct: local class with bounded type param works");
        } catch (CompileException ce) {
            System.out.println("[FAIL] test_L2_boundedTypeParamLocalClass_direct (CompileException): " + ce.getMessage());
            fail("Local class with <T extends Comparable<T>> should compile. Error: " + ce.getMessage(), ce);
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L2_boundedTypeParamLocalClass_direct (" + t.getClass().getSimpleName() + "): " + t.getMessage());
            fail("Local class with <T extends Comparable<T>> should not throw " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        }
    }

    /**
     * Test 2c: Top-level class with &lt;T extends Comparable&lt;T&gt;&gt; (control - should work).
     * <p>
     * This verifies the issue is specific to LOCAL classes, not generic classes in general.
     * </p>
     */
    @Disabled("Janino bug: IClass.parameterize() fails on <T extends Comparable<T>> (affects all classes, not just local)")
    @Test
    public void test_L2_boundedTypeParamTopLevelClass_control() {
        try {
            Map<String, byte[]> classes = new HashMap<>();
            ClassLoader loader = createMemoryClassLoader(getClass().getClassLoader(), classes);

            compile(classes, loader,
                    new StringResource(
                            "lim2ctrl/Pair.java",
                            "package lim2ctrl; " +
                            "public class Pair<T extends Comparable<T>> { " +
                            "    T a, b; " +
                            "    public Pair(T a, T b) { this.a = a; this.b = b; } " +
                            "    public T max() { return a.compareTo(b) >= 0 ? a : b; } " +
                            "}"
                    ),
                    new StringResource(
                            "lim2ctrl/PairUser.java",
                            "package lim2ctrl; " +
                            "public class PairUser { " +
                            "    public static String test() { " +
                            "        Pair<String> p = new Pair<>(\"apple\", \"banana\"); " +
                            "        return (String) p.max(); " +
                            "    } " +
                            "}"
                    )
            );

            loader = createMemoryClassLoader(getClass().getClassLoader(), classes);
            Class<?> clazz = loader.loadClass("lim2ctrl.PairUser");
            Object result = clazz.getDeclaredMethod("test").invoke(null);
            assertEquals("banana", result);
            System.out.println("[PASS] test_L2_boundedTypeParamTopLevelClass_control: top-level class with bounded type param works");
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L2_boundedTypeParamTopLevelClass_control: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            fail("Top-level class with <T extends Comparable<T>> should work: " + t.getMessage(), t);
        }
    }

    /**
     * Test 2d: Local class with unbounded type param (control - should work).
     * <p>
     * Unbounded &lt;T&gt; on local classes should work fine since no parameterize() call needed.
     * </p>
     */
    @Test
    public void test_L2_unboundedTypeParamLocalClass_control() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "public Object run() {\n" +
                "    class Box<T> {\n" +
                "        private T value;\n" +
                "        Box(T value) { this.value = value; }\n" +
                "        T get() { return value; }\n" +
                "    }\n" +
                "    Box<String> box = new Box<>(\"hello\");\n" +
                "    return (String) box.get();\n" +
                "}\n";

        IScriptClassBody<ObjectResult> body = IScriptBodyBuilder.getBuilder(ObjectResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            ObjectResult result = body.get();
            assertNotNull(result);
            assertEquals("hello", result.run());
            System.out.println("[PASS] test_L2_unboundedTypeParamLocalClass_control: unbounded <T> on local class works");
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L2_unboundedTypeParamLocalClass_control: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            fail("Unbounded <T> on local class should work: " + t.getMessage(), t);
        }
    }

    /**
     * Test 2e: Local class with simple bounded type param (no parameterized bound).
     * <p>&lt;T extends Number&gt; should erase T to Number - no IClass.parameterize() needed.</p>
     */
    @Test
    public void test_L2_simpleBoundedTypeParamLocalClass() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "public Object run() {\n" +
                "    class NumBox<T extends Number> {\n" +
                "        private T value;\n" +
                "        NumBox(T value) { this.value = value; }\n" +
                "        double doubleVal() { return value.doubleValue(); }\n" +
                "    }\n" +
                "    NumBox<Integer> box = new NumBox<>(42);\n" +
                "    return box.doubleVal();\n" +
                "}\n";

        IScriptClassBody<ObjectResult> body = IScriptBodyBuilder.getBuilder(ObjectResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            ObjectResult result = body.get();
            assertNotNull(result);
            assertEquals(42.0, result.run());
            System.out.println("[PASS] test_L2_simpleBoundedTypeParamLocalClass: <T extends Number> on local class works");
        } catch (Throwable t) {
            System.out.println("[FAIL] test_L2_simpleBoundedTypeParamLocalClass: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            fail("<T extends Number> on local class should work: " + t.getMessage(), t);
        }
    }
}
