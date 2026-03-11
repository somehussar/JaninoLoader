package io.github.somehussar.janinoloader;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended lambda/method-reference test suite targeting cases NOT covered by
 * the 120 tests in {@link JaninoLambdaTest}.
 *
 * <p>Categories covered here:
 * <ul>
 *   <li>Primitive specialization FIs (LongConsumer, DoubleConsumer, etc.)</li>
 *   <li>Cross-type primitive FIs (IntToLongFunction, etc.)</li>
 *   <li>Advanced currying (4+ params)</li>
 *   <li>Intersection type casts with lambdas</li>
 *   <li>Lambda in anonymous/inner classes</li>
 *   <li>Parallel streams</li>
 *   <li>Advanced collectors (toMap, partitioningBy, counting)</li>
 *   <li>Stream terminal operations (findFirst, anyMatch, etc.)</li>
 *   <li>Comparator factory methods (comparing, thenComparing)</li>
 *   <li>Stream.generate / Stream.iterate</li>
 *   <li>Constructor references with generics</li>
 *   <li>Method reference to overloaded methods</li>
 *   <li>Lambda in enum context</li>
 *   <li>Lambda with finally block</li>
 *   <li>Multiple lambdas in array initializer</li>
 *   <li>BooleanSupplier</li>
 *   <li>Recursive lambda patterns</li>
 *   <li>Lambda capturing outer class fields from inner class</li>
 * </ul>
 */
public class JaninoLambdaExtraTest {

