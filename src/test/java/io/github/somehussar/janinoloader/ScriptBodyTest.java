package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptBodyBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptBodyTest {
    public interface Int_to_Int {
        int apply(int num);
    }

    @Test
    public void testCompile() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        IScriptClassBody<Int_to_Int> classBody = IScriptBodyBuilder.getBuilder(Int_to_Int.class, jlc)
                .setScript(
                        "public int apply(int x) {" +
                        "   return x*x;" +
                        "}"
                ).build();

        Int_to_Int test = null;
        try {
            classBody.assertCompiled();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(25, test.apply(5));

    }

    @Test
    public void reloadTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        IScriptClassBody<Int_to_Int> classBody = IScriptBodyBuilder.getBuilder(Int_to_Int.class, jlc)
                .setScript(
                        "public int apply(int x) {" +
                        "   return x*x;" +
                        "}"
                ).build();

        Int_to_Int test = null;
        try {
            classBody.assertCompiled();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(25, test.apply(5));

        try {
            classBody.setScript(
                    "public int apply(int x) {" +
                    "   return x*x*x;" +
                    "}"
            );
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(125, test.apply(5));

    }

    @SuppressWarnings("unused")
    public static class ImportTestClass {
        public static int square(int x) {
            return x*x;
        }
    }

    @Test
    public void importGlobalTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        IScriptClassBody<Int_to_Int> classBody = IScriptBodyBuilder.getBuilder(Int_to_Int.class, jlc)
                .setScript(
                        "import io.github.somehussar.janinoloader.ScriptBodyTest.ImportTestClass;" +
                        "public int apply(int x) {" +
                        "   return ImportTestClass.square(x);" +
                        "}").build();

        Int_to_Int test = null;
        try {
            classBody.assertCompiled();
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

        IScriptClassBody<Int_to_Int> classBody = IScriptBodyBuilder.getBuilder(Int_to_Int.class, jlc)
                .setDefaultImports("io.github.somehussar.janinoloader.ScriptBodyTest.ImportTestClass")
                .setScript(
                        
                        "public int apply(int x) {" +
                        "   return ImportTestClass.square(x);" +
                        "}").build();

        Int_to_Int test = null;
        try {
            classBody.assertCompiled();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(100, test.apply(10));

    }

    @Test
    public void importDynamicTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        IScriptClassBody<Int_to_Int> classBody = IScriptBodyBuilder.getBuilder(Int_to_Int.class, jlc)
                .setScript(
                        "import pkg1.TestClass;" +
                        "public int apply(int x) {" +
                        "   return TestClass.value * x;" +
                        "}").build();

        Int_to_Int test = null;
        try {
            jlc.compileClass(new StringResource(
                    "pkg1.TestClass", 
                    "package pkg1;" +
                    
                    "public class TestClass {" +
                    "   public static int value = 3;" +
                    "}"
            ));
            classBody.assertCompiled();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(3*17, test.apply(17));

    }

    @Test
    public void importDynamic() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        IScriptClassBody<Int_to_Int> classBody = IScriptBodyBuilder.getBuilder(Int_to_Int.class, jlc)
                                                .setDefaultImports("pkg1.TestClass")
                                                .setScript(
                                                        "public int apply(int x) {" +
                                                        "   return TestClass.value * x;" +
                                                        "}"
                                                ).build();

        Int_to_Int test = null;
        try {
            jlc.compileClass(new StringResource(
                    "pkg1.TestClass", 
                    "package pkg1;" +
                    
                    "public class TestClass {" +
                    "   public static int value = 3;" +
                    "}"
            ));
            classBody.assertCompiled();
            test = classBody.get();
        } catch (CompileException compileException) {
            fail(compileException);
        } catch (Throwable ignored) {}
        assertNotNull(test);
        assertEquals(3*17, test.apply(17));

    }

    public abstract static class TestAbstractClass {
        public abstract int count();
        public abstract int TestValue();
    }

    @Test
    public void importReloadTest() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        IScriptClassBody<TestAbstractClass> classBody = IScriptBodyBuilder.getBuilder(TestAbstractClass.class, jlc)
                .setImplementedTypes(Serializable.class)
                .setDefaultImports("pkg1.TestClass")
                .setScript(
                        
                        "int TestValue() {" +
                        "   return TestClass.getValue();" +
                        "}" +
                        "int count = 0;" +
                        "int count() {" +
                        "   return count++;" +
                        "}").build();

        try {
            jlc.compileClass(new StringResource(
                    "pkg1.TestClass", 
                    "package pkg1;" +
                    "public class TestClass {" +
                    "   public static int getValue() {return 10;} " +
                    "}"
            ));
            classBody.assertCompiled();

            classBody.get().count();
            classBody.get().count();
            int expected = 1 + classBody.get().count();
            assertEquals(classBody.get().TestValue(),  jlc.getClassLoader().loadClass("pkg1.TestClass").getMethod("getValue").invoke(null));

            jlc.recompileClass(new StringResource(
                    "pkg1.TestClass", 
                    "package pkg1;" +
                    
                    "public class TestClass {" +
                    "   public static int testValueForRecompile = 11;" +
                    "   public static int getValue() {return 5;}" +
                    "}"
            ));

//            classBody.attemptRecompile();

            assertEquals(classBody.get().TestValue(),  jlc.getClassLoader().loadClass("pkg1.TestClass").getMethod("getValue").invoke(null));
//            System.out.println("NEW TEST VALUE: " + classBody.get().TestValue());

            assertEquals(expected, classBody.get().count());
        } catch (AssertionFailedError error) {
            throw error;
        } catch(Throwable error) {
            fail(error);
        }

    }
}
