package bluedolphin.file

import java.io.File

class CSVGenerator:FileGenerator {
    override fun generateFile(file: File, data: List<List<Cell>>, columnStyleConfig:MutableMap<Int, StyleConfig>?, freezePaneAt:Pair<Int?, Int?>?) {
        file.printWriter().use { out ->
            data.forEach { rowData ->
                println(rowData.joinToString("; ") {if (it.value != null) it.value.toString() else ""})
            }

        }
        println("created CSV file ${file.absolutePath} (${data.size} rows)")
    }
}