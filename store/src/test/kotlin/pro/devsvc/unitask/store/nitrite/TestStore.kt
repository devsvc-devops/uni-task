package pro.devsvc.unitask.store.nitrite

import com.google.gson.Gson
import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import org.junit.Test
import kotlin.reflect.full.memberProperties

class TestStore {

    @Test
    fun test() {
        val db = Nitrite.builder()
            .compressed()
            .filePath("tmp/test2.db")
            .openOrCreate()

        val taskCollection = db.getCollection("tasks")
        val doc = Document()
        doc["id"] = 1
        doc["data"] = mapOf("a" to 1, "b" to 2)
        taskCollection.insert(doc)

        val doc2 = taskCollection.find()
        for (d in doc2) {
            println(d)
        }
    }

    @Test
    fun test2() {
        class User(val map: Map<String, Any?>) {
            val name: String by map
            val cp: MutableMap<String, Any?> by map
        }

        class Data(
            val data: Map<String, Any?>
        )

        val d1 = Data(mapOf("a" to 1, "b" to null))
        val map = d1.asMap()

        // val d2 =
    }

    inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }
}