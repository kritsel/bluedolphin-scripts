package bluedolphin

import bluedolphin.api.ObjectAPI
import bluedolphin.file.*
import bluedolphin.ui_api.*
import util.lightenColor
import util.trustStoreMagic
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Prerequisites to run this function:
 *
 * - VM option: --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED
 * - environment variables:
 *   - BD_API_KEY: User API key secret
 *     (ask Kristel,
 *      or see https://support.valueblue.nl/hc/en-us/articles/13296899552668-Quick-Start-Guide
 *      to create your own Key Management API key and User API key)
 *   - BD_UI_ACCESS_TOKEN: Access token used by the bluedolphin webapp
 *       steps to obtain this token:
 *       1) login to bluedolphin
 *       2) open the developer tools in your browser, go to Network
 *       3) navigate to another bluedolphin page
 *       4) select one of the calls to https://bd-presentation-api.eu.bd-cloud.app/.....
 *       5) open the 'Headers' tab, go to request headers and get the token for the 'Authorization' header
 *          (omit the 'Bearer ' part)
 */
fun main() {
    val test = false
    // company managed windows laptop --> use Windows truststore
    trustStoreMagic()

    // based on the order in which categories (a.k.a. layers) are displayed on https://bluedolphin.app/kramp/settings/display
    val categoryOrder = listOf("generic_layer",  "motivationextension", "strategy_layer",
        "migrationimplementationextension", "bpmn", "business_layer", "application_layer", "technology_layer",
        "physical_layer", "logical_data_dictionary")
    val categoryPrefixMap = mapOf(
        "generic_layer" to "GEN",  "motivationextension" to "MOT", "strategy_layer" to "STRAT",
        "migrationimplementationextension" to "I&M", "bpmn" to "BPMN", "business_layer" to "BUS",
        "application_layer" to "APP", "technology_layer" to "TECH", "physical_layer" to "PHYS",
        "logical_data_dictionary" to "DD")

    println()
    println("*********************************************************************************************************")
    println("* fetch BlueDolphin data")
    println("* (takes about a minute as we need to space REST API calls by 0.5 secs to prevent hitting the rate limit)")

    // get all role details (contains members and the object definition specific permissions for each role)
    val allRoleDetails = Roles_UI_API().getAllRoleDetailsUI(test = test)
    val sortedRoleDetails = allRoleDetails.values.sortedBy { it.members.size }.reversed()
    // get all object definitions
    val allObjectDefinitions = ObjectDefinition_UI_API().getAllObjectDefinitionsUI(test = test)
    // determine the number of object instances per object definition
    println("getting object count per object definition")
    println("  (single REST call per object definition with 0.5 sec sleep in between calls to prevent hitting the rate limit)")
    val objectCountPerObjectDefinitionMap = allObjectDefinitions.keys.associateWith { ObjectAPI().getObjects(1, 0, it).total_items }

    println()
    println("*********************************************************************************************************")
    println("* mix data")

    // determine the questionnaire names linked to each object definition (prefix with '#')
    val questionnairesPerObjectDef =
        allRoleDetails.values
            .first()
            .object_definition_permissions
            .associate{ it.id to it.permissions.filter{ isHexValue(it.key) }.map{"#${it.label}"} }

    // init objectDefinitionsPermissionsMap
    val objectDefinitionsPermissionsMap = allObjectDefinitions.values
        .associate { it.id to ObjectDefinitionPermissions(
            it.id, it.name, it.name_internal,
            it.is_bpmn, it.object_type,
            objectCountPerObjectDefinitionMap[it.id]!!,
            questionnairesPerObjectDef[it.id]!!)  }

    // loop through all roles
    allRoleDetails.values.forEachIndexed { i, role ->
        // per role, loop through all object definitions and populate objectDefinitionsPermissionsMap
        role.object_definition_permissions.forEach { objectDef ->
            val objectDefRoleInfo = ObjectDefinitionRoleInfo()
            objectDefinitionsPermissionsMap[objectDef.id]?.roleInfoMap?.set(role.id, objectDefRoleInfo)
            objectDef.permissions.forEach { permission ->
                objectDefRoleInfo.permissions?.add(permission)
                objectDefRoleInfo.is_default_visible = objectDef.is_default_visible
            }
        }
    }

    // determine all categories
    val categoryMap:Map<String, Category> = allObjectDefinitions.values
        .associate { it.object_type.category_internal to
                Category(it.object_type.category, it.object_type.category_internal, it.object_type.color ?: "#a9a9a9") }


    val categoriesPermissionsMap = objectDefinitionsPermissionsMap.values.groupBy {it.type.category_internal}
    val sortedCategoryObjectDefs = categoriesPermissionsMap.entries.sortedBy { categoryOrder.indexOf(it.key) }


    println("found ${allObjectDefinitions.size} object definitions")
    println("  categories: ${categoryMap.values.joinToString (", ") { it.category}}")
    println("found ${allRoleDetails.size} roles with details")
    println("  roles: ${allRoleDetails.values.joinToString(", ") { it.name + "(" + it.members.size + ")" }}")

    println()
    println("*********************************************************************************************************")
    println("* create data structure for file generator")
    val data:MutableList<MutableList<Cell>> = mutableListOf()

    val headerRow = mutableListOf(
        Cell("object definition", HEADER_STYLE),
        Cell("object definition id", HEADER_SMALL_ANGLE_STYLE),
        Cell("object type", HEADER_SMALL_ANGLE_STYLE),
        Cell("object type id", HEADER_SMALL_ANGLE_STYLE),
        Cell("is BPMN", HEADER_ANGLE_STYLE),
        Cell("object questionnaires", HEADER_ANGLE_STYLE),
        Cell("object count", HEADER_ANGLE_STYLE),
        Cell("#non-admin roles with write permissions", HEADER_ANGLE_STYLE)
    )
    sortedRoleDetails.stream().forEach {
        val memberCount = allRoleDetails[it.id]?.members?.size
        headerRow.add(Cell("${it.name} ($memberCount) - default visible?", HEADER_ANGLE_STYLE))
        headerRow.add(Cell("${it.name} ($memberCount) - #write permissions", HEADER_ANGLE_STYLE))
        headerRow.add(Cell("${it.name} ($memberCount) - write permissions", HEADER_ANGLE_STYLE))
    }
    data.add(headerRow)

    sortedCategoryObjectDefs.forEach {
        val category = categoryMap[it.key]!!
        val CAT_STYLE:StyleConfig = mutableMapOf(StyleElement.BOLD to true, StyleElement.COLOR to category.color)
        val CAT_LONG_STYLE = mapOf(StyleElement.COLOR to category.color, StyleElement.TEXT_SIZE to 8, StyleElement.WRAP_TEXT to true)

        val categoryRowData = mutableListOf(
            Cell(category.category, CAT_STYLE),    // object def name
            Cell(null, CAT_LONG_STYLE),      // object def ic
            Cell(null, CAT_LONG_STYLE),      // object def type name
            Cell(null, CAT_LONG_STYLE),      // object def type id
            Cell(null, CAT_STYLE),           // is BPMN
            Cell(null, CAT_LONG_STYLE),      // questionnaire names
            Cell(null, CAT_STYLE),           // object count
            Cell(null, VALUE_STYLE)
        )   // non-admin write permission count
        repeat(allRoleDetails.size) {
            categoryRowData.add (Cell(null, VALUE_STYLE)) // is default visible
            categoryRowData.add (Cell(null, VALUE_STYLE)) // write permissions count
            categoryRowData.add (Cell(null, VALUE_LONG_STYLE)) // write permissions
        }
        data.add(categoryRowData)
        // iterate over the object definitions in this category
        it.value.forEach{od ->
            val lightCategoryColor = lightenColor(category.color, 0.7f)
            val OD_COLOR_STYLE = mapOf(StyleElement.COLOR to lightCategoryColor)
            val OD_COLOR_CENTER_STYLE = mapOf(StyleElement.COLOR to lightCategoryColor, StyleElement.CENTER to true)
            val OD_COLOR_SMALL_STYLE = mapOf(StyleElement.COLOR to lightCategoryColor, StyleElement.TEXT_SIZE to 8 )
            val OD_COLOR_LONG_STYLE = mapOf(StyleElement.COLOR to lightCategoryColor, StyleElement.TEXT_SIZE to 8, StyleElement.WRAP_TEXT to true )
            val nonAdminRolesCountWithWritePermissions = od.roleInfoMap.entries
                .filter{allRoleDetails[it.key]?.internal_name != "administrators"}
                .filter{it.value.permissions.count{p -> p.access == "write"} > 0}.size
            val nonAdminRoleCount = if (nonAdminRolesCountWithWritePermissions > 0) nonAdminRolesCountWithWritePermissions else ""
            val objectCount = if (od.objectCount > 0) od.objectCount else ""
            var odName = od.name
            categoryPrefixMap[category.category_internal]?.let {odName = "[$it] $odName"}
            val objectDefRowData = mutableListOf(
                Cell(odName, OD_COLOR_STYLE),
                Cell(od.id, OD_COLOR_SMALL_STYLE),
                Cell(od.type.name_internal, OD_COLOR_SMALL_STYLE),
                Cell(od.type.template_id, OD_COLOR_SMALL_STYLE),
                Cell(if(od.is_bpmn) "x" else "", OD_COLOR_CENTER_STYLE),
                Cell(od.objectQuestionnaireNames.joinToString("\n"), OD_COLOR_LONG_STYLE),
                Cell(objectCount, OD_COLOR_STYLE),
                Cell(nonAdminRoleCount, VALUE_STYLE)
            )

            // process role related info in the order of the sorted roles
            // (same order as used to create the header row!)
            sortedRoleDetails.stream().forEach { role ->
                val roleInfo = od.roleInfoMap[role.id]!!
                val writePermissions = roleInfo.permissions.filter{ p -> p.access == "write"}.toList()
                val writePermissionsCount = if (writePermissions.isNotEmpty()) writePermissions.size else ""
                val isDefaultVisible = if (roleInfo.is_default_visible) "x" else ""
                objectDefRowData.add(Cell(isDefaultVisible, VALUE_CENTER_STYLE))
                objectDefRowData.add(Cell(writePermissionsCount, VALUE_STYLE))
                // permissions with a hexadecimal value as a key represent an object questionnaire
                // prefix these with '#'
                objectDefRowData.add(Cell(writePermissions.map { if (isHexValue(it.key)) "#${it.label}" else it.label }.toList().joinToString ("\n"), VALUE_LONG_STYLE))
            }
            data.add(objectDefRowData)
        }
    }

    // specify column widths and borders
    val NAME_WIDTH = 25
    val DETAILS_WIDTH = 8
    val PERMISSIONS_WIDTH = 13
    val VALUE_WIDTH = 7
    var colNo = 0
    val columnStyleConfig:MutableMap<Int, StyleConfig> = mutableMapOf()
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * NAME_WIDTH)
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * DETAILS_WIDTH)
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * DETAILS_WIDTH)
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * DETAILS_WIDTH)
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * DETAILS_WIDTH)
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * VALUE_WIDTH)   // is BPMN
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * VALUE_WIDTH, StyleElement.BORDER_RIGHT to true)
    columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * VALUE_WIDTH, StyleElement.BORDER_RIGHT to true)
    repeat(sortedRoleDetails.size) {
        columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * VALUE_WIDTH)
        columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * VALUE_WIDTH)
        columnStyleConfig[colNo++] = mutableMapOf(StyleElement.WIDTH to 256 * PERMISSIONS_WIDTH, StyleElement.BORDER_RIGHT to true)
    }

    println()
    println("*********************************************************************************************************")
    println("* generate Excel file")
    val formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    ExcelGenerator().generateFile(File("BlueDolphin permission matrix ${formattedDate}.xlsx"), data, columnStyleConfig )
}

