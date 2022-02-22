package pro.devsvc.unitask.connector.notion

import notion.api.v1.NotionClient
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.blocks.BlockType
import notion.api.v1.model.common.OptionColor
import notion.api.v1.model.databases.*
import notion.api.v1.model.databases.query.filter.condition.SelectFilter
import notion.api.v1.model.databases.query.filter.condition.TextFilter
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import notion.api.v1.model.pages.PageProperty
import notion.api.v1.model.pages.PageProperty.RichText
import notion.api.v1.model.pages.PageProperty.RichText.Text
import notion.api.v1.request.databases.CreateDatabaseRequest
import notion.api.v1.request.pages.CreatePageRequest
import org.slf4j.LoggerFactory
import pro.devsvc.unitask.connector.notion.ext.RelationPropertySchema
import pro.devsvc.unitask.connector.notion.ext.SelectPropertySchema
import pro.devsvc.unitask.core.connector.Connector
import pro.devsvc.unitask.core.model.*
import pro.devsvc.unitask.core.store.TaskStoreManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

const val NOTION_CONNECTOR_ID = "notion"
const val DEFAULT_PAGE_TITLE = "UniTask"
const val TASK_DATABASE_TITLE = "Tasks"
const val PROJECT_DATABASE_TITLE = "Projects"
const val PRODUCT_DATABASE_TITLE = "Products"
const val PERSON_DATABASE_TITLE = "Person"
const val PLAN_DATABASE_TITLE = "Plans"


