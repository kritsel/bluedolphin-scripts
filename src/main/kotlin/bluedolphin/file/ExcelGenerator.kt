package bluedolphin.file

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.RegionUtil
import org.apache.poi.xssf.usermodel.*
import util.lightenColor
import java.awt.Color
import java.io.File
import java.util.*

//const val NAME: String = "name"
//const val BOLD:String = "bold"
//const val COLOR:String = "color"
//const val ANGLE: String = "angle"
//const val TEXT_SIZE: String = "text-size"
//const val BORDER_RIGHT: String = "border-right"
//const val WRAP_TEXT: String = "wrap-text"
//const val CENTER: String = "center"
//val WIDTH = "width"

class ExcelGenerator : FileGenerator {

    override fun generateFile(file: File, data: List<List<Cell>>, columnStyleConfig:MutableMap<Int, StyleConfig>?) {
        val workbook = XSSFWorkbook()
        val workSheet = workbook.createSheet()

        val styleDescriptors = data.flatMap { row -> row.map { cell -> cell.style } }.toSet()
        val styleMap = styleDescriptors.associateWith { styleDesc ->
            val style = workbook.createCellStyle()
            val font = workbook.createFont()
            font.setFontName("Aptos Narrow")
            style.setFont(font)
            if (styleDesc != null) {
                // font settings
                styleDesc[StyleElement.BOLD]
                    ?.let {style.font.bold = it as Boolean}
                styleDesc[StyleElement.TEXT_SIZE]
                    ?.let {style.font.fontHeightInPoints = (it as Int).toShort()}
                // text positioning settings
                styleDesc[StyleElement.ANGLE]
                    ?.let {if (it as Boolean) style.rotation = 45}
                styleDesc[StyleElement.WRAP_TEXT]
                    ?.let {style.wrapText = it as Boolean}
                styleDesc[StyleElement.CENTER]
                    ?.let {if (it as Boolean) style.alignment = HorizontalAlignment.CENTER}
                // cell settings
                styleDesc[StyleElement.COLOR]
                    ?.let {
                        (style as XSSFCellStyle).setFillForegroundColor(getStyleColor(it as String))
                        style.fillPattern = FillPatternType.SOLID_FOREGROUND
                    }

            }
            style
        }

        data.forEachIndexed { r, dataRow ->
            val row = workSheet.createRow(r)
            dataRow.forEachIndexed { c, dataValue ->
                val cell = row.createCell(c)
                dataValue.value?.let {
                    if (it is String) {
                        cell.setCellValue(it)
                    } else if (it is Int) {
                        cell.setCellValue(it.toDouble())
                    } else if (it is Date) {
                        cell.setCellValue(it)
                    } else {
                        cell.setCellValue(it.toString())
                    }
                }
                dataValue.style?.let { cell.cellStyle = styleMap[it] }
            }
        }

        columnStyleConfig?.entries?.forEach { (colNo, colConfig) ->
            colConfig?.let {
                colConfig[StyleElement.WIDTH]
                    ?.let { workSheet.setColumnWidth(colNo, it as Int) }
                colConfig[StyleElement.BORDER_RIGHT]
                    ?.let {
                        val range = CellRangeAddress (0, data.size-1, colNo, colNo)
                        RegionUtil.setBorderRight(BorderStyle.THICK, range, workSheet);
                    }
            }
        }

        // add filter to header row
        workSheet.setAutoFilter(
            CellRangeAddress(0, 0, 0, data[0].size - 1)
        )

        // freeze pane (freeze top row and first column)
        workSheet.createFreezePane(1, 1);

        workbook.write(file.outputStream())
        workbook.close()

        println("created Excel file ${file.absolutePath} (${data.size} rows)")
    }

    private fun getStyleColor(hexColor:String): XSSFColor {
        val red = hexColor.substring(1, 3).toInt(16)
        val green = hexColor.substring(3, 5).toInt(16)
        val blue = hexColor.substring(5, 7).toInt(16)


        // Create a new color and set it as the foreground color
        val color = XSSFColor(Color(red, green, blue), null)
        return color
    }
}

fun main() {

    val DARK_COLOR = "#1EAAF0"
    val LIGHT_COLOR = lightenColor(DARK_COLOR, 0.5f)
    val LIGHT2_COLOR = lightenColor(DARK_COLOR, 0.75f)
    val HEADER1 = mapOf(StyleElement.BOLD to true, StyleElement.COLOR to DARK_COLOR)
    val HEADER2 = mapOf(StyleElement.BOLD to true, StyleElement.COLOR to LIGHT_COLOR)
    val HEADER_RIGHT_BORDER = mapOf(StyleElement.BOLD to true, StyleElement.COLOR to LIGHT2_COLOR, StyleElement.ANGLE to true, StyleElement.BORDER_RIGHT to true)
    val CELL:StyleConfig = mapOf(StyleElement.TEXT_SIZE to 8)

    val testdata:List<List<Cell>> = listOf(
        listOf(
            Cell("header 1", HEADER1),
            Cell("header 2", HEADER2),
            Cell("header 3", HEADER_RIGHT_BORDER)
        ),
        listOf(
            Cell("value 1", CELL),
            Cell("value 2", CELL),
            Cell("value 3", CELL)
        )
    )
    ExcelGenerator().generateFile(File("test-excel.xlsx"), testdata)
}



