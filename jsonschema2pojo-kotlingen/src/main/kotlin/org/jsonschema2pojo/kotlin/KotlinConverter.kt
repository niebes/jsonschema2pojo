package org.jsonschema2pojo.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.COLLECTION
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.ITERABLE
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
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
                            ParameterSpec.builder(
                                name, jTypeToPoetClassName(fieldType.type()).copy(
                                    nullable = isNullable(fieldType)
                                )
                            )
                                .build()
                        )
                    }

                }.build()
            ).apply {
                clazz.fields().entries.forEach { (name, fieldType) ->
                    addProperty(
                        PropertySpec.builder(
                            name, jTypeToPoetClassName(fieldType.type()).copy(
                                nullable = isNullable(fieldType)
                            )
                        ).initializer(name).apply {
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

    private fun isNullable(fieldType: JFieldVar) = with(fieldType.annotations()
        .map { it.annotationClass.fullName() }) {
        contains("javax.annotation.Nullable") || !contains("javax.annotation.Nonnull")
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

        private fun jTypeToPoetClassName(jType: JType): ClassName {
            return fullQualifiedNameToPoetClassName(jType.erasure().fullName())
        }

        private fun jClassToPoetClassName(jClass: JClass): ClassName {
            return fullQualifiedNameToPoetClassName(jClass.fullName())
        }

        private fun fullQualifiedNameToPoetClassName(fullName: String): ClassName =
            javaToKotlinClasses.getOrElse(fullName) {
                ClassName.bestGuess(fullName)
            }

        private val javaToKotlinClasses = mapOf(
            "java.lang.String" to STRING,
            "java.lang.Integer" to INT,
            "java.lang.Double" to DOUBLE,
            "java.lang.Byte" to BYTE,
            "java.lang.Short" to SHORT,
            "java.lang.Long" to LONG,
            "java.lang.Boolean" to BOOLEAN,
            "java.lang.Iterable" to ITERABLE,
            "java.util.Iterable" to COLLECTION,
            "java.util.List" to LIST,
            "java.util.Set" to SET,
            "java.util.Map" to MAP
        )
    }
}
