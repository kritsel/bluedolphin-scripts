package bluedolphin.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

const val OBJECT_DEFINITION_URL = "https://public-api.eu.bluedolphin.app/v1/object-definitions"

class ObjectDefinitionAPI {

    val apiKey: String = System.getenv("BD_API_KEY")

    fun getObjectDefinition(objectDefId: String): ObjectDefinition {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)
        val response = khttp.get(
            url = "$OBJECT_DEFINITION_URL/$objectDefId",
            headers = mapOf(
                "x-api-key" to apiKey,
                "tenant" to TENANT
            )
        )
        if (response.statusCode == 200) {
            return Json.decodeFromString<ObjectDefinition>(response.text)
        } else if (response.statusCode == 404) {
            throw Exception("no object definition with id $objectDefId")
        } else {
            throw Exception("error ${response.statusCode} when fetching object definition $objectDefId")
        }
    }

    fun getAllObjectDefinitions(test: Boolean = false): Map<String, ObjectDefinition> {
        if (test) {
            println("get all object definitions (test)")
        } else {
            println("get all object definitions")
        }
        val allObjectDefinitions: MutableMap<String, ObjectDefinition> = mutableMapOf()
        var startAt: Int = 0
        // retrieve object defs in batches
        val batchSize = if (test) 10 else 50
        while (true) {
            // get the next batch of object defs
            println("  retrieve next batch of $batchSize object definitions (start with $startAt)")
            val objectDefsBatch = getObjectDefinitions(batchSize, startAt)
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


    fun getObjectDefinitions(batchSize: Int? = null, startAt: Int? = null): List<ObjectDefinition> {
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
            url = OBJECT_DEFINITION_URL,
            headers = mapOf(
                "x-api-key" to apiKey,
                "tenant" to TENANT
            ),
            params = params
        )

        if (response.statusCode == 200) {
            val deserializedResponse = Json.decodeFromString<ObjectDefinitionsResponse>(response.text)
            return deserializedResponse.items
        } else {
            throw Exception("error ${response.statusCode} when fetching object definitions: ${response.text}")
        }
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionsResponse(
    val total_items: Int,
    val items: List<ObjectDefinition>
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinition(
    val id: String,
    val name: String,
    val type: ObjectDefinitionType
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectDefinitionType(
    val id: String,
    val name: String,
    val name_internal: String,
    val category: String,
    val category_internal: String,
    val color: String
)


