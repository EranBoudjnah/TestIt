package com.mitteloupe.testit.parser

import java.io.File

class FileProvider {
    fun getFile(filePath: String) = File(filePath)
}