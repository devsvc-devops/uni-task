package pro.devsvc.unitask.store.nitrite

import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import pro.devsvc.unitask.core.model.Task
import org.dizitart.no2.Document.createDocument
import org.dizitart.no2.IndexOptions
import org.dizitart.no2.IndexType
import org.dizitart.no2.filters.Filters.*
import kotlinx.serialization.properties.*

class NitriteStore : TaskStore {

    val db = Nitrite.builder()
        .compressed()
        .filePath("tmp/test.db")
        .openOrCreate()

    val collection = db.getCollection("tasks")

    init {
        if (!collection.hasIndex("id")) {
            collection.createIndex("id", IndexOptions.indexOptions(IndexType.Unique))
        }
        if (collection.hasIndex("title")) {
            collection.createIndex("title", IndexOptions.indexOptions(IndexType.NonUnique))
        }
    }

    override fun store(task: Task) {
        val document = createDocument("id", task.id)
        val existing = load(task.id)
        if (existing != null) {
            val map = Properties.encodeToMap(task)
            document.putAll(map)
        }
        collection.update(eq("id", task.id), document)
    }

    override fun store(tasks: List<Task>) {
    }

    override fun load(): List<Task> {
        return listOf()
    }

    override fun load(id: String): Task? {
        val doc = collection.find(eq("id", id)).firstOrDefault()
        return docToTask(doc)
    }

    fun docToTask(doc: Document?): Task? {
        if (doc == null) {
            return null
        }
        val task = Properties.decodeFromMap<Task>(doc)
        return task
    }
}