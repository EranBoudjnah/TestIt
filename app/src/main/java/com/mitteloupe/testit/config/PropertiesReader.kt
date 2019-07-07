package com.mitteloupe.testit.config

import com.mitteloupe.testit.file.FileInputStreamProvider
import com.mitteloupe.testit.file.FileProvider
import com.mitteloupe.testit.model.Configuration
import java.util.Properties

class PropertiesReader(
    private val fileProvider: FileProvider,
    private val fileInputStreamProvider: FileInputStreamProvider,
    private val configurationBuilder: ConfigurationBuilder
) {
    fun readFromFile(fileName: String): Configuration {
        val properties = Properties()
        val propertiesFile = fileProvider.getFile(fileName)

        val inputStream = fileInputStreamProvider.getFileInputStream(propertiesFile)
        properties.load(inputStream)

        properties.forEach { (key, value) ->
            configurationBuilder.addProperty(key, value)
        }

        return configurationBuilder.build()
    }
}