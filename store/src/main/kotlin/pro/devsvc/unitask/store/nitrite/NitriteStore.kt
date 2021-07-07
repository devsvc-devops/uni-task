package pro.devsvc.unitask.store.nitrite

import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import pro.devsvc.unitask.core.model.Task
import org.dizitart.no2.Document.createDocument
import org.dizitart.no2.IndexOptions
import org.dizitart.no2.IndexType
import org.dizitart.no2.filters.Filters


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
            document["title"] = task.title
        }
        collection.update(document, true)
    }

    override fun store(tasks: List<Task>) {

    }

    override fun load(): List<Task> {
        return listOf()
    }

    override fun load(id: String): Task? {
        val doc = collection.find(Filters.eq("id", id)).firstOrDefault()
        return docToTask(doc)
    }

    fun docToTask(doc: Document?): Task? {
        if (doc == null) {
            return null
        }
        val task = Task(doc["id"] as String, doc["title"] as String)
        return task
    }
}