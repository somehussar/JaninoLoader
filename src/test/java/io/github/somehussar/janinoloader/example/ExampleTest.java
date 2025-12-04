package io.github.somehussar.janinoloader.example;

import io.github.somehussar.janinoloader.api.IDynamicCompiler;
import io.github.somehussar.janinoloader.api.IDynamicCompilerBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptBodyBuilder;
import io.github.somehussar.janinoloader.api.script.IScriptClassBody;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ExampleTest {

    @Test
    public void exampleScriptUsage() {
        IDynamicCompiler jlc = IDynamicCompilerBuilder.createBuilder().getCompiler();

        try {
            Npc mark = new Npc("Mark", 20, 10);
            IScriptClassBody<AbstractNPCScript> scripted = IScriptBodyBuilder.getBuilder(AbstractNPCScript.class, jlc).setScript(
                            "void onDamaged(int number) {" +
                            "   System.out.println(\"I'm hit for \" + number + \"dmg!\");" +
                            "}"
            )
            // Define how to create a new instance of this class properly.
            // Classes don't inherit constructors.
            .setInstanceDelegate(clazz -> {
                try {
                    AbstractNPCScript obj = clazz.newInstance();
                    return AbstractNPCScript.initializationHandler(obj, mark);
                } catch (Throwable i) {
                    throw new ExceptionInInitializerError(i);
                }
            })
            // Define how to handle classloader hierarchy reload.
            .setReloadDelegate(
                    (oldObj, newObj, classLoader) -> AbstractNPCScript.reloadHandler(oldObj, newObj)
            ).build();

            // Compile the object for the first time
            scripted.assertCompiled();

            // The compiled class doesn't actually modify the health value
            // Expected to be the same as before.
            int healthExpected = mark.getHealth();
            scripted.get().onDamaged(5);
            assertEquals(healthExpected, scripted.get().getHealth());

            // We're modifying the original NPC directly, not through script
            // Script should return NPC's current health.
            mark.setHealth(5);
            assertEquals(mark.getHealth(), scripted.get().getHealth());

            // Simulating an updated script
            // This time no methods are modified. onDamaged should remove health
            scripted.setScript("");

            healthExpected = mark.getHealth();
            scripted.get().onDamaged(5);
            assertEquals(healthExpected-5, mark.getHealth());

        } catch (Throwable e) {
            fail(e);
        }
    }
}