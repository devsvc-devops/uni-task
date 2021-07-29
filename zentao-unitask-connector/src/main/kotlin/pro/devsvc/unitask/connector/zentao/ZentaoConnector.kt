package pro.devsvc.unitask.connector.zentao

import pro.devsvc.unitask.connector.Connector
import pro.devsvc.unitask.core.model.TaskType
import pro.devsvc.unitask.store.nitrite.TaskStore
import pro.devsvc.zentao.sdk.ZentaoSDK
import pro.devsvc.zentao.sdk.usermodel.Project
import pro.devsvc.zentao.sdk.usermodel.Task
import java.time.LocalDateTime
import java.time.ZoneId
import pro.devsvc.unitask.core.model.Task as UniTask

class ZentaoConnector(baseUrl: String,
                      username: String,
                      password: String) : Connector {

    val sdk: ZentaoSDK = ZentaoSDK(baseUrl)

    init {
        sdk.login(username, password)
    }

    override fun start(store: TaskStore) {
        syncToStore(store)
        syncFromStore(store)
    }

    private fun syncToStore(store: TaskStore) {
        val products = sdk.getAllProducts()
        for (product in products) {
            sdk.getProjectsOfProduct(product.id)?.forEach {
                if (it.begin!!.isBefore(LocalDateTime.of(2020, 12, 20, 0, 0, 0))
                    || it.status == "closed"
                    || it.status == "canceled") {
                    return@forEach
                }

                val uTask = zProjectToUniTask(it)
                store.store(uTask)
                store.store(zProjectTeamToPerson(it))

                val zTasks = sdk.getProjectTasks(it.id)
                zTasks.forEach { (k, v) ->
                    store.store(zTaskToUniTask(v))
                }
            }
        }
    }

    private fun syncFromStore(store: TaskStore) {

    }

    private fun zProjectToUniTask(project: Project): UniTask {
        val uTask = UniTask("zProject-" + project.id.toString(), project.name, TaskType.PROJECT)
        uTask.estStarted = project.begin?.atZone(ZoneId.systemDefault())
        uTask.deadline = project.end?.atZone(ZoneId.systemDefault())
        uTask.planId = project.plans.joinToString()
        uTask.projectId = project.id.toString()
        uTask.productId = project.products.joinToString()
        return uTask
    }

    private fun zProjectTeamToPerson(zTask: Project): MutableList<UniTask> {
        val persons = mutableListOf<UniTask>()
        zTask.teamMembers.map {
            val uTask = UniTask("zPerson-" + it.id, it.realName, TaskType.PERSON)
            persons.add(uTask)
        }
        return persons
    }

    private fun zTaskToUniTask(zTask: Task): UniTask {
        val uTask = UniTask("zTask-" + zTask.id.toString(), zTask.name, TaskType.TASK)
        uTask.estStarted = zTask.estStarted?.atZone(ZoneId.systemDefault())
        uTask.deadline = zTask.deadline?.atZone(ZoneId.systemDefault())
        uTask.planId = sdk.getProject(zTask.project)?.plans?.joinToString()
        uTask.projectId = zTask.project?.toString()
        uTask.productId = zTask.product?.toString()
        uTask.assignedUserId = zTask.assignedTo
        uTask.status = zTask.status
        uTask.lastEditTime = zTask.lastEditedDate?.atZone(ZoneId.systemDefault())
        return uTask
    }

}