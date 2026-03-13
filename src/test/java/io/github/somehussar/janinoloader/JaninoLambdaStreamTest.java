package io.github.somehussar.janinoloader;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Java Streams, Collections, Maps, Arrays operations
 * with lambdas and method references in the Janino compiler.
 *
 * <p>Each test exercises a specific Stream/Collection feature and documents whether
 * Janino supports it. Tests that fail document Janino limitations — failures are
 * expected for unsupported features and should NOT be "fixed".</p>
 *
 * <p>Results from this suite inform which Stream/Collection features are safe to use
 * in dynamically-compiled scripts via JaninoLoader.</p>
 */
public class JaninoLambdaStreamTest {

    // ========================================================================
    // Infrastructure (same pattern as JaninoLambdaTest)
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
        compiler.setTargetVersion(8);
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
    // CATEGORY 1: Arrays Stream (6 tests)
    // ========================================================================

    @Test
    public void arrays_stream_intSum() {
        try {
            Object result = compileAndRun("t.AS1",
                "package t;" +
                "import java.util.Arrays;" +
                "public class AS1 {" +
                "  public static Object test() {" +
                "    int[] arr = {1, 2, 3, 4, 5};" +
                "    return Arrays.stream(arr).sum();" +
                "  }" +
                "}");
            assertEquals(15, result);
        } catch (Throwable t) { fail("arrays_stream_intSum: " + t); }
    }

    @Test
    public void arrays_stream_doubleAverage() {
        try {
            Object result = compileAndRun("t.AS2",
                "package t;" +
                "import java.util.Arrays;" +
                "public class AS2 {" +
                "  public static Object test() {" +
                "    double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0};" +
                "    return Arrays.stream(arr).average().orElse(0.0);" +
                "  }" +
                "}");
            assertEquals(3.0, result);
        } catch (Throwable t) { fail("arrays_stream_doubleAverage: " + t); }
    }

    @Test
    public void arrays_stream_objectFilterToArray() {
        try {
            Object result = compileAndRun("t.AS3",
                "package t;" +
                "import java.util.Arrays;" +
                "public class AS3 {" +
                "  public static Object test() {" +
                "    String[] arr = {\"a\", \"bb\", \"ccc\", \"d\"};" +
                "    Object[] filtered = Arrays.stream(arr).filter(s -> ((String)s).length() > 1).toArray();" +
                "    return Arrays.toString(filtered);" +
                "  }" +
                "}");
            assertEquals("[bb, ccc]", result);
        } catch (Throwable t) { fail("arrays_stream_objectFilterToArray: " + t); }
    }

    @Test
    public void arrays_stream_stringMapToUpperCase() {
        try {
            Object result = compileAndRun("t.AS4",
                "package t;" +
                "import java.util.Arrays;" +
                "import java.util.stream.Collectors;" +
                "public class AS4 {" +
                "  public static Object test() {" +
                "    String[] arr = {\"hello\", \"world\"};" +
                "    return Arrays.stream(arr).map(s -> ((String)s).toUpperCase()).collect(Collectors.joining(\",\"));" +
                "  }" +
                "}");
            assertEquals("HELLO,WORLD", result);
        } catch (Throwable t) { fail("arrays_stream_stringMapToUpperCase: " + t); }
    }

    @Test
    public void arrays_stream_longReduceSum() {
        try {
            Object result = compileAndRun("t.AS5",
                "package t;" +
                "import java.util.Arrays;" +
                "public class AS5 {" +
                "  public static Object test() {" +
                "    long[] arr = {10L, 20L, 30L};" +
                "    return Arrays.stream(arr).reduce(0L, (a, b) -> a + b);" +
                "  }" +
                "}");
            assertEquals(60L, result);
        } catch (Throwable t) { fail("arrays_stream_longReduceSum: " + t); }
    }

    @Test
    public void arrays_stream_intSorted() {
        try {
            Object result = compileAndRun("t.AS6",
                "package t;" +
                "import java.util.Arrays;" +
                "public class AS6 {" +
                "  public static Object test() {" +
                "    int[] arr = {5, 3, 1, 4, 2};" +
                "    return Arrays.toString(Arrays.stream(arr).sorted().toArray());" +
                "  }" +
                "}");
            assertEquals("[1, 2, 3, 4, 5]", result);
        } catch (Throwable t) { fail("arrays_stream_intSorted: " + t); }
    }

