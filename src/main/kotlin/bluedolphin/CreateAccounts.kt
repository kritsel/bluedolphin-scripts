package bluedolphin

import bluedolphin.api.UserAPI
import util.readFileFromResources
import util.trustStoreMagic

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

    val emailAddresses = collectAllUserEmailAddresses()
    if (emailAddresses.isNotEmpty()) {
        createAccounts("All Tech Employees Jan25.csv", "tech", emailAddresses)
        createAccounts("Subset of Floris minions Jan25.csv", "commerce", emailAddresses)
    }
}

fun createAccounts(filePath:String, description: String, emailAddresses: Set<String>) {
    println()
    println("create accounts for $description")
    val reader = readFileFromResources(filePath)

    var createCount = 0
    reader?.forEachLine { line ->
        // ensure we skip any header lines
        if (line.contains("@")) {
            val tokens = line.split(";")
            if (tokens.size > 3) {
                val firstname = tokens[1]
                val lastname = tokens[2]
                val email = tokens[3].lowercase()

                if (!emailAddresses.contains(email)  ) {
                    println("create account for $firstname $lastname $email")
                    UserAPI().createUser(firstname, lastname, email)
                    createCount++
                }
            }
        }
    }
    println("created $createCount accounts for $description")
}

fun collectAllUserEmailAddresses() : Set<String>{
    return UserAPI().getAllUserDetails().values.map{it.get("email") as String}.toSet()
}