class NotionConnector(
    token: String = System.getProperty("NOTION_TOKEN"),
    private val database: String = "UniTask") : Connector {

    private val log = LoggerFactory.getLogger(javaClass)
    private var formatter: DateTimeFormatter
    private val NOTION_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    init {
        val dtfb = DateTimeFormatterBuilder()
        dtfb.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        formatter = dtfb.toFormatter().withZone(ZoneId.systemDefault())
    }

    private val client = NotionClient(
        token = token,
        httpClient = OkHttp4Client(connectTimeoutMillis = 10_000),
        logger = Slf4jLogger(),
    )

    lateinit var pageId: String
    private var taskDatabaseId: String? = null
    private var productDatabaseId: String? = null
    private var projectDabaseId: String? = null
    private var personDatabaseId: String? = null
    private var planDatabaseId: String? = null

    private val schema = mutableMapOf<String, DatabaseProperty>()
    private val store = TaskStoreManager.store
    override val id: String
        get() = NOTION_CONNECTOR_ID


    override fun start() {
        val pages = client.search("UniTask").results
        if (pages.isNotEmpty()) {
            pageId = pages[0].id
        } else return

        createDatabase()
        val database = client.retrieveDatabase(taskDatabaseId!!)
        schema.putAll(database.properties)
    }

    override fun listProducts(): List<Product> {
        return listOf()
    }

    override fun listPlans(): List<Plan> {
        return listOf()
    }

    override fun listProjects(): List<Project> {
        return listOf()
    }

    override fun listPersons(): List<Person> {
        TODO("Not yet implemented")
    }

    private fun createDatabase() {
        val children = client.retrieveBlockChildren(pageId)
        for (child in children.results) {
            if (child.type == BlockType.ChildDatabase && child.asChildDatabase().childDatabase.title == TASK_DATABASE_TITLE) {
                this.taskDatabaseId = child.asChildDatabase().id
            }
            if (child.type == BlockType.ChildDatabase && child.asChildDatabase().childDatabase.title == PROJECT_DATABASE_TITLE) {
                this.projectDabaseId = child.asChildDatabase().id
            }
            if (child.type == BlockType.ChildDatabase && child.asChildDatabase().childDatabase.title == PRODUCT_DATABASE_TITLE) {
                this.productDatabaseId = child.asChildDatabase().id
            }
            if (child.type == BlockType.ChildDatabase && child.asChildDatabase().childDatabase.title == PERSON_DATABASE_TITLE) {
                this.personDatabaseId = child.asChildDatabase().id
            }
            if (child.type == BlockType.ChildDatabase && child.asChildDatabase().childDatabase.title == PLAN_DATABASE_TITLE) {
                this.planDatabaseId = child.asChildDatabase().id
            }
        }
        if (personDatabaseId == null) {
            createPersonDatabase()
        }
        if (productDatabaseId == null) {
            createProductDatabase()
        }
        if (planDatabaseId == null) {
            createPlanDatabase()
        }
        if (projectDabaseId == null) {
            createProjectDatabase()
        }
        if (taskDatabaseId == null) {
            createTaskDatabase()
        }
    }

    private fun createPersonDatabase() {
        this.personDatabaseId = client.createDatabase(
            CreateDatabaseRequest(
                parent = DatabaseParent.page(pageId),
                title = listOf(DatabaseProperty.RichText(text = DatabaseProperty.RichText.Text(content = PERSON_DATABASE_TITLE))),
                properties = mapOf(
                    "Name" to TitlePropertySchema()
                )
            )
        ).id
    }

    private fun createPlanDatabase() {
        this.planDatabaseId = client.createDatabase(
            CreateDatabaseRequest(
                parent = DatabaseParent.page(pageId),
                title = listOf(DatabaseProperty.RichText(text = DatabaseProperty.RichText.Text(content = PLAN_DATABASE_TITLE))),
                properties = mapOf(
                    "Name" to TitlePropertySchema()
                )
            )
        ).id
    }

    private fun createProjectDatabase() {
        this.projectDabaseId = client.createDatabase(
            CreateDatabaseRequest(
                parent = DatabaseParent.page(pageId),
                title = listOf(DatabaseProperty.RichText(text = DatabaseProperty.RichText.Text(content = PROJECT_DATABASE_TITLE))),
                properties = mapOf(
                    "Title" to TitlePropertySchema(),
                    "Date" to DatePropertySchema()
                )
            )
        ).id
    }

    private fun createProductDatabase() {
        this.productDatabaseId = client.createDatabase(
            CreateDatabaseRequest(
                parent = DatabaseParent.page(pageId),
                title = listOf(DatabaseProperty.RichText(text = DatabaseProperty.RichText.Text(content = PRODUCT_DATABASE_TITLE))),
                properties = mapOf(
                    "Name" to TitlePropertySchema()
                )
            )
        ).id
    }

    private fun createTaskDatabase() {
        if (personDatabaseId == null) {
            log.error("no person database...")
        }
        if (productDatabaseId == null) {
            log.error("no product database...")
        }
        if (planDatabaseId == null) {
            log.error("no plan database...")
        }
        if (projectDabaseId == null) {
            log.error("no project database...")
        }

        this.taskDatabaseId = client.createDatabase(
            CreateDatabaseRequest(
                parent = DatabaseParent.page(pageId),
                title = listOf(DatabaseProperty.RichText(text = DatabaseProperty.RichText.Text(content = TASK_DATABASE_TITLE))),
                properties = mapOf(
                    "Title" to TitlePropertySchema(),
                    "AssignedTo" to RelationPropertySchema(RelationPropertySchema.Relation(personDatabaseId!!)),
                    "Date" to DatePropertySchema(),
                    "Module" to RichTextPropertySchema(),
                    "Product" to RelationPropertySchema(RelationPropertySchema.Relation(productDatabaseId!!)),
                    "Plan" to RelationPropertySchema(RelationPropertySchema.Relation(planDatabaseId!!)),
                    "Project" to RelationPropertySchema(RelationPropertySchema.Relation(projectDabaseId!!)),
                    "Status" to SelectPropertySchema(listOf(
                        SelectOptionSchema(TaskStatus.WAIT.name, OptionColor.Yellow),
                        SelectOptionSchema(TaskStatus.DOING.name, OptionColor.Green),
                        SelectOptionSchema(TaskStatus.DONE.name, OptionColor.Blue),
                        SelectOptionSchema(TaskStatus.CLOSED.name, OptionColor.Gray)
                    )),
                    "Priority" to SelectPropertySchema(listOf(
                        SelectOptionSchema(TaskPriority.NORMAL.cname, OptionColor.Blue),
                        SelectOptionSchema(TaskPriority.IMPORTANT.cname, OptionColor.Yellow),
                        SelectOptionSchema(TaskPriority.UNIMPORTANT.cname, OptionColor.Gray),
                        SelectOptionSchema(TaskPriority.URGENT.cname, OptionColor.Red)
                    )),
                )
            )
        ).id
    }

    override fun listTasks(): List<Task> {
        val result = mutableListOf<Task>()
        val pages = client.queryDatabase(taskDatabaseId!!).results
        for (page in pages) {
            val title = page.properties["Name"]?.title?.firstOrNull()?.plainText
            if (title != null) {
                val task = Task(title)
                pageToUniTask(page, task)
                result.add(task)
            }
        }
        return result
    }

    private fun updateTask(task: Task) {
        log.info("sync task $task to notion...")
        val properties = taskToNotionPageProperties(task)
        if (properties.isEmpty()) {
            return
        }

        doUpdateModel(task, properties, taskDatabaseId!!)
    }

    override fun update(model: Model) {
        when (model) {
            is Task -> {
                updateTask(model)
                return
            }
        }

        val databaseId = getDatabaseId(model) ?: return
        log.info("sync model $model to notion...")
        val properties = taskToNotionPageProperties(model)
        if (properties.isEmpty()) {
            return
        }

        doUpdateModel(model, properties, databaseId)
    }

    private fun doUpdateModel(
        model: Model,
        properties: MutableMap<String, PageProperty>,
        databaseId: String
    ) {
        val notionId = model.getIdInConnector(NOTION_CONNECTOR_ID)
        if (notionId != null) {
            val existingPage = client.retrievePage(notionId)
            val existingLastEditTime = parseDateTime(existingPage.lastEditedTime)!!
            if (properties.isNotEmpty()) {
                val lastEditTime = model.lastEditTime!!
                if (lastEditTime.isAfter(existingLastEditTime)) {
                    try {
                        val result = client.updatePageProperties(notionId, properties)
                        log.debug("update result $result")

                        model.setLastEditOfConnector(NOTION_CONNECTOR_ID, parseDateTime(result.lastEditedTime)!!)
                        store.store(model)
                    } catch (e: NotionAPIError) {
                        if (e.message.startsWith("Could not find page with ID")) {
                            log.warn("deleting model $model")
                            //store.deleteTask(task)
                        }
                    } catch (e: Throwable) {
                        log.error("error update model: $model", e)
                    }
                } else {
                    log.debug("model $model last edit time is before or equals to notion's last edit time, skip...")
                }
            }
        } else {
            val page = client.createPage(
                CreatePageRequest(
                    PageParent.database(databaseId),
                    properties
                )
            )
            model.setIdInConnector(NOTION_CONNECTOR_ID, page.id)
            model.setLastEditOfConnector(NOTION_CONNECTOR_ID, parseDateTime(page.lastEditedTime)!!)
            store.store(model)
        }
    }

    private fun pageToUniTask(page: Page, task: Task) {
        val typeName = page.properties["Type"]?.select?.name
        if (typeName != null && typeName.isNotBlank()) {
            task.type = TaskType.valueOf(typeName.toUpperCase())
        }

        task.setIdInConnector(NOTION_CONNECTOR_ID, page.id)
        val pageStatus = page.properties["Status"]?.select?.name
        if (pageStatus != null) {
            task.status = TaskStatus.getByName(pageStatus)
        }
        task.createTime = parseDateTime(page.createdTime)
        task.estStarted = parseDateTime(page.properties["Due Date"]?.date?.start)
        task.deadline = parseDateTime(page.properties["Due Date"]?.date?.end)
        task.assignedUserName = page.properties["AssignedTo"]?.select?.name
        task.lastEditTime = parseDateTime(page.lastEditedTime)!!
    }

    private fun pageToUniProject(page: Page, project: Project) {
        project.estStarted = parseDateTime(page.properties["Due Date"]?.date?.start)
        project.deadline = parseDateTime(page.properties["Due Date"]?.date?.end)
    }

    private fun getDatabaseId(model: Model): String? {
        return when (model) {
            is Product -> productDatabaseId
            is Project -> projectDabaseId
            is Plan -> planDatabaseId
            is Person -> personDatabaseId
            else -> null
        }
    }

    private fun taskToNotionPageProperties(model: Model): MutableMap<String, PageProperty> {
        val properties = mutableMapOf<String, PageProperty>()
        // val databaseId = when (model) {
        //     is Project -> productDatabaseId
        //     is Project -> projectDabaseId
        //     is Plan -> planDatabaseId
        //     else -> null
        // }
        // if (databaseId == null) {
        //     log.error("no database for model: $model")
        //     return properties
        // }
        // val database = client.retrieveDatabase(databaseId)
        when (model) {
            is Product -> {
                properties["Name"] = PageProperty(title = listOf(
                    RichText(text = Text(model.name))
                ))
            }
            is Person -> {
                properties["Name"] = PageProperty(title = listOf(
                    RichText(text = Text(model.name))
                ))
            }
            is Plan -> {
                properties["Title"] = PageProperty(title = listOf(
                    RichText(text = Text(model.title))
                ))
            }
            is Project -> {
                properties["Title"] = PageProperty(title = listOf(
                    RichText(text = Text(model.title))
                ))
                if (model.estStarted == null) {
                    model.estStarted = model.deadline
                }
                properties["Date"] = PageProperty(date = PageProperty.Date(
                    start = model.estStarted?.format(NOTION_FMT),
                    end = model.deadline?.format(NOTION_FMT),
                ))
            }
        }
        return properties
    }

    private fun taskToNotionPageProperties(task: Task): MutableMap<String, PageProperty> {
        val properties = mutableMapOf<String, PageProperty>()
        if (task.title.isNotBlank()) {
            properties["Title"] = PageProperty(title = listOf(
                RichText(text = Text(task.title))
            ))
        }

        if (task.estStarted != null || task.deadline != null) {
            if (task.estStarted == null) {
                task.estStarted = task.deadline
            }
            properties["Date"] = PageProperty(date = PageProperty.Date(
                start = task.estStarted?.format(NOTION_FMT),
                end = task.deadline?.format(NOTION_FMT),
            ))
        }
        safeCreatePageProperty(properties,"Status", task.status.name)
        safeCreatePageProperty(properties,"AssignedTo", task.assignedUserName)
        safeCreatePageProperty(properties,"ProjectName", task.projectName)
        safeCreatePageProperty(properties,"ProductName", task.productName)
        safeCreatePageProperty(properties,"Type", task.type.name)
        safeCreatePageProperty(properties,"Priority", task.priority.cname)

        if (task.type == TaskType.TASK && task.projectName != null) {
            // if project is already in notion, construct the relation
            // if not, have to do this in next sync. TODO: better solution?
            val projectInNotion = client.queryDatabase(projectDabaseId!!, NCompoundFilter(
                and = listOf(
                    NPropertyFilter("Title", title = TextFilter(task.projectName)),
                )
            ))
            if (projectInNotion.results.isNotEmpty()) {
                val projectInNotionPage = projectInNotion.results[0]
                properties["Project"] = PageProperty(relation = listOf(
                    PageProperty.PageReference(projectInNotionPage.id)
                ))
            }
        }
        if (task.type == TaskType.TASK && task.productName != null) {
            // if project is already in notion, construct the relation
            // if not, have to do this in next sync. TODO: better solution?
            val projectInNotion = client.queryDatabase(productDatabaseId!!, NCompoundFilter(
                and = listOf(
                    NPropertyFilter("Name", title = TextFilter(task.productName)),
                )
            ))
            if (projectInNotion.results.isNotEmpty()) {
                val projectInNotionPage = projectInNotion.results[0]
                properties["Product"] = PageProperty(relation = listOf(
                    PageProperty.PageReference(projectInNotionPage.id)
                ))
            }
        }
        return properties
    }

    private fun safeCreatePageProperty(properties: MutableMap<String, PageProperty>, name: String, value: String?) {
        if (!value.isNullOrBlank()) {
            val propertyDef = schema[name]
            var pageProperty: PageProperty? = null
            if (propertyDef != null) {
                if (propertyDef.select != null) {
                    val option = findOptionInSchema(name, value)
                    if (option != null) {
                        pageProperty = PageProperty(select = option)
                    } else {
                        pageProperty = PageProperty(select = DatabaseProperty.Select.Option(name = value))
                    }
                } else if (propertyDef.richText != null) {
                    pageProperty = PageProperty(richText = listOf(RichText(text = Text(value))))
                }
            }
            if (pageProperty != null) {
                properties[name] = pageProperty
            }
        }
    }

    private fun findOptionInSchema(propertyName: String, optionValue: String): DatabaseProperty.Select.Option? {
        val property = schema[propertyName]
        if (property != null) {
            return property.select?.options?.find { it.name.equals(optionValue, ignoreCase = true) }
        }
        return null
    }

    private fun parseDateTime(str: String?): ZonedDateTime? {
        if (str == null) {
            return null
        }
        return ZonedDateTime.parse(str, formatter)
    }

    fun listDatabase(): Databases {
        return client.listDatabases()
    }
}

