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
 * Comprehensive test suite for Java lambda expressions and functional interfaces
 * in the Janino compiler. Each test method exercises a specific lambda feature
 * and records whether it compiles and executes successfully.
 *
 * <p>Results from this suite inform which lambda features are safe to use in
 * dynamically-compiled scripts via JaninoLoader.</p>
 */
public class JaninoLambdaTest {

    // ========================================================================
    // Infrastructure
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
        compiler.setIClassLoader(new ClassLoaderIClassLoader(loader));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.compile(sources);
    }

    /**
     * Compiles the given source, loads the class, invokes the static "test()" method
     * and returns its result as a String. Throws on compilation or runtime error.
     */
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
    // CATEGORY 1: Single-Line Lambdas (Expression Lambdas)
    // ========================================================================

    @Test
    public void lambda_singleLine_identity() {
        try {
            Object result = compileAndRun("t.T1", 
                "package t;" +
                "import java.util.function.*;" +
                "public class T1 {" +
                "  public static String test() {" +
                "    Function<String, String> f = x -> x;" +
                "    return f.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals("hello", result);
        } catch (Throwable t) { fail("lambda_singleLine_identity: " + t); }
    }

    @Test
    public void lambda_singleLine_multiply() {
        try {
            Object result = compileAndRun("t.T2",
                "package t;" +
                "import java.util.function.*;" +
                "public class T2 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> f = x -> x * 2;" +
                "    return f.apply(21);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("lambda_singleLine_multiply: " + t); }
    }

    @Test
    public void lambda_singleLine_noArgs() {
        try {
            Object result = compileAndRun("t.T3",
                "package t;" +
                "import java.util.function.*;" +
                "public class T3 {" +
                "  public static Object test() {" +
                "    Supplier<Integer> s = () -> 42;" +
                "    return s.get();" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("lambda_singleLine_noArgs: " + t); }
    }

    @Test
    public void lambda_singleLine_twoArgs() {
        try {
            Object result = compileAndRun("t.T4",
                "package t;" +
                "import java.util.function.*;" +
                "public class T4 {" +
                "  public static Object test() {" +
                "    BiFunction<Integer, Integer, Integer> f = (a, b) -> a + b;" +
                "    return f.apply(17, 25);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("lambda_singleLine_twoArgs: " + t); }
    }

    @Test
    public void lambda_singleLine_stringConcat() {
        try {
            Object result = compileAndRun("t.T5",
                "package t;" +
                "import java.util.function.*;" +
                "public class T5 {" +
                "  public static Object test() {" +
                "    BiFunction<String, String, String> f = (a, b) -> a + b;" +
                "    return f.apply(\"foo\", \"bar\");" +
                "  }" +
                "}");
            assertEquals("foobar", result);
        } catch (Throwable t) { fail("lambda_singleLine_stringConcat: " + t); }
    }

    @Test
    public void lambda_singleLine_ternary() {
        try {
            Object result = compileAndRun("t.T6",
                "package t;" +
                "import java.util.function.*;" +
                "public class T6 {" +
                "  public static Object test() {" +
                "    Function<Integer, String> f = x -> x > 0 ? \"pos\" : \"non-pos\";" +
                "    return f.apply(5);" +
                "  }" +
                "}");
            assertEquals("pos", result);
        } catch (Throwable t) { fail("lambda_singleLine_ternary: " + t); }
    }

    @Test
    public void lambda_singleLine_castInBody() {
        try {
            Object result = compileAndRun("t.T7",
                "package t;" +
                "import java.util.function.*;" +
                "public class T7 {" +
                "  public static Object test() {" +
                "    Function<Object, String> f = x -> (String) x;" +
                "    return f.apply(\"test\");" +
                "  }" +
                "}");
            assertEquals("test", result);
        } catch (Throwable t) { fail("lambda_singleLine_castInBody: " + t); }
    }

    @Test
    public void lambda_singleLine_explicitTypes() {
        try {
            Object result = compileAndRun("t.T8",
                "package t;" +
                "import java.util.function.*;" +
                "public class T8 {" +
                "  public static Object test() {" +
                "    BiFunction<Integer, Integer, Integer> f = (Integer a, Integer b) -> a + b;" +
                "    return f.apply(20, 22);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("lambda_singleLine_explicitTypes: " + t); }
    }

    // ========================================================================
    // CATEGORY 2: Multi-Line (Block) Lambdas
    // ========================================================================

    @Test
    public void lambda_block_simpleReturn() {
        try {
            Object result = compileAndRun("t.B1",
                "package t;" +
                "import java.util.function.*;" +
                "public class B1 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> f = x -> { int y = x * 2; return y; };" +
                "    return f.apply(21);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("lambda_block_simpleReturn: " + t); }
    }

    @Test
    public void lambda_block_multipleStatements() {
        try {
            Object result = compileAndRun("t.B2",
                "package t;" +
                "import java.util.function.*;" +
                "public class B2 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> f = x -> {" +
                "      int sum = 0;" +
                "      for (int i = 1; i <= x; i++) { sum += i; }" +
                "      return sum;" +
                "    };" +
                "    return f.apply(10);" +
                "  }" +
                "}");
            assertEquals(55, result);
        } catch (Throwable t) { fail("lambda_block_multipleStatements: " + t); }
    }

    @Test
    public void lambda_block_voidReturn() {
        try {
            Object result = compileAndRun("t.B3",
                "package t;" +
                "import java.util.function.*;" +
                "public class B3 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Consumer<String> c = s -> { sb.append(s); sb.append(\"!\"); };" +
                "    c.accept(\"hi\");" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("hi!", result);
        } catch (Throwable t) { fail("lambda_block_voidReturn: " + t); }
    }

    @Test
    public void lambda_block_ifElse() {
        try {
            Object result = compileAndRun("t.B4",
                "package t;" +
                "import java.util.function.*;" +
                "public class B4 {" +
                "  public static Object test() {" +
                "    Function<Integer, String> f = x -> {" +
                "      if (x > 0) { return \"positive\"; }" +
                "      else if (x < 0) { return \"negative\"; }" +
                "      else { return \"zero\"; }" +
                "    };" +
                "    return f.apply(0);" +
                "  }" +
                "}");
            assertEquals("zero", result);
        } catch (Throwable t) { fail("lambda_block_ifElse: " + t); }
    }

    @Test
    public void lambda_block_tryCatch() {
        try {
            Object result = compileAndRun("t.B5",
                "package t;" +
                "import java.util.function.*;" +
                "public class B5 {" +
                "  public static Object test() {" +
                "    Function<String, Integer> f = s -> {" +
                "      try { return Integer.parseInt(s); }" +
                "      catch (NumberFormatException e) { return -1; }" +
                "    };" +
                "    return f.apply(\"notanumber\");" +
                "  }" +
                "}");
            assertEquals(-1, result);
        } catch (Throwable t) { fail("lambda_block_tryCatch: " + t); }
    }

    @Test
    public void lambda_block_localVariable() {
        try {
            Object result = compileAndRun("t.B6",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class B6 {" +
                "  public static Object test() {" +
                "    Function<Integer, List<Integer>> f = n -> {" +
                "      List<Integer> list = new ArrayList<Integer>();" +
                "      for (int i = 0; i < n; i++) { list.add(i); }" +
                "      return list;" +
                "    };" +
                "    return f.apply(3).toString();" +
                "  }" +
                "}");
            assertEquals("[0, 1, 2]", result);
        } catch (Throwable t) { fail("lambda_block_localVariable: " + t); }
    }

    // ========================================================================
    // CATEGORY 3: java.util.function Interfaces
    // ========================================================================

    @Test
    public void funcInterface_Function() {
        try {
            Object result = compileAndRun("t.F1",
                "package t;" +
                "import java.util.function.*;" +
                "public class F1 {" +
                "  public static Object test() {" +
                "    Function<String, Integer> f = s -> s.length();" +
                "    return f.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("funcInterface_Function: " + t); }
    }

    @Test
    public void funcInterface_Consumer() {
        try {
            Object result = compileAndRun("t.F2",
                "package t;" +
                "import java.util.function.*;" +
                "public class F2 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Consumer<String> c = s -> sb.append(s);" +
                "    c.accept(\"consumed\");" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("consumed", result);
        } catch (Throwable t) { fail("funcInterface_Consumer: " + t); }
    }

    @Test
    public void funcInterface_Supplier() {
        try {
            Object result = compileAndRun("t.F3",
                "package t;" +
                "import java.util.function.*;" +
                "public class F3 {" +
                "  public static Object test() {" +
                "    Supplier<String> s = () -> \"supplied\";" +
                "    return s.get();" +
                "  }" +
                "}");
            assertEquals("supplied", result);
        } catch (Throwable t) { fail("funcInterface_Supplier: " + t); }
    }

    @Test
    public void funcInterface_Predicate() {
        try {
            Object result = compileAndRun("t.F4",
                "package t;" +
                "import java.util.function.*;" +
                "public class F4 {" +
                "  public static Object test() {" +
                "    Predicate<String> p = s -> s.length() > 3;" +
                "    return p.test(\"hello\") + \",\" + p.test(\"hi\");" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("funcInterface_Predicate: " + t); }
    }

    @Test
    public void funcInterface_BiFunction() {
        try {
            Object result = compileAndRun("t.F5",
                "package t;" +
                "import java.util.function.*;" +
                "public class F5 {" +
                "  public static Object test() {" +
                "    BiFunction<String, Integer, String> f = (s, n) -> s.substring(0, n);" +
                "    return f.apply(\"hello\", 3);" +
                "  }" +
                "}");
            assertEquals("hel", result);
        } catch (Throwable t) { fail("funcInterface_BiFunction: " + t); }
    }

    @Test
    public void funcInterface_BiConsumer() {
        try {
            Object result = compileAndRun("t.F6",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class F6 {" +
                "  public static Object test() {" +
                "    Map<String, Integer> map = new HashMap<String, Integer>();" +
                "    BiConsumer<String, Integer> bc = (k, v) -> map.put(k, v);" +
                "    bc.accept(\"a\", 1);" +
                "    bc.accept(\"b\", 2);" +
                "    return map.get(\"a\") + \",\" + map.get(\"b\");" +
                "  }" +
                "}");
            assertEquals("1,2", result);
        } catch (Throwable t) { fail("funcInterface_BiConsumer: " + t); }
    }

    @Test
    public void funcInterface_BiPredicate() {
        try {
            Object result = compileAndRun("t.F7",
                "package t;" +
                "import java.util.function.*;" +
                "public class F7 {" +
                "  public static Object test() {" +
                "    BiPredicate<String, String> bp = (a, b) -> a.equals(b);" +
                "    return bp.test(\"x\", \"x\") + \",\" + bp.test(\"x\", \"y\");" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("funcInterface_BiPredicate: " + t); }
    }

    @Test
    public void funcInterface_UnaryOperator() {
        try {
            Object result = compileAndRun("t.F8",
                "package t;" +
                "import java.util.function.*;" +
                "public class F8 {" +
                "  public static Object test() {" +
                "    UnaryOperator<String> op = s -> s.toUpperCase();" +
                "    return op.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("funcInterface_UnaryOperator: " + t); }
    }

    @Test
    public void funcInterface_BinaryOperator() {
        try {
            Object result = compileAndRun("t.F9",
                "package t;" +
                "import java.util.function.*;" +
                "public class F9 {" +
                "  public static Object test() {" +
                "    BinaryOperator<String> op = (a, b) -> a + b;" +
                "    return op.apply(\"foo\", \"bar\");" +
                "  }" +
                "}");
            assertEquals("foobar", result);
        } catch (Throwable t) { fail("funcInterface_BinaryOperator: " + t); }
    }

    @Test
    public void funcInterface_IntFunction() {
        try {
            Object result = compileAndRun("t.F10",
                "package t;" +
                "import java.util.function.*;" +
                "public class F10 {" +
                "  public static Object test() {" +
                "    IntFunction<String> f = n -> \"num:\" + n;" +
                "    return f.apply(42);" +
                "  }" +
                "}");
            assertEquals("num:42", result);
        } catch (Throwable t) { fail("funcInterface_IntFunction: " + t); }
    }

    @Test
    public void funcInterface_IntSupplier() {
        try {
            Object result = compileAndRun("t.F11",
                "package t;" +
                "import java.util.function.*;" +
                "public class F11 {" +
                "  public static Object test() {" +
                "    IntSupplier s = () -> 99;" +
                "    return s.getAsInt();" +
                "  }" +
                "}");
            assertEquals(99, result);
        } catch (Throwable t) { fail("funcInterface_IntSupplier: " + t); }
    }

    @Test
    public void funcInterface_IntConsumer() {
        try {
            Object result = compileAndRun("t.F12",
                "package t;" +
                "import java.util.function.*;" +
                "public class F12 {" +
                "  public static Object test() {" +
                "    final int[] holder = {0};" +
                "    IntConsumer c = n -> holder[0] = n;" +
                "    c.accept(77);" +
                "    return holder[0];" +
                "  }" +
                "}");
            assertEquals(77, result);
        } catch (Throwable t) { fail("funcInterface_IntConsumer: " + t); }
    }

    @Test
    public void funcInterface_IntPredicate() {
        try {
            Object result = compileAndRun("t.F13",
                "package t;" +
                "import java.util.function.*;" +
                "public class F13 {" +
                "  public static Object test() {" +
                "    IntPredicate p = n -> n % 2 == 0;" +
                "    return p.test(4) + \",\" + p.test(5);" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("funcInterface_IntPredicate: " + t); }
    }

    @Test
    public void funcInterface_IntUnaryOperator() {
        try {
            Object result = compileAndRun("t.F14",
                "package t;" +
                "import java.util.function.*;" +
                "public class F14 {" +
                "  public static Object test() {" +
                "    IntUnaryOperator op = n -> n * n;" +
                "    return op.applyAsInt(7);" +
                "  }" +
                "}");
            assertEquals(49, result);
        } catch (Throwable t) { fail("funcInterface_IntUnaryOperator: " + t); }
    }

    @Test
    public void funcInterface_IntBinaryOperator() {
        try {
            Object result = compileAndRun("t.F15",
                "package t;" +
                "import java.util.function.*;" +
                "public class F15 {" +
                "  public static Object test() {" +
                "    IntBinaryOperator op = (a, b) -> a * b;" +
                "    return op.applyAsInt(6, 7);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("funcInterface_IntBinaryOperator: " + t); }
    }

    @Test
    public void funcInterface_LongFunction() {
        try {
            Object result = compileAndRun("t.F16",
                "package t;" +
                "import java.util.function.*;" +
                "public class F16 {" +
                "  public static Object test() {" +
                "    LongFunction<String> f = n -> \"long:\" + n;" +
                "    return f.apply(100L);" +
                "  }" +
                "}");
            assertEquals("long:100", result);
        } catch (Throwable t) { fail("funcInterface_LongFunction: " + t); }
    }

    @Test
    public void funcInterface_DoubleFunction() {
        try {
            Object result = compileAndRun("t.F17",
                "package t;" +
                "import java.util.function.*;" +
                "public class F17 {" +
                "  public static Object test() {" +
                "    DoubleFunction<String> f = d -> \"dbl:\" + d;" +
                "    return f.apply(3.14);" +
                "  }" +
                "}");
            assertEquals("dbl:3.14", result);
        } catch (Throwable t) { fail("funcInterface_DoubleFunction: " + t); }
    }

    @Test
    public void funcInterface_ToIntFunction() {
        try {
            Object result = compileAndRun("t.F18",
                "package t;" +
                "import java.util.function.*;" +
                "public class F18 {" +
                "  public static Object test() {" +
                "    ToIntFunction<String> f = s -> s.length();" +
                "    return f.applyAsInt(\"hello\");" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("funcInterface_ToIntFunction: " + t); }
    }

    @Test
    public void funcInterface_ToLongFunction() {
        try {
            Object result = compileAndRun("t.F19",
                "package t;" +
                "import java.util.function.*;" +
                "public class F19 {" +
                "  public static Object test() {" +
                "    ToLongFunction<String> f = s -> (long) s.length();" +
                "    return f.applyAsLong(\"test\");" +
                "  }" +
                "}");
            assertEquals(4L, result);
        } catch (Throwable t) { fail("funcInterface_ToLongFunction: " + t); }
    }

    @Test
    public void funcInterface_ToDoubleFunction() {
        try {
            Object result = compileAndRun("t.F20",
                "package t;" +
                "import java.util.function.*;" +
                "public class F20 {" +
                "  public static Object test() {" +
                "    ToDoubleFunction<String> f = s -> (double) s.length();" +
                "    return f.applyAsDouble(\"hi\");" +
                "  }" +
                "}");
            assertEquals(2.0, result);
        } catch (Throwable t) { fail("funcInterface_ToDoubleFunction: " + t); }
    }

    @Test
    public void funcInterface_Runnable() {
        try {
            Object result = compileAndRun("t.F21",
                "package t;" +
                "public class F21 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Runnable r = () -> sb.append(\"ran\");" +
                "    r.run();" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("ran", result);
        } catch (Throwable t) { fail("funcInterface_Runnable: " + t); }
    }

    @Test
    public void funcInterface_Callable() {
        try {
            Object result = compileAndRun("t.F22",
                "package t;" +
                "import java.util.concurrent.Callable;" +
                "public class F22 {" +
                "  public static Object test() throws Exception {" +
                "    Callable<String> c = () -> \"called\";" +
                "    return c.call();" +
                "  }" +
                "}");
            assertEquals("called", result);
        } catch (Throwable t) { fail("funcInterface_Callable: " + t); }
    }

    @Test
    public void funcInterface_Comparator() {
        try {
            Object result = compileAndRun("t.F23",
                "package t;" +
                "import java.util.*;" +
                "public class F23 {" +
                "  public static Object test() {" +
                "    Comparator<String> c = (a, b) -> a.length() - b.length();" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"cc\"); list.add(\"a\"); list.add(\"bbb\");" +
                "    Collections.sort(list, c);" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[a, cc, bbb]", result);
        } catch (Throwable t) { fail("funcInterface_Comparator: " + t); }
    }

    // ========================================================================
    // CATEGORY 4: Custom Functional Interfaces
    // ========================================================================

    @Test
    public void customFI_basic() {
        try {
            Object result = compileAndRun("t.C1",
                "package t;" +
                "public class C1 {" +
                "  @FunctionalInterface" +
                "  interface MyFunc { int apply(int x); }" +
                "  public static Object test() {" +
                "    MyFunc f = x -> x * 3;" +
                "    return f.apply(14);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("customFI_basic: " + t); }
    }

    @Test
    public void customFI_noAnnotation() {
        try {
            Object result = compileAndRun("t.C2",
                "package t;" +
                "public class C2 {" +
                "  interface StringTransform { String transform(String s); }" +
                "  public static Object test() {" +
                "    StringTransform t = s -> s.toUpperCase();" +
                "    return t.transform(\"hello\");" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("customFI_noAnnotation: " + t); }
    }

    @Test
    public void customFI_withDefaultMethod() {
        try {
            Object result = compileAndRun("t.C3",
                "package t;" +
                "public class C3 {" +
                "  interface Greeter {" +
                "    String greet(String name);" +
                "    default String greetAll(String[] names) {" +
                "      StringBuilder sb = new StringBuilder();" +
                "      for (String n : names) { sb.append(greet(n)).append(\",\"); }" +
                "      return sb.toString();" +
                "    }" +
                "  }" +
                "  public static Object test() {" +
                "    Greeter g = name -> \"Hi \" + name;" +
                "    return g.greetAll(new String[]{\"A\", \"B\"});" +
                "  }" +
                "}");
            assertEquals("Hi A,Hi B,", result);
        } catch (Throwable t) { fail("customFI_withDefaultMethod: " + t); }
    }

    @Test
    public void customFI_generic() {
        try {
            Object result = compileAndRun("t.C4",
                "package t;" +
                "public class C4 {" +
                "  interface Transformer<T, R> { R transform(T input); }" +
                "  public static Object test() {" +
                "    Transformer<String, Integer> t = s -> s.length();" +
                "    return t.transform(\"hello\");" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("customFI_generic: " + t); }
    }

    @Test
    public void customFI_voidReturn() {
        try {
            Object result = compileAndRun("t.C5",
                "package t;" +
                "public class C5 {" +
                "  interface Callback { void onComplete(String msg); }" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Callback cb = msg -> sb.append(\"done:\" + msg);" +
                "    cb.onComplete(\"ok\");" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("done:ok", result);
        } catch (Throwable t) { fail("customFI_voidReturn: " + t); }
    }

    @Test
    public void customFI_throwsException() {
        try {
            Object result = compileAndRun("t.C6",
                "package t;" +
                "public class C6 {" +
                "  interface Parser<T> { T parse(String s) throws Exception; }" +
                "  public static Object test() throws Exception {" +
                "    Parser<Integer> p = s -> Integer.parseInt(s);" +
                "    return p.parse(\"123\");" +
                "  }" +
                "}");
            assertEquals(123, result);
        } catch (Throwable t) { fail("customFI_throwsException: " + t); }
    }

    @Test
    public void customFI_multipleParams() {
        try {
            Object result = compileAndRun("t.C7",
                "package t;" +
                "public class C7 {" +
                "  interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }" +
                "  public static Object test() {" +
                "    TriFunction<String, String, String, String> f = (a, b, c) -> a + b + c;" +
                "    return f.apply(\"x\", \"y\", \"z\");" +
                "  }" +
                "}");
            assertEquals("xyz", result);
        } catch (Throwable t) { fail("customFI_multipleParams: " + t); }
    }

    @Test
    public void customFI_primitiveParams() {
        try {
            Object result = compileAndRun("t.C8",
                "package t;" +
                "public class C8 {" +
                "  interface IntOp { int op(int a, int b); }" +
                "  public static Object test() {" +
                "    IntOp add = (a, b) -> a + b;" +
                "    IntOp mul = (a, b) -> a * b;" +
                "    return add.op(3, 4) + mul.op(2, 5);" +
                "  }" +
                "}");
            assertEquals(17, result);
        } catch (Throwable t) { fail("customFI_primitiveParams: " + t); }
    }

    // ========================================================================
    // CATEGORY 5: Method References
    // ========================================================================

    @Test
    public void methodRef_staticMethod() {
        try {
            Object result = compileAndRun("t.M1",
                "package t;" +
                "import java.util.function.*;" +
                "public class M1 {" +
                "  public static Object test() {" +
                "    Function<String, Integer> f = Integer::parseInt;" +
                "    return f.apply(\"42\");" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("methodRef_staticMethod: " + t); }
    }

    @Test
    public void methodRef_instanceMethodOnType() {
        try {
            Object result = compileAndRun("t.M2",
                "package t;" +
                "import java.util.function.*;" +
                "public class M2 {" +
                "  public static Object test() {" +
                "    Function<String, String> f = String::toUpperCase;" +
                "    return f.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("methodRef_instanceMethodOnType: " + t); }
    }

    @Test
    public void methodRef_instanceMethodOnObject() {
        try {
            Object result = compileAndRun("t.M3",
                "package t;" +
                "import java.util.function.*;" +
                "public class M3 {" +
                "  public static Object test() {" +
                "    String str = \"HELLO\";" +
                "    Supplier<String> s = str::toLowerCase;" +
                "    return s.get();" +
                "  }" +
                "}");
            assertEquals("hello", result);
        } catch (Throwable t) { fail("methodRef_instanceMethodOnObject: " + t); }
    }

    @Test
    public void methodRef_constructorRef() {
        try {
            Object result = compileAndRun("t.M4",
                "package t;" +
                "import java.util.function.*;" +
                "public class M4 {" +
                "  public static Object test() {" +
                "    Function<String, StringBuilder> f = StringBuilder::new;" +
                "    StringBuilder sb = f.apply(\"hello\");" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("hello", result);
        } catch (Throwable t) { fail("methodRef_constructorRef: " + t); }
    }

    @Test
    public void methodRef_noArgConstructor() {
        try {
            Object result = compileAndRun("t.M5",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class M5 {" +
                "  public static Object test() {" +
                "    Supplier<ArrayList> s = ArrayList::new;" +
                "    ArrayList list = s.get();" +
                "    list.add(\"test\");" +
                "    return list.get(0);" +
                "  }" +
                "}");
            assertEquals("test", result);
        } catch (Throwable t) { fail("methodRef_noArgConstructor: " + t); }
    }

    @Test
    public void methodRef_staticMethodTwoArgs() {
        try {
            Object result = compileAndRun("t.M6",
                "package t;" +
                "import java.util.function.*;" +
                "public class M6 {" +
                "  public static Object test() {" +
                "    BiFunction<Integer, Integer, Integer> f = Integer::compare;" +
                "    return f.apply(5, 3);" +
                "  }" +
                "}");
            assertEquals(1, result);
        } catch (Throwable t) { fail("methodRef_staticMethodTwoArgs: " + t); }
    }

    @Test
    public void methodRef_instanceMethodTwoArgs() {
        try {
            Object result = compileAndRun("t.M7",
                "package t;" +
                "import java.util.function.*;" +
                "public class M7 {" +
                "  public static Object test() {" +
                "    BiPredicate<String, String> bp = String::startsWith;" +
                "    return bp.test(\"hello\", \"hel\") + \",\" + bp.test(\"hello\", \"xyz\");" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("methodRef_instanceMethodTwoArgs: " + t); }
    }

    @Test
    public void methodRef_arrayConstructor() {
        try {
            Object result = compileAndRun("t.M8",
                "package t;" +
                "import java.util.function.*;" +
                "public class M8 {" +
                "  public static Object test() {" +
                "    IntFunction<String[]> f = String[]::new;" +
                "    String[] arr = f.apply(5);" +
                "    return arr.length;" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("methodRef_arrayConstructor: " + t); }
    }

    @Test
    public void methodRef_asComparator() {
        try {
            Object result = compileAndRun("t.M9",
                "package t;" +
                "import java.util.*;" +
                "public class M9 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"banana\"); list.add(\"apple\"); list.add(\"cherry\");" +
                "    Collections.sort(list, String::compareToIgnoreCase);" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[apple, banana, cherry]", result);
        } catch (Throwable t) { fail("methodRef_asComparator: " + t); }
    }

    @Test
    public void methodRef_toPrimitiveFI() {
        try {
            Object result = compileAndRun("t.M10",
                "package t;" +
                "import java.util.function.*;" +
                "public class M10 {" +
                "  public static Object test() {" +
                "    ToIntFunction<String> f = String::length;" +
                "    return f.applyAsInt(\"hello\");" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("methodRef_toPrimitiveFI: " + t); }
    }

    // ========================================================================
    // CATEGORY 6: Variable Capture
    // ========================================================================

    @Test
    public void capture_finalLocal() {
        try {
            Object result = compileAndRun("t.V1",
                "package t;" +
                "import java.util.function.*;" +
                "public class V1 {" +
                "  public static Object test() {" +
                "    final int x = 10;" +
                "    Supplier<Integer> s = () -> x;" +
                "    return s.get();" +
                "  }" +
                "}");
            assertEquals(10, result);
        } catch (Throwable t) { fail("capture_finalLocal: " + t); }
    }

    @Test
    public void capture_effectivelyFinal() {
        try {
            Object result = compileAndRun("t.V2",
                "package t;" +
                "import java.util.function.*;" +
                "public class V2 {" +
                "  public static Object test() {" +
                "    int x = 20;" +
                "    Supplier<Integer> s = () -> x;" +
                "    return s.get();" +
                "  }" +
                "}");
            assertEquals(20, result);
        } catch (Throwable t) { fail("capture_effectivelyFinal: " + t); }
    }

    @Test
    public void capture_finalString() {
        try {
            Object result = compileAndRun("t.V3",
                "package t;" +
                "import java.util.function.*;" +
                "public class V3 {" +
                "  public static Object test() {" +
                "    final String prefix = \"hello \";" +
                "    Function<String, String> f = s -> prefix + s;" +
                "    return f.apply(\"world\");" +
                "  }" +
                "}");
            assertEquals("hello world", result);
        } catch (Throwable t) { fail("capture_finalString: " + t); }
    }

    @Test
    public void capture_multipleVars() {
        try {
            Object result = compileAndRun("t.V4",
                "package t;" +
                "import java.util.function.*;" +
                "public class V4 {" +
                "  public static Object test() {" +
                "    final int a = 10;" +
                "    final int b = 20;" +
                "    final int c = 12;" +
                "    Supplier<Integer> s = () -> a + b + c;" +
                "    return s.get();" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("capture_multipleVars: " + t); }
    }

    @Test
    public void capture_arrayMutation() {
        try {
            Object result = compileAndRun("t.V5",
                "package t;" +
                "import java.util.function.*;" +
                "public class V5 {" +
                "  public static Object test() {" +
                "    final int[] counter = {0};" +
                "    Runnable r = () -> counter[0]++;" +
                "    r.run(); r.run(); r.run();" +
                "    return counter[0];" +
                "  }" +
                "}");
            assertEquals(3, result);
        } catch (Throwable t) { fail("capture_arrayMutation: " + t); }
    }

    @Test
    public void capture_instanceField() {
        try {
            Object result = compileAndRun("t.V6",
                "package t;" +
                "import java.util.function.*;" +
                "public class V6 {" +
                "  private String value = \"field\";" +
                "  public Supplier<String> getSupplier() {" +
                "    return () -> value;" +
                "  }" +
                "  public static Object test() throws Exception {" +
                "    V6 obj = new V6();" +
                "    return obj.getSupplier().get();" +
                "  }" +
                "}");
            assertEquals("field", result);
        } catch (Throwable t) { fail("capture_instanceField: " + t); }
    }

    @Test
    public void capture_thisReference() {
        try {
            Object result = compileAndRun("t.V7",
                "package t;" +
                "import java.util.function.*;" +
                "public class V7 {" +
                "  private String name = \"obj\";" +
                "  public String getName() { return name; }" +
                "  public Supplier<String> getNameSupplier() {" +
                "    return () -> this.getName();" +
                "  }" +
                "  public static Object test() throws Exception {" +
                "    V7 obj = new V7();" +
                "    return obj.getNameSupplier().get();" +
                "  }" +
                "}");
            assertEquals("obj", result);
        } catch (Throwable t) { fail("capture_thisReference: " + t); }
    }

    @Test
    public void capture_methodParam() {
        try {
            Object result = compileAndRun("t.V8",
                "package t;" +
                "import java.util.function.*;" +
                "public class V8 {" +
                "  public static Supplier<String> makeSupplier(String val) {" +
                "    return () -> val;" +
                "  }" +
                "  public static Object test() {" +
                "    return makeSupplier(\"param\").get();" +
                "  }" +
                "}");
            assertEquals("param", result);
        } catch (Throwable t) { fail("capture_methodParam: " + t); }
    }

    @Test
    public void capture_loopVariable() {
        try {
            Object result = compileAndRun("t.V9",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class V9 {" +
                "  public static Object test() {" +
                "    List<Supplier<Integer>> list = new ArrayList<Supplier<Integer>>();" +
                "    for (int i = 0; i < 3; i++) {" +
                "      final int val = i;" +
                "      list.add(() -> val);" +
                "    }" +
                "    return list.get(0).get() + \",\" + list.get(1).get() + \",\" + list.get(2).get();" +
                "  }" +
                "}");
            assertEquals("0,1,2", result);
        } catch (Throwable t) { fail("capture_loopVariable: " + t); }
    }

    // ========================================================================
    // CATEGORY 7: Nested / Chained Lambdas
    // ========================================================================

    @Test
    public void nested_lambdaReturningLambda() {
        try {
            Object result = compileAndRun("t.N1",
                "package t;" +
                "import java.util.function.*;" +
                "public class N1 {" +
                "  public static Object test() {" +
                "    Function<Integer, Function<Integer, Integer>> f = a -> b -> a + b;" +
                "    return f.apply(10).apply(32);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("nested_lambdaReturningLambda: " + t); }
    }

    @Test
    public void nested_lambdaInsideLambda() {
        try {
            Object result = compileAndRun("t.N2",
                "package t;" +
                "import java.util.function.*;" +
                "public class N2 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> f = x -> {" +
                "      Function<Integer, Integer> g = y -> y * 2;" +
                "      return g.apply(x) + 1;" +
                "    };" +
                "    return f.apply(20);" +
                "  }" +
                "}");
            assertEquals(41, result);
        } catch (Throwable t) { fail("nested_lambdaInsideLambda: " + t); }
    }

    @Test
    public void nested_tripleNesting() {
        try {
            Object result = compileAndRun("t.N3",
                "package t;" +
                "import java.util.function.*;" +
                "public class N3 {" +
                "  public static Object test() {" +
                "    Function<Integer, Function<Integer, Function<Integer, Integer>>> f =" +
                "      a -> b -> c -> a + b + c;" +
                "    return f.apply(10).apply(20).apply(12);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("nested_tripleNesting: " + t); }
    }

    @Test
    public void nested_lambdaInConditional() {
        try {
            Object result = compileAndRun("t.N4",
                "package t;" +
                "import java.util.function.*;" +
                "public class N4 {" +
                "  public static Object test() {" +
                "    boolean flag = true;" +
                "    Function<String, String> f = flag ? (s -> s.toUpperCase()) : (s -> s.toLowerCase());" +
                "    return f.apply(\"Hello\");" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("nested_lambdaInConditional: " + t); }
    }

    // ========================================================================
    // CATEGORY 8: Lambda as Argument
    // ========================================================================

    @Test
    public void lambdaArg_toMethod() {
        try {
            Object result = compileAndRun("t.A1",
                "package t;" +
                "import java.util.function.*;" +
                "public class A1 {" +
                "  static String transform(String s, Function<String, String> f) { return f.apply(s); }" +
                "  public static Object test() {" +
                "    return transform(\"hello\", s -> s.toUpperCase());" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("lambdaArg_toMethod: " + t); }
    }

    @Test
    public void lambdaArg_toConstructor() {
        try {
            Object result = compileAndRun("t.A2",
                "package t;" +
                "import java.util.function.*;" +
                "public class A2 {" +
                "  static class Wrapper {" +
                "    String value;" +
                "    Wrapper(Supplier<String> s) { this.value = s.get(); }" +
                "  }" +
                "  public static Object test() {" +
                "    Wrapper w = new Wrapper(() -> \"from-lambda\");" +
                "    return w.value;" +
                "  }" +
                "}");
            assertEquals("from-lambda", result);
        } catch (Throwable t) { fail("lambdaArg_toConstructor: " + t); }
    }

    @Test
    public void lambdaArg_inChainedCall() {
        try {
            Object result = compileAndRun("t.A3",
                "package t;" +
                "import java.util.*;" +
                "public class A3 {" +
                "  public static Object test() {" +
                "    List<String> list = new ArrayList<String>();" +
                "    list.add(\"banana\"); list.add(\"apple\"); list.add(\"cherry\");" +
                "    list.sort((a, b) -> a.compareTo(b));" +
                "    return list.toString();" +
                "  }" +
                "}");
            assertEquals("[apple, banana, cherry]", result);
        } catch (Throwable t) { fail("lambdaArg_inChainedCall: " + t); }
    }

    @Test
    public void lambdaArg_multipleArgs() {
        try {
            Object result = compileAndRun("t.A4",
                "package t;" +
                "import java.util.function.*;" +
                "public class A4 {" +
                "  static String combine(Function<Integer, String> f, Supplier<Integer> s) {" +
                "    return f.apply(s.get());" +
                "  }" +
                "  public static Object test() {" +
                "    return combine(n -> \"val:\" + n, () -> 42);" +
                "  }" +
                "}");
            assertEquals("val:42", result);
        } catch (Throwable t) { fail("lambdaArg_multipleArgs: " + t); }
    }

    @Test
    public void lambdaArg_toThread() {
        try {
            Object result = compileAndRun("t.A5",
                "package t;" +
                "public class A5 {" +
                "  public static Object test() throws Exception {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Thread t = new Thread(() -> sb.append(\"threaded\"));" +
                "    t.start();" +
                "    t.join();" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("threaded", result);
        } catch (Throwable t) { fail("lambdaArg_toThread: " + t); }
    }

    // ========================================================================
    // CATEGORY 9: Predicate Composition & Default Methods on FI
    // ========================================================================

    @Test
    public void predicateComposition_and() {
        try {
            Object result = compileAndRun("t.P1",
                "package t;" +
                "import java.util.function.*;" +
                "public class P1 {" +
                "  public static Object test() {" +
                "    Predicate<Integer> positive = n -> n > 0;" +
                "    Predicate<Integer> even = n -> n % 2 == 0;" +
                "    Predicate<Integer> positiveAndEven = positive.and(even);" +
                "    return positiveAndEven.test(4) + \",\" + positiveAndEven.test(-2) + \",\" + positiveAndEven.test(3);" +
                "  }" +
                "}");
            assertEquals("true,false,false", result);
        } catch (Throwable t) { fail("predicateComposition_and: " + t); }
    }

    @Test
    public void predicateComposition_or() {
        try {
            Object result = compileAndRun("t.P2",
                "package t;" +
                "import java.util.function.*;" +
                "public class P2 {" +
                "  public static Object test() {" +
                "    Predicate<Integer> positive = n -> n > 0;" +
                "    Predicate<Integer> zero = n -> n == 0;" +
                "    Predicate<Integer> nonNeg = positive.or(zero);" +
                "    return nonNeg.test(1) + \",\" + nonNeg.test(0) + \",\" + nonNeg.test(-1);" +
                "  }" +
                "}");
            assertEquals("true,true,false", result);
        } catch (Throwable t) { fail("predicateComposition_or: " + t); }
    }

    @Test
    public void predicateComposition_negate() {
        try {
            Object result = compileAndRun("t.P3",
                "package t;" +
                "import java.util.function.*;" +
                "public class P3 {" +
                "  public static Object test() {" +
                "    Predicate<String> empty = s -> s.isEmpty();" +
                "    Predicate<String> notEmpty = empty.negate();" +
                "    return notEmpty.test(\"hello\") + \",\" + notEmpty.test(\"\");" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("predicateComposition_negate: " + t); }
    }

    @Test
    public void functionComposition_andThen() {
        try {
            Object result = compileAndRun("t.P4",
                "package t;" +
                "import java.util.function.*;" +
                "public class P4 {" +
                "  public static Object test() {" +
                "    Function<String, String> upper = s -> s.toUpperCase();" +
                "    Function<String, String> exclaim = s -> s + \"!\";" +
                "    Function<String, String> composed = upper.andThen(exclaim);" +
                "    return composed.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals("HELLO!", result);
        } catch (Throwable t) { fail("functionComposition_andThen: " + t); }
    }

    @Test
    public void functionComposition_compose() {
        try {
            Object result = compileAndRun("t.P5",
                "package t;" +
                "import java.util.function.*;" +
                "public class P5 {" +
                "  public static Object test() {" +
                "    Function<String, String> upper = s -> s.toUpperCase();" +
                "    Function<String, String> exclaim = s -> s + \"!\";" +
                "    Function<String, String> composed = upper.compose(exclaim);" +
                "    return composed.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals("HELLO!", result);
        } catch (Throwable t) { fail("functionComposition_compose: " + t); }
    }

    @Test
    public void consumerComposition_andThen() {
        try {
            Object result = compileAndRun("t.P6",
                "package t;" +
                "import java.util.function.*;" +
                "public class P6 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Consumer<String> first = s -> sb.append(s);" +
                "    Consumer<String> second = s -> sb.append(s.toUpperCase());" +
                "    Consumer<String> both = first.andThen(second);" +
                "    both.accept(\"hi\");" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("hiHI", result);
        } catch (Throwable t) { fail("consumerComposition_andThen: " + t); }
    }

    // ========================================================================
    // CATEGORY 10: Streams with Lambdas
    // ========================================================================

    @Test
    public void stream_filter() {
        try {
            Object result = compileAndRun("t.S1",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S1 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);" +
                "    List<Integer> evens = list.stream().filter(n -> n % 2 == 0).collect(Collectors.toList());" +
                "    return evens.toString();" +
                "  }" +
                "}");
            assertEquals("[2, 4, 6]", result);
        } catch (Throwable t) { fail("stream_filter: " + t); }
    }

    @Test
    public void stream_map() {
        try {
            Object result = compileAndRun("t.S2",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S2 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"a\", \"b\", \"c\");" +
                "    List<String> upper = list.stream().map(s -> s.toUpperCase()).collect(Collectors.toList());" +
                "    return upper.toString();" +
                "  }" +
                "}");
            assertEquals("[A, B, C]", result);
        } catch (Throwable t) { fail("stream_map: " + t); }
    }

    @Test
    public void stream_reduce() {
        try {
            Object result = compileAndRun("t.S3",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S3 {" +
                "  public static Object test() {" +
                "    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);" +
                "    int sum = list.stream().reduce(0, (a, b) -> a + b);" +
                "    return sum;" +
                "  }" +
                "}");
            assertEquals(15, result);
        } catch (Throwable t) { fail("stream_reduce: " + t); }
    }

    @Test
    public void stream_forEach() {
        try {
            Object result = compileAndRun("t.S4",
                "package t;" +
                "import java.util.*;" +
                "public class S4 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Arrays.asList(\"x\", \"y\", \"z\").forEach(s -> sb.append(s));" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("xyz", result);
        } catch (Throwable t) { fail("stream_forEach: " + t); }
    }

    @Test
    public void stream_sorted() {
        try {
            Object result = compileAndRun("t.S5",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S5 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"banana\", \"apple\", \"cherry\");" +
                "    List<String> sorted = list.stream().sorted((a, b) -> a.compareTo(b)).collect(Collectors.toList());" +
                "    return sorted.toString();" +
                "  }" +
                "}");
            assertEquals("[apple, banana, cherry]", result);
        } catch (Throwable t) { fail("stream_sorted: " + t); }
    }

    @Test
    public void stream_chainedOperations() {
        try {
            Object result = compileAndRun("t.S6",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S6 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"hello\", \"hi\", \"hey\", \"world\", \"wow\");" +
                "    String result = list.stream()" +
                "      .filter(s -> s.startsWith(\"h\"))" +
                "      .map(s -> s.toUpperCase())" +
                "      .sorted((a, b) -> a.compareTo(b))" +
                "      .collect(Collectors.joining(\",\"));" +
                "    return result;" +
                "  }" +
                "}");
            assertEquals("HELLO,HEY,HI", result);
        } catch (Throwable t) { fail("stream_chainedOperations: " + t); }
    }

    @Test
    public void stream_mapToInt() {
        try {
            Object result = compileAndRun("t.S7",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S7 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"a\", \"bb\", \"ccc\");" +
                "    int sum = list.stream().mapToInt(s -> s.length()).sum();" +
                "    return sum;" +
                "  }" +
                "}");
            assertEquals(6, result);
        } catch (Throwable t) { fail("stream_mapToInt: " + t); }
    }

    @Test
    public void stream_flatMap() {
        try {
            Object result = compileAndRun("t.S8",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S8 {" +
                "  public static Object test() {" +
                "    List<List<Integer>> lists = Arrays.asList(" +
                "      Arrays.asList(1, 2), Arrays.asList(3, 4));" +
                "    List<Integer> flat = lists.stream()" +
                "      .flatMap(l -> l.stream())" +
                "      .collect(Collectors.toList());" +
                "    return flat.toString();" +
                "  }" +
                "}");
            assertEquals("[1, 2, 3, 4]", result);
        } catch (Throwable t) { fail("stream_flatMap: " + t); }
    }

    @Test
    public void stream_collect_groupingBy() {
        try {
            Object result = compileAndRun("t.S9",
                "package t;" +
                "import java.util.*;" +
                "import java.util.stream.*;" +
                "public class S9 {" +
                "  public static Object test() {" +
                "    List<String> list = Arrays.asList(\"aa\", \"b\", \"cc\", \"d\");" +
                "    Map<Integer, List<String>> grouped = list.stream()" +
                "      .collect(Collectors.groupingBy(s -> s.length()));" +
                "    return grouped.get(1).size() + \",\" + grouped.get(2).size();" +
                "  }" +
                "}");
            assertEquals("2,2", result);
        } catch (Throwable t) { fail("stream_collect_groupingBy: " + t); }
    }

    // ========================================================================
    // CATEGORY 11: Type Inference Edge Cases
    // ========================================================================

    @Test
    public void typeInference_returnType() {
        try {
            Object result = compileAndRun("t.I1",
                "package t;" +
                "import java.util.function.*;" +
                "public class I1 {" +
                "  static <T> T apply(Supplier<T> s) { return s.get(); }" +
                "  public static Object test() {" +
                "    String s = apply(() -> \"inferred\");" +
                "    return s;" +
                "  }" +
                "}");
            assertEquals("inferred", result);
        } catch (Throwable t) { fail("typeInference_returnType: " + t); }
    }

    @Test
    public void typeInference_lambdaInGenericMethod() {
        try {
            Object result = compileAndRun("t.I2",
                "package t;" +
                "import java.util.function.*;" +
                "public class I2 {" +
                "  static <T, R> R transform(T input, Function<T, R> f) { return f.apply(input); }" +
                "  public static Object test() {" +
                "    int len = transform(\"hello\", s -> s.length());" +
                "    return len;" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("typeInference_lambdaInGenericMethod: " + t); }
    }

    @Test
    public void typeInference_diamondWithLambda() {
        try {
            Object result = compileAndRun("t.I3",
                "package t;" +
                "import java.util.*;" +
                "import java.util.function.*;" +
                "public class I3 {" +
                "  public static Object test() {" +
                "    Map<String, Function<String, String>> map = new HashMap<>();" +
                "    map.put(\"upper\", s -> s.toUpperCase());" +
                "    return map.get(\"upper\").apply(\"test\");" +
                "  }" +
                "}");
            assertEquals("TEST", result);
        } catch (Throwable t) { fail("typeInference_diamondWithLambda: " + t); }
    }

    @Test
    public void typeInference_nullReturn() {
        try {
            Object result = compileAndRun("t.I4",
                "package t;" +
                "import java.util.function.*;" +
                "public class I4 {" +
                "  public static Object test() {" +
                "    Function<String, String> f = s -> null;" +
                "    String result = f.apply(\"anything\");" +
                "    return result == null ? \"null\" : result;" +
                "  }" +
                "}");
            assertEquals("null", result);
        } catch (Throwable t) { fail("typeInference_nullReturn: " + t); }
    }

    @Test
    public void typeInference_overloadedMethod() {
        try {
            Object result = compileAndRun("t.I5",
                "package t;" +
                "import java.util.function.*;" +
                "public class I5 {" +
                "  static String run(Runnable r) { r.run(); return \"runnable\"; }" +
                "  static String run(Supplier<String> s) { return s.get(); }" +
                "  public static Object test() {" +
                "    return run(() -> \"supplier\");" +
                "  }" +
                "}");
            // Either result is valid; we just want to see if it compiles
            assertNotNull(result);
        } catch (Throwable t) { fail("typeInference_overloadedMethod: " + t); }
    }

    @Test
    public void typeInference_assignToObject() {
        try {
            Object result = compileAndRun("t.I6",
                "package t;" +
                "import java.util.function.*;" +
                "public class I6 {" +
                "  public static Object test() {" +
                "    Function<String, String> f = s -> s + \"!\";" +
                "    Object o = f;" +
                "    Function<String, String> g = (Function<String, String>) o;" +
                "    return g.apply(\"test\");" +
                "  }" +
                "}");
            assertEquals("test!", result);
        } catch (Throwable t) { fail("typeInference_assignToObject: " + t); }
    }

    // ========================================================================
    // CATEGORY 12: Lambda with Generics
    // ========================================================================

    @Test
    public void generics_genericLambdaReturnType() {
        try {
            Object result = compileAndRun("t.G1",
                "package t;" +
                "import java.util.function.*;" +
                "import java.util.*;" +
                "public class G1 {" +
                "  public static Object test() {" +
                "    Function<String, List<String>> f = s -> {" +
                "      List<String> list = new ArrayList<String>();" +
                "      list.add(s); list.add(s);" +
                "      return list;" +
                "    };" +
                "    return f.apply(\"x\").toString();" +
                "  }" +
                "}");
            assertEquals("[x, x]", result);
        } catch (Throwable t) { fail("generics_genericLambdaReturnType: " + t); }
    }

    @Test
    public void generics_wildcardBound() {
        try {
            Object result = compileAndRun("t.G2",
                "package t;" +
                "import java.util.function.*;" +
                "public class G2 {" +
                "  public static Object test() {" +
                "    Function<? super String, ? extends Object> f = s -> s.length();" +
                "    Object result = f.apply(\"test\");" +
                "    return result;" +
                "  }" +
                "}");
            assertEquals(4, result);
        } catch (Throwable t) { fail("generics_wildcardBound: " + t); }
    }

    @Test
    public void generics_functionInGenericClass() {
        try {
            Object result = compileAndRun("t.G3",
                "package t;" +
                "import java.util.function.*;" +
                "public class G3 {" +
                "  static class Box<T> {" +
                "    T value;" +
                "    Box(T value) { this.value = value; }" +
                "    <R> Box<R> map(Function<T, R> f) { return new Box<R>(f.apply(value)); }" +
                "  }" +
                "  public static Object test() {" +
                "    Box<String> box = new Box<String>(\"hello\");" +
                "    Box<Integer> mapped = box.map(s -> ((String) s).length());" +
                "    return mapped.value;" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("generics_functionInGenericClass: " + t); }
    }

    // ========================================================================
    // CATEGORY 13: Edge Cases & Corner Cases
    // ========================================================================

    @Test
    public void edge_emptyBody() {
        try {
            Object result = compileAndRun("t.E1",
                "package t;" +
                "public class E1 {" +
                "  public static Object test() {" +
                "    Runnable r = () -> {};" +
                "    r.run();" +
                "    return \"ok\";" +
                "  }" +
                "}");
            assertEquals("ok", result);
        } catch (Throwable t) { fail("edge_emptyBody: " + t); }
    }

    @Test
    public void edge_singleStatementBlock() {
        try {
            Object result = compileAndRun("t.E2",
                "package t;" +
                "import java.util.function.*;" +
                "public class E2 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> f = x -> { return x; };" +
                "    return f.apply(42);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("edge_singleStatementBlock: " + t); }
    }

    @Test
    public void edge_lambdaThrowsUnchecked() {
        try {
            Object result = compileAndRun("t.E3",
                "package t;" +
                "import java.util.function.*;" +
                "public class E3 {" +
                "  public static Object test() {" +
                "    Function<String, Integer> f = s -> {" +
                "      if (s == null) throw new IllegalArgumentException(\"null!\");" +
                "      return s.length();" +
                "    };" +
                "    try { f.apply(null); return \"no exception\"; }" +
                "    catch (IllegalArgumentException e) { return e.getMessage(); }" +
                "  }" +
                "}");
            assertEquals("null!", result);
        } catch (Throwable t) { fail("edge_lambdaThrowsUnchecked: " + t); }
    }

    @Test
    public void edge_lambdaWithInstanceof() {
        try {
            Object result = compileAndRun("t.E4",
                "package t;" +
                "import java.util.function.*;" +
                "public class E4 {" +
                "  public static Object test() {" +
                "    Function<Object, String> f = o -> o instanceof String ? \"str\" : \"other\";" +
                "    return f.apply(\"hello\") + \",\" + f.apply(42);" +
                "  }" +
                "}");
            assertEquals("str,other", result);
        } catch (Throwable t) { fail("edge_lambdaWithInstanceof: " + t); }
    }

    @Test
    public void edge_lambdaAccessingStatic() {
        try {
            Object result = compileAndRun("t.E5",
                "package t;" +
                "import java.util.function.*;" +
                "public class E5 {" +
                "  static String PREFIX = \"pre_\";" +
                "  public static Object test() {" +
                "    Function<String, String> f = s -> PREFIX + s;" +
                "    return f.apply(\"test\");" +
                "  }" +
                "}");
            assertEquals("pre_test", result);
        } catch (Throwable t) { fail("edge_lambdaAccessingStatic: " + t); }
    }

    @Test
    public void edge_lambdaWithArrayParam() {
        try {
            Object result = compileAndRun("t.E6",
                "package t;" +
                "import java.util.function.*;" +
                "public class E6 {" +
                "  interface ArrayOp { int apply(int[] arr); }" +
                "  public static Object test() {" +
                "    ArrayOp sum = arr -> {" +
                "      int s = 0; for (int v : arr) s += v; return s;" +
                "    };" +
                "    return sum.apply(new int[]{1, 2, 3, 4, 5});" +
                "  }" +
                "}");
            assertEquals(15, result);
        } catch (Throwable t) { fail("edge_lambdaWithArrayParam: " + t); }
    }

    @Test
    public void edge_lambdaReturningArray() {
        try {
            Object result = compileAndRun("t.E7",
                "package t;" +
                "import java.util.function.*;" +
                "public class E7 {" +
                "  public static Object test() {" +
                "    Function<Integer, int[]> f = n -> new int[]{n, n * 2, n * 3};" +
                "    int[] arr = f.apply(5);" +
                "    return arr[0] + \",\" + arr[1] + \",\" + arr[2];" +
                "  }" +
                "}");
            assertEquals("5,10,15", result);
        } catch (Throwable t) { fail("edge_lambdaReturningArray: " + t); }
    }

    @Test
    public void edge_lambdaWithStringSwitch() {
        try {
            Object result = compileAndRun("t.E8",
                "package t;" +
                "import java.util.function.*;" +
                "public class E8 {" +
                "  public static Object test() {" +
                "    Function<String, Integer> f = s -> {" +
                "      switch (s) {" +
                "        case \"a\": return 1;" +
                "        case \"b\": return 2;" +
                "        default: return 0;" +
                "      }" +
                "    };" +
                "    return f.apply(\"a\") + \",\" + f.apply(\"b\") + \",\" + f.apply(\"c\");" +
                "  }" +
                "}");
            assertEquals("1,2,0", result);
        } catch (Throwable t) { fail("edge_lambdaWithStringSwitch: " + t); }
    }

    @Test
    public void edge_recursiveViaHolder() {
        try {
            Object result = compileAndRun("t.E9",
                "package t;" +
                "import java.util.function.*;" +
                "public class E9 {" +
                "  public static Object test() {" +
                "    final Function<Integer, Integer>[] holder = new Function[1];" +
                "    holder[0] = n -> n <= 1 ? 1 : n * holder[0].apply(n - 1);" +
                "    return holder[0].apply(5);" +
                "  }" +
                "}");
            assertEquals(120, result);
        } catch (Throwable t) { fail("edge_recursiveViaHolder: " + t); }
    }

    @Test
    public void edge_lambdaVarargs() {
        try {
            Object result = compileAndRun("t.E10",
                "package t;" +
                "public class E10 {" +
                "  interface VarargFunc { String apply(String... args); }" +
                "  public static Object test() {" +
                "    VarargFunc f = args -> {" +
                "      StringBuilder sb = new StringBuilder();" +
                "      for (String a : args) sb.append(a);" +
                "      return sb.toString();" +
                "    };" +
                "    return f.apply(\"a\", \"b\", \"c\");" +
                "  }" +
                "}");
            assertEquals("abc", result);
        } catch (Throwable t) { fail("edge_lambdaVarargs: " + t); }
    }

    @Test
    public void edge_lambdaInStaticInit() {
        try {
            Object result = compileAndRun("t.E11",
                "package t;" +
                "import java.util.function.*;" +
                "public class E11 {" +
                "  static Function<Integer, Integer> DOUBLER = x -> x * 2;" +
                "  public static Object test() {" +
                "    return DOUBLER.apply(21);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("edge_lambdaInStaticInit: " + t); }
    }

    @Test
    public void edge_lambdaInInstanceInit() {
        try {
            Object result = compileAndRun("t.E12",
                "package t;" +
                "import java.util.function.*;" +
                "public class E12 {" +
                "  Function<String, String> f = s -> s + \"!\";" +
                "  public static Object test() {" +
                "    E12 obj = new E12();" +
                "    return obj.f.apply(\"hello\");" +
                "  }" +
                "}");
            assertEquals("hello!", result);
        } catch (Throwable t) { fail("edge_lambdaInInstanceInit: " + t); }
    }

    @Test
    public void edge_castToFunctionalInterface() {
        try {
            Object result = compileAndRun("t.E13",
                "package t;" +
                "import java.util.function.*;" +
                "public class E13 {" +
                "  public static Object test() {" +
                "    Object o = (Function<String, String>) s -> s.toUpperCase();" +
                "    Function<String, String> f = (Function<String, String>) o;" +
                "    return f.apply(\"cast\");" +
                "  }" +
                "}");
            assertEquals("CAST", result);
        } catch (Throwable t) { fail("edge_castToFunctionalInterface: " + t); }
    }

    @Test
    public void edge_lambdaWithEnhancedFor() {
        try {
            Object result = compileAndRun("t.E14",
                "package t;" +
                "import java.util.*;" +
                "import java.util.function.*;" +
                "public class E14 {" +
                "  public static Object test() {" +
                "    Function<List<String>, String> f = list -> {" +
                "      StringBuilder sb = new StringBuilder();" +
                "      for (String s : list) sb.append(s);" +
                "      return sb.toString();" +
                "    };" +
                "    return f.apply(Arrays.asList(\"a\", \"b\", \"c\"));" +
                "  }" +
                "}");
            assertEquals("abc", result);
        } catch (Throwable t) { fail("edge_lambdaWithEnhancedFor: " + t); }
    }

    // ========================================================================
    // CATEGORY 14: Optional with Lambdas
    // ========================================================================

    @Test
    public void optional_map() {
        try {
            Object result = compileAndRun("t.O1",
                "package t;" +
                "import java.util.*;" +
                "public class O1 {" +
                "  public static Object test() {" +
                "    Optional<String> opt = Optional.of(\"hello\");" +
                "    return opt.map(s -> s.toUpperCase()).get();" +
                "  }" +
                "}");
            assertEquals("HELLO", result);
        } catch (Throwable t) { fail("optional_map: " + t); }
    }

    @Test
    public void optional_orElseGet() {
        try {
            Object result = compileAndRun("t.O2",
                "package t;" +
                "import java.util.*;" +
                "public class O2 {" +
                "  public static Object test() {" +
                "    Optional<String> opt = Optional.empty();" +
                "    return opt.orElseGet(() -> \"default\");" +
                "  }" +
                "}");
            assertEquals("default", result);
        } catch (Throwable t) { fail("optional_orElseGet: " + t); }
    }

    @Test
    public void optional_filter() {
        try {
            Object result = compileAndRun("t.O3",
                "package t;" +
                "import java.util.*;" +
                "public class O3 {" +
                "  public static Object test() {" +
                "    Optional<String> opt = Optional.of(\"hello\");" +
                "    boolean present = opt.filter(s -> s.length() > 3).isPresent();" +
                "    boolean absent = opt.filter(s -> s.length() > 10).isPresent();" +
                "    return present + \",\" + absent;" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("optional_filter: " + t); }
    }

    @Test
    public void optional_flatMap() {
        try {
            Object result = compileAndRun("t.O4",
                "package t;" +
                "import java.util.*;" +
                "public class O4 {" +
                "  public static Object test() {" +
                "    Optional<String> opt = Optional.of(\"hello\");" +
                "    Optional<Integer> len = opt.flatMap(s -> Optional.of(s.length()));" +
                "    return len.get();" +
                "  }" +
                "}");
            assertEquals(5, result);
        } catch (Throwable t) { fail("optional_flatMap: " + t); }
    }

    @Test
    public void optional_ifPresent() {
        try {
            Object result = compileAndRun("t.O5",
                "package t;" +
                "import java.util.*;" +
                "public class O5 {" +
                "  public static Object test() {" +
                "    final StringBuilder sb = new StringBuilder();" +
                "    Optional.of(\"present\").ifPresent(s -> sb.append(s));" +
                "    Optional.empty().ifPresent(s -> sb.append(\"WRONG\"));" +
                "    return sb.toString();" +
                "  }" +
                "}");
            assertEquals("present", result);
        } catch (Throwable t) { fail("optional_ifPresent: " + t); }
    }

    // ========================================================================
    // CATEGORY 15: Autoboxing / Unboxing with Lambdas
    // ========================================================================

    @Test
    public void autobox_intToInteger() {
        try {
            Object result = compileAndRun("t.X1",
                "package t;" +
                "import java.util.function.*;" +
                "public class X1 {" +
                "  public static Object test() {" +
                "    Function<Integer, Integer> f = x -> x + 1;" +
                "    return f.apply(41);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("autobox_intToInteger: " + t); }
    }

    @Test
    public void autobox_primitiveConsumer() {
        try {
            Object result = compileAndRun("t.X2",
                "package t;" +
                "import java.util.function.*;" +
                "public class X2 {" +
                "  public static Object test() {" +
                "    final int[] holder = {0};" +
                "    Consumer<Integer> c = n -> holder[0] = n;" +
                "    c.accept(42);" +
                "    return holder[0];" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("autobox_primitiveConsumer: " + t); }
    }

    @Test
    public void autobox_intToIntFunction() {
        try {
            Object result = compileAndRun("t.X3",
                "package t;" +
                "import java.util.function.*;" +
                "public class X3 {" +
                "  public static Object test() {" +
                "    IntFunction<Integer> f = n -> n * 2;" +
                "    return f.apply(21);" +
                "  }" +
                "}");
            assertEquals(42, result);
        } catch (Throwable t) { fail("autobox_intToIntFunction: " + t); }
    }

    @Test
    public void autobox_booleanPredicate() {
        try {
            Object result = compileAndRun("t.X4",
                "package t;" +
                "import java.util.function.*;" +
                "public class X4 {" +
                "  public static Object test() {" +
                "    Predicate<Boolean> p = b -> !b;" +
                "    return p.test(false) + \",\" + p.test(true);" +
                "  }" +
                "}");
            assertEquals("true,false", result);
        } catch (Throwable t) { fail("autobox_booleanPredicate: " + t); }
    }
}
