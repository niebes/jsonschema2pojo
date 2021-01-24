package org.jsonschema2pojo.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.sun.codemodel.JAnnotationUse
import com.sun.codemodel.JAnnotationValue
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JFormatter
import com.sun.codemodel.JGenerable
import com.sun.codemodel.JPackage
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
                        val erasure = fieldType.type().erasure()
                        addParameter(
                            ParameterSpec.builder(name, fullQualifiedNameToPoetClassName(erasure.fullName()))
                                .addModifiers(KModifier.PUBLIC)
                                .build()
                        )
                    }

                }.build()
            ).apply {
                clazz.fields().entries.forEach { (name, fieldType) ->
                    val erasure = fieldType.type().erasure()
                    addProperty(
                        PropertySpec.builder(name, fullQualifiedNameToPoetClassName(erasure.fullName()))
                            .initializer(name).apply {
                                val propertySpec = this
                                toAnnotationSpec(fieldType).onEach {
                                    propertySpec.addAnnotation(it)
                                }
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
            val type = fullQualifiedNameToPoetClassName(jAnnotationUse.annotationClass.fullName())
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

        private fun fullQualifiedNameToPoetClassName(fullName: String): ClassName {
            val fullNameSplit = fullName.split(".")
            return ClassName(fullNameSplit.dropLast(1).joinToString("."), fullNameSplit.last())
        }
    }
}
