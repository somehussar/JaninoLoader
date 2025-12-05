package io.github.somehussar.janinoloader.annotations;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("io.github.somehussar.janinoloader.annotations.PermissableScriptHandler")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PermissableProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    TypeElement classElement = (TypeElement) element;

                    // iterate methods
                    for (ExecutableElement method : ElementFilter.methodsIn(classElement.getEnclosedElements())) {
                        if (method.getSimpleName().toString().equals("prepareScriptToUnload")) {
                            processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Method 'prepareScriptToUnload' is reserved and should not be implemented here",
                                    method  // attach to the method element
                            );
                            return true;
                        }
                    }
                }
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(PermissableScriptHandler.class)) {
            if (!(element instanceof TypeElement)) continue;

            generatePermissibleWrapper((TypeElement) element);
        }

        return true;
    }

    private void generatePermissibleWrapper(TypeElement original) {
        try {
            String pkg = processingEnv.getElementUtils()
                    .getPackageOf(original).getQualifiedName().toString();

            PermissableScriptHandler annotation = original.getAnnotation(PermissableScriptHandler.class);
            String interfaceName = null;
            if (annotation != null) {
                try {
                    // This will throw MirroredTypeException at compile-time
                    Class<?> iface = annotation.implementedInterface();
                    if (iface != Void.class) { // sentinel check
                        interfaceName = iface.getCanonicalName();
                    }
                } catch (MirroredTypeException mte) {

                    TypeMirror tm = mte.getTypeMirror();
                    if (!tm.toString().equals("java.lang.Void")) { // sentinel check
                        interfaceName = mte.toString();
                    }
                }
            }

            String originalName = original.getSimpleName().toString();
            String wrapperName  = "Permissible" + originalName;

            StringBuilder src = new StringBuilder();

            src.append("package ").append(pkg).append(";\n\n");

            src.append("import org.codehaus.commons.compiler.Sandbox;\n");
            src.append("import io.github.somehussar.janinoloader.api.script.IScriptClassBody;\n");

            src.append("public final class ").append(wrapperName);

            if (interfaceName != null) src.append(" implements ").append(interfaceName);

            src.append(" {\n\n");

            src.append("    private final Sandbox sandbox;\n");
            src.append("    private final IScriptClassBody<")
                    .append(originalName)
                    .append("> scriptBody;\n\n");

            src.append("    public ").append(wrapperName).append("(Sandbox sandbox, ")
                    .append("IScriptClassBody<").append(originalName).append("> scriptBody) {\n")
                    .append("       this.sandbox = sandbox;\n")
                    .append("       this.scriptBody = scriptBody;\n")
                    .append("   }\n\n");

            for (ExecutableElement method
                    : ElementFilter.methodsIn(original.getEnclosedElements())) {

                if (method.getModifiers().contains(Modifier.PRIVATE) ||
                        method.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                generateMethod(method, src);
            }


            src.append("}\n");

            JavaFileObject file = processingEnv.getFiler()
                    .createSourceFile(pkg + "." + wrapperName, original);

            try (Writer writer = file.openWriter()) {
                writer.write(src.toString());
            }

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed generating permissible wrapper: " + e.getMessage()
            );
        }
    }

    private void generateMethod(ExecutableElement method, StringBuilder src) {
        String returnType = method.getReturnType().toString();
        boolean returns = method.getReturnType().getKind() != TypeKind.VOID;

        // Method signature
        src.append("    public ").append(returnType).append(" ")
                .append(method.getSimpleName()).append("(");

        // parameters
        StringBuilder argList = new StringBuilder();
        boolean first = true;
        for (VariableElement param : method.getParameters()) {
            if (!first) src.append(", ");
            src.append(param.asType().toString()).append(" ")
                    .append(param.getSimpleName());

            if (!first) argList.append(", ");
            argList.append(param.getSimpleName());

            first = false;
        }
        src.append(") {\n");


        if (returns) {
            src.append("        return sandbox.confine(")
                    .append("(java.security.PrivilegedAction<")
                    .append(boxIfPrimitive(method.getReturnType()))
                    .append(">) () -> scriptBody.get().")
                    .append(method.getSimpleName()).append("(")
                    .append(argList)
                    .append(")")
                    .append(");\n");

        } else {
            src.append("        sandbox.confine(")
                    .append("(java.security.PrivilegedAction<Void>) () -> { ")
                    .append("scriptBody.get().")
                    .append(method.getSimpleName()).append("(")
                    .append(argList)
                    .append("); return null; }")
                    .append(");\n");
        }

        src.append("    }\n\n");
    }

    private String boxIfPrimitive(TypeMirror type) {
        switch (type.getKind()) {
            case INT:
                return "Integer";
            case LONG:
                return "Long";
            case BOOLEAN:
                return "Boolean";
            case BYTE:
                return "Byte";
            case SHORT:
                return "Short";
            case CHAR:
                return "Character";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case VOID:
                return "Void";
            default:
                return type.toString(); // non-primitive → leave as is
        }
    }


}