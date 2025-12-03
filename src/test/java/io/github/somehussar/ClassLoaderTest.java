package io.github.somehussar;


import io.github.somehussar.janinoloader.classloader.JaninoClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ClassLoaderTest {

    @Test
    public void compilationTest() {
        try {
            JaninoClassLoader jlc = new JaninoClassLoader(this.getClass().getClassLoader());
            jlc.addClass(
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; public class A { public static int meth() { return 11; } }"
                    )
            );
            ClassLoader mcl = jlc.getManagedClassLoader();

            assertEquals(11, mcl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));
        } catch (Throwable i) {
            fail(i);
        }
    }

    @Test
    public void classLoaderSeparation() {
        try {
            ClassLoader parentClassLoader = this.getClass().getClassLoader();
            JaninoClassLoader jlc = new JaninoClassLoader(parentClassLoader);
            jlc.addClass(
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; public class A { public static int meth() { return 11; } }"
                    )
            );
            ClassLoader mcl = jlc.getManagedClassLoader();

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
            JaninoClassLoader.LoadClassCondition condition = (name) -> !name.contains("Math");
            JaninoClassLoader jlc = new JaninoClassLoader(parentClassLoader, condition);
            jlc.addClass(
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; import java.lang.Math; public class A { public static double meth() { return Math.random(); } }"
                    )
            );
            ClassLoader mcl = jlc.getManagedClassLoader();

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
            JaninoClassLoader jlc = new JaninoClassLoader(this.getClass().getClassLoader());
            jlc.batchCompile(new StringResource[] {
                    new StringResource(
                            "pkg2/B.java",
                            "package pkg2; public class B { public static int meth() { return 77;            } }"
                    ),
                    new StringResource(
                            "pkg1/A.java",
                            "package pkg1; public class A { public static int meth() { return pkg2.B.meth(); } }"
                    ),
            });
            ClassLoader mcl = jlc.getManagedClassLoader();

            assertEquals(77, mcl.loadClass("pkg1.A").getDeclaredMethod("meth").invoke(null));
        } catch (Throwable i) {
            fail(i);
        }
    }
}
