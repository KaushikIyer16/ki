package com.knsi.ki.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

/**
 * Created by kaushiknsiyer on 09/09/18.
 */
public class ElementUtil {
    public static String getPackageNameFromAnnotatedField(ProcessingEnvironment processingEnv, Element element){
        return processingEnv.getElementUtils().getPackageOf(element).toString();
    }
    public static String getClassNameFromAnnotatedField(Element element){
        return element.getSimpleName().toString();
    }
    public static TypeMirror getTypeFromProcessingEnv(ProcessingEnvironment processingEnvironment, Class clazz){
        return processingEnvironment.getElementUtils().getTypeElement(clazz.getName()).asType();
    }
    public static Class getFieldMemberType(Map<String, Object> member) throws ClassNotFoundException {
        return Class.forName("java.lang."+member.get("type").toString());
    }
}
