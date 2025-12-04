package io.github.somehussar.janinoloader.example;

public abstract class AbstractNPCScript {
    // Make it transient so serializable doesn't nip this.
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
