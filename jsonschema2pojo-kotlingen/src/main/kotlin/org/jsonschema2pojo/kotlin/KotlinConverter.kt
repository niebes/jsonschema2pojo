package org.jsonschema2pojo.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.sun.codemodel.JAnnotationUse
import com.sun.codemodel.JAnnotationValue
import com.sun.codemodel.JClass
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JFormatter
import com.sun.codemodel.JGenerable
import com.sun.codemodel.JPackage
import com.sun.codemodel.JType
import java.io.CharArrayWriter


class KotlinConverter {

    fun convert(pkg: JPackage, sourceFileName: String): String {
        val clazz = pkg.classes().next()
        val packageName = pkg.name()
        val className = clazz.name()
        val typeSpec = TypeSpec.classBuilder(ClassName(packageName, className))
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    clazz.fields().entries.forEach { (name, fieldType) ->
                        addParameter(
                            ParameterSpec.builder(name, jTypeToPoetClassName(fieldType.type()))
                                .build()
                        )
                    }

                }.build()
            ).apply {
                clazz.fields().entries.forEach { (name, fieldType) ->
                    addProperty(
                        PropertySpec.builder(name, jTypeToPoetClassName(fieldType.type()))
                            .initializer(name).apply {
                                val propertySpec = this
                                toAnnotationSpec(fieldType).onEach(propertySpec::addAnnotation)
                            }
                            .build()
                    )
                }

            }
            .build()
        return FileSpec.builder(packageName, sourceFileName)
            .addType(typeSpec)
            .build()
            .toString()
    }

    private fun toAnnotationSpec(jFieldVar: JFieldVar): List<AnnotationSpec> =
        jFieldVar.annotations().map { jAnnotationUse ->
            val type = jClassToPoetClassName(jAnnotationUse.annotationClass)
            AnnotationSpec.builder(type)
                .apply {
                    jAnnotationUse.annotations().map { (key, value) ->
                        addMember("$key = ${generateToString(value)}")
                    }
                }
                .build()
        }

    companion object {
        private fun generateToString(jGenerable: JGenerable): String {
            val charArrayWriter = CharArrayWriter()
            val jFormatter = JFormatter(charArrayWriter)
            jGenerable.generate(jFormatter)
            return charArrayWriter.toCharArray().joinToString("")
        }

        fun JAnnotationUse.annotations(): Map<String, JAnnotationValue> = try {
            annotationMembers
        } catch (NPE: NullPointerException) {
            mapOf()
        }

        private fun jTypeToPoetClassName(jType: JType): ClassName =
            fullQualifiedNameToPoetClassName(jType.erasure().fullName())

        private fun jClassToPoetClassName(jClass: JClass): ClassName =
            fullQualifiedNameToPoetClassName(jClass.fullName())

        private fun fullQualifiedNameToPoetClassName(fullName: String): ClassName = when (fullName) {
            "java.lang.String" -> STRING
            "java.lang.Integer" -> INT
            "java.lang.Double" -> DOUBLE
            "java.lang.Byte" -> BYTE
            "java.lang.Short" -> SHORT
            "java.lang.Long" -> LONG
            "java.lang.Boolean" -> BOOLEAN
            else -> {
                ClassName.bestGuess(fullName)
            }
        }
    }
}
