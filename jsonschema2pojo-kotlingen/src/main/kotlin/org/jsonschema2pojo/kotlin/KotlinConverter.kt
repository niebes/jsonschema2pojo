package org.jsonschema2pojo.kotlin

import com.sun.codemodel.JPackage

class KotlinConverter {

    fun convert(pkg: JPackage, sourceFileName: String): String {
        print(sourceFileName)
        print("$pkg")
        return "todo"
    }
}