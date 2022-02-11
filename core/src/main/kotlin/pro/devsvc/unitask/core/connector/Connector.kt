package pro.devsvc.unitask.core.connector

import pro.devsvc.unitask.core.model.Task

interface Connector {
    val id: String
    fun start()
    fun listTasks(): List<Task>
    fun update(task: Task)
}