    // ========================================================================
    // CATEGORY 2: Collection Streams (8 tests)
    // ========================================================================

    @Test
    public void collection_stream_filterMapCollect() {
        try {
            Object result = compileAndRun("t.CS1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CS1 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"a\"); list.add(\"bb\"); list.add(\"ccc\"); list.add(\"d\");" +
                "    List result = list.stream()" +
                "      .filter(s -> ((String)s).length() > 1)" +
                "      .map(s -> ((String)s).toUpperCase())" +
                "      .collect(Collectors.toList());" +
                "    return result.toString();" +
                "  }" +
                "}");
            assertEquals("[BB, CCC]", result);
        } catch (Throwable t) { fail("collection_stream_filterMapCollect: " + t); }
    }

    @Test
    public void collection_stream_distinctSortedCollect() {
        try {
            Object result = compileAndRun("t.CS2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CS2 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"c\"); list.add(\"a\"); list.add(\"b\"); list.add(\"a\"); list.add(\"c\");" +
                "    List result = list.stream().distinct().sorted().collect(Collectors.toList());" +
                "    return result.toString();" +
                "  }" +
                "}");
            assertEquals("[a, b, c]", result);
        } catch (Throwable t) { fail("collection_stream_distinctSortedCollect: " + t); }
    }

    @Test
    public void collection_removeIf() {
        try {
            Object result = compileAndRun("t.CS3",
                "package t;" +
                "import java.util.*;" +
                "public class CS3 {" +
                "  public static Object test() {" +
                "    List<Integer> list = new ArrayList<Integer>();" +
                "    list.add(1); list.add(6); list.add(3); list.add(8); list.add(2);" +
                "    list.removeIf(e -> ((Integer)e).intValue() > 5);" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[1, 3, 2]", result);
        } catch (Throwable t) { fail("collection_removeIf: " + t); }
    }

    @Test
    public void collection_replaceAll() {
        try {
            Object result = compileAndRun("t.CS4",
                "package t;" +
                "import java.util.*;" +
                "public class CS4 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"hello\"); list.add(\"world\");" +
                "    list.replaceAll(s -> ((String)s).toUpperCase());" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[HELLO, WORLD]", result);
        } catch (Throwable t) { fail("collection_replaceAll: " + t); }
    }

    @Test
    public void collection_forEach() {
        try {
            Object result = compileAndRun("t.CS5",
                "package t;" +
                "import java.util.*;" +
                "public class CS5 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"a\"); list.add(\"b\"); list.add(\"c\");" +
                "    StringBuilder sb = new StringBuilder();" +
                "    list.forEach(s -> sb.append(s));" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("abc", result);
        } catch (Throwable t) { fail("collection_forEach: " + t); }
    }

    @Test
    public void collection_stream_count() {
        try {
            Object result = compileAndRun("t.CS6",
                "package t;" +
                "import java.util.*;" +
                "public class CS6 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"a\"); list.add(\"bb\"); list.add(\"ccc\");" +
                "    return list.stream().filter(s -> ((String)s).length() > 1).count();" +
                "  }" +
                "}");
            assertEquals(2L, result);
        } catch (Throwable t) { fail("collection_stream_count: " + t); }
    }

    @Test
    public void collection_stream_findFirst() {
        try {
            Object result = compileAndRun("t.CS7",
                "package t;" +
                "import java.util.*;" +
                "public class CS7 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"a\"); list.add(\"bb\"); list.add(\"ccc\");" +
                "    return list.stream().filter(s -> ((String)s).length() > 1).findFirst().orElse(\"none\");" +
                "  }" +
                "}");
            assertEquals("bb", result);
        } catch (Throwable t) { fail("collection_stream_findFirst: " + t); }
    }

    @Test
    public void collection_stream_anyMatch() {
        try {
            Object result = compileAndRun("t.CS8",
                "package t;" +
                "import java.util.*;" +
                "public class CS8 {" +
                "  public static Object test() {" +
                "    List<Integer> list = new ArrayList<Integer>();" +
                "    list.add(1); list.add(2); list.add(3);" +
                "    boolean any = list.stream().anyMatch(n -> ((Integer)n).intValue() > 2);" +
                "    boolean all = list.stream().allMatch(n -> ((Integer)n).intValue() > 0);" +
                "    boolean none = list.stream().noneMatch(n -> ((Integer)n).intValue() > 5);" +
                "    return any + \",\" + all + \",\" + none;" +
                "  }" +
                "}");
            assertEquals("true,true,true", result);
        } catch (Throwable t) { fail("collection_stream_anyMatch: " + t); }
    }

    // ========================================================================
    // CATEGORY 3: Map Streams (8 tests)
    // ========================================================================

    @Test
    public void map_entrySet_stream_getKeys() {
        try {
            Object result = compileAndRun("t.MS1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class MS1 {" +
                "  public static Object test() {" +
                "    Map<String, Integer> map = new TreeMap<String, Integer>();" +
                "    map.put(\"a\", 1); map.put(\"b\", 2); map.put(\"c\", 3);" +
                "    List keys = map.entrySet().stream()" +
                "      .map(e -> ((Map.Entry)e).getKey())" +
                "      .collect(Collectors.toList());" +
                "    return keys.toString();" +
                "  }" +
                "}");
            assertEquals("[a, b, c]", result);
        } catch (Throwable t) { t.printStackTrace(); fail("map_entrySet_stream_getKeys: " + t); }
    }

    @Test
    public void map_values_stream_filterNonNull() {
        try {
            Object result = compileAndRun("t.MS2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class MS2 {" +
                "  public static Object test() {" +
                "    Map<String, String> map = new HashMap<String, String>();" +
                "    map.put(\"a\", \"x\"); map.put(\"b\", null); map.put(\"c\", \"z\");" +
                "    long count = map.values().stream().filter(v -> v != null).count();" +
                "    return count;" +
                "  }" +
                "}");
            assertEquals(2L, result);
        } catch (Throwable t) { fail("map_values_stream_filterNonNull: " + t); }
    }

    @Test
    public void map_keySet_stream_mapToLowerCase() {
        try {
            Object result = compileAndRun("t.MS3",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class MS3 {" +
                "  public static Object test() {" +
                "    Set<String> keys = new TreeSet<String>();" +
                "    keys.add(\"ABC\"); keys.add(\"DEF\");" +
                "    List result = keys.stream().map(s -> ((String)s).toLowerCase()).collect(Collectors.toList());" +
                "    return result.toString();" +
                "  }" +
                "}");
            assertEquals("[abc, def]", result);
        } catch (Throwable t) { fail("map_keySet_stream_mapToLowerCase: " + t); }
    }

    @Test
    public void map_values_stream_mapToDouble_sum() {
        try {
            Object result = compileAndRun("t.MS4",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class MS4 {" +
                "  public static Object test() {" +
                "    Map<String, Double> map = new HashMap<String, Double>();" +
                "    map.put(\"a\", Double.valueOf(1.5)); map.put(\"b\", Double.valueOf(2.5)); map.put(\"c\", Double.valueOf(3.0));" +
                "    double sum = map.values().stream().mapToDouble(d -> ((Double)d).doubleValue()).sum();" +
                "    return sum;" +
                "  }" +
                "}");
            assertEquals(7.0, result);
        } catch (Throwable t) { fail("map_values_stream_mapToDouble_sum: " + t); }
    }

    @Test
    public void map_forEach_biConsumer() {
        try {
            Object result = compileAndRun("t.MS5",
                "package t;" +
                "import java.util.*;" +
                "public class MS5 {" +
                "  public static Object test() {" +
                "    Map<String, Integer> map = new TreeMap<String, Integer>();" +
                "    map.put(\"a\", 1); map.put(\"b\", 2);" +
                "    StringBuilder sb = new StringBuilder();" +
                "    map.forEach((k, v) -> sb.append(k).append(\"=\").append(v).append(\";\"));" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("a=1;b=2;", result);
        } catch (Throwable t) { fail("map_forEach_biConsumer: " + t); }
    }

    @Test
    public void map_computeIfAbsent() {
        try {
            Object result = compileAndRun("t.MS6",
                "package t;" +
                "import java.util.*;" +
                "public class MS6 {" +
                "  public static Object test() {" +
                "    Map<String, List> map = new HashMap<String, List>();" +
                "    map.computeIfAbsent(\"key\", k -> new ArrayList());" +
                "    ((List)map.get(\"key\")).add(\"val\");" +
                "    return map.get(\"key\").toString();" +
                "  }" +
                "}");
            assertEquals("[val]", result);
        } catch (Throwable t) { fail("map_computeIfAbsent: " + t); }
    }

    @Test
    public void map_merge() {
        try {
            Object result = compileAndRun("t.MS7",
                "package t;" +
                "import java.util.*;" +
                "public class MS7 {" +
                "  public static Object test() {" +
                "    Map<String, Integer> map = new HashMap<String, Integer>();" +
                "    map.put(\"a\", Integer.valueOf(10));" +
                "    map.merge(\"a\", Integer.valueOf(5), (v1, v2) -> Integer.valueOf(((Integer)v1).intValue() + ((Integer)v2).intValue()));" +
                "    map.merge(\"b\", Integer.valueOf(7), (v1, v2) -> Integer.valueOf(((Integer)v1).intValue() + ((Integer)v2).intValue()));" +
                "    return map.get(\"a\") + \",\" + map.get(\"b\");" +
                "  }" +
                "}");
            assertEquals("15,7", result);
        } catch (Throwable t) { fail("map_merge: " + t); }
    }

    @Test
    public void map_intStream_boxed_toMap() {
        try {
            Object result = compileAndRun("t.MS8",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "import java.util.function.*;" +
                "public class MS8 {" +
                "  public static Object test() {" +
                "    Map result = IntStream.of(1, 2, 3)" +
                "      .boxed()" +
                "      .collect(Collectors.toMap(i -> i, i -> ((Integer)i).intValue() * ((Integer)i).intValue()));" +
                "    return new TreeMap(result).toString();" +
                "  }" +
                "}");
            assertEquals("{1=1, 2=4, 3=9}", result);
        } catch (Throwable t) { fail("map_intStream_boxed_toMap: " + t); }
    }

    // ========================================================================
    // CATEGORY 4: Stream Operations (10 tests)
    // ========================================================================

    @Test
    public void stream_of_mapToInt_sum() {
        try {
            Object result = compileAndRun("t.SO1",
                "package t;" +
                "import java.util.stream.*;" +
                "public class SO1 {" +
                "  public static Object test() {" +
                "    return Stream.of(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3))" +
                "      .mapToInt(i -> ((Integer)i).intValue()).sum();" +
                "  }" +
                "}");
            assertEquals(6, result);
        } catch (Throwable t) { fail("stream_of_mapToInt_sum: " + t); }
    }

    @Test
    public void stream_iterate_limit_toArray() {
        try {
            Object result = compileAndRun("t.SO2",
                "package t;" +
                "import java.util.stream.*;" +
                "import java.util.Arrays;" +
                "public class SO2 {" +
                "  public static Object test() {" +
                "    Object[] arr = Stream.iterate(Integer.valueOf(0), i -> Integer.valueOf(((Integer)i).intValue() + 1))" +
                "      .limit(5).toArray();" +
                "    return Arrays.toString(arr);" +
                "  }" +
                "}");
            assertEquals("[0, 1, 2, 3, 4]", result);
        } catch (Throwable t) { fail("stream_iterate_limit_toArray: " + t); }
    }

    @Test
    public void stream_generate_limit() {
        try {
            Object result = compileAndRun("t.SO3",
                "package t;" +
                "import java.util.stream.*;" +
                "import java.util.Arrays;" +
                "public class SO3 {" +
                "  public static Object test() {" +
                "    Object[] arr = Stream.generate(() -> \"x\").limit(3).toArray();" +
                "    return Arrays.toString(arr);" +
                "  }" +
                "}");
            assertEquals("[x, x, x]", result);
        } catch (Throwable t) { fail("stream_generate_limit: " + t); }
    }

    @Test
    public void stream_concat_distinct() {
        try {
            Object result = compileAndRun("t.SO4",
                "package t;" +
                "import java.util.stream.*;" +
                "import java.util.Arrays;" +
                "public class SO4 {" +
                "  public static Object test() {" +
                "    Stream s1 = Stream.of(\"a\", \"b\", \"c\");" +
                "    Stream s2 = Stream.of(\"b\", \"c\", \"d\");" +
                "    Object[] arr = Stream.concat(s1, s2).distinct().sorted().toArray();" +
                "    return Arrays.toString(arr);" +
                "  }" +
                "}");
            assertEquals("[a, b, c, d]", result);
        } catch (Throwable t) { fail("stream_concat_distinct: " + t); }
    }

    @Test
    public void stream_flatMap() {
        try {
            Object result = compileAndRun("t.SO5",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class SO5 {" +
                "  public static Object test() {" +
                "    List<List> lists = new ArrayList<List>();" +
                "    List a = new ArrayList(); a.add(\"a\"); a.add(\"b\");" +
                "    List b = new ArrayList(); b.add(\"c\"); b.add(\"d\");" +
                "    lists.add(a); lists.add(b);" +
                "    List result = (List) lists.stream().flatMap(l -> ((List)l).stream()).collect(Collectors.toList());" +
                "    return result.toString();" +
                "  }" +
                "}");
            assertEquals("[a, b, c, d]", result);
        } catch (Throwable t) { fail("stream_flatMap: " + t); }
    }

    @Test
    public void intStream_range_filter() {
        try {
            Object result = compileAndRun("t.SO6",
                "package t;" +
                "import java.util.stream.*;" +
                "import java.util.Arrays;" +
                "public class SO6 {" +
                "  public static Object test() {" +
                "    int[] arr = IntStream.range(0, 10).filter(i -> i % 2 == 0).toArray();" +
                "    return Arrays.toString(arr);" +
                "  }" +
                "}");
            assertEquals("[0, 2, 4, 6, 8]", result);
        } catch (Throwable t) { fail("intStream_range_filter: " + t); }
    }

    @Test
    public void longStream_rangeClosed_reduce() {
        try {
            Object result = compileAndRun("t.SO7",
                "package t;" +
                "import java.util.stream.*;" +
                "public class SO7 {" +
                "  public static Object test() {" +
                "    long factorial = LongStream.rangeClosed(1, 5).reduce(1L, (a, b) -> a * b);" +
                "    return factorial;" +
                "  }" +
                "}");
            assertEquals(120L, result);
        } catch (Throwable t) { fail("longStream_rangeClosed_reduce: " + t); }
    }

    @Test
    public void doubleStream_of_average() {
        try {
            Object result = compileAndRun("t.SO8",
                "package t;" +
                "import java.util.stream.*;" +
                "public class SO8 {" +
                "  public static Object test() {" +
                "    double avg = DoubleStream.of(1.0, 2.0, 3.0).average().orElse(0.0);" +
                "    return avg;" +
                "  }" +
                "}");
            assertEquals(2.0, result);
        } catch (Throwable t) { fail("doubleStream_of_average: " + t); }
    }

    @Test
    public void stream_reduce_withIdentity() {
        try {
            Object result = compileAndRun("t.SO9",
                "package t;" +
                "import java.util.stream.*;" +
                "public class SO9 {" +
                "  public static Object test() {" +
                "    String joined = (String) Stream.of(\"a\", \"b\", \"c\")" +
                "      .reduce(\"\", (a, b) -> ((String)a) + ((String)b));" +
                "    return joined;" +
                "  }" +
                "}");
            assertEquals("abc", result);
        } catch (Throwable t) { fail("stream_reduce_withIdentity: " + t); }
    }

    @Test
    public void stream_peek_forEach() {
        try {
            Object result = compileAndRun("t.SO10",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class SO10 {" +
                "  public static Object test() {" +
                "    List<String> peeked = new ArrayList<String>();" +
                "    long count = Stream.of(\"a\", \"b\", \"c\")" +
                "      .peek(s -> peeked.add((String)s))" +
                "      .count();" +
                "    return count + \":\" + peeked.toString();" +
                "  }" +
                "}");
            assertNotNull(result);
        } catch (Throwable t) { fail("stream_peek_forEach: " + t); }
    }

    // ========================================================================
    // CATEGORY 5: Collectors (8 tests)
    // ========================================================================

    @Test
    public void collectors_groupingBy() {
        try {
            Object result = compileAndRun("t.CL1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CL1 {" +
                "  public static Object test() {" +
                "    Map result = (Map) Stream.of(\"a\", \"bb\", \"cc\", \"ddd\")" +
                "      .collect(Collectors.groupingBy(s -> Integer.valueOf(((String)s).length())));" +
                "    return new TreeMap(result).toString();" +
                "  }" +
                "}");
            assertEquals("{1=[a], 2=[bb, cc], 3=[ddd]}", result);
        } catch (Throwable t) { fail("collectors_groupingBy: " + t); }
    }

    @Test
    public void collectors_partitioningBy() {
        try {
            Object result = compileAndRun("t.CL2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CL2 {" +
                "  public static Object test() {" +
                "    Map result = (Map) Stream.of(\"a\", \"bbbb\", \"cc\", \"ddddd\")" +
                "      .collect(Collectors.partitioningBy(s -> ((String)s).length() > 3));" +
                "    return result.toString();" +
                "  }" +
                "}");
            assertNotNull(result);
        } catch (Throwable t) { fail("collectors_partitioningBy: " + t); }
    }

    @Test
    public void collectors_joining() {
        try {
            Object result = compileAndRun("t.CL3",
                "package t;" +
                "import java.util.stream.*;" +
                "public class CL3 {" +
                "  public static Object test() {" +
                "    return Stream.of(\"a\", \"b\", \"c\").collect(Collectors.joining(\",\"));" +
                "  }" +
                "}");
            assertEquals("a,b,c", result);
        } catch (Throwable t) { fail("collectors_joining: " + t); }
    }

    @Test
    public void collectors_counting() {
        try {
            Object result = compileAndRun("t.CL4",
                "package t;" +
                "import java.util.stream.*;" +
                "public class CL4 {" +
                "  public static Object test() {" +
                "    return Stream.of(\"a\", \"b\", \"c\", \"d\").collect(Collectors.counting());" +
                "  }" +
                "}");
            assertEquals(4L, result);
        } catch (Throwable t) { fail("collectors_counting: " + t); }
    }

    @Test
    public void collectors_summarizingInt() {
        try {
            Object result = compileAndRun("t.CL5",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CL5 {" +
                "  public static Object test() {" +
                "    IntSummaryStatistics stats = (IntSummaryStatistics) Stream.of(\"a\", \"bb\", \"ccc\")" +
                "      .collect(Collectors.summarizingInt(s -> ((String)s).length()));" +
                "    return stats.getSum() + \",\" + stats.getCount() + \",\" + stats.getMax();" +
                "  }" +
                "}");
            assertEquals("6,3,3", result);
        } catch (Throwable t) { fail("collectors_summarizingInt: " + t); }
    }

    @Test
    public void collectors_toMap() {
        try {
            Object result = compileAndRun("t.CL6",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CL6 {" +
                "  public static Object test() {" +
                "    Map result = (Map) Stream.of(\"hello\", \"world\")" +
                "      .collect(Collectors.toMap(s -> s, s -> Integer.valueOf(((String)s).length())));" +
                "    return new TreeMap(result).toString();" +
                "  }" +
                "}");
            assertEquals("{hello=5, world=5}", result);
        } catch (Throwable t) { fail("collectors_toMap: " + t); }
    }

    @Test
    public void collectors_maxBy() {
        try {
            Object result = compileAndRun("t.CL7",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class CL7 {" +
                "  public static Object test() {" +
                "    Optional max = Stream.of(\"a\", \"ccc\", \"bb\")" +
                "      .collect(Collectors.maxBy((Comparator)(a, b) -> ((String)a).length() - ((String)b).length()));" +
                "    return max.orElse(\"none\");" +
                "  }" +
                "}");
            assertEquals("ccc", result);
        } catch (Throwable t) { t.printStackTrace(); fail("collectors_maxBy: " + t); }
    }

    @Test
    public void collectors_joiningWithDelimiterPrefixSuffix() {
        try {
            Object result = compileAndRun("t.CL8",
                "package t;" +
                "import java.util.stream.*;" +
                "public class CL8 {" +
                "  public static Object test() {" +
                "    return Stream.of(\"a\", \"b\", \"c\").collect(Collectors.joining(\", \", \"[\", \"]\"));" +
                "  }" +
                "}");
            assertEquals("[a, b, c]", result);
        } catch (Throwable t) { fail("collectors_joiningWithDelimiterPrefixSuffix: " + t); }
    }

    // ========================================================================
    // CATEGORY 6: Optional with Streams (4 tests)
    // ========================================================================

    @Test
    public void optional_of_map() {
        try {
            Object result = compileAndRun("t.OP1",
                "package t;" +
                "import java.util.*;" +
                "public class OP1 {" +
                "  public static Object test() {" +
                "    Optional opt = Optional.of(Integer.valueOf(5));" +
                "    Optional mapped = opt.map(i -> Integer.valueOf(((Integer)i).intValue() * 2));" +
                "    return mapped.orElse(Integer.valueOf(0));" +
                "  }" +
                "}");
            assertEquals(10, result);
        } catch (Throwable t) { fail("optional_of_map: " + t); }
    }

    @Test
    public void optional_empty_orElse() {
        try {
            Object result = compileAndRun("t.OP2",
                "package t;" +
                "import java.util.*;" +
                "public class OP2 {" +
                "  public static Object test() {" +
                "    Optional opt = Optional.empty();" +
                "    return opt.orElse(\"default\");" +
                "  }" +
                "}");
            assertEquals("default", result);
        } catch (Throwable t) { fail("optional_empty_orElse: " + t); }
    }

    @Test
    public void optional_flatMap() {
        try {
            Object result = compileAndRun("t.OP3",
                "package t;" +
                "import java.util.*;" +
                "public class OP3 {" +
                "  public static Object test() {" +
                "    Optional opt = Optional.of(\"hello\");" +
                "    Optional result = opt.flatMap(s -> Optional.of(((String)s).toUpperCase()));" +
                "    return result.orElse(\"none\");" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("optional_flatMap: " + t); }
    }

    @Test
    public void optional_ifPresent() {
        try {
            Object result = compileAndRun("t.OP4",
                "package t;" +
                "import java.util.*;" +
                "public class OP4 {" +
                "  public static Object test() {" +
                "    StringBuilder sb = new StringBuilder();" +
                "    Optional.of(\"hello\").ifPresent(s -> sb.append(s));" +
                "    Optional.empty().ifPresent(s -> sb.append(\"nope\"));" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("hello", result);
        } catch (Throwable t) { fail("optional_ifPresent: " + t); }
    }

    // ========================================================================
    // CATEGORY 7: Method Reference Edge Cases (6 tests)
    // ========================================================================

    @Test
    public void methodRef_stringConcat() {
        try {
            Object result = compileAndRun("t.MR1",
                "package t;" +
                "import java.util.function.*;" +
                "public class MR1 {" +
                "  public static Object test() {" +
                "    BiFunction<String, String, String> f = String::concat;" +
                "    return f.apply(\"hello\", \" world\");" +
                "  }" +
                "}");
            assertEquals("hello world", result);
        } catch (Throwable t) { fail("methodRef_stringConcat: " + t); }
    }

    @Test
    public void methodRef_mathMax() {
        try {
            Object result = compileAndRun("t.MR2",
                "package t;" +
                "import java.util.function.*;" +
                "public class MR2 {" +
                "  public static Object test() {" +
                "    IntBinaryOperator op = Math::max;" +
                "    return op.applyAsInt(5, 10);" +
                "  }" +
                "}");
            assertEquals(10, result);
        } catch (Throwable t) { fail("methodRef_mathMax: " + t); }
    }

    @Test
    public void methodRef_stringBuilderAppend() {
        try {
            Object result = compileAndRun("t.MR3",
                "package t;" +
                "import java.util.function.*;" +
                "public class MR3 {" +
                "  public static Object test() {" +
                "    StringBuilder sb = new StringBuilder();" +
                "    Consumer<String> appender = sb::append;" +
                "    appender.accept(\"hello\");" +
                "    appender.accept(\" world\");" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("hello world", result);
        } catch (Throwable t) { fail("methodRef_stringBuilderAppend: " + t); }
    }

    @Test
    public void methodRef_arrayListConstructor() {
        try {
            Object result = compileAndRun("t.MR4",
                "package t;" +
                "import java.util.*;" +
                "import java.util.function.*;" +
                "public class MR4 {" +
                "  public static Object test() {" +
                "    Supplier<ArrayList> s = ArrayList::new;" +
                "    ArrayList list = s.get();" +
                "    list.add(\"a\"); list.add(\"b\");" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[a, b]", result);
        } catch (Throwable t) { fail("methodRef_arrayListConstructor: " + t); }
    }

    @Test
    public void methodRef_hashMapConstructor() {
        try {
            Object result = compileAndRun("t.MR5",
                "package t;" +
                "import java.util.*;" +
                "import java.util.function.*;" +
                "public class MR5 {" +
                "  public static Object test() {" +
                "    Supplier<HashMap> s = HashMap::new;" +
                "    HashMap map = s.get();" +
                "    map.put(\"k\", \"v\");" +
                "    return map.get(\"k\");" +
                "  }" +
                "}");
            assertEquals("v", result);
        } catch (Throwable t) { fail("methodRef_hashMapConstructor: " + t); }
    }

    @Test
    public void methodRef_integerValueOf() {
        try {
            Object result = compileAndRun("t.MR6",
                "package t;" +
                "import java.util.function.*;" +
                "public class MR6 {" +
                "  public static Object test() {" +
                "    Function<String, Integer> parser = Integer::valueOf;" +
                "    return parser.apply(\"42\");" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("methodRef_integerValueOf: " + t); }
    }

    // ========================================================================
    // CATEGORY 8: Primitive Specializations (4 tests)
    // ========================================================================

    @Test
    public void primitive_intConsumer_longConsumer_doubleConsumer() {
        try {
            Object result = compileAndRun("t.PS1",
                "package t;" +
                "import java.util.function.*;" +
                "public class PS1 {" +
                "  public static Object test() {" +
                "    int[] iHolder = {0};" +
                "    long[] lHolder = {0L};" +
                "    double[] dHolder = {0.0};" +
                "    IntConsumer ic = n -> iHolder[0] = n;" +
                "    LongConsumer lc = n -> lHolder[0] = n;" +
                "    DoubleConsumer dc = n -> dHolder[0] = n;" +
                "    ic.accept(42);" +
                "    lc.accept(100L);" +
                "    dc.accept(3.14);" +
                "    return iHolder[0] + \",\" + lHolder[0] + \",\" + dHolder[0];" +
                "  }" +
                "}");
            assertEquals("42,100,3.14", result);
        } catch (Throwable t) { fail("primitive_intConsumer_longConsumer_doubleConsumer: " + t); }
    }

    @Test
    public void primitive_intPredicate_longPredicate_doublePredicate() {
        try {
            Object result = compileAndRun("t.PS2",
                "package t;" +
                "import java.util.function.*;" +
                "public class PS2 {" +
                "  public static Object test() {" +
                "    IntPredicate ip = n -> n > 5;" +
                "    LongPredicate lp = n -> n > 100L;" +
                "    DoublePredicate dp = n -> n > 1.5;" +
                "    return ip.test(10) + \",\" + lp.test(50L) + \",\" + dp.test(2.0);" +
                "  }" +
                "}");
            assertEquals("true,false,true", result);
        } catch (Throwable t) { fail("primitive_intPredicate_longPredicate_doublePredicate: " + t); }
    }

    @Test
    public void primitive_intFunction_longFunction_doubleFunction() {
        try {
            Object result = compileAndRun("t.PS3",
                "package t;" +
                "import java.util.function.*;" +
                "public class PS3 {" +
                "  public static Object test() {" +
                "    IntFunction<String> iff = n -> \"int:\" + n;" +
                "    LongFunction<String> lf = n -> \"long:\" + n;" +
                "    DoubleFunction<String> df = n -> \"double:\" + n;" +
                "    return iff.apply(1) + \"|\" + lf.apply(2L) + \"|\" + df.apply(3.0);" +
                "  }" +
                "}");
            assertEquals("int:1|long:2|double:3.0", result);
        } catch (Throwable t) { fail("primitive_intFunction_longFunction_doubleFunction: " + t); }
    }

    @Test
    public void primitive_intToDoubleFunction_intToLongFunction() {
        try {
            Object result = compileAndRun("t.PS4",
                "package t;" +
                "import java.util.function.*;" +
                "public class PS4 {" +
                "  public static Object test() {" +
                "    IntToDoubleFunction itdf = n -> n * 1.5;" +
                "    IntToLongFunction itlf = n -> (long) n * 1000L;" +
                "    return itdf.applyAsDouble(4) + \",\" + itlf.applyAsLong(5);" +
                "  }" +
                "}");
            assertEquals("6.0,5000", result);
        } catch (Throwable t) { fail("primitive_intToDoubleFunction_intToLongFunction: " + t); }
    }
}
