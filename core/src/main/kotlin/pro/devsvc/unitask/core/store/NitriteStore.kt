package pro.devsvc.unitask.core.store


import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.dizitart.no2.filters.Filters.*
import org.dizitart.no2.*
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.core.model.*
import java.text.DateFormat
import java.text.SimpleDateFormat

const val UNI_TASK = "unitask"

class NitriteStore : TaskStore {

    private val log = LoggerFactory.getLogger(javaClass)

    val mapper = jacksonObjectMapper()
    init {
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // mapper.dateFormat = SimpleDateFormat.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    val mapType = object: TypeReference<MutableMap<String, Any>>() {}

    private val db = Nitrite.builder()
        .compressed()
        .filePath("store/test.db")
        .openOrCreate()

    private val personCollection = db.getCollection("person")
    private val productCollection = db.getCollection("products")
    private val planCollection = db.getCollection("plans")
    private val projectCollection = db.getCollection("projects")
    private val bugCollection = db.getCollection("bugs")
    private val taskCollection = db.getCollection("tasks")


    init {
        if (!personCollection.hasIndex("name")) {
            personCollection.createIndex("name", IndexOptions.indexOptions(IndexType.Unique))
        }
        if (!productCollection.hasIndex("name")) {
            productCollection.createIndex("name", IndexOptions.indexOptions(IndexType.Unique))
        }
        if (!planCollection.hasIndex("title")) {
            planCollection.createIndex("title", IndexOptions.indexOptions(IndexType.Unique))
        }
        if (!projectCollection.hasIndex("title")) {
            projectCollection.createIndex("title", IndexOptions.indexOptions(IndexType.Unique))
        }
        if (!bugCollection.hasIndex("title")) {
            bugCollection.createIndex("title", IndexOptions.indexOptions(IndexType.Unique))
        }
        if (!taskCollection.hasIndex("title")) {
            taskCollection.createIndex("title", IndexOptions.indexOptions(IndexType.Unique))
        }
    }

    private fun processRef(task: Task) {
        if (task.productName != null) {
            val product = findProduct(task.productName!!)
            if (product != null) {
                task.uProductId = product.getIdInConnector(UNI_TASK)?.toLong()
            }
        }
        if (task.projectName != null) {
            val project = findProduct(task.projectName!!)
            if (project != null) {
                task.uProjectId = project.getIdInConnector(UNI_TASK)?.toLong()
            }
        }
        if (task.planName != null) {
            val plan = findProduct(task.planName!!)
            if (plan != null) {
                task.uPlanId = plan.getIdInConnector(UNI_TASK)?.toLong()
            }
        }
    }

    private fun model2Document(model: Model): Document {
        val document = Document()
        val map = mapper.convertValue(model, mapType)
        val customDoc = Document()

        if (model.customProperties != null) {
            val customPropertiesMap = mapper.convertValue(model.customProperties, mapType)
            customDoc.putAll(customPropertiesMap)
            map["customProperties"] = customDoc
        }
        document.putAll(map)
        return document
    }

    private fun model2Document(model: Model, document: Document) {
        val map = mapper.convertValue(model, mapType)
        document.putAll(map)

        val customProperties = map["customProperties"] as Map<String, Any>
        var customDoc = document["customProperties"] as MutableMap<String, Any>
        if (customDoc == null) {
            customDoc = Document()
            document["customProperties"] = customDoc
        }
        if (customProperties != null) {
            customDoc.putAll(customProperties)
        }
    }


    override fun store(tasks: List<Task>) {
    }

    override fun store(model: Model) {
        if (model is Task) {
            processRef(model)
        }

        val collection = getCollection(model)
        if (collection == null) {
            log.error("no collection for model: $model")
            return
        }
        val existing = findExistingModelDoc(model)
        if (existing != null) {
            model2Document(model, existing)
            collection.update(existing)
        } else {
            val document = model2Document(model)
            collection.insert(document)
        }
    }

    private fun getCollection(model: Model): NitriteCollection? {
        when (model) {
            is Product -> return productCollection
            is Plan -> return planCollection
            is Project -> return projectCollection
            is Task -> return taskCollection
            is Person -> return personCollection
            is Bug -> return bugCollection
        }
        return null
    }

    private fun findExistingModel(model: Model): Model? {
        when (model) {
            is Product -> return findProduct(model.name)
            is Plan -> return findPlan(model.title)
            is Project -> return findProject(model.title)
            is Task -> return findTask(model.title)
        }
        return null
    }

    override fun list() = sequence {
        for (doc in taskCollection.find()) {
            val task = docToTask(doc)
            if (task != null) {
                yield(task)
            }
        }
    }

    override fun listModels() = sequence<Model> {
        for (doc in productCollection.find()) {
            val task = docToProduct(doc)
            if (task != null) {
                yield(task)
            }
        }
        for (doc in projectCollection.find()) {
            val project = docToProject(doc)
            if (project != null) {
                yield(project)
            }
        }
    }

    private fun findExistingModelDoc(model: Model): Document? {
        val collection = getCollection(model)
        when (model) {
            is Product -> return productCollection.find(eq("name", model.name)).firstOrDefault()
            is Plan -> return planCollection.find(eq("title", model.title)).firstOrDefault()
            is Project -> return projectCollection.find(eq("title", model.title)).firstOrDefault()
            is Task -> return taskCollection.find(eq("title", model.title)).firstOrDefault()
        }
        return null
    }

    override fun findTask(title: String): Task? {
        val doc = taskCollection.find(eq("title", title)).firstOrDefault()
        return docToTask(doc)
    }

    override fun findTask(map: Map<String, Any?>): Task? {
        val filters = map.map { (k, v) ->
            eq(k, v)
        }.toTypedArray()
        val doc = taskCollection.find(and(*filters)).firstOrDefault()
        return docToTask(doc)
    }

    override fun findProduct(name: String): Product? {
        val doc = productCollection.find(eq("name", name)).firstOrDefault()
        return docToProduct(doc)
    }

    override fun findPlan(title: String): Plan? {
        val doc = planCollection.find(eq("title", title)).firstOrDefault()
        return docToPlan(doc)
    }

    override fun findProject(title: String): Project? {
        val doc = projectCollection.find(eq("title", title)).firstOrDefault()
        return docToProject(doc)
    }

    override fun findPerson(name: String): Person? {
        val doc = personCollection.find(eq("name", name)).firstOrDefault()
        return docToPerson(doc)
    }

    override fun findBug(title: String): Bug? {
        TODO("Not yet implemented")
    }

    override fun deleteTask(task: Task) {
        taskCollection.remove(eq("title", task.title))
    }

    override fun deleteProduct(name: String) {
        TODO("Not yet implemented")
    }

    override fun deletePlan(name: String) {
        TODO("Not yet implemented")
    }

    override fun deleteProject(title: String) {
        TODO("Not yet implemented")
    }

    override fun deletePerson(name: String) {
        TODO("Not yet implemented")
    }

    override fun deleteBug(title: String) {
        TODO("Not yet implemented")
    }

    fun docToProduct(doc: Document?): Product? {
        if (doc == null) {
            return null
        }
        val product = mapper.convertValue(doc, Product::class.java)
        product.setIdInConnector(UNI_TASK, doc.id.idValue.toString())
        return product
    }

    fun docToPlan(doc: Document?): Plan? {
        if (doc == null) {
            return null
        }
        val plan = mapper.convertValue(doc, Plan::class.java)
        plan.setIdInConnector(UNI_TASK, doc.id.idValue.toString())
        return plan
    }

    fun docToProject(doc: Document?): Project? {
        if (doc == null) {
            return null
        }
        val project = mapper.convertValue(doc, Project::class.java)
        project.setIdInConnector(UNI_TASK, doc.id.idValue.toString())
        return project
    }

    fun docToPerson(doc: Document?): Person? {
        if (doc == null) {
            return null
        }
        val person = mapper.convertValue(doc, Person::class.java)
        person.setIdInConnector(UNI_TASK, doc.id.idValue.toString())
        return person
    }

    fun docToTask(doc: Document?): Task? {
        if (doc == null) {
            return null
        }
        val task = mapper.convertValue(doc, Task::class.java)
        task.setIdInConnector(UNI_TASK, doc.id.idValue.toString())
        return task
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
