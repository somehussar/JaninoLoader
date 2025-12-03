package io.github.somehussar;


import io.github.somehussar.janinoloader.JaninoCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ClassLoaderTest {

    @Test
    public void compilationTest() {
        try {
            JaninoCompiler jlc = new JaninoCompiler(this.getClass().getClassLoader());
            jlc.compileClass(
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; public class A { public static int meth() { return 11; } }"
                    )
            );
            ClassLoader mcl = jlc.getClassLoader();

            assertEquals(11, mcl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));
        } catch (Throwable i) {
            fail(i);
        }
    }

    @Test
    public void classLoaderSeparation() {
        try {
            ClassLoader parentClassLoader = this.getClass().getClassLoader();
            JaninoCompiler jlc = new JaninoCompiler(parentClassLoader);
            jlc.compileClass(
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; public class A { public static int meth() { return 11; } }"
                    )
            );
            ClassLoader mcl = jlc.getClassLoader();

            assertEquals(11, mcl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));

            try {
                parentClassLoader.loadClass("pkg1.A");
                fail("Classes loaded to the main classloader");
            } catch (AssertionFailedError error) {
                throw error;
            } catch (Throwable ignored) {

            }
        } catch (Throwable i) {
            fail(i);
        }
    }
    @Test
    public void filterClassTest() {
        try {
            ClassLoader parentClassLoader = this.getClass().getClassLoader();
            IDynamicCompiler.LoadClassCondition condition = (name) -> !name.contains("Math");
            JaninoCompiler jlc = new JaninoCompiler(parentClassLoader, condition);
            jlc.compileClass(
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; import java.lang.Math; public class A { public static double meth() { return Math.random(); } }"
                    )
            );
            ClassLoader mcl = jlc.getClassLoader();

            mcl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null);
            fail("Did not filter out class");

        } catch (CompileException ignored){

        } catch (Throwable i) {
            fail(i);
        }
    }

    @Test
    public void circularDependencyTest() {
        try {
            JaninoCompiler jlc = new JaninoCompiler(this.getClass().getClassLoader());
            jlc.compileClass(new StringResource(
                            "pkg2.B",
                            "package pkg2; public class B { public static int meth() { return pkg1.A.test;            } }"
                    ),
                    new StringResource(
                            "pkg1.B",
                            "package pkg1; public class A { public static int test = 77; public static int meth() { return pkg2.B.meth(); } }"
                    ));
            ClassLoader mcl = jlc.getClassLoader();

            assertEquals(77, mcl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));
        } catch (Throwable i) {
            fail(i);
        }
    }

    @Test
    public void removeTest() {
        try {
            JaninoCompiler jlc = new JaninoCompiler(this.getClass().getClassLoader());
            jlc.compileClass(new StringResource(
                            "pkg2.B",
                            "package pkg2; public class B { public static int meth() { return pkg1.A.test;            } }"
                    ),
                    new StringResource(
                            "pkg1.A",
                            "package pkg1; public class A { public static int test = 77; public static int meth() { return pkg2.B.meth(); } }"
                    ));
            AtomicReference<ClassLoader> mcl = new AtomicReference<>(jlc.getClassLoader());
            jlc.addReloadListener((cl) -> {
                mcl.set(cl); return false;});

            assertEquals(77, mcl.get().loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));

            jlc.removeClass("pkg2.B");
            try {
                mcl.get().loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null);
            } catch (Throwable ignored) {

            }
        } catch (Throwable i) {
            fail(i);
        }
    }

    @Test
    public void recompileTest() {
        try {
            JaninoCompiler jlc = new JaninoCompiler(this.getClass().getClassLoader());
            jlc.compileClass(new StringResource(
                            "pkg2/B.java",
                            "package pkg2; public class B { public static int meth() { return pkg1.A.test;            } }"
                    ),
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; public class A { public static int test = 77; public static int meth() { return pkg2.B.meth(); } }"
                    ));
            AtomicReference<ClassLoader> mcl = new AtomicReference<>(jlc.getClassLoader());
            jlc.addReloadListener((cl) -> {
                mcl.set(cl); return false;});

            assertEquals(77, mcl.get().loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));

            jlc.recompileClass(new StringResource(
                    "pkg2/B.java",
                    "package pkg2; import java.lang.Math; public class B { public static int meth() { return (int) Math.pow(2, 6);            } }"
            ));
            assertEquals((int) Math.pow(2, 6), mcl.get().loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));
        } catch (Throwable i) {
            fail(i);
        }
    }
}
