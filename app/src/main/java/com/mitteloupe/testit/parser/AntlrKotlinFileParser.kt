package com.mitteloupe.testit.parser

import com.mitteloupe.testit.grammer.KotlinParseTree
import com.mitteloupe.testit.grammer.parseKotlinCode
import com.mitteloupe.testit.model.ClassMetadata
import com.mitteloupe.testit.model.DataType
import com.mitteloupe.testit.model.FileMetadata
import com.mitteloupe.testit.model.FunctionMetadata
import com.mitteloupe.testit.model.StaticFunctionsMetadata
import com.mitteloupe.testit.model.TypedParameter

private val unknownDataType = DataType.Specific("Unknown", false)
private val unitDataType = DataType.Specific("Unit", false)

private const val NODE_NAME_SIMPLE_IDENTIFIER = "simpleIdentifier"
private const val NODE_NAME_IDENTIFIER = "Identifier"
private const val NODE_NAME_HEADER_IDENTIFIER = "identifier"
private const val MODIFIER_NODE_NAME = "modifier"

private val dataTypeNodeNames = listOf(
    "typeReference",
    "userType",
    "simpleUserType",
    NODE_NAME_SIMPLE_IDENTIFIER,
    NODE_NAME_IDENTIFIER
)
private val nullableDataTypeNodeNames = listOf("nullableType") + dataTypeNodeNames

