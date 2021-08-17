package pro.devsvc.unitask.store.nitrite

import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import org.dizitart.no2.filters.Filters.eq
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
    fun testNested() {
        val db = Nitrite.builder()
            .compressed()
            .filePath("tmp/test2.db")
            .openOrCreate()

        val taskCollection = db.getCollection("tasks")
        val doc = Document()
        doc["id"] = 1
        doc["data"] = mapOf("a" to 1, "b" to 2)

        val nestedDoc = Document()
        nestedDoc["a"] = "A"
        doc["nest"] = nestedDoc
        taskCollection.insert(doc)


        val doc11 = Document()
        doc11["id"] = 1
        doc11["data"] = mapOf("a" to 1, "b" to 2)

        val nestedDoc11 = Document()
        nestedDoc11["b"] = "B"
        doc11["nest"] = nestedDoc11

        taskCollection.update(eq("id", 1), doc11)

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

    inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }
}