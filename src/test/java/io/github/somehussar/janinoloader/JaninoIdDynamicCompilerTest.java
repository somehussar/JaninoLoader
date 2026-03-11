package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive generics and lambda test suite exercising JaninoLoader's
 * {@link IDynamicCompiler} and {@link IDynamicCompilerBuilder} APIs — the EXACT
 * path end users take when using JaninoLoader.
 *
 * <p>Unlike {@link JaninoGenericsTest} and {@link JaninoLambdaTest} which test
 * the low-level Janino {@code Compiler} directly, every test here goes through:
 * <ol>
 *   <li>{@code IDynamicCompilerBuilder.createBuilder()} → builder</li>
 *   <li>{@code builder.getCompiler()} → {@code IDynamicCompiler}</li>
 *   <li>{@code compiler.compileClass(StringResource...)} → bytecode in MemoryClassLoader</li>
 *   <li>{@code compiler.getClassLoader().loadClass(...)} → reflection invoke</li>
 * </ol>
 *
 * <p>Some tests intentionally compile sources in SEPARATE {@code compileClass} calls
 * to exercise the MemoryClassLoader cross-compile resolution path.</p>
 */
public class JaninoIdDynamicCompilerTest {

    // =========================================================================
    //  Helper
    // =========================================================================

    /**
     * Creates a fresh IDynamicCompiler via the builder API — the user-facing entry point.
     */
    private static IDynamicCompiler compiler() {
        return IDynamicCompilerBuilder.createBuilder().getCompiler();
    }

    /**
     * Compile source, load class, invoke static "test()" and return the result.
     */
    private static Object compileAndRun(IDynamicCompiler compiler, String fqcn, StringResource... sources) throws Throwable {
        compiler.compileClass(sources);
        ClassLoader cl = compiler.getClassLoader();
        Class<?> clazz = cl.loadClass(fqcn);
        return clazz.getDeclaredMethod("test").invoke(null);
    }

    // =========================================================================
    //  GENERICS — Category 1: Simple Generic Classes
    // =========================================================================

