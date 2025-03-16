package bluedolphin.ui_api

import bluedolphin.api.B2CAUTHORIZATION
import bluedolphin.api.SLEEP
import bluedolphin.api.TENANT
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

const val OBJECT_DEFINITION_UI_URL = "https://bd-presentation-api.eu.bd-cloud.app/api/admin/objects"

class ObjectDefinition_UI_API {

    val accessToken: String = System.getenv("BD_UI_ACCESS_TOKEN")

    fun getAllObjectDefinitionsUI(test: Boolean = false): Map<String, ObjectDefinitionUI> {
        if (test) {
            println("get all object definitions (test)")
        } else {
            println("get all object definitions")
        }
        val allObjectDefinitions: MutableMap<String, ObjectDefinitionUI> = mutableMapOf()
        var startAt: Int = 0
        // retrieve object defs in batches
        val batchSize = if (test) 10 else 50
        while (true) {
            // get the next batch of object defs
            println("  retrieve next batch of $batchSize object definitions (start with $startAt)")
            val objectDefsBatch = getObjectDefinitionsUI(batchSize, startAt)
            objectDefsBatch.forEach { objectDef ->
                allObjectDefinitions[objectDef.id] = objectDef
            }

            if (test) {
                break
            } else {
                // update 'startWith' for the next iteration
                startAt = if (objectDefsBatch.size == batchSize) {
                    startAt + batchSize
                } else {
                    // when a smaller list then 'batchSize' was returned, it means we've come to the end of the list;
                    break;
                }
            }
        }
        println("retrieved ${allObjectDefinitions.size} object definitions")
        return allObjectDefinitions
    }


    fun getObjectDefinitionsUI(batchSize: Int? = null, startAt: Int? = null): List<ObjectDefinitionUI> {
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
            url = OBJECT_DEFINITION_UI_URL,
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
                "b2cauthorization" to B2CAUTHORIZATION,
                "tenant" to TENANT
            ),
            params = params
        )

        if (response.statusCode == 200) {
            val deserializedResponse = Json.decodeFromString<ObjectDefinitionsUIResponse>(response.text)
            return deserializedResponse.data.objects
        } else {
            throw Exception("error ${response.statusCode} when fetching object definitions: ${response.text}")
        }
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionsUIResponse(
    val error_code: Int,
    val error_message: String?,
    val error_message_explain: String?,
    val data: ObjectDefinitionsUIData
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionsUIData(
    val objects: List<ObjectDefinitionUI>
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionUI(
    val id: String,
    val name: String,
    val name_internal: String?,
    val color: String?,
    val object_type: ObjectDefinitionTypeUI
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionTypeUI(
    val template_id: String,
    val name: String,
    val name_internal: String,
    val category: String,
    val category_internal: String,
    val color: String?
)


