package pro.devsvc.unitask.store.nitrite

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import pro.devsvc.unitask.core.model.Task
import org.dizitart.no2.Document.createDocument
import org.dizitart.no2.IndexOptions
import org.dizitart.no2.IndexType
import org.dizitart.no2.filters.Filters.*
import kotlinx.serialization.properties.*
import pro.devsvc.unitask.core.model.Project

class NitriteStore : TaskStore {

    private val db = Nitrite.builder()
        .compressed()
        .filePath("tmp/test.db")
        .openOrCreate()

    private val taskCollection = db.getCollection("tasks")

    init {
        if (!taskCollection.hasIndex("id")) {
            taskCollection.createIndex("id", IndexOptions.indexOptions(IndexType.Unique))
        }
    }

    override fun store(task: Task) {
        val document = createDocument("id", task.id)
        val map = Properties.encodeToMap(KSerializer)
        document.putAll(map)
        val existing = load(task.id)
        if (existing != null) {
            taskCollection.update(eq("id", task.id), document)
        } else {
            taskCollection.insert(document)
        }
    }

    override fun store(tasks: List<Task>) {
    }

    override fun load() = sequence {
        for (doc in taskCollection.find()) {
            val task = docToTask(doc)
            if (task != null) {
                yield(task)
            }
        }
    }

    override fun load(id: String): Task? {
        val doc = taskCollection.find(eq("id", id)).firstOrDefault()
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