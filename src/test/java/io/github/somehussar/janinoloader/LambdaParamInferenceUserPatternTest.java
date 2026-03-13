package io.github.somehussar.janinoloader;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LambdaParamInferenceUserPatternTest {

    @Test
    void userPatternWithFluent() throws Exception {
        IDynamicCompiler c = IDynamicCompilerBuilder.createBuilder().getCompiler();
        c.compileClass(new StringResource("Test.java",
            "interface Action { void execute(ActionContext ctx); }\n" +
            "class ActionContext { \n" +
            "    public Object getData(String key) { return null; }\n" +
            "    public ActionContext setData(String key, Object value) { return this; }\n" +
            "}\n" +
            "class Queue { \n" +
            "    public Queue schedule(String name, Action action) { return this; }\n" +
            "    public Queue setData(String key, Object value) { return this; }\n" +
            "}\n" +
            "class Manager { \n" +
            "    public Queue getOrCreateQueue(String name) { return new Queue(); }\n" +
            "}\n" +
            "public class Test {\n" +
            "    public void test() {\n" +
            "        Manager manager = new Manager();\n" +
            "        manager.getOrCreateQueue(\"test\").schedule(\"action\", (act) -> {\n" +
            "            Object player = act.getData(\"player\");\n" +
            "            int value = (int) act.getData(\"value\");\n" +
            "        }).setData(\"player\", null).setData(\"value\", 0);\n" +
            "    }\n" +
            "}"
        ));
        assertTrue(true, "Test compiled successfully");
    }
}
