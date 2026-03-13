package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;
import java.util.function.Function;

public class LambdaParamInferenceTest {

    @Test
    void lambdaParamFromTargetType() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        
        // This simulates: Consumer<String> c = (event) -> { event.getClass(); };
        // Where 'event' should be inferred as String from Consumer<String>
        c.compileClass(new StringResource("Test.java",
            "import java.util.function.Consumer;\n" +
            "public class Test {\n" +
            "    public Consumer<String> configurator = (event) -> {\n" +
            "        event.getClass();\n" +
            "    };\n" +
            "}"
        ));
        
        ClassLoader loader = c.getClassLoader();
        Class<?> testClass = loader.loadClass("Test");
        Object instance = testClass.getDeclaredConstructor().newInstance();
        Object consumer = testClass.getField("configurator").get(instance);
        assertNotNull(consumer);
    }
    
    @Test
    void lambdaPassedToConstructor() throws Exception {
        // Simulates: new EnergyConfig("name", "hook", (event) -> { event.getHookName(); })
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        
        c.compileClass(new StringResource("Test.java",
            "class Event {\n" +
            "    public String getHookName() { return \"hook\"; }\n" +
            "}\n" +
            "class Wrapper {\n" +
            "    public java.util.function.Consumer<Event> callback;\n" +
            "    public Wrapper(java.util.function.Consumer<Event> c) {\n" +
            "        this.callback = c;\n" +
            "    }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public static Wrapper create() {\n" +
            "        return new Wrapper((event) -> {\n" +
            "            event.getHookName();\n" +
            "        });\n" +
            "    }\n" +
            "}"
        ));
        
        ClassLoader loader = c.getClassLoader();
        Class<?> testClass = loader.loadClass("Test");
        java.lang.reflect.Method m = testClass.getMethod("create");
        Object wrapper = m.invoke(null);
        assertNotNull(wrapper);
    }
    
    @Test
    void lambdaWithMultipleParams() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        
        // BinaryOperator<Integer> - should infer both params as Integer
        c.compileClass(new StringResource("Test.java",
            "import java.util.function.BinaryOperator;\n" +
            "public class Test {\n" +
            "    public BinaryOperator<Integer> adder = (a, b) -> a + b;\n" +
            "}"
        ));
        
        ClassLoader loader = c.getClassLoader();
        Class<?> testClass = loader.loadClass("Test");
        Object instance = testClass.getDeclaredConstructor().newInstance();
        Object binaryOp = testClass.getField("adder").get(instance);
        assertNotNull(binaryOp);
    }
}
