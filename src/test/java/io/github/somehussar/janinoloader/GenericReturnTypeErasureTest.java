package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite demonstrating Janino's generic return type erasure issue (RC1 bug).
 *
 * <h2>The Problem</h2>
 * <p>When Janino compiles code using generic types from the JDK (loaded via reflection/class files),
 * methods that return type parameters (e.g., {@code Map.Entry<K,V>.getValue()} which returns {@code V})
 * have their return types erased to {@code Object}. This means:</p>
 * <ul>
 *     <li>{@code entry.getValue()} returns {@code Object}, not {@code V} (Integer, String, etc.)</li>
 *     <li>Calling methods on the return value fails: "method not declared"</li>
 *     <li>Direct assignment to typed variables fails: "cannot convert Object to X"</li>
 * </ul>
 *
 * <h2>Root Cause</h2>
 * <p>Janino's type parameter resolution fails specifically for <b>inner types</b> like
 * {@code Map.Entry<K,V>} where K and V come from the enclosing {@code Map<K,V>} declaration.
 * The 3.1.13-SNAPSHOT fixed generic return type resolution for direct generic interfaces
 * (List, Iterator, Optional, etc.) but <b>not</b> for inner/nested generic types.</p>
 *
 * <h2>User-Facing Error</h2>
 * <pre>
 * for (Map.Entry&lt;String, EnergyConfig&gt; config : ENERGY_REGISTRY.entrySet()) {
 *     config.getValue().apply(config.getKey(), null);
 *     // ERROR: A method named "apply" is not declared in any enclosing class
 *     //        nor any supertype, nor through a static import
 *
 *     // WORKAROUND: explicit cast
 *     ((EnergyConfig) config.getValue()).apply(config.getKey(), null);
 * }
 * </pre>
 *
 * <h2>Test Structure</h2>
 * <p>Tests are organized into two groups:</p>
 * <ul>
 *     <li><b>FIXED</b> (All categories): Patterns that were broken in 3.1.12 but FIXED in
 *         3.1.13-SNAPSHOT. These compile and run successfully, verifying the fix.</li>
 *     <li><b>CONTROL</b> (Category 7): Explicit cast workarounds that always work.</li>
 * </ul>
 *
 * @see <a href="JANINO_GENERICS.md">Janino Generics Deep Dive</a>
 * @see JaninoLimitationsTest
 */
public class GenericReturnTypeErasureTest {

    private IDynamicCompiler c;

    @BeforeEach
    void setUp() {
        c = IDynamicCompilerBuilder.createBuilder().getCompiler();
    }

    // =====================================================================
    //  Category 1: Map.Entry patterns — FIXED in 3.1.13-SNAPSHOT
    //  Inner type Map.Entry<K,V> — type params K,V now resolved from Map<K,V>
    // =====================================================================

    @Nested
    @DisplayName("1. FIXED: Map.Entry generic return type resolution")
    class MapEntryPatterns {

