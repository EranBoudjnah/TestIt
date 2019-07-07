package com.mitteloupe.testit.file

import java.io.File

class FileProvider {
    fun getFile(filePath: String) = File(filePath)
}