package com.mitteloupe.testit.parser

import com.mitteloupe.testit.grammer.KotlinParseTree
import com.mitteloupe.testit.grammer.parseKotlinCode
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.FileMetadata
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.TypedParameter

/**
 * Created by Eran Boudjnah on 2019-07-05.
 */
class AntlrKotlinFileParser : KotlinFileParser {
    private val genericsRegex by lazy { Regex("<(\\w+\\.)*\\w+>") }

    private var packageName: String = ""

    private val usedImports = mutableMapOf<String, String>()

    private val knownImports = mutableMapOf<String, String>()

    override fun String.parse(): FileMetadata {
        val code = parseKotlinCode(this)

        packageName = code.applyToChildNodes(
            listOf("packageHeader", "identifier"),
            ::getNameRecursivelyFromChildren
        ).joinToString("")

        code.applyToChildNodes(
            listOf("importList", "importHeader", "identifier"),
            ::getNameRecursivelyFromChildren
        ).forEach { qualifiedName ->
            val entityName = qualifiedName.substringAfterLast(".")
            knownImports[entityName] = qualifiedName
        }

        val classes = code.applyToChildNodes(
            listOf("topLevelObject", "declaration", "classDeclaration"),
            ::extractClassFromNode
        )

        return FileMetadata(classes)
    }

    private fun extractClassFromNode(node: KotlinParseTree): ClassMetadata? {
        var className: String? = null
        var isClassAbstract = false
        val classParameters = mutableListOf<TypedParameter>()
        val functions = mutableListOf<FunctionMetadata>()

        if (!isClassType(node)) {
            return null
        }

        node.children.forEach { childNode ->
            when (childNode.name) {
                "simpleIdentifier" -> className = getNameFromNode(childNode)
                "primaryConstructor" -> {
                    extractConstructorParametersListFromNode(childNode).let { parameters ->
                        classParameters.addAll(parameters)
                    }
                }
                "classBody" -> {
                    extractPublicFunctionsFromNodes(childNode).let { parameters ->
                        functions.addAll(parameters)
                    }
                }
                "modifiers" -> {
                    if (isDataClass(childNode) ||
                        isEnumClass(childNode) ||
                        isSealedClass(childNode) ||
                        isPrivateClass(childNode)
                    ) {
                        return null
                    }
                    if (isAbstractClass(childNode)) {
                        isClassAbstract = true
                    }
                }
            }
        }

        val classMetadata = className?.let { validClassName ->
            ClassMetadata(
                packageName,
                HashMap(usedImports),
                validClassName,
                isClassAbstract,
                classParameters,
                functions
            )
        }

        resetUsedImports()

        return classMetadata
    }

    private fun isClassType(node: KotlinParseTree) =
        node.children.any { childNode -> childNode.name == "CLASS" }

    private fun isDataClass(childNode: KotlinParseTree) =
        childNode.hasClassModifier("DATA")

    private fun isEnumClass(childNode: KotlinParseTree) =
        childNode.hasClassModifier("ENUM")

    private fun isSealedClass(childNode: KotlinParseTree) =
        childNode.hasClassModifier("SEALED")

    private fun isPrivateClass(childNode: KotlinParseTree) =
        childNode.hasInheritanceModifier("PRIVATE")

    private fun isAbstractClass(childNode: KotlinParseTree) =
        childNode.hasInheritanceModifier("ABSTRACT")

    private fun KotlinParseTree.hasClassModifier(modifier: String) =
        hasModifier("classModifier", modifier)

    private fun KotlinParseTree.hasInheritanceModifier(modifier: String) =
        hasModifier("inheritanceModifier", modifier)

    private fun KotlinParseTree.hasVisibilityModifier(modifier: String) =
        hasModifier("visibilityModifier", modifier)

    private fun KotlinParseTree.hasModifier(modifierType: String, modifier: String) =
        extractChildNode(listOf("modifier", modifierType, modifier)) != null

    private fun extractPublicFunctionsFromNodes(node: KotlinParseTree) =
        node.applyToChildNodes(
            listOf(
                "classMemberDeclarations",
                "classMemberDeclaration",
                "declaration",
                "functionDeclaration"
            ),
            ::extractFunctionMetadataFromNode
        )

