package com.knsi.ki.processor;


import com.google.auto.service.AutoService;
import com.knsi.ki.interfaces.EnforceNoArgsConstructor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Created by kaushiknsiyer on 07/09/18.
 */

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.knsi.ki.interfaces.EnforceNoArgsConstructor")
public class EnforceArgsProcessor extends AbstractProcessor {

    public EnforceArgsProcessor() {
    }

    private void printMessage(Diagnostic.Kind kind, String message){
        processingEnv.getMessager().printMessage(kind, message);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private boolean doesNotHaveNoArgsConstructor(ExecutableElement constructor) {
        return !constructor.getParameters().isEmpty();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(EnforceNoArgsConstructor.class))
                .forEach(typeElement -> ElementFilter.constructorsIn(typeElement.getEnclosedElements())
                        .stream()
                        .filter(this::doesNotHaveNoArgsConstructor)
                        .forEach(executableElement -> printMessage(Diagnostic.Kind.ERROR, "a default constructor is not present")));

        return true;
    }
}