    /** G1: Simple generic Box<T> with get/set through IDynamicCompiler. */
    @Test
    public void generics_simpleBoxGetSet() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.BoxUser",
                new StringResource("g/Box.java",
                    "package g; " +
                    "public class Box<T> { " +
                    "    private T value; " +
                    "    public Box(T v) { this.value = v; } " +
                    "    public T get() { return value; } " +
                    "    public void set(T v) { this.value = v; } " +
                    "}"),
                new StringResource("g/BoxUser.java",
                    "package g; " +
                    "public class BoxUser { " +
                    "    public static String test() { " +
                    "        Box<String> box = new Box<String>(\"hello\"); " +
                    "        box.set(\"world\"); " +
                    "        return (String) box.get(); " +
                    "    } " +
                    "}")
            );
            assertEquals("world", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G2: Generic class with multiple type parameters — Pair<A, B>. */
    @Test
    public void generics_multipleTypeParams() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.PairUser",
                new StringResource("g/Pair.java",
                    "package g; " +
                    "public class Pair<A, B> { " +
                    "    private A first; private B second; " +
                    "    public Pair(A a, B b) { first = a; second = b; } " +
                    "    public A getFirst() { return first; } " +
                    "    public B getSecond() { return second; } " +
                    "}"),
                new StringResource("g/PairUser.java",
                    "package g; " +
                    "public class PairUser { " +
                    "    public static String test() { " +
                    "        Pair<String, Integer> p = new Pair<String, Integer>(\"age\", Integer.valueOf(30)); " +
                    "        return (String) p.getFirst() + \"=\" + ((Integer) p.getSecond()).toString(); " +
                    "    } " +
                    "}")
            );
            assertEquals("age=30", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G3: Nested generics — List<Set<String>>. */
    @Test
    public void generics_nestedListOfSets() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.NestedUser",
                new StringResource("g/NestedUser.java",
                    "package g; " +
                    "import java.util.*; " +
                    "public class NestedUser { " +
                    "    public static String test() { " +
                    "        List<Set<String>> list = new ArrayList<Set<String>>(); " +
                    "        Set<String> s = new HashSet<String>(); " +
                    "        s.add(\"nested\"); " +
                    "        list.add(s); " +
                    "        Set<String> retrieved = (Set<String>) list.get(0); " +
                    "        return retrieved.contains(\"nested\") ? \"ok\" : \"fail\"; " +
                    "    } " +
                    "}")
            );
            assertEquals("ok", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G4: Generic inheritance — LabeledBox<T> extends Box<T>. */
    @Test
    public void generics_genericInheritance() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.InhUser",
                new StringResource("g/GBox.java",
                    "package g; " +
                    "public class GBox<T> { " +
                    "    protected T value; " +
                    "    public GBox(T v) { this.value = v; } " +
                    "    public T get() { return value; } " +
                    "}"),
                new StringResource("g/LabeledBox.java",
                    "package g; " +
                    "public class LabeledBox<T> extends GBox<T> { " +
                    "    private String label; " +
                    "    public LabeledBox(String lbl, T v) { super(v); label = lbl; } " +
                    "    public String getLabel() { return label; } " +
                    "}"),
                new StringResource("g/InhUser.java",
                    "package g; " +
                    "public class InhUser { " +
                    "    public static String test() { " +
                    "        LabeledBox<String> lb = new LabeledBox<String>(\"name\", \"Janino\"); " +
                    "        return lb.getLabel() + \":\" + (String) lb.get(); " +
                    "    } " +
                    "}")
            );
            assertEquals("name:Janino", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G5: Generic method on non-generic class. */
    @Test
    public void generics_genericMethodOnNonGenericClass() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.GenMethodUser",
                new StringResource("g/GenMethodUser.java",
                    "package g; " +
                    "public class GenMethodUser { " +
                    "    public static <T> T identity(T t) { return t; } " +
                    "    public static String test() { " +
                    "        return (String) identity(\"generic-method\"); " +
                    "    } " +
                    "}")
            );
            assertEquals("generic-method", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G6: Type inference — Janino erases, so we test that cast-free assignment may fail. */
    @Test
    public void generics_typeInferenceWithCast() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.InferUser",
                new StringResource("g/InferUser.java",
                    "package g; " +
                    "import java.util.*; " +
                    "public class InferUser { " +
                    "    public static String test() { " +
                    "        List<String> list = new ArrayList<>(); " +
                    "        list.add(\"inferred\"); " +
                    "        return (String) list.get(0); " +
                    "    } " +
                    "}")
            );
            assertEquals("inferred", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G7: Wildcard types — ? extends Number and ? super Integer. */
    @Test
    public void generics_wildcardTypes() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.WildcardUser",
                new StringResource("g/WildcardUser.java",
                    "package g; " +
                    "import java.util.*; " +
                    "public class WildcardUser { " +
                    "    public static double sumExtends(List<? extends Number> nums) { " +
                    "        double total = 0; " +
                    "        for (int i = 0; i < nums.size(); i++) { " +
                    "            total += ((Number) nums.get(i)).doubleValue(); " +
                    "        } " +
                    "        return total; " +
                    "    } " +
                    "    public static void addSuper(List<? super Integer> list) { " +
                    "        list.add(Integer.valueOf(100)); " +
                    "    } " +
                    "    public static String test() { " +
                    "        List<Integer> nums = new ArrayList<Integer>(); " +
                    "        nums.add(Integer.valueOf(10)); " +
                    "        nums.add(Integer.valueOf(20)); " +
                    "        double sum = sumExtends(nums); " +
                    "        List<Number> targets = new ArrayList<Number>(); " +
                    "        addSuper(targets); " +
                    "        return String.valueOf((int) sum) + \",\" + targets.size(); " +
                    "    } " +
                    "}")
            );
            assertEquals("30,1", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G8: Generic collections — Map<String, List<Integer>> with diamond. */
    @Test
    public void generics_mapOfLists() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.MapListUser",
                new StringResource("g/MapListUser.java",
                    "package g; " +
                    "import java.util.*; " +
                    "public class MapListUser { " +
                    "    public static String test() { " +
                    "        Map<String, List<Integer>> map = new HashMap<String, List<Integer>>(); " +
                    "        List<Integer> vals = new ArrayList<Integer>(); " +
                    "        vals.add(Integer.valueOf(1)); vals.add(Integer.valueOf(2)); vals.add(Integer.valueOf(3)); " +
                    "        map.put(\"nums\", vals); " +
                    "        List<Integer> got = (List<Integer>) map.get(\"nums\"); " +
                    "        return String.valueOf(got.size()); " +
                    "    } " +
                    "}")
            );
            assertEquals("3", result);
        } catch (Throwable t) { fail(t); }
    }

    /**
     * G9: SEPARATE compilation — compile generic class first, then compile a class
     * that depends on it in a second call. Tests MemoryClassLoader resolution path.
     */
    @Test
    public void generics_separateCompilationWithGenericDeps() {
        try {
            IDynamicCompiler c = compiler();
            // First compile: generic Container<T>
            c.compileClass(new StringResource("g/Container.java",
                "package g; " +
                "public class Container<T> { " +
                "    private T item; " +
                "    public Container(T item) { this.item = item; } " +
                "    public T getItem() { return item; } " +
                "    public String describe() { return item.toString(); } " +
                "}")
            );
            // Second compile: class using Container<String> — resolved via MemoryClassLoader
            c.compileClass(new StringResource("g/ContainerUser.java",
                "package g; " +
                "public class ContainerUser { " +
                "    public static String test() { " +
                "        Container<String> c = new Container<String>(\"separate\"); " +
                "        return c.describe(); " +
                "    } " +
                "}")
            );
            ClassLoader cl = c.getClassLoader();
            Object result = cl.loadClass("g.ContainerUser").getDeclaredMethod("test").invoke(null);
            assertEquals("separate", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G10: Generic constructor — new Box<>(value) with diamond operator. */
    @Test
    public void generics_diamondConstructor() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.DiamondUser",
                new StringResource("g/DBox.java",
                    "package g; " +
                    "public class DBox<T> { " +
                    "    private T val; " +
                    "    public DBox(T v) { val = v; } " +
                    "    public T getVal() { return val; } " +
                    "}"),
                new StringResource("g/DiamondUser.java",
                    "package g; " +
                    "public class DiamondUser { " +
                    "    public static String test() { " +
                    "        DBox<String> b = new DBox<>(\"diamond\"); " +
                    "        return (String) b.getVal(); " +
                    "    } " +
                    "}")
            );
            assertEquals("diamond", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G11: Generic array-like — using Object[] inside generic wrapper. */
    @Test
    public void generics_genericWithArrayStorage() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.GArrayUser",
                new StringResource("g/GArray.java",
                    "package g; " +
                    "public class GArray<T> { " +
                    "    private Object[] data; " +
                    "    private int size; " +
                    "    public GArray(int capacity) { data = new Object[capacity]; size = 0; } " +
                    "    public void add(T item) { data[size++] = item; } " +
                    "    public T get(int i) { return (T) data[i]; } " +
                    "    public int size() { return size; } " +
                    "}"),
                new StringResource("g/GArrayUser.java",
                    "package g; " +
                    "public class GArrayUser { " +
                    "    public static String test() { " +
                    "        GArray<String> arr = new GArray<String>(10); " +
                    "        arr.add(\"first\"); arr.add(\"second\"); " +
                    "        return (String) arr.get(1) + \",\" + arr.size(); " +
                    "    } " +
                    "}")
            );
            assertEquals("second,2", result);
        } catch (Throwable t) { fail(t); }
    }

    /** G12: Bounded type parameter — <T extends Comparable> erases to Comparable. */
    @Test
    public void generics_boundedTypeParam() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "g.BoundedUser",
                new StringResource("g/BoundedUser.java",
                    "package g; " +
                    "public class BoundedUser { " +
                    "    public static <T extends Comparable> T max(T a, T b) { " +
                    "        return a.compareTo(b) >= 0 ? a : b; " +
                    "    } " +
                    "    public static String test() { " +
                    "        return (String) max(\"apple\", \"banana\"); " +
                    "    } " +
                    "}")
            );
            assertEquals("banana", result);
        } catch (Throwable t) { fail(t); }
    }

    // =========================================================================
    //  LAMBDAS — Category 2: Lambda Expressions through IDynamicCompiler
    // =========================================================================

    /** L1: Simple lambda with generic Function interface. */
    @Test
    public void lambda_simpleFunctionLambda() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L1",
                new StringResource("l/L1.java",
                    "package l; " +
                    "import java.util.function.*; " +
                    "public class L1 { " +
                    "    public static String test() { " +
                    "        Function<String, String> upper = s -> s.toUpperCase(); " +
                    "        return upper.apply(\"hello\"); " +
                    "    } " +
                    "}")
            );
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L2: Lambda in generic context — Predicate<T> filtering. */
    @Test
    public void lambda_predicateInGenericContext() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L2",
                new StringResource("l/L2.java",
                    "package l; " +
                    "import java.util.*; " +
                    "import java.util.function.*; " +
                    "public class L2 { " +
                    "    public static <T> List<T> filter(List<T> list, Predicate<T> pred) { " +
                    "        List<T> out = new ArrayList<T>(); " +
                    "        for (int i = 0; i < list.size(); i++) { " +
                    "            T item = (T) list.get(i); " +
                    "            if (pred.test(item)) out.add(item); " +
                    "        } " +
                    "        return out; " +
                    "    } " +
                    "    public static String test() { " +
                    "        List<String> words = new ArrayList<String>(); " +
                    "        words.add(\"apple\"); words.add(\"ax\"); words.add(\"banana\"); " +
                    "        List<String> filtered = filter(words, (Predicate) (s -> ((String)s).startsWith(\"a\"))); " +
                    "        return String.valueOf(filtered.size()); " +
                    "    } " +
                    "}")
            );
            assertEquals("2", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L3: Method reference — String::toUpperCase via Function. */
    @Test
    public void lambda_methodReference() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L3",
                new StringResource("l/L3.java",
                    "package l; " +
                    "import java.util.function.*; " +
                    "public class L3 { " +
                    "    public static String test() { " +
                    "        Function<String, String> upper = String::toUpperCase; " +
                    "        return upper.apply(\"world\"); " +
                    "    } " +
                    "}")
            );
            assertEquals("WORLD", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L4: Stream operations on generic collections. */
    @Test
    public void lambda_streamOperationsOnGenericCollection() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L4",
                new StringResource("l/L4.java",
                    "package l; " +
                    "import java.util.*; " +
                    "import java.util.stream.*; " +
                    "public class L4 { " +
                    "    public static String test() { " +
                    "        List<String> words = new ArrayList<String>(); " +
                    "        words.add(\"a\"); words.add(\"bb\"); words.add(\"ccc\"); " +
                    "        long count = words.stream().filter(s -> s.length() > 1).count(); " +
                    "        return String.valueOf(count); " +
                    "    } " +
                    "}")
            );
            assertEquals("2", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L5: Lambda type inference — Supplier<String>. */
    @Test
    public void lambda_typeInferenceSupplier() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L5",
                new StringResource("l/L5.java",
                    "package l; " +
                    "import java.util.function.*; " +
                    "public class L5 { " +
                    "    public static String test() { " +
                    "        Supplier<String> sup = () -> \"supplied\"; " +
                    "        return sup.get(); " +
                    "    } " +
                    "}")
            );
            assertEquals("supplied", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L6: Nested lambdas — Function composing with another Function. */
    @Test
    public void lambda_nestedLambdas() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L6",
                new StringResource("l/L6.java",
                    "package l; " +
                    "import java.util.function.*; " +
                    "public class L6 { " +
                    "    public static String test() { " +
                    "        Function<String, Function<String, String>> curried = " +
                    "            prefix -> (suffix -> prefix + suffix); " +
                    "        Function<String, String> greeter = (Function<String, String>) curried.apply(\"Hello, \"); " +
                    "        return greeter.apply(\"World\"); " +
                    "    } " +
                    "}")
            );
            assertEquals("Hello, World", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L7: Generic functional interface compiled separately, used with lambda. */
    @Test
    public void lambda_genericFunctionalInterfaceSeparateCompile() {
        try {
            IDynamicCompiler c = compiler();
            // First compile: generic functional interface
            c.compileClass(new StringResource("l/Transform.java",
                "package l; " +
                "@FunctionalInterface " +
                "public interface Transform<T, R> { " +
                "    R apply(T input); " +
                "}")
            );
            // Second compile: user that creates a lambda implementing it
            c.compileClass(new StringResource("l/L7.java",
                "package l; " +
                "public class L7 { " +
                "    public static String test() { " +
                "        Transform<String, Integer> len = (Transform) (s -> ((String)s).length()); " +
                "        return String.valueOf(len.apply(\"hello\")); " +
                "    } " +
                "}")
            );
            ClassLoader cl = c.getClassLoader();
            Object result = cl.loadClass("l.L7").getDeclaredMethod("test").invoke(null);
            assertEquals("5", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L8: Lambda with generic parameters — BiFunction<String, Integer, String>. */
    @Test
    public void lambda_biFunctionWithGenericParams() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L8",
                new StringResource("l/L8.java",
                    "package l; " +
                    "import java.util.function.*; " +
                    "public class L8 { " +
                    "    public static String test() { " +
                    "        BiFunction<String, Integer, String> repeat = " +
                    "            (s, n) -> { " +
                    "                StringBuilder sb = new StringBuilder(); " +
                    "                for (int i = 0; i < (Integer)n; i++) sb.append((String)s); " +
                    "                return sb.toString(); " +
                    "            }; " +
                    "        return (String) repeat.apply(\"ab\", Integer.valueOf(3)); " +
                    "    } " +
                    "}")
            );
            assertEquals("ababab", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L9: Consumer + forEach on generic list (default method). */
    @Test
    public void lambda_consumerForEachDefaultMethod() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "l.L9",
                new StringResource("l/L9.java",
                    "package l; " +
                    "import java.util.*; " +
                    "import java.util.function.*; " +
                    "public class L9 { " +
                    "    public static String test() { " +
                    "        List<String> items = new ArrayList<String>(); " +
                    "        items.add(\"x\"); items.add(\"y\"); items.add(\"z\"); " +
                    "        StringBuilder sb = new StringBuilder(); " +
                    "        items.forEach((Consumer) (o -> sb.append((String)o))); " +
                    "        return sb.toString(); " +
                    "    } " +
                    "}")
            );
            assertEquals("xyz", result);
        } catch (Throwable t) { fail(t); }
    }

    /** L10: Hard combined case — generics + lambdas + separate compilation + stream-like. */
    @Test
    public void lambda_generics_combined_hardCase() {
        try {
            IDynamicCompiler c = compiler();
            // First compile: generic Transformer interface + generic Pipeline class
            c.compileClass(
                new StringResource("l/Mapper.java",
                    "package l; " +
                    "@FunctionalInterface " +
                    "public interface Mapper<T, R> { " +
                    "    R map(T input); " +
                    "}"),
                new StringResource("l/Pipeline.java",
                    "package l; " +
                    "import java.util.*; " +
                    "public class Pipeline<T> { " +
                    "    private List<T> data; " +
                    "    public Pipeline(List<T> data) { this.data = data; } " +
                    "    public <R> Pipeline<R> transform(Mapper<T, R> mapper) { " +
                    "        List<R> out = new ArrayList<R>(); " +
                    "        for (int i = 0; i < data.size(); i++) { " +
                    "            out.add(mapper.map((T) data.get(i))); " +
                    "        } " +
                    "        return new Pipeline<R>(out); " +
                    "    } " +
                    "    public List<T> collect() { return data; } " +
                    "    public int size() { return data.size(); } " +
                    "}")
            );
            // Second compile: user that chains pipeline transformations with lambdas
            c.compileClass(new StringResource("l/L10.java",
                "package l; " +
                "import java.util.*; " +
                "public class L10 { " +
                "    public static String test() { " +
                "        List<String> words = new ArrayList<String>(); " +
                "        words.add(\"hi\"); words.add(\"there\"); words.add(\"world\"); " +
                "        Pipeline<String> p = new Pipeline<String>(words); " +
                "        Pipeline<Integer> lengths = p.transform((Mapper) (s -> Integer.valueOf(((String)s).length()))); " +
                "        Pipeline<String> strs = lengths.transform((Mapper) (n -> String.valueOf(n))); " +
                "        List<String> result = strs.collect(); " +
                "        StringBuilder sb = new StringBuilder(); " +
                "        for (int i = 0; i < result.size(); i++) { " +
                "            if (i > 0) sb.append(\",\"); " +
                "            sb.append((String)result.get(i)); " +
                "        } " +
                "        return sb.toString(); " +
                "    } " +
                "}")
            );
            ClassLoader cl = c.getClassLoader();
            Object result = cl.loadClass("l.L10").getDeclaredMethod("test").invoke(null);
            assertEquals("2,5,5", result);
        } catch (Throwable t) { fail(t); }
    }

    // =========================================================================
    //  EXTRA — Cross-cutting / Edge Cases
    // =========================================================================

    /** E1: Three separate compiles — generic base, generic sub, user. */
    @Test
    public void extra_threeStepSeparateCompilation() {
        try {
            IDynamicCompiler c = compiler();
            // Step 1: compile generic base
            c.compileClass(new StringResource("e/Holder.java",
                "package e; " +
                "public class Holder<T> { " +
                "    private T val; " +
                "    public Holder(T v) { val = v; } " +
                "    public T getVal() { return val; } " +
                "}")
            );
            // Step 2: compile generic subclass that depends on Holder
            c.compileClass(new StringResource("e/NamedHolder.java",
                "package e; " +
                "public class NamedHolder<T> extends Holder<T> { " +
                "    private String name; " +
                "    public NamedHolder(String name, T v) { super(v); this.name = name; } " +
                "    public String getName() { return name; } " +
                "}")
            );
            // Step 3: compile user that depends on NamedHolder
            c.compileClass(new StringResource("e/E1.java",
                "package e; " +
                "public class E1 { " +
                "    public static String test() { " +
                "        NamedHolder<Integer> nh = new NamedHolder<Integer>(\"count\", Integer.valueOf(42)); " +
                "        return nh.getName() + \"=\" + ((Integer) nh.getVal()).toString(); " +
                "    } " +
                "}")
            );
            ClassLoader cl = c.getClassLoader();
            Object result = cl.loadClass("e.E1").getDeclaredMethod("test").invoke(null);
            assertEquals("count=42", result);
        } catch (Throwable t) { fail(t); }
    }

    /** E2: Lambda capturing local variable + generic context. */
    @Test
    public void extra_lambdaCapturingLocalWithGenerics() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "e.E2",
                new StringResource("e/E2.java",
                    "package e; " +
                    "import java.util.function.*; " +
                    "public class E2 { " +
                    "    public static String test() { " +
                    "        String prefix = \"captured:\"; " +
                    "        Function<String, String> f = s -> prefix + s; " +
                    "        return (String) f.apply(\"value\"); " +
                    "    } " +
                    "}")
            );
            assertEquals("captured:value", result);
        } catch (Throwable t) { fail(t); }
    }

    /** E3: Recompile a generic class and verify updated behavior. */
    @Test
    public void extra_recompileGenericClass() {
        try {
            IDynamicCompiler c = compiler();
            // Initial compilation
            c.compileClass(
                new StringResource("e/Wrap.java",
                    "package e; " +
                    "public class Wrap<T> { " +
                    "    private T v; " +
                    "    public Wrap(T v) { this.v = v; } " +
                    "    public String describe() { return \"v1:\" + v.toString(); } " +
                    "}"),
                new StringResource("e/E3.java",
                    "package e; " +
                    "public class E3 { " +
                    "    public static String test() { " +
                    "        Wrap<String> w = new Wrap<String>(\"data\"); " +
                    "        return w.describe(); " +
                    "    } " +
                    "}")
            );

            Object r1 = c.getClassLoader().loadClass("e.E3").getDeclaredMethod("test").invoke(null);
            assertEquals("v1:data", r1);

            // Recompile Wrap with different behavior
            c.recompileClass(new StringResource("e/Wrap.java",
                "package e; " +
                "public class Wrap<T> { " +
                "    private T v; " +
                "    public Wrap(T v) { this.v = v; } " +
                "    public String describe() { return \"v2[\" + v.toString() + \"]\"; } " +
                "}")
            );

            Object r2 = c.getClassLoader().loadClass("e.E3").getDeclaredMethod("test").invoke(null);
            assertEquals("v2[data]", r2);
        } catch (Throwable t) { fail(t); }
    }

    /** E4: Comparable generic interface implementation through IDynamicCompiler. */
    @Test
    public void extra_genericInterfaceImplementation() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "e.E4",
                new StringResource("e/Priority.java",
                    "package e; " +
                    "public class Priority implements Comparable<Priority> { " +
                    "    private int level; " +
                    "    public Priority(int level) { this.level = level; } " +
                    "    public int getLevel() { return level; } " +
                    "    public int compareTo(Object other) { " +
                    "        return this.level - ((Priority) other).level; " +
                    "    } " +
                    "}"),
                new StringResource("e/E4.java",
                    "package e; " +
                    "public class E4 { " +
                    "    public static String test() { " +
                    "        Priority high = new Priority(10); " +
                    "        Priority low = new Priority(1); " +
                    "        return high.compareTo(low) > 0 ? \"high>low\" : \"unexpected\"; " +
                    "    } " +
                    "}")
            );
            assertEquals("high>low", result);
        } catch (Throwable t) { fail(t); }
    }

    /** E5: Stream map + collect through IDynamicCompiler. */
    @Test
    public void extra_streamMapCollect() {
        try {
            IDynamicCompiler c = compiler();
            Object result = compileAndRun(c, "e.E5",
                new StringResource("e/E5.java",
                    "package e; " +
                    "import java.util.*; " +
                    "import java.util.stream.*; " +
                    "public class E5 { " +
                    "    public static String test() { " +
                    "        List<String> words = new ArrayList<String>(); " +
                    "        words.add(\"hello\"); words.add(\"world\"); " +
                    "        String joined = words.stream() " +
                    "            .map(s -> s.toUpperCase()) " +
                    "            .collect(Collectors.joining(\",\")); " +
                    "        return joined; " +
                    "    } " +
                    "}")
            );
            assertEquals("HELLO,WORLD", result);
        } catch (Throwable t) { fail(t); }
    }
}