    private fun extractFunctionMetadataFromNode(node: KotlinParseTree): FunctionMetadata? {
        var functionName: String? = null
        var isAbstract = false
        var returnType: String? = null
        val functionParameters = mutableListOf<TypedParameter>()

        node.children.forEach { childNode ->
            when (childNode.name) {
                "simpleIdentifier" -> {
                    functionName = getNameFromNode(childNode)
                }
                "modifiers" -> {
                    if (isPrivateFunction(childNode)) {
                        return null
                    }
                    if (isAbstractFunction(childNode)) {
                        isAbstract = true
                    }
                }
                "functionValueParameters" -> {
                    extractFunctionParametersListFromNode(childNode).let { parameters ->
                        functionParameters.addAll(parameters.map {
                            val typeWithoutGenerics = getTypeWithoutGenerics(it.type)
                            addImportIfKnown(typeWithoutGenerics)

                            TypedParameter(
                                it.name,
                                typeWithoutGenerics
                            )
                        })
                    }
                }
                "type" -> {
                    extractFunctionReturnType(childNode)?.let {
                        returnType = it.text
                    }
                }
                "functionBody" -> {
                    if (returnType == null) {
                        extractTypeByAssignment(childNode)?.let {
                            returnType = "Unknown"
                        }
                    }
                }
            }
        }

        return functionName?.let { validFunctionName ->
            FunctionMetadata(validFunctionName, isAbstract, functionParameters, returnType ?: "Unit")
        }
    }

    private fun isPrivateFunction(childNode: KotlinParseTree) =
        childNode.extractChildNode(listOf("modifier", "visibilityModifier", "PRIVATE")) != null

    private fun isAbstractFunction(childNode: KotlinParseTree) =
        childNode.extractChildNode(listOf("modifier", "inheritanceModifier", "ABSTRACT")) != null

    private fun extractFunctionReturnType(childNode: KotlinParseTree) = childNode.extractChildNode(
        listOf("typeReference", "userType", "simpleUserType", "simpleIdentifier", "Identifier")
    )

    private fun extractTypeByAssignment(childNode: KotlinParseTree) = childNode.extractChildNode(
        listOf("expression", "disjunction", "conjunction", "equality")
    )

    private fun getNameFromNode(node: KotlinParseTree) = node.extractChildNode(listOf("Identifier"))?.text

    private fun getNameRecursivelyFromChildren(node: KotlinParseTree): String =
        when (node.name) {
            "simpleIdentifier" -> getNameFromNode(node)
            "DOT" -> "."
            "LANGLE" -> "<"
            "RANGLE" -> ">"
            in Regex("[A-Z]+") -> node.text ?: ""
            else -> null
        } ?: node.children.joinToString("") { childNode ->
            getNameRecursivelyFromChildren(childNode)
        }

    private fun extractConstructorParametersListFromNode(node: KotlinParseTree) = node.applyToChildNodes(
        listOf("classParameters", "classParameter"),
        ::extractTypedParameterFromNode
    )

    private fun extractFunctionParametersListFromNode(node: KotlinParseTree) = node.applyToChildNodes(
        listOf("functionValueParameter", "parameter"),
        ::extractTypedParameterFromNode
    )

    private fun extractTypedParameterFromNode(node: KotlinParseTree): TypedParameter? {
        var parameterName: String? = null
        var parameterType: String? = null

        node.children.forEach { childNode ->
            when (childNode.name) {
                "simpleIdentifier" -> {
                    parameterName = getNameFromNode(childNode)
                }
                "type" -> {
                    parameterType = getNameRecursivelyFromChildren(childNode)
                }
            }
        }

        return if (parameterName != null && parameterType != null) {
            addImportIfKnown(getTypeWithoutGenerics(parameterType!!))
            TypedParameter(parameterName!!, parameterType!!)
        } else {
            null
        }
    }

    private fun addImportIfKnown(entityName: String) {
        knownImports[entityName]?.let { qualifiedName ->
            usedImports[entityName] = qualifiedName
            knownImports.remove(entityName)
        }
    }

    private fun resetUsedImports() {
        usedImports.forEach { (entityName, qualifiedName) -> knownImports[entityName] = qualifiedName }
        usedImports.clear()
    }

    private fun getTypeWithoutGenerics(parameterType: String) = parameterType.replace(genericsRegex, "")

    private fun <T : Any> KotlinParseTree.applyToChildNodes(
        nodeNames: List<String>,
        action: (KotlinParseTree) -> T?
    ): List<T> =
        children.filter { child ->
            child.name == nodeNames[0]
        }.flatMap { matchingChild ->
            if (nodeNames.size == 1) {
                listOf(action(matchingChild))
            } else {
                matchingChild.applyToChildNodes(nodeNames.subList(1, nodeNames.size), action)
            }
        }.mapNotNull { it }

    private fun KotlinParseTree.extractChildNode(nodeNames: List<String>): KotlinParseTree? =
        children.firstOrNull { child ->
            child.name == nodeNames[0]
        }?.let { matchingChild ->
            if (nodeNames.size == 1) {
                matchingChild
            } else {
                matchingChild.extractChildNode(nodeNames.subList(1, nodeNames.size))
            }
        }

    private operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)
}
