package io.github.somehussar.janinoloader.example;

public abstract class AbstractNPCScript {
    // If using Serializable
    // (for example, when wanting to preserve script-defined variable states)
    // Mark these fields as transient so they don't get picked up.
    // marked private as to not allow scripts to edit it.
    private transient Npc npc;

    public static AbstractNPCScript reloadHandler(AbstractNPCScript old, AbstractNPCScript newObj) {
        newObj.npc = old.npc;
        return newObj;
    }

    public static AbstractNPCScript initializationHandler(AbstractNPCScript instance, Npc npc) {
        instance.npc = npc;
        return instance;
    }

    public final int getHealth() {
        return npc.getHealth();
    }

    public final int getHeight() {
        return npc.getHeight();
    }

    public final String getName() {
        return npc.getName();
    }

    public void onDamaged(int number) {
        npc.setHealth(npc.getHealth() - number);
    }
}
