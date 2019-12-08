package com.mitteloupe.testit.parser

import com.mitteloupe.testit.model.DataType

class DataTypeParser {
    fun parse(dataTypeToParse: String): DataType {
        val parsedTreeResult = parseToTree(dataTypeToParse)
        val dataType = tokenParsingResultToDataType(parsedTreeResult)
        if (dataType.name.isEmpty()) {
            throw IllegalArgumentException("Input could not be parsed")
        }
        return dataType
    }

    private fun tokenParsingResultToDataType(result: TokenParsingResult) =
        when (result.tokens.size) {
            1 -> result.tokens.first().toDataType()
            3 -> result.tokens.toDataType()
            else -> throw IllegalArgumentException("Input could not be parsed")
        }

    private fun parseToTree(sourceData: String): TokenParsingResult {
        val tokens = mutableListOf<ParsingToken>()

        val charactersCount = sourceData.length
        var position = 0
        var tokenStartPosition = 0
        var lastCharacter = ""
        do {
            val currentCharacter = sourceData.substring(position, position + 1)
            when (currentCharacter) {
                "<", "(" -> {
                    val childrenParsingResult = parseToTree(sourceData.substring(position + 1))
                    val tokenName = if (position != tokenStartPosition) {
                        sourceData.take(position)
                    } else {
                        ""
                    }
                    tokens.add(ParsingToken(tokenName, childrenParsingResult.tokens))
                    position += childrenParsingResult.charactersParsed + 1
                    tokenStartPosition = position
                }
                "," -> {
                    position = handleDivider(position, sourceData, tokenStartPosition, tokens)
                    tokenStartPosition = position
                }
                ">", ")" -> {
                    if (lastCharacter == "-") {
                        position =
                            handleDivider(position, sourceData, tokenStartPosition, tokens) + 1
                        tokenStartPosition = position
                    } else {
                        if (tokenStartPosition != position) {
                            val tokenName = sourceData.substring(tokenStartPosition, position)
                            tokens.addOrAppendIfNullable(tokenName)
                        }
                        return TokenParsingResult(tokens, position + 1)
                    }
                }
                else -> {
                    position++
                }
            }
            lastCharacter = currentCharacter
        } while (position < charactersCount)

        if (position != tokenStartPosition) {
            val tokenName = sourceData.substring(tokenStartPosition, position)
            tokens.addOrAppendIfNullable(tokenName)
        }

        return TokenParsingResult(tokens, position)
    }

    private fun handleDivider(
        position: Int,
        sourceData: String,
        tokenStartPosition: Int,
        tokens: MutableList<ParsingToken>
    ): Int {
        if (position != 0) {
            val tokenName = sourceData.substring(tokenStartPosition, position)
            tokens.addOrAppendIfNullable(tokenName)
        }
        val siblingsParsingResult = parseToTree(sourceData.substring(position + 1))
        tokens.addAll(siblingsParsingResult.tokens)
        return position + siblingsParsingResult.charactersParsed
    }
}

private fun MutableList<ParsingToken>.addOrAppendIfNullable(tokenName: String) {
    if (tokenName == "?") {
        val lastToken = removeAt(size - 1)
        add(ParsingToken(lastToken.name + "?", lastToken.children))
    } else {
        add(ParsingToken(tokenName))
    }
}

private fun ParsingToken.toDataType(): DataType {
    val isNullable = name.endsWith("?")
    val dataTypeName = if (isNullable) {
        name.dropLast(1)
    } else {
        name
    }
    return if (children.isEmpty()) {
        DataType.Specific(dataTypeName, isNullable)
    } else {
        DataType.Generic(
            dataTypeName,
            isNullable,
            *children.map { token -> token.toDataType() }.toTypedArray()
        )
    }
}

private fun List<ParsingToken>.toDataType(): DataType {
    val returnType = get(2).toDataType()
    return DataType.Lambda(
        returnType.name,
        returnType.isNullable,
        *get(0).children.map { token -> token.toDataType() }.toTypedArray()
    )
}

private data class ParsingToken(
    val name: String,
    val children: List<ParsingToken> = mutableListOf()
)

private data class TokenParsingResult(
    val tokens: List<ParsingToken>,
    val charactersParsed: Int
)