    // ========================================================================
    // Infrastructure (same as JaninoLambdaTest)
    // ========================================================================

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
        compiler.setTargetVersion(8);  // Enable Java 8 features (lambdas, default methods, etc.)
        compiler.setIClassLoader(new ClassLoaderIClassLoader(loader));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.compile(sources);
    }

    private Object compileAndRun(String className, String source) throws Throwable {
        Map<String, byte[]> classes = new HashMap<>();
        ClassLoader parent = getClass().getClassLoader();
        ClassLoader loader = createMemoryClassLoader(parent, classes);
        compile(classes, loader, new StringResource(
                className.replace('.', '/') + ".java", source));
        loader = createMemoryClassLoader(parent, classes);
        Class<?> clazz = loader.loadClass(className);
        Method m = clazz.getDeclaredMethod("test");
        return m.invoke(null);
    }

    // ========================================================================
    // CATEGORY A: Primitive Specialization FIs (not in original)
    // ========================================================================

    @Test
    public void extra_longConsumer() {
        try {
            Object result = compileAndRun("t.XA1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XA1 {" +
                "  public static Object test() {" +
                "    final long[] holder = {0L};" +
                "    LongConsumer c = n -> holder[0] = n * 2;" +
                "    c.accept(21L);" +
                "    return holder[0];" +
                "  }" +
                "}");
            assertEquals(42L, result);
        } catch (Throwable t) { fail("extra_longConsumer: " + t); }
    }

    @Test
    public void extra_longSupplier() {
        try {
            Object result = compileAndRun("t.XA3",
                "package t;" +
                "import java.util.function.*;" +
                "public class XA3 {" +
                "  public static Object test() {" +
                "    LongSupplier s = () -> 9999999999L;" +
                "    return s.getAsLong();" +
                "  }" +
                "}");
            assertEquals(9999999999L, result);
        } catch (Throwable t) { fail("extra_longSupplier: " + t); }
    }

    @Test
    public void extra_doubleBinaryOperator() {
        try {
            Object result = compileAndRun("t.XA6",
                "package t;" +
                "import java.util.function.*;" +
                "public class XA6 {" +
                "  public static Object test() {" +
                "    DoubleBinaryOperator op = (a, b) -> a * b;" +
                "    return op.applyAsDouble(3.0, 7.0);" +
                "  }" +
                "}");
            assertEquals(21.0, result);
        } catch (Throwable t) { fail("extra_doubleBinaryOperator: " + t); }
    }

    @Test
    public void extra_booleanSupplier() {
        try {
            Object result = compileAndRun("t.XA7",
                "package t;" +
                "import java.util.function.*;" +
                "public class XA7 {" +
                "  public static Object test() {" +
                "    BooleanSupplier bs = () -> 5 > 3;" +
                "    return bs.getAsBoolean();" +
                "  }" +
                "}");
            assertEquals(true, result);
        } catch (Throwable t) { fail("extra_booleanSupplier: " + t); }
    }

    @Test
    public void extra_doublePredicate() {
        try {
            Object result = compileAndRun("t.XA8",
                "package t;" +
                "import java.util.function.*;" +
                "public class XA8 {" +
                "  public static Object test() {" +
                "    DoublePredicate p = d -> d > 0.0 && d < 1.0;" +
                "    return p.test(0.5) + \",\" + p.test(1.5);" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("extra_doublePredicate: " + t); }
    }

    // ========================================================================
    // CATEGORY B: Cross-Type Primitive FIs
    // ========================================================================

    @Test
    public void extra_intToLongFunction() {
        try {
            Object result = compileAndRun("t.XB1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XB1 {" +
                "  public static Object test() {" +
                "    IntToLongFunction f = n -> (long) n * 1000000L;" +
                "    return f.applyAsLong(42);" +
                "  }" +
                "}");
            assertEquals(42000000L, result);
        } catch (Throwable t) { fail("extra_intToLongFunction: " + t); }
    }

    @Test
    public void extra_objIntConsumer() {
        try {
            Object result = compileAndRun("t.XB5",
                "package t;" +
                "import java.util.function.*;" +
                "public class XB5 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    ObjIntConsumer<String> c = (s, n) -> {" +
                "      for (int i = 0; i < n; i++) sb.append(s);" +
                "    };" +
                "    c.accept(\"ab\", 3);" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("ababab", result);
        } catch (Throwable t) { fail("extra_objIntConsumer: " + t); }
    }

    @Test
    public void extra_toIntBiFunction() {
        try {
            Object result = compileAndRun("t.XB6",
                "package t;" +
                "import java.util.function.*;" +
                "public class XB6 {" +
                "  public static Object test() {" +
                "    ToIntBiFunction<String, String> f = (a, b) -> a.length() + b.length();" +
                "    return f.applyAsInt(\"hello\", \"world\");" +
                "  }" +
                "}");
            assertEquals(10, result);
        } catch (Throwable t) { fail("extra_toIntBiFunction: " + t); }
    }

    // ========================================================================
    // CATEGORY C: Advanced Currying (4+ params)
    // ========================================================================

    @Test
    public void extra_curriedFourParam() {
        try {
            Object result = compileAndRun("t.XC1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XC1 {" +
                "  public static Object test() {" +
                "    Function<Integer, Function<Integer, Function<Integer, Function<Integer, Integer>>>> f =" +
                "      a -> b -> c -> d -> a + b + c + d;" +
                "    return f.apply(1).apply(2).apply(3).apply(4);" +
                "  }" +
                "}");
            assertEquals(10, result);
        } catch (Throwable t) { fail("extra_curriedFourParam: " + t); }
    }

    @Test
    public void extra_curriedFiveParamString() {
        try {
            Object result = compileAndRun("t.XC2",
                "package t;" +
                "import java.util.function.*;" +
                "public class XC2 {" +
                "  public static Object test() {" +
                "    Function<String, Function<String, Function<String, Function<String, Function<String, String>>>>> f =" +
                "      a -> b -> c -> d -> e -> a + b + c + d + e;" +
                "    return f.apply(\"a\").apply(\"b\").apply(\"c\").apply(\"d\").apply(\"e\");" +
                "  }" +
                "}");
            assertEquals("abcde", result);
        } catch (Throwable t) { fail("extra_curriedFiveParamString: " + t); }
    }

    // ========================================================================
    // CATEGORY D: Intersection Type Casts
    // ========================================================================

    @Disabled("Janino limitation: Intersection type casts (Type & Type) not supported by parser")
    @Test
    public void extra_intersectionCastSerializable() {
        try {
            Object result = compileAndRun("t.XD1",
                "package t;" +
                "import java.io.*;" +
                "import java.util.function.*;" +
                "public class XD1 {" +
                "  public static Object test() {" +
                "    Comparator<String> c = (Comparator<String> & Serializable) (a, b) -> a.length() - b.length();" +
                "    return c instanceof Serializable ? \"serializable\" : \"not\";" +
                "  }" +
                "}");
            assertEquals("serializable", result);
        } catch (Throwable t) { fail("extra_intersectionCastSerializable: " + t); }
    }

    @Disabled("Janino limitation: Intersection type casts (Type & Type) not supported by parser")
    @Test
    public void extra_intersectionCastRunnable() {
        try {
            Object result = compileAndRun("t.XD2",
                "package t;" +
                "import java.io.*;" +
                "public class XD2 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Runnable r = (Runnable & Serializable) (() -> sb.append(\"intersected\"));" +
                "    r.run();" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("intersected", result);
        } catch (Throwable t) { fail("extra_intersectionCastRunnable: " + t); }
    }

    // ========================================================================
    // CATEGORY E: Lambda in Anonymous/Inner Class
    // ========================================================================

    @Test
    public void extra_lambdaInAnonymousClass() {
        try {
            Object result = compileAndRun("t.XE1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XE1 {" +
                "  interface Greeter { String greet(); }" +
                "  public static Object test() {" +
                "    Greeter g = new Greeter() {" +
                "      public String greet() {" +
                "        Function<String, String> f = s -> s + \"!\";" +
                "        return f.apply(\"hello\");" +
                "      }" +
                "    };" +
                "    return g.greet();" +
                "  }" +
                "}");
            assertEquals("hello!", result);
        } catch (Throwable t) { fail("extra_lambdaInAnonymousClass: " + t); }
    }

    @Test
    public void extra_lambdaCapturingOuterFromInner() {
        try {
            Object result = compileAndRun("t.XE2",
                "package t;" +
                "import java.util.function.*;" +
                "public class XE2 {" +
                "  private String prefix = \"outer:\";" +
                "  class Inner {" +
                "    String transform(String s) {" +
                "      Function<String, String> f = x -> prefix + x;" +
                "      return f.apply(s);" +
                "    }" +
                "  }" +
                "  public static Object test() {" +
                "    XE2 outer = new XE2();" +
                "    return outer.new Inner().transform(\"val\");" +
                "  }" +
                "}");
            assertEquals("outer:val", result);
        } catch (Throwable t) { fail("extra_lambdaCapturingOuterFromInner: " + t); }
    }

    @Test
    public void extra_lambdaInConstructor() {
        try {
            Object result = compileAndRun("t.XE3",
                "package t;" +
                "import java.util.function.*;" +
                "public class XE3 {" +
                "  private String value;" +
                "  public XE3(String input) {" +
                "    Function<String, String> f = s -> \"[\" + s + \"]\";" +
                "    this.value = f.apply(input);" +
                "  }" +
                "  public static Object test() {" +
                "    XE3 obj = new XE3(\"data\");" +
                "    return obj.value;" +
                "  }" +
                "}");
            assertEquals("[data]", result);
        } catch (Throwable t) { fail("extra_lambdaInConstructor: " + t); }
    }

    @Test
    public void extra_lambdaInStaticBlock() {
        try {
            Object result = compileAndRun("t.XE4",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class XE4 {" +
                "  static final List<String> ITEMS;" +
                "  static {" +
                "    List<String> raw = Arrays.asList(\"banana\", \"apple\", \"cherry\");" +
                "    List<String> sorted = new ArrayList<String>(raw);" +
                "    sorted.sort((a, b) -> a.compareTo(b));" +
                "    ITEMS = sorted;" +
                "  }" +
                "  public static Object test() {" +
                "    return ITEMS.toString();" +
                "  }" +
                "}");
            assertEquals("[apple, banana, cherry]", result);
        } catch (Throwable t) { fail("extra_lambdaInStaticBlock: " + t); }
    }

    // ========================================================================
    // CATEGORY F: Parallel Streams
    // ========================================================================

    @Test
    public void extra_parallelStreamFilter() {
        try {
            Object result = compileAndRun("t.XF1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XF1 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);" +
                "    long count = list.parallelStream().filter(n -> n % 2 == 0).count();" +
                "    return count;" +
                "  }" +
                "}");
            assertEquals(5L, result);
        } catch (Throwable t) { fail("extra_parallelStreamFilter: " + t); }
    }

    @Test
    public void extra_parallelStreamReduce() {
        try {
            Object result = compileAndRun("t.XF2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XF2 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);" +
                "    int sum = list.parallelStream().reduce(0, (a, b) -> a + b, (a, b) -> a + b);" +
                "    return sum;" +
                "  }" +
                "}");
            assertEquals(15, result);
        } catch (Throwable t) { fail("extra_parallelStreamReduce: " + t); }
    }

    // ========================================================================
    // CATEGORY G: Advanced Collectors
    // ========================================================================

    @Test
    public void extra_collectorsToMap() {
        try {
            Object result = compileAndRun("t.XG1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XG1 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"aa\", \"bbb\", \"c\");" +
                "    Map<String, Integer> map = list.stream()" +
                "      .collect(Collectors.toMap(s -> s, s -> s.length()));" +
                "    return map.get(\"bbb\");" +
                "  }" +
                "}");
            assertEquals(3, result);
        } catch (Throwable t) { fail("extra_collectorsToMap: " + t); }
    }

    @Test
    public void extra_collectorsPartitioningBy() {
        try {
            Object result = compileAndRun("t.XG2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XG2 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);" +
                "    Map<Boolean, List<Integer>> parts = list.stream()" +
                "      .collect(Collectors.partitioningBy(n -> n % 2 == 0));" +
                "    return parts.get(true).size() + \",\" + parts.get(false).size();" +
                "  }" +
                "}");
            assertEquals("3,3", result);
        } catch (Throwable t) { fail("extra_collectorsPartitioningBy: " + t); }
    }

    @Test
    public void extra_collectorsCounting() {
        try {
            Object result = compileAndRun("t.XG3",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XG3 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"a\", \"b\", \"a\", \"c\", \"a\");" +
                "    Map<String, Long> counts = list.stream()" +
                "      .collect(Collectors.groupingBy(s -> s, Collectors.counting()));" +
                "    return counts.get(\"a\");" +
                "  }" +
                "}");
            assertEquals(3L, result);
        } catch (Throwable t) { fail("extra_collectorsCounting: " + t); }
    }

    // ========================================================================
    // CATEGORY H: Stream Terminal Operations
    // ========================================================================

    @Test
    public void extra_streamFindFirst() {
        try {
            Object result = compileAndRun("t.XH1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XH1 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"apple\", \"banana\", \"avocado\");" +
                "    String found = list.stream().filter(s -> s.startsWith(\"b\")).findFirst().orElse(\"none\");" +
                "    return found;" +
                "  }" +
                "}");
            assertEquals("banana", result);
        } catch (Throwable t) { fail("extra_streamFindFirst: " + t); }
    }

    @Test
    public void extra_streamAnyMatchAllMatchNoneMatch() {
        try {
            Object result = compileAndRun("t.XH2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XH2 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Arrays.asList(2, 4, 6, 8);" +
                "    boolean any = list.stream().anyMatch(n -> n > 5);" +
                "    boolean all = list.stream().allMatch(n -> n % 2 == 0);" +
                "    boolean none = list.stream().noneMatch(n -> n < 0);" +
                "    return any + \",\" + all + \",\" + none;" +
                "  }" +
                "}");
            assertEquals("true,true,true", result);
        } catch (Throwable t) { fail("extra_streamAnyMatchAllMatchNoneMatch: " + t); }
    }

    @Test
    public void extra_streamPeek() {
        try {
            Object result = compileAndRun("t.XH3",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XH3 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    List<String> list = Arrays.asList(\"a\", \"b\", \"c\");" +
                "    long count = list.stream().peek(s -> sb.append(s)).count();" +
                "    return sb.toString() + \":\" + count;" +
                "  }" +
                "}");
            assertEquals("abc:3", result);
        } catch (Throwable t) { fail("extra_streamPeek: " + t); }
    }

    // ========================================================================
    // CATEGORY I: Comparator Factory Methods
    // ========================================================================

    @Test
    public void extra_comparatorComparing() {
        try {
            Object result = compileAndRun("t.XI1",
                "package t;" +
                "import java.util.*;" +
                "public class XI1 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"bb\", \"a\", \"ccc\");" +
                "    list.sort(Comparator.comparing(s -> s.length()));" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[a, bb, ccc]", result);
        } catch (Throwable t) { fail("extra_comparatorComparing: " + t); }
    }

    @Test
    public void extra_comparatorThenComparing() {
        try {
            Object result = compileAndRun("t.XI2",
                "package t;" +
                "import java.util.*;" +
                "public class XI2 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"ba\", \"ab\", \"bb\", \"aa\");" +
                "    list.sort(Comparator.comparingInt((String s) -> s.length())" +
                "      .thenComparing((String s) -> s));" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[aa, ab, ba, bb]", result);
        } catch (Throwable t) { fail("extra_comparatorThenComparing: " + t); }
    }

    // ========================================================================
    // CATEGORY J: Stream.generate / Stream.iterate
    // ========================================================================

    @Test
    public void extra_streamGenerate() {
        try {
            Object result = compileAndRun("t.XJ1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XJ1 {" +
                "  public static Object test() {" +
                "    List<String> list = Stream.generate(() -> \"x\").limit(4).collect(Collectors.toList());" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[x, x, x, x]", result);
        } catch (Throwable t) { fail("extra_streamGenerate: " + t); }
    }

    @Test
    public void extra_streamIterate() {
        try {
            Object result = compileAndRun("t.XJ2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XJ2 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Stream.iterate(1, n -> n * 2).limit(5).collect(Collectors.toList());" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[1, 2, 4, 8, 16]", result);
        } catch (Throwable t) { fail("extra_streamIterate: " + t); }
    }

    // ========================================================================
    // CATEGORY K: Constructor References (advanced)
    // ========================================================================

    @Test
    public void extra_constructorRefWithGenericClass() {
        try {
            Object result = compileAndRun("t.XK1",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class XK1 {" +
                "  static class Pair<A, B> {" +
                "    A first; B second;" +
                "    Pair(A a, B b) { this.first = a; this.second = b; }" +
                "    public String toString() { return first + \":\" + second; }" +
                "  }" +
                "  interface PairFactory<A, B> { Pair<A, B> create(A a, B b); }" +
                "  public static Object test() {" +
                "    PairFactory<String, Integer> factory = Pair::new;" +
                "    Pair<String, Integer> p = factory.create(\"age\", 25);" +
                "    return p.toString();" +
                "  }" +
                "}");
            assertEquals("age:25", result);
        } catch (Throwable t) { fail("extra_constructorRefWithGenericClass: " + t); }
    }

    @Test
    public void extra_constructorRefArrayCollect() {
        try {
            Object result = compileAndRun("t.XK2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XK2 {" +
                "  public static Object test() {" +
                "    String[] arr = Arrays.asList(\"a\", \"b\", \"c\").stream().toArray(String[]::new);" +
                "    return arr.length + \":\" + arr[1];" +
                "  }" +
                "}");
            assertEquals("3:b", result);
        } catch (Throwable t) { fail("extra_constructorRefArrayCollect: " + t); }
    }

    // ========================================================================
    // CATEGORY L: Method Reference Edge Cases
    // ========================================================================

    @Test
    public void extra_methodRefToOverloadedMethod() {
        try {
            Object result = compileAndRun("t.XL1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XL1 {" +
                "  public static Object test() {" +
                "    Function<String, String> f = String::valueOf;" +
                "    return f.apply(\"test\");" +
                "  }" +
                "}");
            assertEquals("test", result);
        } catch (Throwable t) { fail("extra_methodRefToOverloadedMethod: " + t); }
    }

    @Test
    public void extra_methodRefBoundWithArgs() {
        try {
            Object result = compileAndRun("t.XL2",
                "package t;" +
                "import java.util.function.*;" +
                "public class XL2 {" +
                "  public static Object test() {" +
                "    String str = \"hello world\";" +
                "    Function<String, Boolean> f = str::contains;" +
                "    return f.apply(\"world\") + \",\" + f.apply(\"xyz\");" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("extra_methodRefBoundWithArgs: " + t); }
    }

    // ========================================================================
    // CATEGORY M: Lambda with Finally / Try-with-resources
    // ========================================================================

    @Test
    public void extra_lambdaWithFinally() {
        try {
            Object result = compileAndRun("t.XM1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XM1 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Function<String, String> f = s -> {" +
                "      try { return s.toUpperCase(); }" +
                "      finally { sb.append(\"finally\"); }" +
                "    };" +
                "    String result = f.apply(\"hello\");" +
                "    return result + \":\" + sb.toString();" +
                "  }" +
                "}");
            assertEquals("HELLO:finally", result);
        } catch (Throwable t) { fail("extra_lambdaWithFinally: " + t); }
    }

    @Test
    public void extra_lambdaThrowsCheckedInCustomFI() {
        try {
            Object result = compileAndRun("t.XM2",
                "package t;" +
                "public class XM2 {" +
                "  interface ThrowingFunction<T, R> { R apply(T t) throws Exception; }" +
                "  static <T, R> R safeApply(ThrowingFunction<T, R> f, T val) {" +
                "    try { return f.apply(val); }" +
                "    catch (Exception e) { return null; }" +
                "  }" +
                "  public static Object test() {" +
                "    String result = safeApply(s -> {" +
                "      if (s.isEmpty()) throw new Exception(\"empty!\");" +
                "      return s.toUpperCase();" +
                "    }, \"hello\");" +
                "    return result;" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("extra_lambdaThrowsCheckedInCustomFI: " + t); }
    }

    // ========================================================================
    // CATEGORY N: Multiple Lambdas in Array/Collection
    // ========================================================================

    @Test
    public void extra_lambdaArrayInit() {
        try {
            Object result = compileAndRun("t.XN1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XN1 {" +
                "  public static Object test() {" +
                "    UnaryOperator<String>[] ops = new UnaryOperator[]{" +
                "      s -> ((String) s).toUpperCase()," +
                "      s -> ((String) s) + \"!\"," +
                "      s -> \"[\" + ((String) s) + \"]\"" +
                "    };" +
                "    String result = \"hello\";" +
                "    for (UnaryOperator<String> op : ops) { result = op.apply(result); }" +
                "    return result;" +
                "  }" +
                "}");
            assertEquals("[HELLO!]", result);
        } catch (Throwable t) { fail("extra_lambdaArrayInit: " + t); }
    }

    // ========================================================================
    // CATEGORY O: Method Overloading Ambiguity with Lambdas
    // ========================================================================

    @Test
    public void extra_overloadConsumerVsFunction() {
        try {
            Object result = compileAndRun("t.XO1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XO1 {" +
                "  static String process(Consumer<String> c) { c.accept(\"x\"); return \"consumer\"; }" +
                "  static String process(Function<String, Integer> f) { f.apply(\"x\"); return \"function\"; }" +
                "  public static Object test() {" +
                "    return process((Consumer<String>) s -> System.out.println(s));" +
                "  }" +
                "}");
            assertEquals("consumer", result);
        } catch (Throwable t) { fail("extra_overloadConsumerVsFunction: " + t); }
    }

    // ========================================================================
    // CATEGORY P: Recursive / Self-referential Lambda Patterns
    // ========================================================================

    @Test
    public void extra_recursiveFibonacciViaHolder() {
        try {
            Object result = compileAndRun("t.XP1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XP1 {" +
                "  public static Object test() {" +
                "    final Function<Integer, Integer>[] fib = new Function[1];" +
                "    fib[0] = n -> n <= 1 ? n : fib[0].apply(n - 1) + fib[0].apply(n - 2);" +
                "    return fib[0].apply(10);" +
                "  }" +
                "}");
            assertEquals(55, result);
        } catch (Throwable t) { fail("extra_recursiveFibonacciViaHolder: " + t); }
    }

    // ========================================================================
    // CATEGORY Q: Enum with Lambda
    // ========================================================================

    @Test
    public void extra_enumWithLambdaField() {
        try {
            Object result = compileAndRun("t.XQ1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XQ1 {" +
                "  enum Op {" +
                "    ADD((a, b) -> a + b)," +
                "    MUL((a, b) -> a * b);" +
                "    private final IntBinaryOperator op;" +
                "    Op(IntBinaryOperator op) { this.op = op; }" +
                "    int apply(int a, int b) { return op.applyAsInt(a, b); }" +
                "  }" +
                "  public static Object test() {" +
                "    return Op.ADD.apply(3, 4) + \",\" + Op.MUL.apply(3, 4);" +
                "  }" +
                "}");
            assertEquals("7,12", result);
        } catch (Throwable t) { fail("extra_enumWithLambdaField: " + t); }
    }

    // ========================================================================
    // CATEGORY R: Stream.collect with custom reduction
    // ========================================================================

    @Test
    public void extra_reduceWithCombiner() {
        try {
            Object result = compileAndRun("t.XR1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XR1 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"a\", \"bb\", \"ccc\", \"dddd\");" +
                "    int totalLen = list.stream().reduce(0, (acc, s) -> acc + s.length(), (a, b) -> a + b);" +
                "    return totalLen;" +
                "  }" +
                "}");
            assertEquals(10, result);
        } catch (Throwable t) { fail("extra_reduceWithCombiner: " + t); }
    }

    @Test
    public void extra_collectThreeArgForm() {
        try {
            Object result = compileAndRun("t.XR2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XR2 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"hello\", \"world\");" +
                "    StringBuilder result = list.stream().collect(" +
                "      () -> new StringBuilder()," +
                "      (sb, s) -> sb.append(s).append(\" \")," +
                "      (sb1, sb2) -> sb1.append(sb2)" +
                "    );" +
                "    return result.toString().trim();" +
                "  }" +
                "}");
            assertEquals("hello world", result);
        } catch (Throwable t) { fail("extra_collectThreeArgForm: " + t); }
    }

    // ========================================================================
    // CATEGORY S: Map.forEach / Map.replaceAll / Map.computeIfAbsent
    // ========================================================================

    @Test
    public void extra_mapForEach() {
        try {
            Object result = compileAndRun("t.XS1",
                "package t;" +
                "import java.util.*;" +
                "public class XS1 {" +
                "  public static Object test() {" +
                "    Map<String, Integer> map = new LinkedHashMap<String, Integer>();" +
                "    map.put(\"a\", 1); map.put(\"b\", 2);" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    map.forEach((k, v) -> sb.append(k).append(\"=\").append(v).append(\",\"));" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("a=1,b=2,", result);
        } catch (Throwable t) { fail("extra_mapForEach: " + t); }
    }

    @Test
    public void extra_mapComputeIfAbsent() {
        try {
            Object result = compileAndRun("t.XS2",
                "package t;" +
                "import java.util.*;" +
                "public class XS2 {" +
                "  public static Object test() {" +
                "    Map<String, List<String>> map = new HashMap<String, List<String>>();" +
                "    map.computeIfAbsent(\"key\", k -> new ArrayList<String>()).add(\"val1\");" +
                "    map.computeIfAbsent(\"key\", k -> new ArrayList<String>()).add(\"val2\");" +
                "    return map.get(\"key\").toString();" +
                "  }" +
                "}");
            assertEquals("[val1, val2]", result);
        } catch (Throwable t) { fail("extra_mapComputeIfAbsent: " + t); }
    }

    @Test
    public void extra_mapReplaceAll() {
        try {
            Object result = compileAndRun("t.XS3",
                "package t;" +
                "import java.util.*;" +
                "public class XS3 {" +
                "  public static Object test() {" +
                "    Map<String, String> map = new LinkedHashMap<String, String>();" +
                "    map.put(\"a\", \"hello\"); map.put(\"b\", \"world\");" +
                "    map.replaceAll((k, v) -> v.toUpperCase());" +
                "    return map.get(\"a\") + \",\" + map.get(\"b\");" +
                "  }" +
                "}");
            assertEquals("HELLO,WORLD", result);
        } catch (Throwable t) { fail("extra_mapReplaceAll: " + t); }
    }

    // ========================================================================
    // CATEGORY T: Effectively-final in for-each, method ref chains
    // ========================================================================

    @Test
    public void extra_lambdaCaptureForEachVar() {
        try {
            Object result = compileAndRun("t.XT1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.function.*;" +
                "public class XT1 {" +
                "  public static Object test() {" +
                "    List<Supplier<String>> suppliers = new ArrayList<Supplier<String>>();" +
                "    for (String s : new String[]{\"a\", \"b\", \"c\"}) {" +
                "      suppliers.add(() -> s.toUpperCase());" +
                "    }" +
                "    StringBuilder sb = new StringBuilder();" +
                "    for (Supplier<String> sup : suppliers) sb.append(sup.get());" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("ABC", result);
        } catch (Throwable t) { fail("extra_lambdaCaptureForEachVar: " + t); }
    }

    @Test
    public void extra_methodRefChainOnStream() {
        try {
            Object result = compileAndRun("t.XT2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class XT2 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"hello\", \"world\");" +
                "    List<String> upper = list.stream().map(String::toUpperCase).collect(Collectors.toList());" +
                "    return upper.toString();" +
                "  }" +
                "}");
            assertEquals("[HELLO, WORLD]", result);
        } catch (Throwable t) { fail("extra_methodRefChainOnStream: " + t); }
    }

    // ========================================================================
    // CATEGORY U: Functional composition chains
    // ========================================================================

    @Test
    public void extra_functionIdentity() {
        try {
            Object result = compileAndRun("t.XU1",
                "package t;" +
                "import java.util.function.*;" +
                "public class XU1 {" +
                "  public static Object test() {" +
                "    Function<String, String> id = Function.identity();" +
                "    return id.apply(\"same\");" +
                "  }" +
                "}");
            assertEquals("same", result);
        } catch (Throwable t) { fail("extra_functionIdentity: " + t); }
    }

    @Test
    public void extra_predicateIsEqual() {
        try {
            Object result = compileAndRun("t.XU2",
                "package t;" +
                "import java.util.function.*;" +
                "public class XU2 {" +
                "  public static Object test() {" +
                "    Predicate<String> isHello = Predicate.isEqual(\"hello\");" +
                "    return isHello.test(\"hello\") + \",\" + isHello.test(\"world\");" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("extra_predicateIsEqual: " + t); }
    }

    @Test
    public void extra_longChainedAndThen() {
        try {
            Object result = compileAndRun("t.XU3",
                "package t;" +
                "import java.util.function.*;" +
                "public class XU3 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> add1 = x -> x + 1;" +
                "    Function<Integer, Integer> mul2 = x -> x * 2;" +
                "    Function<Integer, Integer> sub3 = x -> x - 3;" +
                "    Function<Integer, Integer> chain = add1.andThen(mul2).andThen(sub3);" +
                "    return chain.apply(5);" +
                "  }" +
                "}");
            // (5+1)*2 - 3 = 9
            assertEquals(9, result);
        } catch (Throwable t) { fail("extra_longChainedAndThen: " + t); }
    }

    // ========================================================================
    // CATEGORY V: List.removeIf / replaceAll with lambdas
    // ========================================================================

    @Test
    public void extra_listRemoveIf() {
        try {
            Object result = compileAndRun("t.XV1",
                "package t;" +
                "import java.util.*;" +
                "public class XV1 {" +
                "  public static Object test() {" +
                "    List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));" +
                "    list.removeIf(n -> n % 2 != 0);" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[2, 4, 6]", result);
        } catch (Throwable t) { fail("extra_listRemoveIf: " + t); }
    }

    @Test
    public void extra_listReplaceAll() {
        try {
            Object result = compileAndRun("t.XV2",
                "package t;" +
                "import java.util.*;" +
                "public class XV2 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>(Arrays.asList(\"a\", \"b\", \"c\"));" +
                "    list.replaceAll(s -> s.toUpperCase());" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[A, B, C]", result);
        } catch (Throwable t) { fail("extra_listReplaceAll: " + t); }
    }
}
