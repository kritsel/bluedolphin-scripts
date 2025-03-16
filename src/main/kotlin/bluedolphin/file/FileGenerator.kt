package bluedolphin.file

import java.io.File

interface FileGenerator {
    fun generateFile(file: File, data:List<List<Cell>>, columnStyleConfig:MutableMap<Int, StyleConfig>? = null)
}

data class Cell (
    val value:Any?,
    val style: StyleConfig
)

typealias StyleConfig = Map<StyleElement, Any>?

enum class StyleElement{
    // font styling
    BOLD,
    TEXT_SIZE,
    // text positioning styling
    WRAP_TEXT,
    CENTER,
    ANGLE,
    // cell styling
    COLOR,
    // column styling
    BORDER_RIGHT,
    WIDTH
}