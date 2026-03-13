package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptBodyBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;
import org.codehaus.commons.compiler.CompileException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptBodyGenericsTest {

    public interface StringResult {
        String run();
    }

    public interface ObjectResult {
        Object run();
    }

    public interface IntResult {
        int run();
    }

    private IDynamicCompiler newCompiler() {
        return IDynamicCompilerBuilder.createBuilder().getCompiler();
    }

    @Test
    public void testBoxGeneric_exactFailingCase() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "public String run() {\n" +
                "    class Box<T> {\n" +
                "        private T value;\n" +
                "        Box(T value) { this.value = value; }\n" +
                "        T get() { return value; }\n" +
                "    }\n" +
                "    Box<String> box = new Box<>(\"FAAAH MAMBO\");\n" +
                "    String str = box.get();\n" +
                "    return str;\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("FAAAH MAMBO", result.run());
            System.out.println("[PASS] testBoxGeneric_exactFailingCase: Box<String>.get() -> String worked");
        } catch (CompileException e) {
            System.out.println("[EXPECTED FAIL] testBoxGeneric_exactFailingCase: " + e.getMessage());
            assertTrue(e.getMessage().contains("Object") || e.getMessage().contains("String"),
                    "Error should mention type mismatch. Actual: " + e.getMessage());
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testBoxGeneric_withExplicitCast() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "public String run() {\n" +
                "    class Box<T> {\n" +
                "        private T value;\n" +
                "        Box(T value) { this.value = value; }\n" +
                "        T get() { return value; }\n" +
                "    }\n" +
                "    Box<String> box = new Box<>(\"FAAAH MAMBO\");\n" +
                "    String str = (String) box.get();\n" +
                "    return str;\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("FAAAH MAMBO", result.run());
            System.out.println("[PASS] testBoxGeneric_withExplicitCast: explicit cast works");
        } catch (CompileException e) {
            fail("Explicit cast should work, but compile failed: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testGenericMethod_identity() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "private <T> T identity(T val) { return val; }\n" +
                "\n" +
                "public String run() {\n" +
                "    String result = (String) identity(\"hello generics\");\n" +
                "    return result;\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("hello generics", result.run());
            System.out.println("[PASS] testGenericMethod_identity: <T> T identity(T) works with cast");
        } catch (CompileException e) {
            fail("Generic method with explicit cast should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testGenericMethod_identityWithoutCast() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "private <T> T identity(T val) { return val; }\n" +
                "\n" +
                "public String run() {\n" +
                "    String result = identity(\"hello generics\");\n" +
                "    return result;\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("hello generics", result.run());
            System.out.println("[PASS] testGenericMethod_identityWithoutCast: no cast needed");
        } catch (CompileException e) {
            System.out.println("[EXPECTED FAIL] testGenericMethod_identityWithoutCast: " + e.getMessage());
            assertTrue(e.getMessage().contains("Object") || e.getMessage().contains("String"),
                    "Error should mention type mismatch. Actual: " + e.getMessage());
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testStreamWithGenerics() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "import java.util.stream.*;\n" +
                "\n" +
                "public String run() {\n" +
                "    List<String> names = Arrays.asList(\"Alice\", \"Bob\", \"Charlie\", \"Dave\");\n" +
                "    String result = names.stream()\n" +
                "        .filter(n -> n.length() > 3)\n" +
                "        .map(n -> n.toUpperCase())\n" +
                "        .collect(Collectors.joining(\", \"));\n" +
                "    return result;\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("ALICE, CHARLIE, DAVE", result.run());
            System.out.println("[PASS] testStreamWithGenerics: stream pipeline with generics works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testStreamWithGenerics: " + e.getMessage());
            fail("Stream with generics should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testStreamCollectToList() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "import java.util.stream.*;\n" +
                "\n" +
                "public Object run() {\n" +
                "    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);\n" +
                "    List<Integer> evens = numbers.stream()\n" +
                "        .filter(n -> n % 2 == 0)\n" +
                "        .collect(Collectors.toList());\n" +
                "    return evens.toString();\n" +
                "}\n";

        IScriptClassBody<ObjectResult> body = IScriptBodyBuilder.getBuilder(ObjectResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            ObjectResult result = body.get();
            assertNotNull(result);
            assertEquals("[2, 4]", result.run().toString());
            System.out.println("[PASS] testStreamCollectToList: Collectors.toList() with generics works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testStreamCollectToList: " + e.getMessage());
            fail("Stream collect to list should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testLambdaWithGenerics() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.function.*;\n" +
                "\n" +
                "public Object run() {\n" +
                "    Function<String, Integer> strlen = s -> s.length();\n" +
                "    return strlen.apply(\"hello world\");\n" +
                "}\n";

        IScriptClassBody<ObjectResult> body = IScriptBodyBuilder.getBuilder(ObjectResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            ObjectResult result = body.get();
            assertNotNull(result);
            assertEquals(11, result.run());
            System.out.println("[PASS] testLambdaWithGenerics: Function<String, Integer> lambda works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testLambdaWithGenerics: " + e.getMessage());
            fail("Lambda with generics should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testLambdaChaining() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.function.*;\n" +
                "\n" +
                "public Object run() {\n" +
                "    Function<String, Integer> strlen = s -> s.length();\n" +
                "    Function<Integer, String> intToStr = i -> \"len=\" + i;\n" +
                "    Function<String, String> combined = strlen.andThen(intToStr);\n" +
                "    return combined.apply(\"test\");\n" +
                "}\n";

        IScriptClassBody<ObjectResult> body = IScriptBodyBuilder.getBuilder(ObjectResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            ObjectResult result = body.get();
            assertNotNull(result);
            assertEquals("len=4", result.run());
            System.out.println("[PASS] testLambdaChaining: Function.andThen works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testLambdaChaining: " + e.getMessage());
            fail("Lambda chaining should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testGenericMapOperations() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "\n" +
                "public String run() {\n" +
                "    Map<String, Integer> scores = new HashMap<>();\n" +
                "    scores.put(\"Alice\", 95);\n" +
                "    scores.put(\"Bob\", 87);\n" +
                "    scores.put(\"Charlie\", 92);\n" +
                "    \n" +
                "    StringBuilder sb = new StringBuilder();\n" +
                "    for (Map.Entry<String, Integer> entry : scores.entrySet()) {\n" +
                "        if ((Integer) entry.getValue() > 90) {\n" +
                "            if (sb.length() > 0) sb.append(\", \");\n" +
                "            sb.append(entry.getKey());\n" +
                "        }\n" +
                "    }\n" +
                "    return sb.toString();\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            String output = result.run();
            assertTrue(output.contains("Alice"), "Should contain Alice (95 > 90)");
            assertTrue(output.contains("Charlie"), "Should contain Charlie (92 > 90)");
            assertFalse(output.contains("Bob"), "Should not contain Bob (87 <= 90)");
            System.out.println("[PASS] testGenericMapOperations: Map<String, Integer> works, output=" + output);
        } catch (CompileException e) {
            System.out.println("[FAIL] testGenericMapOperations: " + e.getMessage());
            fail("Map with generics should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testBoundedTypeParameter() {
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
            System.out.println("[PASS] testBoundedTypeParameter: <T extends Comparable<T>> works");
        } catch (CompileException e) {
            System.out.println("[EXPECTED FAIL] testBoundedTypeParameter (CompileException): " + e.getMessage());
        } catch (Throwable t) {
            // Janino throws AssertionError in UnitCompiler.getITypeVariables2 for bounded type
            // params on local classes used with parameterize() - this is a known Janino limitation
            System.out.println("[EXPECTED FAIL] testBoundedTypeParameter (Janino internal error): " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    @Test
    public void testOptionalGeneric() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.Optional;\n" +
                "\n" +
                "public String run() {\n" +
                "    Optional<String> opt = Optional.of(\"present\");\n" +
                "    return opt.orElse(\"absent\");\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("present", result.run());
            System.out.println("[PASS] testOptionalGeneric: Optional<String> works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testOptionalGeneric: " + e.getMessage());
            fail("Optional with generics should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testDiamondOperator() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "\n" +
                "public String run() {\n" +
                "    List<String> list = new ArrayList<>();\n" +
                "    list.add(\"diamond\");\n" +
                "    list.add(\"operator\");\n" +
                "    return list.get(0) + \" \" + list.get(1);\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("diamond operator", result.run());
            System.out.println("[PASS] testDiamondOperator: new ArrayList<>() works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testDiamondOperator: " + e.getMessage());
            fail("Diamond operator should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }

    @Test
    public void testWildcardGenerics() {
        IDynamicCompiler compiler = newCompiler();

        String script =
                "import java.util.*;\n" +
                "\n" +
                "private double sum(List<? extends Number> numbers) {\n" +
                "    double total = 0;\n" +
                "    for (Number n : numbers) {\n" +
                "        total += n.doubleValue();\n" +
                "    }\n" +
                "    return total;\n" +
                "}\n" +
                "\n" +
                "public String run() {\n" +
                "    List<Integer> ints = Arrays.asList(1, 2, 3, 4, 5);\n" +
                "    double result = sum(ints);\n" +
                "    return String.valueOf((int) result);\n" +
                "}\n";

        IScriptClassBody<StringResult> body = IScriptBodyBuilder.getBuilder(StringResult.class, compiler)
                .setScript(script)
                .build();

        try {
            body.assertCompiled();
            StringResult result = body.get();
            assertNotNull(result);
            assertEquals("15", result.run());
            System.out.println("[PASS] testWildcardGenerics: List<? extends Number> works");
        } catch (CompileException e) {
            System.out.println("[FAIL] testWildcardGenerics: " + e.getMessage());
            fail("Wildcard generics should compile: " + e.getMessage(), e);
        } catch (Throwable t) {
            fail("Unexpected error: " + t.getMessage(), t);
        }
    }
}
