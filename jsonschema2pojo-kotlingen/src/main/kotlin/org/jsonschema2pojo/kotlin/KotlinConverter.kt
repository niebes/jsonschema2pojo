package org.jsonschema2pojo.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.sun.codemodel.JAnnotationUse
import com.sun.codemodel.JAnnotationValue
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JPackage


class KotlinConverter {

    fun convert(pkg: JPackage, sourceFileName: String): String {
        val clazz = pkg.classes().next()

        val packageName = pkg.name()
        val className = clazz.name()

        val classBuilder = TypeSpec.classBuilder(ClassName(packageName, className))
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    clazz.fields().entries.forEach { (name, fieldType) ->
                        addParameter(name, fieldType)
                    }
                }.build()
            )
            .build()
        val fileSpec = FileSpec.builder(packageName, sourceFileName)
            .addType(classBuilder)
            .build()
        return fileSpec.toString()
    }

    private fun FunSpec.Builder.addParameter(
        name: String,
        fieldType: JFieldVar
    ) {
        val type = TypeVariableName(fieldType.type().fullName()).apply {
            toAnnotationSpec(fieldType).forEach { annotationSpec ->
               // addAnnotation(annotationSpec)
            }
        }
        addParameter(name, type)
    }

    private fun toAnnotationSpec(jFieldVar: JFieldVar): List<AnnotationSpec> {
        return jFieldVar.annotations().map { jAnnotationUse ->
            val annotationClass = jAnnotationUse.annotationClass
            AnnotationSpec.builder(ClassName(annotationClass.fullName(), annotationClass.name()))
                .apply {
                    jAnnotationUse.annotations()?.map { (key, value) ->
                        addMember("$key = %S", value.toString())
                    }
                }
                .build()
        }
    }

    fun JAnnotationUse.annotations(): Map<String, JAnnotationValue> {
        return try {
            annotationMembers
        }catch (NPE: NullPointerException){
            return mapOf()
        }
    }
}