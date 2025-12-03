package io.github.somehussar;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import io.github.somehussar.janinoloader.script.SafeScriptClassBody;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassBodyTest {
    public interface Int_to_Int {
        int apply(int num);
    }

    @Test
    public void testCompile() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        SafeScriptClassBody<Int_to_Int> classBody = new SafeScriptClassBody<>(Int_to_Int.class, jlc, null, "" +
                "" +
                "public int apply(int x) {" +
                "   return x*x;" +
                "}" +
                "",
                null);
        Int_to_Int test = null;
        try {
            classBody.attemptRecompile();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(25, test.apply(5));

    }

    public static class ImportTestClass {
        public static int square(int x) {
            return x*x;
        }
    }

    @Test
    public void importGlobalTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        SafeScriptClassBody<Int_to_Int> classBody = new SafeScriptClassBody<>(Int_to_Int.class, jlc, null, "" +
                "import io.github.somehussar.ClassBodyTest.ImportTestClass;" +
                "public int apply(int x) {" +
                "   return ImportTestClass.square(x);" +
                "}" +
                "",
                null);
        Int_to_Int test = null;
        try {
            classBody.attemptRecompile();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(100, test.apply(10));

    }

    @Test
    public void importGlobalDefaultTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        SafeScriptClassBody<Int_to_Int> classBody = new SafeScriptClassBody<>(Int_to_Int.class, jlc, new String[]{"io.github.somehussar.ClassBodyTest.ImportTestClass"}, "" +
                "" +
                "public int apply(int x) {" +
                "   return ImportTestClass.square(x);" +
                "}" +
                "",
                null);
        Int_to_Int test = null;
        try {
            classBody.attemptRecompile();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(100, test.apply(10));

    }

    @Test
    public void importLocalTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        SafeScriptClassBody<Int_to_Int> classBody = new SafeScriptClassBody<>(Int_to_Int.class, jlc, null, "" +
                "import pkg1.TestClass;" +
                "public int apply(int x) {" +
                "   return TestClass.value * x;" +
                "}" +
                "",
                null);
        Int_to_Int test = null;
        try {
            jlc.compileClass(new StringResource(
                    "pkg1.TestClass", "" +
                    "package pkg1;" +
                    "" +
                    "public class TestClass {" +
                    "   public static int value = 3;" +
                    "}"
            ));
            classBody.attemptRecompile();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(3*17, test.apply(17));

    }

    @Test
    public void importLocalDefaultTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        SafeScriptClassBody<Int_to_Int> classBody = new SafeScriptClassBody<>(Int_to_Int.class, jlc, new String[]{"pkg1.TestClass"}, "" +
                "" +
                "public int apply(int x) {" +
                "   return TestClass.value * x;" +
                "}" +
                "",
                null);
        Int_to_Int test = null;
        try {
            jlc.compileClass(new StringResource(
                    "pkg1.TestClass", "" +
                    "package pkg1;" +
                    "" +
                    "public class TestClass {" +
                    "   public static int value = 3;" +
                    "}"
            ));
            classBody.attemptRecompile();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(3*17, test.apply(17));

    }
}
