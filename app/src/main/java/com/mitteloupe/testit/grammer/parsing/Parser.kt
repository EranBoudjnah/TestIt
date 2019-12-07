package com.mitteloupe.testit.grammer.parsing

import com.mitteloupe.testit.grammer.KotlinLexerException
import com.mitteloupe.testit.grammer.KotlinParseTree
import com.mitteloupe.testit.grammer.KotlinParseTreeNodeType
import com.mitteloupe.testit.grammer.KotlinParserException
import com.mitteloupe.testit.grammer.KotlinToken
import com.mitteloupe.testit.grammer.KotlinTokensList
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ListTokenSource
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token.DEFAULT_CHANNEL
import org.antlr.v4.runtime.misc.Pair
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import org.jetbrains.kotlin.spec.grammar.KotlinLexer
import org.jetbrains.kotlin.spec.grammar.KotlinParser

internal object Parser {
    private val errorLexerListener = object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            message: String,
            e: RecognitionException?
        ) = throw KotlinLexerException(message, kotlin.Pair(line, charPositionInLine))
    }

    private val errorParserListener = object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            message: String,
            e: RecognitionException?
        ) = throw KotlinParserException(message, kotlin.Pair(line, charPositionInLine))
    }

    private fun getCharsStream(str: String) =
        CharStreams.fromStream(ByteArrayInputStream(str.toByteArray()), StandardCharsets.UTF_8)

    private fun getAntlrTokenByKotlinToken(token: KotlinToken, tokenTypeMap: Map<String, Int>): CommonToken? {
        return tokenTypeMap[token.type]?.let { tokenNumber ->
            if (token.channel == DEFAULT_CHANNEL) {
                CommonToken(tokenNumber, token.text)
            } else {
                CommonToken(Pair(null, getCharsStream(token.text)), tokenNumber, token.channel, 0, 0)
            }
        }
    }

    fun tokenize(sourceCode: String): KotlinTokensList {
        val lexer = KotlinLexer(getCharsStream(sourceCode)).apply {
            removeErrorListeners()
            addErrorListener(errorLexerListener)
        }

        return KotlinTokensList(
            lexer.allTokens.map {
                KotlinToken(
                    lexer.vocabulary.getSymbolicName(it.type),
                    it.text,
                    it.channel
                )
            }
        )
    }

    private fun buildTree(
        parser: KotlinParser,
        tokenTypeMap: Map<String, Int>,
        antlrParseTree: ParseTree,
        kotlinParseTree: KotlinParseTree
    ): KotlinParseTree {
        for (i in 0..antlrParseTree.childCount) {
            val antlrParseTreeNode = antlrParseTree.getChild(i) ?: continue
            val kotlinParseTreeNode = when (antlrParseTreeNode) {
                is TerminalNodeImpl ->
                    KotlinParseTree(
                        KotlinParseTreeNodeType.TERMINAL,
                        KotlinLexer.VOCABULARY.getSymbolicName(antlrParseTreeNode.symbol.type),
                        antlrParseTreeNode.symbol.text
                    )
                else ->
                    KotlinParseTree(
                        KotlinParseTreeNodeType.RULE,
                        parser.ruleNames[(antlrParseTreeNode as RuleContext).ruleIndex]
                    )
            }

            kotlinParseTree.children.add(kotlinParseTreeNode)
            buildTree(parser, tokenTypeMap, antlrParseTreeNode, kotlinParseTreeNode)
        }

        return kotlinParseTree
    }

    fun parse(tokens: List<KotlinToken>): KotlinParseTree {
        val tokenTypeMap = KotlinLexer(null).tokenTypeMap
        val tokensList = ListTokenSource(tokens.mapNotNull { getAntlrTokenByKotlinToken(it, tokenTypeMap) })
        val parser = KotlinParser(CommonTokenStream(tokensList)).apply {
            removeErrorListeners()
            addErrorListener(errorParserListener)
        }
        val tree = parser.kotlinFile()

        return buildTree(
            parser,
            tokenTypeMap,
            tree,
            KotlinParseTree(
                KotlinParseTreeNodeType.RULE,
                parser.ruleNames[parser.ruleIndexMap["kotlinFile"]!!]
            )
        )
    }
}
