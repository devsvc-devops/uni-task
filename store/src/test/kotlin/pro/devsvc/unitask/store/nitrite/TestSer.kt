package pro.devsvc.unitask.store.nitrite

import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
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
    }

}