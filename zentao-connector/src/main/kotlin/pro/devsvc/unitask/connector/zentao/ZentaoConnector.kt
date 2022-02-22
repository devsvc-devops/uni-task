package pro.devsvc.unitask.connector.zentao

import cn.hutool.core.map.BiMap
import pro.devsvc.unitask.core.connector.Connector
import pro.devsvc.unitask.core.model.*
import pro.devsvc.unitask.core.store.TaskStore
import pro.devsvc.unitask.core.store.TaskStoreManager
import pro.devsvc.zentao.sdk.ZentaoSDK
import pro.devsvc.zentao.sdk.getUserList
import pro.devsvc.zentao.sdk.quickCreateStoryAndTask
import pro.devsvc.zentao.sdk.saveTask
import pro.devsvc.zentao.sdk.usermodel.Plan
import pro.devsvc.zentao.sdk.usermodel.Project
import pro.devsvc.zentao.sdk.usermodel.Task
import pro.devsvc.zentao.sdk.usermodel.User
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import pro.devsvc.unitask.core.model.Task as UniTask
import pro.devsvc.unitask.core.model.Product as UniProduct
import pro.devsvc.unitask.core.model.Project as UniProject
import pro.devsvc.unitask.core.model.Plan as UniPlan

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

    override fun listProducts(): List<UniProduct> {
        val result = mutableListOf<UniProduct>()
        val products = sdk.getAllProducts()
        for (product in products) {
            productMap[product.id] = product.name
            result.add(zProductToUniProduct(product))
        }
        return result
    }

    override fun listPlans(): List<UniPlan> {
        val result = mutableListOf<UniPlan>()
        val products = sdk.getAllProducts()
        for (product in products) {
            productMap[product.id] = product.name
            val plans = sdk.getPlansOfProduct(product.id)
            for (plan in plans) {
                result.add(zPlanToUniPlan(plan))
            }
        }
        return result
    }

    override fun listProjects(): List<pro.devsvc.unitask.core.model.Project> {
        val result = mutableListOf<UniProject>()
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
                result.add(zProjectToUniProject(it))
            }
        }
        return result
    }

    override fun listPersons(): List<Person> {
        val result = mutableListOf<Person>()
        val users = sdk.getUserList()
        for (user in users) {
            result.add(zUserToUniPerson(user))
        }
        return result
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

                val zTasks = sdk.getProjectTasks(it.id)
                zTasks.forEach { (k, v) ->
                    result.add(zTaskToUniTask(v))
                }
            }
        }
        return result
    }

    override fun update(model: Model) {
        when (model) {
            is UniTask -> updateTask(model)
        }
    }

    private fun updateTask(task: UniTask) {
        if (task.projectName.isNullOrBlank() || task.projectName == "任务池") {
            return
        }
        if (task.type != TaskType.TASK) {
            return
        }
        val zId = task.getIdInConnector(ZENTAO_CONNECTOR_ID)?.substringAfterLast("-")?.toInt()
        val lastSyncTime = task.getLastEditOfConnector(ZENTAO_CONNECTOR_ID)
        if (zId != null) {
            val zTask = sdk.getTask(zId) ?: return
            val storeLastEditTime = task.lastEditTime
            if (storeLastEditTime != null && storeLastEditTime.isAfter(lastSyncTime)) {
                uniTaskToZTask(task, zTask)
                sdk.saveTask(zTask)
                val newTask = sdk.getTask(zTask.id)
                task.setLastEditOfConnector(ZENTAO_CONNECTOR_ID, newTask!!.lastEditedDate!!.atZone(ZoneId.systemDefault()))
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
                newUTask.setIdInConnector(ZENTAO_CONNECTOR_ID, newTask.id.toString())
                newUTask.setLastEditOfConnector(ZENTAO_CONNECTOR_ID, newUTask.lastEditTime)
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

    private fun zProductToUniProduct(product: pro.devsvc.zentao.sdk.usermodel.Product): UniProduct {
        val uProduct = UniProduct(product.name)
        uProduct.name = product.name
        uProduct.setIdInConnector(ZENTAO_CONNECTOR_ID,product.id.toString())
        return uProduct
    }

    private fun zProjectToUniProject(project: Project): UniProject {
        val uProject = UniProject(project.name)
        uProject.title = project.name
        uProject.estStarted = project.begin!!.atZone(ZoneId.systemDefault())
        uProject.deadline = project.end!!.atZone(ZoneId.systemDefault())
        uProject.setIdInConnector(ZENTAO_CONNECTOR_ID, project.id.toString())
        return uProject
    }

    private fun zPlanToUniPlan(plan: Plan): UniPlan {
        val estStart = plan.begin?.atZone(ZoneId.systemDefault())
        val deadline = plan.end?.atZone(ZoneId.systemDefault())
        val uPlan = UniPlan(plan.title, estStart, deadline)
        uPlan.title = plan.title
        uPlan.setIdInConnector(ZENTAO_CONNECTOR_ID, plan.id.toString())
        return uPlan
    }

    private fun zUserToUniPerson(user: User): Person {
        val person = Person(user.realName)
        person.setIdInConnector(ZENTAO_CONNECTOR_ID, user.id.toString())
        return person
    }

    private fun zProjectToUniTask(project: Project): UniTask {
        val uTask = UniTask(project.name, TaskType.PROJECT)
        uTask.estStarted = project.begin?.atZone(ZoneId.systemDefault())
        uTask.deadline = project.end?.atZone(ZoneId.systemDefault())
        uTask.projectName = project.name

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
        uTask.lastEditTime = zTask.lastEditedDate?.atZone(ZoneId.systemDefault()) ?: zTask.openedDate!!.atZone(ZoneId.systemDefault())
        uTask.setIdInConnector(id, zTask.id.toString())

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