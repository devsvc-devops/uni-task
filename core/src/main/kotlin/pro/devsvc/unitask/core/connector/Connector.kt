package pro.devsvc.unitask.core.connector

import pro.devsvc.unitask.core.model.*

interface Connector {
    val id: String
    fun start()
    fun listProducts(): List<Product>
    fun listPlans(): List<Plan>
    fun listProjects(): List<Project>
    fun listPersons(): List<Person>
    fun listTasks(): List<Task>

    fun update(model: Model)
}