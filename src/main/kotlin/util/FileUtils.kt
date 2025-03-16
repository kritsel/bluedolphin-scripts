package util

import java.io.BufferedReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun readFileFromResources(fileName: String) : BufferedReader?{
    val inputStream = object {}.javaClass.getResourceAsStream(fileName)
    return inputStream?.bufferedReader()
}

fun readFileContentFromFile(file: File) : String{
    val url = file.toURI().toURL()
    return url.readText() ?: ""
}

fun getAllFilesInResources(subDirectory:String? = null) : List<File>{
    val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
    var resourcesPath = Paths.get(projectDirAbsolutePath, "/src/main/resources")
    if (subDirectory != null) {
        resourcesPath = Paths.get(projectDirAbsolutePath, "/src/main/resources/$subDirectory")
    }
    return Files.walk(resourcesPath)
        .filter { item -> Files.isRegularFile(item) }
        .map { it.toFile() }
        .toList()
}