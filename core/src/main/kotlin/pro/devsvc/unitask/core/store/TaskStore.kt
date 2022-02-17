package pro.devsvc.unitask.core.store

import pro.devsvc.unitask.core.model.*

interface TaskStore {

    fun store(tasks: List<Task>)
    fun list(): Sequence<Task>
    fun listModels(): Sequence<Model>

    fun store(model: Model)

    fun findTask(title: String): Task?
    fun findTask(map: Map<String, Any?>): Task?

    fun findProduct(name: String): Product?
    fun findPlan(title: String): Plan?
    fun findProject(title: String): Project?
    fun findPerson(name: String): Person?
    fun findBug(title: String): Bug?

    fun deleteTask(task: Task)
    fun deleteProduct(name: String)
    fun deletePlan(name: String)
    fun deleteProject(title: String)
    fun deletePerson(name: String)
    fun deleteBug(title: String)
}

object TaskStoreManager {

    val store = NitriteStore()

}