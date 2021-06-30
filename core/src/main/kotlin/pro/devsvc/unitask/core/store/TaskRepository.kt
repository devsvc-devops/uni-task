package pro.devsvc.unitask.core.store

import pro.devsvc.unitask.core.model.Plan
import pro.devsvc.unitask.core.model.Product
import pro.devsvc.unitask.core.model.Project
import pro.devsvc.unitask.core.model.Task

interface TaskRepository {

    fun listProducts(): List<Product>
    fun createProduct(product: Product)

    fun listPlans(): List<Plan>
    fun createPlan(plan: Plan)

    fun listProjects(): List<Plan>
    fun createProject(): Project
    fun getTasksOfProject(projectId: Long): List<Task>

    fun listTasks(): List<Task>
    fun createTask(task: Task)
}