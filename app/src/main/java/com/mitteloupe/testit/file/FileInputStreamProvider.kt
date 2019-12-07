package com.mitteloupe.testit.file

import java.io.File
import java.io.FileInputStream

/**
 * Created by Eran Boudjnah on 2019-07-07.
 */
class FileInputStreamProvider {
    fun getFileInputStream(file: File) = FileInputStream(file)
}
