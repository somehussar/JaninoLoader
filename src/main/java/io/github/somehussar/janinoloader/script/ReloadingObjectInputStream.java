package io.github.somehussar.janinoloader.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ReloadingObjectInputStream extends ObjectInputStream {
    private final ClassLoader loader;

    ReloadingObjectInputStream(InputStream in, ClassLoader targetLoader) throws IOException {
        super(in);
        this.loader = targetLoader;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();

        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException exception) {
            return super.resolveClass(desc);
        }
    }


}
