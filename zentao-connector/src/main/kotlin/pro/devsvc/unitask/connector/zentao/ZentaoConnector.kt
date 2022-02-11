package pro.devsvc.unitask.connector.zentao

import cn.hutool.core.map.BiMap
import kotlinx.datetime.toLocalDateTime
import org.apache.commons.collections.BidiMap
import pro.devsvc.unitask.core.connector.Connector
import pro.devsvc.unitask.core.model.TaskPriority
import pro.devsvc.unitask.core.model.TaskStatus
import pro.devsvc.unitask.core.model.TaskType
import pro.devsvc.unitask.core.store.TaskStore
import pro.devsvc.unitask.core.store.TaskStoreManager
import pro.devsvc.zentao.sdk.ZTask
import pro.devsvc.zentao.sdk.ZentaoSDK
import pro.devsvc.zentao.sdk.quickCreateStoryAndTask
import pro.devsvc.zentao.sdk.saveTask
import pro.devsvc.zentao.sdk.usermodel.Project
import pro.devsvc.zentao.sdk.usermodel.Task
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.SimpleFormatter
import pro.devsvc.unitask.core.model.Task as UniTask

const val ZENTAO_CONNECTOR_ID = "zentao"

class ZentaoConnector(
    baseUrl: String,
    username: String,
    password: String
) : Connector {

    val sdk: ZentaoSDK = ZentaoSDK(baseUrl)
    val defaultProductId = 18
    val productMap = BiMap(mutableMapOf<Int, String>())
    val projectMap = BiMap(mutableMapOf<Int, String>())
    val store = TaskStoreManager.store

    init {
        sdk.login(username, password)
    }

    override val id: String
        get() = ZENTAO_CONNECTOR_ID

    override fun start() {
        //syncToStore(store)
        //syncFromStore(store)
    }

    override fun listTasks(): List<UniTask> {
        val result = mutableListOf<UniTask>()
        val products = sdk.getAllProducts()
        for (product in products) {
            productMap[product.id] = product.name
            sdk.getProjectsOfProduct(product.id)?.forEach {
                if (it.begin!!.isBefore(LocalDateTime.of(2020, 12, 20, 0, 0, 0)) ||
                    it.status == "closed" ||
                    it.status == "canceled"
                ) {
                    return@forEach
                }
                projectMap[it.id] = it.name

                val uTask = zProjectToUniTask(it)
                result.add(uTask)
                result.addAll(zProjectTeamToPerson(it))

                val zTasks = sdk.getProjectTasks(it.id)
                zTasks.forEach { (k, v) ->
                    result.add(zTaskToUniTask(v))
                }
            }
        }
        return result
    }

    override fun update(task: UniTask) {
        if (task.projectName.isNullOrBlank() || task.projectName == "任务池" || task.projectName == "智能报告长期任务集") {
            return
        }
        if (task.type != TaskType.TASK) {
            return
        }
        val zId = task.getIdInConnector(ZENTAO_CONNECTOR_ID)?.substringAfterLast("-")?.toInt()
        val lastSyncTime = task.getLastSyncFromConnector(ZENTAO_CONNECTOR_ID)
        if (zId != null) {
            val zTask = sdk.getTask(zId) ?: return
            val storeLastEditTime = task.lastEditTime
            if (storeLastEditTime != null && storeLastEditTime.isAfter(lastSyncTime)) {
                uniTaskToZTask(task, zTask)
                sdk.saveTask(zTask)
                task.setLastSyncFromConnector(ZENTAO_CONNECTOR_ID, ZonedDateTime.now())
                store.store(task)
            }
        } else {
            // because we first sync from zt to store, so no need to check exists now; if zid is null, treat as not exist.
            val zTask = Task(sdk, -1, task.title)
            uniTaskToZTask(task, zTask)
            val result = sdk.quickCreateStoryAndTask(zTask, true)
            if (result.taskId > 0) {
                val newTask = sdk.getTask(result.taskId)!!
                val newUTask = zTaskToUniTask(newTask)
                newUTask.setIdInConnector(ZENTAO_CONNECTOR_ID, "zTask-${result.taskId}")
                newUTask.setLastSyncFromConnector(ZENTAO_CONNECTOR_ID, ZonedDateTime.now())
                newUTask.lastEditTime
                store.store(newUTask)
            }
        }
    }

    private fun syncFromStore(store: TaskStore) {
        for (task in store.list()) {
            // if product or project is null, dont sync to zentao
            // because in that case zentao may be mass.
            if (task.projectName.isNullOrBlank() || task.projectName == "任务池" || task.projectName == "智能报告长期任务集") {
                continue
            }
            if (task.type != TaskType.TASK) {
                continue
            }
            val zId = task.customProperties["zId"]?.substringAfterLast("-")?.toInt()
            val lastSyncTimeStr = task.customProperties["zentao_last_sync_time"]
            val lastSyncTime = if (!lastSyncTimeStr.isNullOrBlank()) ZonedDateTime.parse(lastSyncTimeStr, DateTimeFormatter.ISO_DATE_TIME) else ZonedDateTime.now()
            if (zId != null) {
                val zTask = sdk.getTask(zId)!!
                val storeLastEditTime = task.lastEditTime
                if (storeLastEditTime != null && storeLastEditTime.isAfter(lastSyncTime)) {
                    uniTaskToZTask(task, zTask)
                    sdk.saveTask(zTask)
                }
            } else {
                // becuase we first sync from zt to store, so no need to check exists now; if zid is null, treat as not exist.
                val zTask = Task(sdk, -1, task.title)
                uniTaskToZTask(task, zTask)
                val result = sdk.quickCreateStoryAndTask(zTask, true)
                if (result.taskId > 0) {
                    task.customProperties["zId"] = "zTask-${result.taskId}"
                    store.store(task)
                }
            }
        }
    }

    private fun syncTaskToZentao(task: pro.devsvc.unitask.core.model.Task) {

    }

    private fun zProjectToUniTask(project: Project): UniTask {
        val uTask = UniTask(project.name, TaskType.PROJECT)
        uTask.estStarted = project.begin?.atZone(ZoneId.systemDefault())
        uTask.deadline = project.end?.atZone(ZoneId.systemDefault())
        // uTask.planId = project.plans.joinToString()
        // uTask.planName =
        // uTask.projectId = project.id.toString()
        uTask.projectName = project.name
        // uTask.productName = productMap[project.products.firstOrNull()]
        // uTask.productId = project.products.joinToString()

        uTask.setIdInConnector(id,"zProject-${project.id}")
        return uTask
    }

    private fun zProjectTeamToPerson(zTask: Project): MutableList<UniTask> {
        val persons = mutableListOf<UniTask>()
        zTask.teamMembers.map {
            val uTask = UniTask(it.realName, TaskType.PERSON)
            uTask.setIdInConnector(id, "zPerson-${it.id}")
            persons.add(uTask)
        }
        return persons
    }

    private fun zTaskToUniTask(zTask: Task): UniTask {
        val uTask = UniTask(zTask.name, TaskType.TASK)
        uTask.estStarted = zTask.estStarted?.atZone(ZoneId.systemDefault())
        uTask.deadline = zTask.deadline?.atZone(ZoneId.systemDefault())
        // uTask.planId = sdk.getProject(zTask.project)?.plans?.joinToString()
        // uTask.projectId = zTask.project?.toString()
        // uTask.productId = zTask.product?.toString()

        val project = sdk.getProject(zTask.project)
        uTask.projectName = project?.name
        uTask.productName = zTask.ztProduct?.name
        if (!zTask.assignedTo.isNullOrBlank()) {
            uTask.assignedUserId = zTask.assignedTo
        }
        if (!zTask.assignedToRealName.isNullOrBlank()) {
            uTask.assignedUserName = zTask.assignedToRealName
        }
        uTask.status = TaskStatus.getByName(zTask.status)
        uTask.priority = TaskPriority.getById(zTask.pri - 1)
        uTask.lastEditTime = zTask.lastEditedDate?.atZone(ZoneId.systemDefault())
        uTask.setIdInConnector(id, "zTask-${zTask.id}")

        uTask.from = "禅道"
        return uTask
    }

    private fun uniTaskToZTask(uTask: UniTask, zTask: Task) {
        zTask.name = uTask.title
        zTask.estStarted = uTask.estStarted?.toLocalDateTime()
        zTask.deadline = uTask.deadline?.toLocalDateTime()
        zTask.status = uTask.status.name
        zTask.assignedToRealName = uTask.assignedUserName ?: ""
        zTask.pri = TaskPriority.values().indexOf(uTask.priority) + 1

        if (uTask.productName != null) {
            zTask.product = productMap.getKey(uTask.productName)
        }
        if (uTask.projectName != null) {
            zTask.project = projectMap.getKey(uTask.projectName)
        }
    }
}