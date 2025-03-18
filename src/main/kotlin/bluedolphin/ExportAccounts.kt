package bluedolphin

import bluedolphin.api.UserAPI
import bluedolphin.file.*
import org.json.JSONArray
import org.json.JSONObject
import util.trustStoreMagic
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val FIRST_NAME = "first name"
const val LAST_NAME = "last name"
const val EMAIL = "email"

/**
 * Prerequisites to run this function:
 *
 * - VM option: --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED
 * - environment variables:
 *   - BD_API_KEY: User API key secret
 *     (ask Kristel,
 *      or see https://support.valueblue.nl/hc/en-us/articles/13296899552668-Quick-Start-Guide
 *      to create your own Key Management API key and User API key)
 */
fun main() {
    // company managed windows laptop --> use Windows truststore
    trustStoreMagic()

    println()
    println("*********************************************************************************************************")
    println("* fetch BlueDolphin data")
    println("* (takes a couple of minutes as we need to space REST API calls by 0.5 secs to prevent hitting the rate limit)")

    val allUserDetails = UserAPI().getAllUserDetails(test=false)
    val allRoleNames = allUserDetails.values.flatMap{ collectRollNames(it) }.toSet()

    println()
    println("*********************************************************************************************************")
    println("* create data structure for file generator")

    val exportUsers = allUserDetails.values.map{ createExportUser(it, allRoleNames) }.toList()
    val sortedRoleNames = allRoleNames.sorted()

    // init data structure for CSV File Generator
    val data:MutableList<MutableList<Cell>> = mutableListOf()

    // header row
    val headerRow = mutableListOf(
        Cell("FIRST_NAME", HEADER_STYLE_1),
        Cell("LAST_NAME", HEADER_STYLE_1),
        Cell("EMAIL", HEADER_STYLE_1)
    )
    sortedRoleNames.stream().forEach {
        headerRow.add(Cell(it, HEADER_ANGLE_STYLE_1))
    }
    data.add(headerRow)

    // content rows
    exportUsers.forEach { exportUser ->
        val userRow:MutableList<Cell> = mutableListOf(
            Cell(exportUser[FIRST_NAME], VALUE_STYLE_1),
            Cell(exportUser[LAST_NAME], VALUE_STYLE_1),
            Cell(exportUser[EMAIL], VALUE_STYLE_1))

        sortedRoleNames.stream().forEach{
            userRow.add(Cell(exportUser[it], MEMBER_OF_STYLE_1))
        }
        data.add(userRow)
    }

    val MEMBER_OF_WIDTH = 6
    var colNo = 0
    val columnStyleConfig:MutableMap<Int, StyleConfig> = mutableMapOf()
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * 15)   // first name
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * 25)   // last name
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * 35)   // email
    allRoleNames.forEach {
        columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * MEMBER_OF_WIDTH, StyleElement.ANGLE to true)   // member of
    }

    println()
    println("*********************************************************************************************************")
    println("* generate file")

    val formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    ExcelGenerator().generateFile(File("BlueDolphin user-role matrix $formattedDate.xlsx"), data, columnStyleConfig, freezePaneAt = Pair(3, 1))
}

fun collectRollNames(user:JSONObject):Set<String>{
    return (user.get("roles") as JSONArray).map{(it as JSONObject).get("name") as String}.toSet()
}

fun createExportUser (blueDolphinUser:JSONObject, allRoleNames:Set<String>) : Map<String, String>{
    val exportUser:MutableMap<String, String> = mutableMapOf()
    exportUser[FIRST_NAME] = blueDolphinUser["first_name"] as String
    exportUser[LAST_NAME] = blueDolphinUser["last_name"] as String
    exportUser[EMAIL] = blueDolphinUser["email"] as String
    allRoleNames.forEach { roleName ->
        if (collectRollNames(blueDolphinUser).contains(roleName)) {
            exportUser[roleName] = "x"
        } else {
            exportUser[roleName] = ""
        }
    }
    return exportUser
}

val HEADER_STYLE_1 = mapOf(StyleElement.BOLD to true)
val HEADER_ANGLE_STYLE_1:StyleConfig = mapOf(StyleElement.BOLD to true, StyleElement.ANGLE to true, StyleElement.COLOR to "#E8E8E8")
val VALUE_STYLE_1:StyleConfig = mapOf()
val MEMBER_OF_STYLE_1:StyleConfig = mapOf(StyleElement.CENTER to true)




