package pro.devsvc.unitask.store.nitrite

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*

import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import kotlinx.serialization.properties.decodeFromMap

import org.junit.Test

class TestSer {

    @Serializable
    data class Data(
        val m: Map<String, String?> = mapOf()
    )

    @Test
    fun test() {
        val data = Data(mapOf(
            "a" to "1",
            "b" to null
        ))

        val map = Properties.encodeToMap(data)
        println(map)
        val data1: Data = Properties.decodeFromMap(map)
        println(data1)
    }


    @Test
    fun test2() {
        val data = Data(mapOf(
            "a" to "1",
            "b" to null
        ))

        val map = Json.encodeToJsonElement(data) as JsonObject
        println(map)
        val data1: Data = Json.decodeFromJsonElement(map)
        println(data1)
    }
}