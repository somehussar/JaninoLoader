package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lambda parameter type inference in chained method calls.
 *
 * <p>Reproduces the user's exact pattern where {@code act.getData("player")}
 * fails with "method not declared" because the lambda parameter is inferred
 * as {@code Object} instead of the functional interface's SAM parameter type.
 */
public class LambdaParamInferenceChainTest {

    @Test
    void lambdaInChainedMethodCall_nonGenericFunctionalInterface() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        c.compileClass(new StringResource("Test.java",
            "interface Action {\n" +
            "    void execute(Context ctx);\n" +
            "}\n" +
            "class Context {\n" +
            "    public String getData(String key) { return null; }\n" +
            "}\n" +
            "class Manager {\n" +
            "    public Manager schedule(String name, Action action) { return this; }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public void test() {\n" +
            "        Manager m = new Manager();\n" +
            "        m.schedule(\"test\", (ctx) -> {\n" +
            "            ctx.getData(\"key\");\n" +
            "        });\n" +
            "    }\n" +
            "}"
        ));
        assertNotNull(c.getClassLoader().loadClass("Test"));
    }

    @Test
    void lambdaInChainedMethodCall_fluentAPI() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        c.compileClass(new StringResource("Test.java",
            "interface ActionCallback {\n" +
            "    void run(ActionContext act);\n" +
            "}\n" +
            "class ActionContext {\n" +
            "    public Object getData(String key) { return null; }\n" +
            "}\n" +
            "class Queue {\n" +
            "    public Queue killWhenEmpty(boolean kill) { return this; }\n" +
            "    public Queue schedule(String name, ActionCallback callback) { return this; }\n" +
            "}\n" +
            "class Manager {\n" +
            "    public Queue getOrCreateQueue(String name) { return new Queue(); }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public void test() {\n" +
            "        Manager manager = new Manager();\n" +
            "        manager.getOrCreateQueue(\"myQueue\")\n" +
            "               .killWhenEmpty(false)\n" +
            "               .schedule(\"myAction\", (act) -> {\n" +
            "                   act.getData(\"player\");\n" +
            "               });\n" +
            "    }\n" +
            "}"
        ));
        assertNotNull(c.getClassLoader().loadClass("Test"));
    }

    @Test
    void lambdaPassedDirectlyToMethod_nonGenericFI() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        c.compileClass(new StringResource("Test.java",
            "interface Callback {\n" +
            "    void onEvent(EventData data);\n" +
            "}\n" +
            "class EventData {\n" +
            "    public String getName() { return \"test\"; }\n" +
            "}\n" +
            "class Scheduler {\n" +
            "    public static void run(Callback cb) { cb.onEvent(new EventData()); }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public void test() {\n" +
            "        Scheduler.run((data) -> {\n" +
            "            data.getName();\n" +
            "        });\n" +
            "    }\n" +
            "}"
        ));
        assertNotNull(c.getClassLoader().loadClass("Test"));
    }

    @Test
    void lambdaWithGenericFI_passedToMethod() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        c.compileClass(new StringResource("Test.java",
            "class ActionContext {\n" +
            "    public Object getData(String key) { return null; }\n" +
            "}\n" +
            "class Scheduler {\n" +
            "    public static void schedule(String name, java.util.function.Consumer<ActionContext> cb) {\n" +
            "        cb.accept(new ActionContext());\n" +
            "    }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public void test() {\n" +
            "        Scheduler.schedule(\"test\", (act) -> {\n" +
            "            act.getData(\"player\");\n" +
            "        });\n" +
            "    }\n" +
            "}"
        ));
        assertNotNull(c.getClassLoader().loadClass("Test"));
    }

    @Test
    void lambdaExpression_inChainedCall() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        c.compileClass(new StringResource("Test.java",
            "interface Action {\n" +
            "    String execute(Context ctx);\n" +
            "}\n" +
            "class Context {\n" +
            "    public String getData(String key) { return \"value\"; }\n" +
            "}\n" +
            "class Manager {\n" +
            "    public Manager then(Action action) { return this; }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public void test() {\n" +
            "        new Manager().then(ctx -> ctx.getData(\"key\"));\n" +
            "    }\n" +
            "}"
        ));
        assertNotNull(c.getClassLoader().loadClass("Test"));
    }
}
