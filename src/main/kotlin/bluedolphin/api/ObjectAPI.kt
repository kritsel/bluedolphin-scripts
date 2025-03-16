package bluedolphin.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

const val OBJECT_URL = "https://public-api.eu.bluedolphin.app/v1/objects"

// https://support.valueblue.nl/hc/en-us/sections/13287820037276-Objects
class ObjectAPI {

    val apiKey: String = System.getenv("BD_API_KEY")

    fun getObjects(batchSize: Int? = null, startAt: Int? = null, filter: String? = null): ObjectsResponse {
        // sleep to prevent rate limiting (http 429)
        Thread.sleep(SLEEP)

        val params: MutableMap<String, String> = mutableMapOf()
        params["workspace_id"] = KRAMP_WORKSPACE_ID

        // 'take' defaults to 100 when not provided
        if (batchSize != null) {
            params["take"] = batchSize.toString()
        }

        // 'start' defaults to 0 when not provided
        if (startAt != null) {
            params["start"] = startAt.toString()
        }

        if (filter != null) {
            params["filter"] = filter
        }

        val response = khttp.get(
            url = OBJECT_URL,
            headers = mapOf(
                "x-api-key" to apiKey,
                "tenant" to TENANT
            ),
            params = params
        )

        if (response.statusCode == 200) {
            return Json.decodeFromString<ObjectsResponse>(response.text)
        } else {
            throw Exception("error ${response.statusCode} when fetching objects: ${response.text}")
        }
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectsResponse(
    val total_items: Int,
    val items: List<Object>
)

@Serializable
@JsonIgnoreUnknownKeys
data class Object(
    val id: String,
    val title: String,
    val type: ObjectType
)

@Serializable
@JsonIgnoreUnknownKeys
data class ObjectType(
    val id: String,
    val name: String,
    val name_internal: String?
)