val HEADER_STYLE = mapOf(StyleElement.BOLD to true)
val HEADER_ANGLE_STYLE = mapOf(StyleElement.BOLD to true, StyleElement.ANGLE to true)
val HEADER_SMALL_ANGLE_STYLE = mapOf(StyleElement.BOLD to true, StyleElement.TEXT_SIZE to 8, StyleElement.ANGLE to true)
val VALUE_STYLE:StyleConfig = mapOf()
val VALUE_LONG_STYLE = mapOf(StyleElement.TEXT_SIZE to 8, StyleElement.WRAP_TEXT to true)
val VALUE_CENTER_STYLE = mapOf(StyleElement.CENTER to true)

fun isHexValue(input: String): Boolean {
    val hexRegex = Regex("^[0-9a-fA-F]+$")
    return hexRegex.matches(input)
}

data class Category (
    val category: String,
    val category_internal: String,
    val color: String
)

data class ObjectDefinitionPermissions (
    val id: String,
    val name: String,
    val name_internal: String?,
    val is_bpmn: Boolean,
    val type: ObjectDefinitionTypeUI,
    var objectCount:Int = 0,
    var objectQuestionnaireNames: List<String> = listOf(),
    val roleInfoMap:  MutableMap<String, ObjectDefinitionRoleInfo> = mutableMapOf()
)

data class ObjectDefinitionRoleInfo (
    var is_default_visible: Boolean = false,
    val permissions: MutableList<ObjectDefinitionPermissionUI> = mutableListOf()
)


