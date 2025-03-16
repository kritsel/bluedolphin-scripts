package bluedolphin.api

import org.json.JSONArray
import org.json.JSONObject

val USER_URL = "https://public-api.eu.bluedolphin.app/v1/users"

class UserAPI {
    val apiKey: String = System.getenv("BD_API_KEY")

    fun getUserDetails(userId: String): JSONObject {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)
        val response = khttp.get(
            url = "$USER_URL/$userId",
            headers = mapOf(
                "x-api-key" to apiKey,
                "tenant" to TENANT
            )
        )
        if (response.statusCode == 200) {
            return response.jsonObject
        } else if (response.statusCode == 404) {
            throw Exception("no user with userId $userId")
        } else {
            throw Exception("error ${response.statusCode} when fetching user $userId")
        }
    }

    fun getAllUserDetails(test: Boolean = false): Map<String, JSONObject> {
        if (test) {
            println("get all user details (test)")
        } else {
            println("get all user details")
        }
        // first get all users, the result is a map with user objects with limited information per user
        val allUsers = getAllUsers(test)

        // iterate over all users to retrieve a user object with full details for every user
        println("get user details for each retrieved user (0.5 sec sleep between calls to prevent hitting the rate limit)")
        return allUsers.keys.associateWith { getUserDetails(it) }
    }

    fun getAllUsers(test: Boolean = false): Map<String, JSONObject> {
        if (test) {
            println("get all users (test)")
        } else {
            println("get all users")
        }
        val allUsers: MutableMap<String, JSONObject> = mutableMapOf()
        var startWith: String? = null
        // retrieve users in batches
        val batchSize = if (test) 10 else 50
        while (true) {
            // get the next batch of users
            println("  retrieve next batch of $batchSize users")
            val usersBatch = getUsers(batchSize, startWith)
            usersBatch.forEach { user ->
                val userId = (user as JSONObject).get("id") as String
                allUsers[userId] = user
            }

            if (test) {
                break
            } else {
                // update 'startWith' for the next iteration
                startWith = if (usersBatch.length() == batchSize) {
                    // get the id of the last user returned
                    usersBatch.getJSONObject(usersBatch.length() - 1)
                        .get("id") as String
                } else {
                    // when a smaller list then 'batchSize' was returned, it means we've come to the end of the list;
                    break;
                }
            }
        }
        println("retrieved ${allUsers.size} users")
        return allUsers
    }


    fun getUsers(batchSize: Int? = null, startWithUserId: String? = null): JSONArray {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)

        val params: MutableMap<String, String> = mutableMapOf()

        // 'take' defaults to 100 when not provided
        if (batchSize != null) {
            params["take"] = batchSize.toString()
        }

        // 'starts_with' defaults to the first user when not provided
        if (startWithUserId != null) {
            params["start_with"] = startWithUserId
        }
        val response = khttp.get(
            url = USER_URL,
            headers = mapOf(
                "x-api-key" to apiKey,
                "tenant" to TENANT
            ),
            params = params
        )

        if (response.statusCode == 200) {
            return response.jsonObject.get("users") as JSONArray
        } else {
            throw Exception("error ${response.statusCode} when fetching users")
        }
    }

    fun createUser(firstName: String, lastName: String, email: String) {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)
        val response = khttp.post(
            url = USER_URL,
            headers = mapOf(
                "x-api-key" to apiKey,
                "tenant" to TENANT
            ),
            json = mapOf(
                "email" to email,
                "first_name" to firstName,
                "last_name" to lastName,
                "create_bluedolphin_user" to false,
                "send_email_when_created" to false
            )
        )
        if (!(response.statusCode in 200..299)) {
            throw Exception("error ${response.statusCode} when creating account for $email")
        }
    }
}
