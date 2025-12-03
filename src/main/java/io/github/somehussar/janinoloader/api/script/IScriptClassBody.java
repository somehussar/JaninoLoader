package io.github.somehussar.janinoloader.api.script;

import io.github.somehussar.janinoloader.api.IClassReloadListener;
import org.codehaus.commons.compiler.CompileException;

import java.io.IOException;

public interface IScriptClassBody<DesiredType> extends IClassReloadListener {
    DesiredType get();
    void setScript(String script) throws CompileException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException;
    void attemptRecompile() throws CompileException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException;
    void prepareToUnload();
}
