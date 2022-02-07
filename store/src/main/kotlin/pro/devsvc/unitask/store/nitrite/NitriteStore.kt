package pro.devsvc.unitask.store.nitrite

import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import pro.devsvc.unitask.core.model.Task
import org.dizitart.no2.IndexOptions
import org.dizitart.no2.IndexType
import org.dizitart.no2.filters.Filters.*
import kotlinx.serialization.json.*

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

    override fun store(task: Task, oldTask: Task?) {
        val document = Document()
        //
        // TODO a more elegantly way to convert task to document
        val jo = Json.encodeToJsonElement(task) as JsonObject
        val customProperties = jo["customProperties"]
        val customPropertiesMap = jsonObjectToMap(customProperties as JsonObject)
        val map = jsonObjectToMap(jo)
        val customDoc = Document()

        if (customProperties != null) {
            customDoc.putAll(customPropertiesMap)
            map["customProperties"] = customDoc
        }
        document.putAll(map)

        val existing = find(task.title)
        if (existing != null) {
            val existingLastEditTime = existing.lastEditTime
            // when updating, need to merge custom properties manually, or else the old custom properties will disappear
            val existingCustomProperties = existing.customProperties
            customDoc.putAll(existingCustomProperties)
            // task.from is set on create, never change again.
            document.remove("from")
            taskCollection.update(eq("title", task.title), document)
        } else {
            taskCollection.insert(document)
        }
    }

    override fun store(tasks: List<Task>) {
    }

    override fun list() = sequence {
        for (doc in taskCollection.find()) {
            val task = docToTask(doc)
            if (task != null) {
                yield(task)
            }
        }
    }

    override fun find(title: String): Task? {
        val doc = taskCollection.find(eq("title", title)).firstOrDefault()
        return docToTask(doc)
    }

    override fun find(map: Map<String, Any?>): Task? {
        val filters = map.map { (k, v) ->
            eq(k, v)
        }.toTypedArray()
        val doc = taskCollection.find(and(*filters)).firstOrDefault()
        return docToTask(doc)
    }

    override fun delete(task: Task) {
        taskCollection.remove(eq("title", task.title))
    }

    fun docToTask(doc: Document?): Task? {
        if (doc == null) {
            return null
        }
        val jsonObject = docToJsonObject(doc)
        val task: Task = Json { ignoreUnknownKeys = true }.decodeFromJsonElement(jsonObject)
        return task
    }

    private fun docToJsonObject(doc: Map<*, *>): JsonElement {
        val m = mutableMapOf<String, JsonElement>()
        doc.forEach { (k, v) ->
            when (v) {
                is Map<*, *> -> m[k.toString()] = docToJsonObject(v)
                is Array<*> -> m[k.toString()] = arrayToJsonElement(v)
                is String -> m[k.toString()] = JsonPrimitive(v)
                is Number -> m[k.toString()] = JsonPrimitive(v)
                is Boolean -> m[k.toString()] = JsonPrimitive(v)
            }
        }
        return JsonObject(m)
    }

    private fun arrayToJsonElement(array: Array<*>): JsonElement {
        val a = mutableListOf<JsonElement>()
        array.forEach {
            if (it == null) {
                a.add(JsonNull)
            } else {
                when (it) {
                    is Map<*, *> -> docToJsonObject(it)
                    is Array<*> -> arrayToJsonElement(it)
                    is String -> JsonPrimitive(it)
                    is Number -> JsonPrimitive(it)
                    is Boolean -> JsonPrimitive(it)
                }
            }
        }
        return JsonArray(a)
    }

    // json object to map<String, String?>
    private fun jsonObjectToMap(jo: JsonObject): MutableMap<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        jo.forEach { k, v ->
            if (v == null) {
                map[k] = null
            } else if (v is JsonPrimitive) {
                map[k] = v.content
            }
        }
        return map
    }

    private fun taskToMap(task: Task) {
        val map = mutableMapOf<String, Any>()
        map["title"] = task.title
        map["type"] = task.type.name
        map["desc"] = task.desc
        map["assignedUserId"] = task.assignedUserId
        if (task.assignedUserName != null) {
            map["assignedUserName"] = task.assignedUserName!!
        }
        if (task.estStarted != null) {
            map["estStarted"] = task.estStarted!!.toString()
        }
        if (task.deadline != null) {
            map["deadline"] = task.deadline!!
        }
        // if (task.esti)
    }
}