        /** entry.getValue() now correctly returns V=Integer. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("1.1 entry.getValue() → Integer assignment (no cast) — FIXED")
        void test_mapEntryGetValue() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/MapEntryGetValue.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class MapEntryGetValue {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"x\", 42);\n" +
                "        for (Map.Entry<String, Integer> entry : map.entrySet()) {\n" +
                "            Integer val = entry.getValue();\n" +
                "            return val;\n" +
                "        }\n" +
                "        return -1;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("erasure.MapEntryGetValue")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[1.1 FIXED] entry.getValue() → Integer works without cast");
        }

        /** entry.getKey() now correctly returns K=String. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("1.2 entry.getKey() → String assignment (no cast) — FIXED")
        void test_mapEntryGetKey() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/MapEntryGetKey.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class MapEntryGetKey {\n" +
                "    public static String test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"hello\", 1);\n" +
                "        for (Map.Entry<String, Integer> entry : map.entrySet()) {\n" +
                "            String key = entry.getKey();\n" +
                "            return key;\n" +
                "        }\n" +
                "        return null;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("erasure.MapEntryGetKey")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
            System.out.println("[1.2 FIXED] entry.getKey() → String works without cast");
        }

        /** entry.setValue() now correctly returns V=Integer. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("1.3 entry.setValue() returns old V (no cast) — FIXED")
        void test_mapEntrySetValue() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/MapEntrySetValue.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class MapEntrySetValue {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"x\", 42);\n" +
                "        for (Map.Entry<String, Integer> entry : map.entrySet()) {\n" +
                "            Integer old = entry.setValue(99);\n" +
                "            return old;\n" +
                "        }\n" +
                "        return -1;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("erasure.MapEntrySetValue")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[1.3 FIXED] entry.setValue() → Integer works without cast");
        }

        /** entry.getValue().intValue() now works. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("1.4 entry.getValue().intValue() — chained method call — FIXED")
        void test_mapEntryChainedMethod() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/MapEntryChained.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class MapEntryChained {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"x\", 42);\n" +
                "        int total = 0;\n" +
                "        for (Map.Entry<String, Integer> entry : map.entrySet()) {\n" +
                "            total += entry.getValue().intValue();\n" +
                "        }\n" +
                "        return total;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("erasure.MapEntryChained")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[1.4 FIXED] entry.getValue().intValue() works without cast");
        }

        /** String val = entry.getValue(); val.length() now works. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("1.5 String val = entry.getValue(); val.length() — FIXED")
        void test_mapEntryDirectAssignment() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/MapEntryAssign.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class MapEntryAssign {\n" +
                "    public static int test() {\n" +
                "        Map<String, String> map = new HashMap<>();\n" +
                "        map.put(\"key\", \"value\");\n" +
                "        for (Map.Entry<String, String> entry : map.entrySet()) {\n" +
                "            String val = entry.getValue();\n" +
                "            return val.length();\n" +
                "        }\n" +
                "        return -1;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("erasure.MapEntryAssign")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(5, result);
            System.out.println("[1.5 FIXED] String val = entry.getValue() works without cast");
        }

        /** entry.getValue().run() — the RC1 user pattern — now works. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("1.6 entry.getValue().run() — RC1 user pattern — FIXED")
        void test_mapEntryUserPattern() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/MapEntryUserPattern.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class MapEntryUserPattern {\n" +
                "    public static String test() {\n" +
                "        Map<String, Runnable> tasks = new HashMap<>();\n" +
                "        for (Map.Entry<String, Runnable> entry : tasks.entrySet()) {\n" +
                "            entry.getValue().run();\n" +
                "        }\n" +
                "        return \"ok\";\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("erasure.MapEntryUserPattern")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("ok", result);
            System.out.println("[1.6 FIXED] entry.getValue().run() works without cast");
        }
    }

    // =====================================================================
    //  Category 2: Collection iterator patterns — FIXED in 3.1.13-SNAPSHOT
    //  Direct generic interfaces: Iterator<E>.next(), ListIterator<E>.previous()
    // =====================================================================

    @Nested
    @DisplayName("2. FIXED: Collection/Iterator generic return types (3.1.13-SNAPSHOT)")
    class CollectionIteratorPatterns {

        /** iterator.next() now correctly returns E=String. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("2.1 iterator.next() → String assignment — FIXED")
        void test_iteratorNext() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/IteratorNext.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class IteratorNext {\n" +
                "    public static String test() {\n" +
                "        List<String> list = new ArrayList<>();\n" +
                "        list.add(\"hello\");\n" +
                "        Iterator<String> it = list.iterator();\n" +
                "        String s = it.next();\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("fixed.IteratorNext")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
            System.out.println("[2.1 FIXED] iterator.next() → String works without cast");
        }

        /** listIterator.previous() now correctly returns E=Integer. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("2.2 listIterator.previous() → Integer assignment — FIXED")
        void test_listIteratorPrevious() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/ListIteratorPrev.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class ListIteratorPrev {\n" +
                "    public static int test() {\n" +
                "        List<Integer> list = new ArrayList<>();\n" +
                "        list.add(10);\n" +
                "        list.add(20);\n" +
                "        ListIterator<Integer> lit = list.listIterator(2);\n" +
                "        Integer val = lit.previous();\n" +
                "        return val;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.ListIteratorPrev")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(20, result);
            System.out.println("[2.2 FIXED] listIterator.previous() → Integer works without cast");
        }

        /** Enhanced for-each with List<String> now works. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("2.3 for-each loop with List<String> — FIXED")
        void test_forEachList() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/ForEachList.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class ForEachList {\n" +
                "    public static String test() {\n" +
                "        List<String> list = new ArrayList<>();\n" +
                "        list.add(\"hello\");\n" +
                "        list.add(\"world\");\n" +
                "        StringBuilder sb = new StringBuilder();\n" +
                "        for (String s : list) {\n" +
                "            sb.append(s);\n" +
                "        }\n" +
                "        return sb.toString();\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("fixed.ForEachList")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("helloworld", result);
            System.out.println("[2.3 FIXED] for-each List<String> works");
        }

        /** Chained set.iterator().next() — FIXED in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("2.4 set.iterator().next() — chained generic returns — FIXED")
        void test_chainedIteratorNext() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/ChainedIterator.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class ChainedIterator {\n" +
                "    public static String test() {\n" +
                "        Set<String> set = new HashSet<>();\n" +
                "        set.add(\"first\");\n" +
                "        String s = set.iterator().next();\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("fixed.ChainedIterator")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("first", result);
            System.out.println("[2.4 FIXED] set.iterator().next() works without cast");
        }
    }

    // =====================================================================
    //  Category 3: Optional patterns — FIXED in 3.1.13-SNAPSHOT
    // =====================================================================

    @Nested
    @DisplayName("3. FIXED: Optional generic return types (3.1.13-SNAPSHOT)")
    class OptionalPatterns {

        /** optional.get() now correctly returns T=String. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("3.1 optional.get() → String assignment — FIXED")
        void test_optionalGet() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/OptionalGet.java",
                "package fixed;\n" +
                "import java.util.Optional;\n" +
                "public class OptionalGet {\n" +
                "    public static String test() {\n" +
                "        Optional<String> opt = Optional.of(\"hello\");\n" +
                "        String s = opt.get();\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("fixed.OptionalGet")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
            System.out.println("[3.1 FIXED] optional.get() → String works without cast");
        }

        /** optional.orElse() now correctly returns T=Integer. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("3.2 optional.orElse() → Integer assignment — FIXED")
        void test_optionalOrElse() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/OptionalOrElse.java",
                "package fixed;\n" +
                "import java.util.Optional;\n" +
                "public class OptionalOrElse {\n" +
                "    public static int test() {\n" +
                "        Optional<Integer> opt = Optional.empty();\n" +
                "        Integer val = opt.orElse(42);\n" +
                "        return val;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.OptionalOrElse")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[3.2 FIXED] optional.orElse() → Integer works without cast");
        }

        /** optional.map(fn).get() chain now works. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("3.3 optional.map(fn).get() — chained generic returns — FIXED")
        void test_optionalMap() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/OptionalMap.java",
                "package fixed;\n" +
                "import java.util.Optional;\n" +
                "import java.util.function.Function;\n" +
                "public class OptionalMap {\n" +
                "    public static int test() {\n" +
                "        Optional<String> opt = Optional.of(\"hello\");\n" +
                "        Optional<Integer> mapped = opt.map(s -> s.length());\n" +
                "        Integer len = mapped.get();\n" +
                "        return len;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.OptionalMap")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(5, result);
            System.out.println("[3.3 FIXED] optional.map().get() chain works without cast");
        }
    }

    // =====================================================================
    //  Category 4: Enum patterns — FIXED in 3.1.13-SNAPSHOT
    // =====================================================================

    @Nested
    @DisplayName("4. MIXED: Enum generic return types")
    class EnumPatterns {

        /** Enum.valueOf(Class&lt;T&gt;, String) returns T — FIXED in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("4.1 Enum.valueOf() → typed enum assignment — FIXED")
        void test_enumValueOf() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/EnumValueOf.java",
                "package erasure;\n" +
                "public class EnumValueOf {\n" +
                "    public static String test() {\n" +
                "        Thread.State s = Enum.valueOf(Thread.State.class, \"NEW\");\n" +
                "        return s.name();\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("erasure.EnumValueOf")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("NEW", result);
            System.out.println("[4.1 FIXED] Enum.valueOf() → Thread.State works without cast");
        }

        /** EnumSet.allOf() iteration now works with typed loop variable. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("4.2 EnumSet.allOf() — iterate with typed variable — FIXED")
        void test_enumSetAllOf() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/EnumSetAll.java",
                "package fixed;\n" +
                "import java.util.EnumSet;\n" +
                "public class EnumSetAll {\n" +
                "    public static int test() {\n" +
                "        EnumSet<Thread.State> states = EnumSet.allOf(Thread.State.class);\n" +
                "        int count = 0;\n" +
                "        for (Thread.State s : states) {\n" +
                "            count++;\n" +
                "        }\n" +
                "        return count;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.EnumSetAll")
                .getDeclaredMethod("test").invoke(null);
            assertTrue(result > 0);
            System.out.println("[4.2 FIXED] EnumSet.allOf() iteration works with typed variable (count=" + result + ")");
        }
    }

    // =====================================================================
    //  Category 5: Other generic return patterns — FIXED in 3.1.13-SNAPSHOT
    // =====================================================================

    @Nested
    @DisplayName("5. FIXED: Other generic return type patterns (3.1.13-SNAPSHOT)")
    class OtherGenericReturns {

        /** list.get(0) now correctly returns E=String. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("5.1 list.get(0) → String assignment — FIXED")
        void test_listGet() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/ListGet.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class ListGet {\n" +
                "    public static String test() {\n" +
                "        List<String> list = new ArrayList<>();\n" +
                "        list.add(\"hello\");\n" +
                "        String s = list.get(0);\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("fixed.ListGet")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
            System.out.println("[5.1 FIXED] list.get(0) → String works without cast");
        }

        /** map.get(key) now correctly returns V=Integer. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("5.2 map.get(key) → Integer assignment — FIXED")
        void test_mapGet() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/MapGet.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class MapGet {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"x\", 42);\n" +
                "        Integer val = map.get(\"x\");\n" +
                "        return val;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.MapGet")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[5.2 FIXED] map.get(key) → Integer works without cast");
        }

        /** deque.element() now correctly returns E=String. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("5.3 deque.element() → String assignment — FIXED")
        void test_dequeElement() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/DequeElement.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class DequeElement {\n" +
                "    public static String test() {\n" +
                "        Deque<String> deque = new ArrayDeque<>();\n" +
                "        deque.add(\"first\");\n" +
                "        String s = deque.element();\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("fixed.DequeElement")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("first", result);
            System.out.println("[5.3 FIXED] deque.element() → String works without cast");
        }

        /** Comparator.compare() params now match. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("5.4 Comparator<String>.compare(a, b) — FIXED")
        void test_comparatorCompare() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/ComparatorCompare.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class ComparatorCompare {\n" +
                "    public static int test() {\n" +
                "        Comparator<String> comp = Comparator.naturalOrder();\n" +
                "        return comp.compare(\"a\", \"b\");\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.ComparatorCompare")
                .getDeclaredMethod("test").invoke(null);
            assertTrue(result < 0);
            System.out.println("[5.4 FIXED] comparator.compare() works with String args");
        }

        /** Function.apply() now returns R=Integer. Fixed in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("5.5 Function<String,Integer>.apply() → Integer — FIXED")
        void test_functionApply() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/FunctionApply.java",
                "package fixed;\n" +
                "import java.util.function.Function;\n" +
                "public class FunctionApply {\n" +
                "    public static int test() {\n" +
                "        Function<String, Integer> func = s -> s.length();\n" +
                "        Integer len = func.apply(\"hello\");\n" +
                "        return len;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.FunctionApply")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(5, result);
            System.out.println("[5.5 FIXED] function.apply() → Integer works without cast");
        }
    }

    // =====================================================================
    //  Category 6: Chained/nested generic method calls — MIXED
    //  Chaining through Map.Entry: BROKEN. Other chains: some FIXED.
    // =====================================================================

    @Nested
    @DisplayName("6. BROKEN: Chained generic calls through Map.Entry / erased intermediates")
    class ChainedPatterns {

        /** Deep chain through Map.Entry — FIXED in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("6.1 map.entrySet().iterator().next().getValue() — deep chain — FIXED")
        void test_deepChainedGenericCalls() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/DeepChain.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class DeepChain {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"x\", 42);\n" +
                "        Integer val = map.entrySet().iterator().next().getValue();\n" +
                "        return val;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.DeepChain")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[6.1 FIXED] deep chain map.entrySet().iterator().next().getValue() works");
        }

        /** Collections.singletonList().get(0) chained — FIXED in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("6.2 Collections.singletonList().get(0) — factory + accessor — FIXED")
        void test_collectionsFactory() throws Throwable {
            c.compileClass(new StringResource(
                "erasure/CollectionsFactory.java",
                "package erasure;\n" +
                "import java.util.*;\n" +
                "public class CollectionsFactory {\n" +
                "    public static String test() {\n" +
                "        String s = Collections.singletonList(\"hello\").get(0);\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("erasure.CollectionsFactory")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
            System.out.println("[6.2 FIXED] Collections.singletonList().get(0) works without cast");
        }

        /** Collections.unmodifiableMap().get() — FIXED in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("6.3 Collections.unmodifiableMap().get() — wrapper + accessor — FIXED")
        void test_unmodifiableMapGet() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/UnmodifiableMap.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class UnmodifiableMap {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> base = new HashMap<>();\n" +
                "        base.put(\"x\", 42);\n" +
                "        Map<String, Integer> safe = Collections.unmodifiableMap(base);\n" +
                "        Integer val = safe.get(\"x\");\n" +
                "        return val;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.UnmodifiableMap")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[6.3 FIXED] unmodifiableMap().get() works");
        }

        /** Map.computeIfAbsent() — FIXED in 3.1.13-SNAPSHOT. */
        @Test
        @DisplayName("6.4 map.computeIfAbsent() → typed assignment — FIXED")
        void test_mapComputeIfAbsent() throws Throwable {
            c.compileClass(new StringResource(
                "fixed/MapCompute.java",
                "package fixed;\n" +
                "import java.util.*;\n" +
                "public class MapCompute {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        Integer val = map.computeIfAbsent(\"x\", k -> k.length());\n" +
                "        return val;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("fixed.MapCompute")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(1, result);
            System.out.println("[6.4 FIXED] map.computeIfAbsent() works");
        }
    }

    // =====================================================================
    //  Category 7: Control tests — explicit cast workarounds (MUST PASS)
    // =====================================================================

    @Nested
    @DisplayName("7. CONTROL: Explicit cast workarounds (must always pass)")
    class ControlTests {

        @Test
        @DisplayName("7.1 CONTROL: (Integer) entry.getValue() — explicit cast works")
        void test_mapEntryGetValueWithCast() throws Throwable {
            c.compileClass(new StringResource(
                "ctrl/MapEntryCast.java",
                "package ctrl;\n" +
                "import java.util.*;\n" +
                "public class MapEntryCast {\n" +
                "    public static int test() {\n" +
                "        Map<String, Integer> map = new HashMap<>();\n" +
                "        map.put(\"x\", 42);\n" +
                "        for (Map.Entry<String, Integer> entry : map.entrySet()) {\n" +
                "            Integer val = (Integer) entry.getValue();\n" +
                "            return val;\n" +
                "        }\n" +
                "        return -1;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            int result = (int) cl.loadClass("ctrl.MapEntryCast")
                .getDeclaredMethod("test").invoke(null);
            assertEquals(42, result);
            System.out.println("[7.1] CONTROL: explicit cast on entry.getValue() works");
        }

        @Test
        @DisplayName("7.2 CONTROL: (String) list.get(0) — explicit cast works")
        void test_listGetWithCast() throws Throwable {
            c.compileClass(new StringResource(
                "ctrl/ListCast.java",
                "package ctrl;\n" +
                "import java.util.*;\n" +
                "public class ListCast {\n" +
                "    public static String test() {\n" +
                "        List<String> list = new ArrayList<>();\n" +
                "        list.add(\"hello\");\n" +
                "        String s = (String) list.get(0);\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("ctrl.ListCast")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("hello", result);
            System.out.println("[7.2] CONTROL: explicit cast on list.get(0) works");
        }

        @Test
        @DisplayName("7.3 CONTROL: (String) iterator.next() — explicit cast works")
        void test_iteratorNextWithCast() throws Throwable {
            c.compileClass(new StringResource(
                "ctrl/IteratorCast.java",
                "package ctrl;\n" +
                "import java.util.*;\n" +
                "public class IteratorCast {\n" +
                "    public static String test() {\n" +
                "        List<String> list = new ArrayList<>();\n" +
                "        list.add(\"world\");\n" +
                "        Iterator<String> it = list.iterator();\n" +
                "        String s = (String) it.next();\n" +
                "        return s;\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("ctrl.IteratorCast")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("world", result);
            System.out.println("[7.3] CONTROL: explicit cast on iterator.next() works");
        }

        @Test
        @DisplayName("7.4 CONTROL: raw List usage — no generics, no problem")
        void test_rawListNoCast() throws Throwable {
            c.compileClass(new StringResource(
                "ctrl/RawList.java",
                "package ctrl;\n" +
                "import java.util.*;\n" +
                "public class RawList {\n" +
                "    public static String test() {\n" +
                "        List list = new ArrayList();\n" +
                "        list.add(\"raw\");\n" +
                "        return (String) list.get(0);\n" +
                "    }\n" +
                "}"
            ));
            ClassLoader cl = c.getClassLoader();
            String result = (String) cl.loadClass("ctrl.RawList")
                .getDeclaredMethod("test").invoke(null);
            assertEquals("raw", result);
            System.out.println("[7.4] CONTROL: raw list with cast works");
        }
    }
}
