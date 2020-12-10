package com.mitteloupe.testit.file

import java.io.File
import java.io.FileInputStream

class FileInputStreamProvider {
    fun getFileInputStream(file: File) = FileInputStream(file)
}