class AntlrKotlinFileParser(
    private val dataTypeParser: DataTypeParser
) : KotlinFileParser {
    private var packageName: String = ""

    private val usedImports = mutableMapOf<String, String>()

    private val knownImports = mutableMapOf<String, String>()

    override fun String.parse(): FileMetadata {
        val code = parseKotlinCode(this)

        packageName = code.applyToChildNodes(
            listOf("packageHeader", NODE_NAME_HEADER_IDENTIFIER)
        ) { it.recursiveName }.joinToString("")

        code.applyToChildNodes(
            listOf("importList", "importHeader", NODE_NAME_HEADER_IDENTIFIER)
        ) { it.recursiveName }.forEach { qualifiedName ->
            val entityName = qualifiedName.substringAfterLast(".")
            knownImports[entityName] = qualifiedName
        }

        val classes = code.applyToChildNodes(
            listOf("topLevelObject", "declaration", "classDeclaration"),
            ::extractClassFromNode
        )

        val staticFunctionsMetadata = extractStaticFunctionsMetadataFromNode(code)

        return FileMetadata(classes, staticFunctionsMetadata)
    }

    private fun extractClassFromNode(node: KotlinParseTree): ClassMetadata? {
        node.isClassType || return null
        node.children.stream().filter { childNode ->
            childNode.name == "modifiers"
        }.anyMatch { childNode ->
            childNode.isEnumClass || childNode.isSealedClass || childNode.isPrivateClass
        } && return null

        val className = node.children.firstOrNull { childNode ->
            childNode.name == NODE_NAME_SIMPLE_IDENTIFIER
        }?.nestedName
        val isClassAbstract = node.children.stream().filter { childNode ->
            childNode.name == "modifiers"
        }.anyMatch { childNode -> childNode.isAbstractClass }
        val classParameters = node.childrenNamed("primaryConstructor")
            .flatMap(::extractConstructorParametersListFromNode)
            .onEach { parameter -> addAnyKnownImports(parameter.type) }
        val functions = node.childrenNamed("classBody")
            .flatMap(::extractPublicFunctionsFromNodes)

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

    private val KotlinParseTree.isClassType
        get() = children.any { childNode -> childNode.name == "CLASS" }

    private val KotlinParseTree.isEnumClass
        get() = hasClassModifier("ENUM")

    private val KotlinParseTree.isSealedClass
        get() = hasClassModifier("SEALED")

    private val KotlinParseTree.isPrivateClass
        get() = hasVisibilityModifier("PRIVATE")

    private val KotlinParseTree.isAbstractClass
        get() = hasInheritanceModifier("ABSTRACT")

    private fun KotlinParseTree.hasClassModifier(modifier: String) =
        hasModifier("classModifier", modifier)

    private fun KotlinParseTree.hasInheritanceModifier(modifier: String) =
        hasModifier("inheritanceModifier", modifier)

    private fun KotlinParseTree.hasVisibilityModifier(modifier: String) =
        hasModifier("visibilityModifier", modifier)

    private fun KotlinParseTree.hasModifier(modifierType: String, modifier: String) =
        extractChildNode(listOf(MODIFIER_NODE_NAME, modifierType, modifier)) != null

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

    private fun extractStaticFunctionsMetadataFromNode(code: KotlinParseTree): StaticFunctionsMetadata {
        val staticFunctions = code.applyToChildNodes(
            listOf("topLevelObject", "declaration", "functionDeclaration"),
            ::extractFunctionMetadataFromNode
        )

        val staticFunctionsMetadata = StaticFunctionsMetadata(
            packageName,
            HashMap(usedImports),
            staticFunctions
        )

        resetUsedImports()

        return staticFunctionsMetadata
    }

    private fun extractFunctionMetadataFromNode(node: KotlinParseTree): FunctionMetadata? {
        val functionName = node.children.firstOrNull { childNode ->
            childNode.name == NODE_NAME_SIMPLE_IDENTIFIER
        }?.nestedName
        var isAbstract = false
        var returnType: DataType? = null
        var extensionReceiverType: DataType? = null
        val functionParameters = mutableListOf<TypedParameter>()

        node.children.forEach { childNode ->
            when (childNode.name) {
                "modifiers" -> {
                    if (isPrivateFunction(childNode) ||
                        isProtectedFunction(childNode)
                    ) {
                        return null
                    }
                    if (isAbstractFunction(childNode)) {
                        isAbstract = true
                    }
                }

                "receiverType" -> {
                    extensionReceiverType =
                        extractDataType(childNode)?.text?.dataType ?: unknownDataType
                }

                "functionValueParameters" -> {
                    extractFunctionParametersListFromNode(childNode).let { parameters ->
                        functionParameters.addAll(parameters)
                    }
                }

                "type" -> {
                    returnType = extractDataType(childNode)
                        ?.text?.appendNullableIfNeeded(childNode)?.dataType
                        ?: unknownDataType
                }

                "functionBody" -> {
                    if (returnType == null) {
                        extractTypeByAssignment(childNode)?.let {
                            returnType = unknownDataType
                        }
                    }
                }
            }
        }

        return functionName?.let { validFunctionName ->
            if (!isAbstract) {
                functionParameters.forEach { typedParameter -> addAnyKnownImports(typedParameter.type) }
            }

            extensionReceiverType?.let {
                addImportIfKnown(it.name)
            }

            FunctionMetadata(
                validFunctionName,
                isAbstract,
                functionParameters,
                extensionReceiverType,
                returnType ?: unitDataType
            )
        }
    }

    private fun isPrivateFunction(childNode: KotlinParseTree): Boolean {
        return childNode.extractChildNode(
            listOf(
                MODIFIER_NODE_NAME,
                "visibilityModifier",
                "PRIVATE"
            )
        ) != null
    }

    private fun isProtectedFunction(childNode: KotlinParseTree) =
        childNode.extractChildNode(
            listOf(
                MODIFIER_NODE_NAME,
                "visibilityModifier",
                "PROTECTED"
            )
        ) != null

    private fun isAbstractFunction(childNode: KotlinParseTree) =
        childNode.extractChildNode(
            listOf(
                MODIFIER_NODE_NAME,
                "inheritanceModifier",
                "ABSTRACT"
            )
        ) != null

    private fun extractDataType(childNode: KotlinParseTree): KotlinParseTree? =
        childNode.extractChildNode(dataTypeNodeNames) ?: childNode.extractChildNode(
            nullableDataTypeNodeNames
        )

    private fun extractTypeByAssignment(childNode: KotlinParseTree) = childNode.extractChildNode(
        listOf("expression", "disjunction", "conjunction", "equality")
    )

    private val KotlinParseTree.nestedName
        get() = extractChildNode(listOf(NODE_NAME_IDENTIFIER))?.text ?: children.firstOrNull()?.text

    private val KotlinParseTree.recursiveName: String
        get(): String = when (name) {
            NODE_NAME_SIMPLE_IDENTIFIER -> nestedName
            "DOT" -> "."
            "LANGLE" -> "<"
            "RANGLE" -> ">"
            "QUEST_NO_WS" -> "?"
            in Regex("[A-Z]+") -> text.orEmpty()
            else -> null
        } ?: children.joinToString("") { childNode ->
            childNode.recursiveName
        }

    private fun extractConstructorParametersListFromNode(node: KotlinParseTree) =
        node.applyToChildNodes(
            listOf("classParameters", "classParameter"),
            ::extractTypedParameterFromNode
        )

    private fun extractFunctionParametersListFromNode(node: KotlinParseTree) =
        node.applyToChildNodes(
            listOf("functionValueParameter", "parameter"),
            ::extractTypedParameterFromNode
        )

    private fun extractTypedParameterFromNode(node: KotlinParseTree): TypedParameter? {
        val parameterName = node.children.firstOrNull { childNode ->
            childNode.name == NODE_NAME_SIMPLE_IDENTIFIER
        }?.nestedName
        val parameterType = node.children.firstOrNull { childNode ->
            childNode.name == "type"
        }?.recursiveName?.dataType

        return parameterName?.let {
            parameterType?.let {
                TypedParameter(parameterName, parameterType)
            }
        }
    }

    private fun addAnyKnownImports(dataType: DataType) {
        getAllSpecificTypes(dataType).forEach(::addImportIfKnown)
    }

    private fun getAllSpecificTypes(vararg dataTypes: DataType): List<String> =
        dataTypes.flatMap { dataType ->
            when (dataType) {
                is DataType.Specific -> listOf(dataType.name)
                is DataType.Generic -> getAllSpecificTypes(*dataType.genericTypes) + dataType.name
                is DataType.Lambda -> getAllSpecificTypes(*dataType.inputParameterTypes) + dataType.name
            }
        }

    private fun addImportIfKnown(entityName: String) {
        val baseEntity = entityName.substringBefore(".")
        knownImports[baseEntity]?.let { qualifiedName ->
            usedImports[baseEntity] = qualifiedName
            knownImports.remove(baseEntity)
        }
    }

    private fun resetUsedImports() {
        usedImports.forEach { (entityName, qualifiedName) ->
            knownImports[entityName] = qualifiedName
        }
        usedImports.clear()
    }

    private val String.dataType
        get() = dataTypeParser.parse(this)

    private fun <T : Any> KotlinParseTree.applyToChildNodes(
        nodeNames: List<String>,
        action: (KotlinParseTree) -> T?
    ): List<T> = children.filter { child ->
        child.name == nodeNames[0]
    }.flatMap { matchingChild ->
        if (nodeNames.size == 1) {
            listOf(action(matchingChild))
        } else {
            matchingChild.applyToChildNodes(nodeNames.subList(1, nodeNames.size), action)
        }
    }.filterNotNull()

    private fun KotlinParseTree.childrenNamed(nodeName: String) =
        children.filter { childNode -> childNode.name == nodeName }

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

private fun String.appendNullableIfNeeded(childNode: KotlinParseTree) =
    if (childNode.children.first().name == "nullableType") {
        "$this?"
    } else {
        this
    }
