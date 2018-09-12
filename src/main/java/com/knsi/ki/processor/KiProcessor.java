package com.knsi.ki.processor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.knsi.ki.interfaces.Ki;
import com.knsi.ki.interfaces.Strategy;
import com.knsi.ki.utils.ElementUtil;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

/**
 * Created by kaushiknsiyer on 23/08/18.
 */
@SupportedAnnotationTypes({
    "com.knsi.ki.interfaces.Ki"
})
public class KiProcessor extends AbstractProcessor {


    public KiProcessor() {
    }

    private void printMessage(Diagnostic.Kind kind, String message){
        processingEnv.getMessager().printMessage(kind, message);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        String packageName = "";
        for (Element element: roundEnv.getElementsAnnotatedWith(Ki.class) ) {

            packageName = ElementUtil.getPackageNameFromAnnotatedField(element);
            printMessage(Diagnostic.Kind.NOTE, "package name is "+packageName);

            String descriptionJson = element.getAnnotation(Ki.class).value();
            printMessage(Diagnostic.Kind.NOTE, "creating a "+ElementUtil.getClassNameFromAnnotatedField(element)+" class");

            TypeSpec.Builder kiTypeSpecBuilder = TypeSpec.classBuilder(ElementUtil.getClassNameFromAnnotatedField(element))
                    .addModifiers(Modifier.PUBLIC);
            printMessage(Diagnostic.Kind.NOTE, "its description is"+ descriptionJson);

            /* check if the element also has a strategy annotation to it */
            injectStrategyIntoType(kiTypeSpecBuilder, element);

            try {
                List<Map<String, Object>> memberMap = obtainMemberDetails(descriptionJson);

                /* now traversing through the map and generating the fieldSpecs for each description*/
                for(Map<String, Object> member : memberMap) {
                    printMessage(Diagnostic.Kind.NOTE, "creating a type spec of "+member);
                    Class fieldMemberType = ElementUtil.getFieldMemberType(member);
                    FieldSpec.Builder memberFieldBuilder = FieldSpec.builder(fieldMemberType, member.get("name").toString())
                            .addModifiers(Modifier.PRIVATE);

                    /* check if the member has got an alias field*/
                    if(member.keySet().contains("alias")){
                        injectAliasIntoField(memberFieldBuilder, member.get("alias").toString());
                    }
                    kiTypeSpecBuilder.addField(memberFieldBuilder.build());

                    /* generate getters and setters for each and every field */
                    String memberName = member.get("name").toString();
                    kiTypeSpecBuilder.addMethod(MethodSpec.methodBuilder("get"+memberName.substring(0,1).toUpperCase()+memberName.substring(1))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(fieldMemberType)
                            .addStatement("return this.$N", memberName)
                            .build());

                    kiTypeSpecBuilder.addMethod(MethodSpec.methodBuilder("set"+memberName.substring(0,1).toUpperCase()+memberName.substring(1))
                            .addModifiers(Modifier.PUBLIC)
                            .returns(void.class)
                            .addParameter(fieldMemberType, memberName)
                            .addStatement("this.$N= $N", memberName, memberName)
                            .build());
                }

                JavaFile javaFile = JavaFile.builder(packageName, kiTypeSpecBuilder.build()).build();
                javaFile.writeTo(processingEnv.getFiler());

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

        }


        return true;
    }

    private void injectStrategyIntoType(TypeSpec.Builder builder, Element element){
        try {
            TypeMirror strategy = ElementUtil.getTypeFromProcessingEnv(processingEnv, Strategy.class);

            Optional<String> namingStrategy = null;
            for (AnnotationMirror am : element.getAnnotationMirrors()) {
                if (am.getAnnotationType().equals(strategy)) {
                    namingStrategy = am.getElementValues().entrySet()
                            .stream()
                            .filter(entry -> "value".equals(entry.getKey().getSimpleName().toString()))
                            .map(entry -> entry.getValue().toString())
                            .findFirst();
                }
            }
            namingStrategy.ifPresent(userDefinedStrategy -> builder.addAnnotation(AnnotationSpec.builder(JsonNaming.class)
                    .addMember("value", "$N", userDefinedStrategy)
                    .build()
            ));
        }catch (NullPointerException npe){
            printMessage(Diagnostic.Kind.NOTE, "a strategy was not applied to this object");
        }
    }

    private void injectAliasIntoField(FieldSpec.Builder builder, String alias){
        builder.addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                .addMember("value", "$S", alias)
                .build());
    }

    private List<Map<String, Object>> obtainMemberDetails(String descriptionJson) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> memberMap = new ArrayList<>();
        memberMap = objectMapper.readValue(descriptionJson, new TypeReference<List<HashMap>>(){});
        return memberMap;
    }
}
