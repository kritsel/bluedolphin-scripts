package bluedolphin

import bluedolphin.api.UserAPI
import org.json.JSONArray
import org.json.JSONObject
import util.trustStoreMagic
import java.io.File

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

    val allUserDetails = UserAPI().getAllUserDetails(test=true)

    val allRoleNames = allUserDetails.values.flatMap{ collectRollNames(it) }.toSet()
    val csvUsers = allUserDetails.values.map{ createCsvUser(it, allRoleNames) }.toList()

    // print the results
    val csvFile = File("bluedolphin-users.csv")
    csvFile.printWriter().use { out ->
        out.println("$FIRST_NAME;$LAST_NAME;$EMAIL;${allRoleNames.joinToString(";")}")
        csvUsers.forEach { csvUser ->
            out.print("${csvUser[FIRST_NAME]};${csvUser[LAST_NAME]};${csvUser[EMAIL]};")
            allRoleNames.forEach { out.print("${csvUser[it]};")}
            out.println()
        }
    }
    println("CSV file created")
}

fun collectRollNames(user:JSONObject):Set<String>{
    return (user.get("roles") as JSONArray).map{(it as JSONObject).get("name") as String}.toSet()
}

fun createCsvUser (blueDolphinUser:JSONObject, allRoleNames:Set<String>) : Map<String, String>{
    val csvUser:MutableMap<String, String> = mutableMapOf()
    csvUser[FIRST_NAME] = blueDolphinUser["first_name"] as String
    csvUser[LAST_NAME] = blueDolphinUser["last_name"] as String
    csvUser[EMAIL] = blueDolphinUser["email"] as String
    allRoleNames.forEach { roleName ->
        if (collectRollNames(blueDolphinUser).contains(roleName)) {
            csvUser[roleName] = "x"
        } else {
            csvUser[roleName] = ""
        }
    }
    return csvUser
}




