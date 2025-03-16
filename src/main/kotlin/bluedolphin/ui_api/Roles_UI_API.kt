package bluedolphin.ui_api

import bluedolphin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

const val ROLES_UI_URL = "https://bd-presentation-api.eu.bd-cloud.app/api/admin/roles"

class Roles_UI_API {

    val accessToken: String = System.getenv("BD_UI_ACCESS_TOKEN")

    fun getRoleDetailsUI(roleId: String): RoleDetailsUI {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)
        val response = khttp.get(
            url = "$ROLES_UI_URL/$roleId",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
                "b2cauthorization" to B2CAUTHORIZATION,
                "tenant" to TENANT
            )
        )
        if (response.statusCode == 200) {
            val deserializedResponse = Json.decodeFromString<RoleDetailsUIResponse>(response.text)
            return deserializedResponse.data
        } else if (response.statusCode == 404) {
            throw Exception("no role with id $roleId")
        } else {
            throw Exception("error ${response.statusCode} when fetching role $roleId")
        }
    }

    fun getAllRoleDetailsUI(test: Boolean = false): Map<String, RoleDetailsUI> {
        if (test) {
            println("get all role details (test)")
        } else {
            println("get all role details")
        }
        // first get all roles, the result is a map with role objects with limited information per role
        val allRoles = getAllRolesUI(test)

        // iterate over all roles to retrieve a role object with full details for every role
        println("get role details for each retrieved role (0.5 sec sleep between calls to prevent hitting the rate limit)")
        return allRoles.keys.associateWith { getRoleDetailsUI(it) }
    }

    fun getAllRolesUI(test: Boolean = false): Map<String, RoleUI> {
        if (test) {
            println("get all roles (test)")
        } else {
            println("get all roles")
        }
        val allRoles: MutableMap<String, RoleUI> = mutableMapOf()
        var startAt: Int = 0
        // retrieve object defs in batches
        val batchSize = if (test) 3 else 50
        while (true) {
            // get the next batch of items
            println("  retrieve next batch of $batchSize roles (start with $startAt)")
            val rolesBatch = getRolesUI(batchSize, startAt)
            rolesBatch.forEach { role ->
                allRoles[role.id] = role
            }

            if (test) {
                break
            } else {
                // update 'startWith' for the next iteration
                startAt = if (rolesBatch.size == batchSize) {
                    startAt + batchSize
                } else {
                    // when a smaller list then 'batchSize' was returned, it means we've come to the end of the list;
                    break;
                }
            }
        }
        println("retrieved ${allRoles.size} roles")
        return allRoles
    }


    fun getRolesUI(batchSize: Int? = null, startAt: Int? = null): List<RoleUI> {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)

        val params: MutableMap<String, String> = mutableMapOf()

        // 'take' defaults to 100 when not provided
        if (batchSize != null) {
            params["take"] = batchSize.toString()
        }

        // 'start' defaults to 0 when not provided
        if (startAt != null) {
            params["start"] = startAt.toString()
        }

        val response = khttp.get(
            url = ROLES_UI_URL,
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
                "b2cauthorization" to B2CAUTHORIZATION,
                "tenant" to TENANT
            ),
            params = params
        )

        if (response.statusCode == 200) {
            val deserializedResponse = Json.decodeFromString<RolesUIResponse>(response.text)
            return deserializedResponse.data.roles
        } else {
            throw Exception("error ${response.statusCode} when fetching roles: ${response.text}")
        }
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class RolesUIResponse(
    val error_code: Int,
    val error_message: String?,
    val error_message_explain: String?,
    val data: RolesUIData
)

@Serializable
@JsonIgnoreUnknownKeys
data class RolesUIData(
    val roles: List<RoleUI>
)

@Serializable
@JsonIgnoreUnknownKeys
data class RoleUI(
    val id: String,
    val name: String
)

@Serializable
@JsonIgnoreUnknownKeys
data class RoleDetailsUIResponse(
    val error_code: Int,
    val error_message: String?,
    val error_message_explain: String?,
    val data: RoleDetailsUI
)

@Serializable
@JsonIgnoreUnknownKeys
data class RoleDetailsUI(
    val id: String,
    val name: String,
    val internal_name: String,
    val description: String,
    val default_landing_page: String,
    val is_buildin: Boolean,
    val members: List<RoleMemberUI>,
    val object_definition_permissions: List<ObjectDefinitionPermissionsUI>

)

@Serializable
@JsonIgnoreUnknownKeys
data class RoleMemberUI(
    val id: String,
    val first_name: String,
    val last_name: String,
    val email: String,
    val is_guest_user: Boolean?,
    val default_landing_page: String?,
//    val is_buildin: Boolean?,
    val last_log_in: String,
    val user_name: String,
    val external_id: String?,
    val is_scim_provisioned: Boolean
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionPermissionsUI(
    val id: String,
    val name: String,
    val name_internal: String,
    val permissions: List<ObjectDefinitionPermissionUI>,
    val is_default_visible: Boolean,
    val is_bpmn: Boolean
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionPermissionUI(
    val key: String,
    val label: String,
    val access: String,
    val position: Int
)
