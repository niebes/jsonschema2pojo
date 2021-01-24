package org.jsonschema2pojo.kotlin

import com.squareup.kotlinpoet.ANY
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.sun.codemodel.JAnnotationUse
import com.sun.codemodel.JAnnotationValue
import com.sun.codemodel.JClass
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JFormatter
import com.sun.codemodel.JGenerable
import com.sun.codemodel.JPackage
import java.io.CharArrayWriter

/**
 * converts a codemodel jPackage to kotlinpoet FileSpec
 * problems:
 * - codemodel doesnt expose annotation values `@JsonDeserialize(as = java.util.LinkedHashSet.class)`
 * - codemodel doesnt expose generics `Map<String, Any>`
 * - kotlinpoet doesnt escape annotation names reserved in kotlin like "as"
 */
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
                            ParameterSpec.builder(name, toPoetClass(fieldType)).build()
                        )
                    }

                }.build()
            ).apply {
                clazz.fields().entries.forEach { (name, fieldType) ->
                    addProperty(
                        PropertySpec.builder(
                            name, toPoetClass(fieldType)
                        ).initializer(name)
                            .addAnnotations(toAnnotationSpec(fieldType))
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
            val type = toPoetClassName(jAnnotationUse.annotationClass)
            AnnotationSpec.builder(type)
                .apply {
                    jAnnotationUse.annotations().map { (key, value) ->
                        //TODO escape annotation keys reserved in kotlin "as"
                        addMember("$key = ${generateToString(value)}", useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD))
                    }
                }
                .build()
        }

    companion object {

        private fun toPoetClass(fieldType: JFieldVar) = toPoetClassName(fieldType).copy(
            nullable = isNullable(fieldType)
        )

        private fun isNullable(fieldType: JFieldVar): Boolean {
            val annotations = fieldType.annotations().map { it.annotationClass.name() }
            return with(annotations) {
                contains("Nullable") || !contains("Nonnull")
            }
        }

        private fun generateToString(jGenerable: JGenerable): String {
            val charArrayWriter = CharArrayWriter()
            val jFormatter = JFormatter(charArrayWriter)
            jGenerable.generate(jFormatter)
            return charArrayWriter.toCharArray()
                .joinToString("")
                .replace(".class", "::class")
        }

        fun JAnnotationUse.annotations(): Map<String, JAnnotationValue> = try {
            annotationMembers
        } catch (NPE: NullPointerException) {
            mapOf()
        }

        private fun toPoetClassName(jFieldVar: JFieldVar): TypeName {
            val binaryName = jFieldVar.type().binaryName()
            val fullName = jFieldVar.type().erasure().fullName()
            val className = fullQualifiedNameToPoetClassName(fullName)
            if (binaryName == fullName) return className

            // codemodel doesnt expose generics. parse it out
            val generics = binaryName.removePrefix(fullName)
            val unwrap = generics.removePrefix("<").removeSuffix(">")
            // works for simple generics only. TODO: nested generics like Map<String, Map<String, Any>>
            return className.parameterizedBy(
                unwrap.split(",")
                    .map(::fullQualifiedNameToPoetClassName)
            )
        }

        private fun toPoetClassName(jClass: JClass): ClassName =
            fullQualifiedNameToPoetClassName(jClass.fullName())

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
            "java.util.Map" to MAP,
            "java.lang.Object" to ANY
        )
    }
}
