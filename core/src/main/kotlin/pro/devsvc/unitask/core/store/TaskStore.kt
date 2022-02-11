package pro.devsvc.unitask.core.store

import pro.devsvc.unitask.core.model.Task

interface TaskStore {

    /**
     * save (insert or update) the task in local database.
     * NOTE: task.lastEditTime may be changed by this method
     *       if store find task's last-edit-time is before existing one.
     */
    fun store(task: Task, oldTask: Task? = null)
    fun store(tasks: List<Task>)
    fun list(): Sequence<Task>
    fun find(title: String): Task?
    fun find(map: Map<String, Any?>): Task?

    fun delete(task: Task)
}

object TaskStoreManager {

    val store = NitriteStore()

}