package com.mitteloupe.testit.parser

import com.mitteloupe.testit.model.FileMetadata

interface KotlinFileParser {
    fun String.parse(): FileMetadata
}